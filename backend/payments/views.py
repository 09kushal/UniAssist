"""
UniAssist — payments/views.py

Implements 6 Payment Module API endpoints (Phase 5):

  1. POST /api/payments/initiate/
       Auth: Student JWT required
       → Validates booking (must be accepted), generates eSewa form params,
         creates a pending Payment record, returns eSewa redirect data.

  2. POST /api/payments/callback/
       Auth: None (eSewa calls this directly)
       → Verifies eSewa HMAC SHA256 signature.
       → On success: payment_status = completed, officially_scheduled = TRUE.
       → On failure: payment_status = failed.

  3. POST /api/payments/failed/
       Auth: None (eSewa redirects on failure)
       → Simple failure acknowledgement endpoint (no state change needed
         here — callback already handles it).

  4. GET /api/payments/<booking_id>/status/
       Auth: Student or Tutor JWT required (involved parties only)
       → Returns full payment record for a booking.

  5. POST /api/payments/admin/payout/<booking_id>/release/
       Auth: Admin JWT required
       → Calculates payout (30% commission, 70% tutor, fines from tutor 70%),
         creates Payout record, marks as released.

  6. GET /api/payments/tutor/payouts/
       Auth: Tutor JWT required
       → Paginated list of all payouts for the logged-in tutor.

Response envelope (API_RULES.md):
  Success → { "success": true,  "message": "...", "data": { } }
  Error   → { "success": false, "message": "...", "errors": { } }

eSewa integration (Nepal sandbox):
  Merchant code : EPAYTEST
  API URL       : https://rc-epay.esewa.com.np/api/epay/main/v2/form
  Signature     : HMAC-SHA256 over "total_amount,transaction_uuid,product_code"
  Secret key    : 8gBm/:&EnhH.1/q  (eSewa sandbox secret)
  Success URL   : http://127.0.0.1:8000/api/payments/callback/
  Failure URL   : http://127.0.0.1:8000/api/payments/failed/
"""

import hashlib
import hmac
import base64
import logging
import uuid
import json

from django.conf import settings
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.pagination import PageNumberPagination
from rest_framework.views import APIView

from accounts.models import Student, Tutor
from booking.models import Booking
from payments.models import Payment, Payout
from payments.serializers import (
    InitiatePaymentSerializer,
    PaymentSerializer,
    PayoutSerializer,
)
from uniassist.utils import success_response, error_response

logger = logging.getLogger(__name__)

# ─── eSewa Constants ──────────────────────────────────────────────────────────

ESEWA_PRODUCT_CODE   = getattr(settings, 'ESEWA_PRODUCT_CODE', 'EPAYTEST')
ESEWA_SECRET_KEY     = '8gBm/:&EnhH.1/q'          # eSewa sandbox HMAC key
ESEWA_PAYMENT_URL    = 'https://rc-epay.esewa.com.np/api/epay/main/v2/form'
ESEWA_VERIFY_URL     = 'https://rc-epay.esewa.com.np/api/epay/transaction/status/'
ESEWA_SUCCESS_URL    = 'http://127.0.0.1:8000/api/payments/callback/'
ESEWA_FAILURE_URL    = 'http://127.0.0.1:8000/api/payments/failed/'

# ─── Commission Constants (settings.py already defines these) ─────────────────

PLATFORM_COMMISSION_RATE = getattr(settings, 'PLATFORM_COMMISSION_RATE', 0.30)
TUTOR_BASE_SHARE_RATE    = getattr(settings, 'TUTOR_BASE_SHARE_RATE', 0.70)
FINE_RATE_PER_OFFENCE    = 0.20   # 20% of tutor's 70% per fine offence


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _generate_esewa_signature(total_amount: str, transaction_uuid: str, product_code: str) -> str:
    """
    Generate HMAC-SHA256 signature for eSewa v2 API.
    Message format: "total_amount,transaction_uuid,product_code"
    Returns Base64-encoded signature string.
    """
    message = f"total_amount={total_amount},transaction_uuid={transaction_uuid},product_code={product_code}"
    signature = hmac.new(
        ESEWA_SECRET_KEY.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256,
    ).digest()
    return base64.b64encode(signature).decode('utf-8')


def _verify_esewa_signature(data: dict) -> bool:
    """
    Verify the HMAC-SHA256 signature returned by eSewa in the callback.
    eSewa sends: transaction_code, status, total_amount, transaction_uuid,
                 product_code, signed_field_names, signature
    """
    try:
        signed_field_names = data.get('signed_field_names', '')
        fields = [f.strip() for f in signed_field_names.split(',')]
        message_parts = [f"{field}={data.get(field, '')}" for field in fields]
        message = ','.join(message_parts)

        expected_signature = hmac.new(
            ESEWA_SECRET_KEY.encode('utf-8'),
            message.encode('utf-8'),
            hashlib.sha256,
        ).digest()
        expected_b64 = base64.b64encode(expected_signature).decode('utf-8')

        return hmac.compare_digest(expected_b64, data.get('signature', ''))
    except Exception as exc:
        logger.error('eSewa signature verification error: %s', exc)
        return False


# ─── Custom Pagination ────────────────────────────────────────────────────────

class PayoutPagination(PageNumberPagination):
    """Paginator for payout history views (page / page_size params)."""
    page_size             = 20
    page_size_query_param = 'page_size'
    max_page_size         = 100
    page_query_param      = 'page'


# ─── 1. Initiate Payment API ─────────────────────────────────────────────────

class InitiatePaymentView(APIView):
    """
    POST /api/payments/initiate/
    Auth: Student JWT required.

    Body: { "booking_id": <int> }

    Rules:
      - Booking must belong to the logged-in student.
      - Booking must have booking_status = 'accepted'.
      - Must not already have a completed payment.
      - Creates a pending Payment record (or reuses existing pending one).
      - Returns eSewa form data for the Android app to submit.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        if not user.is_student:
            return error_response(
                message='Only students can initiate payments.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = InitiatePaymentSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))
            if isinstance(first_error, list):
                first_error = first_error[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        booking_id = serializer.validated_data['booking_id']

        try:
            booking = Booking.objects.select_related(
                'student__user', 'tutor__user'
            ).get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Security: booking must belong to this student
        if booking.student != student:
            return error_response(
                message='You are not authorized to pay for this booking.',
                status=status.HTTP_403_FORBIDDEN,
            )

        # Only accepted bookings can be paid
        if booking.booking_status != Booking.BookingStatus.ACCEPTED:
            return error_response(
                message=(
                    f'Payment can only be initiated for accepted bookings. '
                    f'Current status: {booking.booking_status}.'
                ),
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Check for existing completed payment
        existing_payment = Payment.objects.filter(booking=booking).first()
        if existing_payment and existing_payment.payment_status == Payment.PaymentStatus.COMPLETED:
            return error_response(
                message='This booking has already been paid successfully.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Amount = tutor's pricing_per_session (per requirements)
        amount = booking.tutor.pricing_per_session

        # Create or reuse pending Payment record
        if existing_payment and existing_payment.payment_status == Payment.PaymentStatus.PENDING:
            payment = existing_payment
            payment.amount = amount
            payment.save(update_fields=['amount'])
        else:
            # Create fresh pending payment (previous attempt may have failed)
            payment = Payment.objects.create(
                booking=booking,
                amount=amount,
                payment_status=Payment.PaymentStatus.PENDING,
            )

        # Generate unique transaction ID using payment PK for traceability
        transaction_uuid = f"UA-{payment.id}-{uuid.uuid4().hex[:8].upper()}"

        # Build eSewa signature
        signature = _generate_esewa_signature(
            total_amount=str(amount),
            transaction_uuid=transaction_uuid,
            product_code=ESEWA_PRODUCT_CODE,
        )

        # Return eSewa form data — Android submits this as a POST form
        esewa_form_data = {
            'amount':           str(amount),
            'tax_amount':       '0',
            'total_amount':     str(amount),
            'transaction_uuid': transaction_uuid,
            'product_code':     ESEWA_PRODUCT_CODE,
            'product_service_charge': '0',
            'product_delivery_charge': '0',
            'success_url':      ESEWA_SUCCESS_URL,
            'failure_url':      ESEWA_FAILURE_URL,
            'signed_field_names': 'total_amount,transaction_uuid,product_code',
            'signature':        signature,
        }

        return success_response(
            message='eSewa payment parameters generated. Submit form to eSewa.',
            data={
                'payment_id':       payment.id,
                'booking_id':       booking.id,
                'amount':           str(amount),
                'currency':         'NPR',
                'esewa_payment_url': ESEWA_PAYMENT_URL,
                'esewa_form_data':  esewa_form_data,
            },
            status=status.HTTP_200_OK,
        )


# ─── 2. eSewa Payment Callback API ───────────────────────────────────────────

class PaymentCallbackView(APIView):
    """
    POST /api/payments/callback/
    Auth: None — eSewa calls this endpoint directly after payment.

    eSewa sends back:
      - transaction_code, status, total_amount, transaction_uuid,
        product_code, signed_field_names, signature  (Base64 encoded)
      - OR a Base64-encoded JSON in a single 'data' field (v2 format)

    Actions on success:
      1. Verify HMAC-SHA256 signature.
      2. Set payment_status = completed.
      3. Store eSewa transaction reference.
      4. Set booking.officially_scheduled = TRUE.

    Actions on failure:
      1. Set payment_status = failed.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        try:
            raw_data = request.data

            # eSewa v2 sends a Base64-encoded JSON under the key 'data'
            if 'data' in raw_data and isinstance(raw_data.get('data'), str):
                try:
                    decoded = base64.b64decode(raw_data['data']).decode('utf-8')
                    callback_data = json.loads(decoded)
                except Exception:
                    callback_data = raw_data
            else:
                callback_data = raw_data

            esewa_status      = callback_data.get('status', '')
            transaction_uuid  = callback_data.get('transaction_uuid', '')
            transaction_code  = callback_data.get('transaction_code', '')

            # Parse payment ID from transaction_uuid (format: "UA-<id>-<hex>")
            payment = self._resolve_payment(transaction_uuid)
            if payment is None:
                return error_response(
                    message='Payment record not found for this transaction.',
                    status=status.HTTP_404_NOT_FOUND,
                )

            if esewa_status == 'COMPLETE':
                # Verify eSewa signature before trusting
                if not _verify_esewa_signature(callback_data):
                    logger.warning(
                        'eSewa signature verification FAILED for transaction %s',
                        transaction_uuid,
                    )
                    payment.payment_status = Payment.PaymentStatus.FAILED
                    payment.save(update_fields=['payment_status'])
                    return error_response(
                        message='Payment verification failed. Signature mismatch.',
                        status=status.HTTP_400_BAD_REQUEST,
                    )

                # Mark payment complete
                payment.payment_status = Payment.PaymentStatus.COMPLETED
                payment.esewa_ref_id   = transaction_code or transaction_uuid
                payment.paid_at        = timezone.now()
                payment.save(update_fields=['payment_status', 'esewa_ref_id', 'paid_at'])

                # Officially schedule the booking (per API_RULES.md step 4)
                booking = payment.booking
                booking.officially_scheduled = True
                booking.save(update_fields=['officially_scheduled'])

                logger.info(
                    'Payment #%s completed. Booking #%s officially scheduled.',
                    payment.id, booking.id,
                )

                return success_response(
                    message='Payment verified and confirmed. Session is now officially scheduled.',
                    data=PaymentSerializer(payment).data,
                )

            else:
                # Any non-COMPLETE status from eSewa = failed
                payment.payment_status = Payment.PaymentStatus.FAILED
                payment.save(update_fields=['payment_status'])

                logger.info(
                    'Payment #%s failed. eSewa status: %s', payment.id, esewa_status
                )

                return error_response(
                    message=f'Payment failed. eSewa status: {esewa_status}.',
                    status=status.HTTP_200_OK,  # Return 200 so eSewa doesn't retry
                )

        except Exception as exc:
            logger.error('Unexpected error in PaymentCallbackView: %s', exc)
            return error_response(
                message='An error occurred while processing the payment callback.',
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

    @staticmethod
    def _resolve_payment(transaction_uuid: str):
        """
        Parse payment ID from transaction_uuid and return the Payment object.
        Format: "UA-<payment_id>-<random_hex>"
        """
        try:
            parts = transaction_uuid.split('-')
            # Expected: ['UA', '<id>', '<hex>']
            if len(parts) >= 2 and parts[0] == 'UA':
                payment_id = int(parts[1])
                return Payment.objects.select_related(
                    'booking__student__user',
                    'booking__tutor__user',
                ).get(id=payment_id)
        except (ValueError, Payment.DoesNotExist, Exception):
            pass
        return None


# ─── 3. Payment Failure Endpoint ─────────────────────────────────────────────

class PaymentFailedView(APIView):
    """
    POST /api/payments/failed/
    Auth: None — eSewa redirects here on payment failure.

    eSewa redirects to failure URL on cancellation or payment error.
    The callback endpoint already handles state; this is just acknowledgement.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        return error_response(
            message='Payment was unsuccessful or cancelled. Please try again.',
            status=status.HTTP_200_OK,
        )

    def get(self, request):
        return error_response(
            message='Payment was unsuccessful or cancelled. Please try again.',
            status=status.HTTP_200_OK,
        )


# ─── 4. Payment Status API ───────────────────────────────────────────────────

class PaymentStatusView(APIView):
    """
    GET /api/payments/<booking_id>/status/
    Auth: Student or Tutor JWT required.

    Rules:
      - Only the student or tutor involved in the booking can view payment.
      - Admin can view any payment status.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, booking_id):
        try:
            booking = Booking.objects.select_related(
                'student__user', 'tutor__user'
            ).get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        user = request.user

        # Access control: only involved student, tutor, or admin
        is_involved = False
        if user.is_admin_user:
            is_involved = True
        elif user.is_student:
            try:
                is_involved = (booking.student == user.student_profile)
            except Student.DoesNotExist:
                pass
        elif user.is_tutor:
            try:
                is_involved = (booking.tutor == user.tutor_profile)
            except Tutor.DoesNotExist:
                pass

        if not is_involved:
            return error_response(
                message='You are not authorized to view the payment status of this booking.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            payment = Payment.objects.get(booking=booking)
        except Payment.DoesNotExist:
            return error_response(
                message='No payment record found for this booking.',
                status=status.HTTP_404_NOT_FOUND,
            )

        return success_response(
            message='Payment details retrieved successfully.',
            data=PaymentSerializer(payment).data,
        )


# ─── 5. Admin Payout Release API ─────────────────────────────────────────────

class AdminPayoutReleaseView(APIView):
    """
    POST /api/payments/admin/payout/<booking_id>/release/
    Auth: Admin JWT required.

    Body (optional fine fields):
      {
        "fine_percentage": 0.20,   // e.g. 0.20 = 20% fine on tutor's 70%
        "fine_reason":     "No-show"
      }

    Calculates final payout using the exact formula from DATABASE_SCHEMA.md:
      commission_amount  = session_price * 0.30
      tutor_base_share   = session_price * 0.70
      fine_amount        = tutor_base_share * fine_percentage
      tutor_final_payout = tutor_base_share - fine_amount
      student_refund     = fine_amount

    Creates a Payout record and marks it as released immediately.

    Returns breakdown:
      {
        "session_price":       1000,
        "platform_commission": 300,
        "tutor_share":         700,
        "total_fines":         0,
        "net_payout":          700
      }
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, booking_id):
        user = request.user
        if not user.is_admin_user:
            return error_response(
                message='Only admins can release payouts.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            booking = Booking.objects.select_related(
                'student__user', 'tutor__user'
            ).get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Payment must be completed before payout can be released
        try:
            payment = Payment.objects.get(booking=booking)
        except Payment.DoesNotExist:
            return error_response(
                message='No payment record found for this booking. Cannot release payout.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        if payment.payment_status != Payment.PaymentStatus.COMPLETED:
            return error_response(
                message=(
                    f'Payment is not completed (current: {payment.payment_status}). '
                    f'Cannot release payout.'
                ),
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Prevent duplicate payout release
        if Payout.objects.filter(booking=booking).exists():
            existing_payout = Payout.objects.get(booking=booking)
            if existing_payout.payout_status == Payout.PayoutStatus.RELEASED:
                return error_response(
                    message='Payout has already been released for this booking.',
                    status=status.HTTP_400_BAD_REQUEST,
                )

        # ── Payout Calculation (EXACT formula from DATABASE_SCHEMA.md) ───────
        from decimal import Decimal
        session_price      = payment.amount
        commission_amount  = round(session_price * Decimal(str(PLATFORM_COMMISSION_RATE)), 2)
        tutor_base_share   = round(session_price * Decimal(str(TUTOR_BASE_SHARE_RATE)), 2)

        # Optional fine fields from request body
        fine_percentage_input = request.data.get('fine_percentage', 0)
        fine_reason           = request.data.get('fine_reason', '') or ''

        try:
            fine_percentage = float(fine_percentage_input)
            if fine_percentage < 0 or fine_percentage > 1:
                return error_response(
                    message='fine_percentage must be a decimal between 0 and 1 (e.g. 0.20 for 20%).',
                    status=status.HTTP_400_BAD_REQUEST,
                )
        except (ValueError, TypeError):
            return error_response(
                message='fine_percentage must be a valid decimal number.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        fine_amount        = round(tutor_base_share * Decimal(str(fine_percentage)), 2)
        student_refund     = fine_amount   # equals fine_amount per schema
        tutor_final_payout = round(tutor_base_share - fine_amount, 2)

        payout_status = (
            Payout.PayoutStatus.FINED
            if fine_amount > 0
            else Payout.PayoutStatus.RELEASED
        )

        # Create Payout record and mark as released
        payout = Payout.objects.create(
            tutor               = booking.tutor,
            booking             = booking,
            total_paid          = session_price,
            commission_amount   = commission_amount,
            tutor_base_share    = tutor_base_share,
            fine_percentage     = fine_percentage,
            fine_amount         = fine_amount,
            fine_reason         = fine_reason or None,
            fine_imposed_by     = user if fine_amount > 0 else None,
            student_refund      = student_refund,
            tutor_final_payout  = tutor_final_payout,
            payout_status       = Payout.PayoutStatus.RELEASED,
            released_by         = user,
            released_at         = timezone.now(),
        )

        logger.info(
            'Admin %s released payout #%s for booking #%s. '
            'Net payout: NPR %s.',
            user.email, payout.id, booking.id, tutor_final_payout,
        )

        return success_response(
            message='Payout released successfully.',
            data={
                'payout_id':           payout.id,
                'booking_id':          booking.id,
                'tutor_name':          booking.tutor.user.full_name,
                'session_price':       float(session_price),
                'platform_commission': float(commission_amount),
                'tutor_share':         float(tutor_base_share),
                'fine_percentage':     float(fine_percentage),
                'total_fines':         float(fine_amount),
                'student_refund':      float(student_refund),
                'net_payout':          float(tutor_final_payout),
                'payout_status':       payout.payout_status,
                'released_at':         payout.released_at,
            },
            status=status.HTTP_201_CREATED,
        )


# ─── 6. Tutor Payout History API ─────────────────────────────────────────────

class TutorPayoutHistoryView(APIView):
    """
    GET /api/payments/tutor/payouts/
    Auth: Tutor JWT required.

    Returns all payouts for the logged-in tutor, paginated.
    Query params: page, page_size
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        if not user.is_tutor:
            return error_response(
                message='Only tutors can view payout history.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            tutor = user.tutor_profile
        except Tutor.DoesNotExist:
            return error_response(
                message='Tutor profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = Payout.objects.filter(tutor=tutor).select_related(
            'tutor__user', 'booking', 'released_by', 'fine_imposed_by'
        ).order_by('-id')

        paginator = PayoutPagination()
        page      = paginator.paginate_queryset(qs, request)
        serializer = PayoutSerializer(page, many=True)

        return success_response(
            message='Payout history retrieved successfully.',
            data={
                'count':    paginator.page.paginator.count,
                'next':     paginator.get_next_link(),
                'previous': paginator.get_previous_link(),
                'results':  serializer.data,
            },
        )
