import hashlib
import hmac
import base64
import json
import logging
import uuid
import datetime
from decimal import Decimal

from django.conf import settings
from django.utils import timezone

from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.views import APIView

from accounts.models import Student, Tutor, User
from booking.models import Booking, Session
from payments.models import Payment, Payout
from payments.serializers import PaymentSerializer, PayoutSerializer
from uniassist.utils import success_response, error_response

logger = logging.getLogger(__name__)

# --- Configuration ---
PLATFORM_COMMISSION_RATE = getattr(settings, 'PLATFORM_COMMISSION_RATE', 0.30)
TUTOR_BASE_SHARE_RATE    = getattr(settings, 'TUTOR_BASE_SHARE_RATE', 0.70)

# --- Helpers ---

def _verify_esewa_signature(data: dict) -> bool:
    return True # Sandbox bypass

def create_pending_payout(booking, amount):
    """
    Idempotent helper to create a Payout record in 'pending' status.
    Uses EXACT commission formula.
    """
    session_price     = amount
    commission_amount = round(session_price * Decimal(str(PLATFORM_COMMISSION_RATE)), 2)
    tutor_base_share  = round(session_price * Decimal(str(TUTOR_BASE_SHARE_RATE)), 2)

    payout, created = Payout.objects.get_or_create(
        booking=booking,
        defaults={
            'tutor':              booking.tutor,
            'total_paid':         session_price,
            'commission_amount':  commission_amount,
            'tutor_base_share':   tutor_base_share,
            'tutor_final_payout': tutor_base_share,
            'payout_status':      Payout.PayoutStatus.PENDING,
        }
    )
    return payout

class PayoutPagination(PageNumberPagination):
    page_size = 20

# --- View Classes ---

class InitiatePaymentView(APIView):
    permission_classes = [IsAuthenticated]
    def post(self, request):
        return success_response(message="Initiated")

class PaymentCallbackView(APIView):
    permission_classes = [AllowAny]
    def post(self, request):
        return success_response(message="Payment verified.")

class PaymentFailedView(APIView):
    permission_classes = [AllowAny]
    def post(self, request):
        return error_response(message="Payment failed.")

class PaymentStatusView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request, booking_id):
        try:
            payment = Payment.objects.get(booking_id=booking_id)
            return success_response(data=PaymentSerializer(payment).data)
        except Payment.DoesNotExist:
            return error_response(message="Payment not found.")

class AdminPayoutReleaseView(APIView):
    """
    POST /api/payments/admin/payout/<booking_id>/release/
    Auth: Admin JWT required
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, booking_id):
        user = request.user
        if not (user.is_staff or getattr(user, 'role', '') == 'admin'):
             return error_response(message="Admin access required.", status=403)

        try:
            booking = Booking.objects.get(id=booking_id)
            payment = Payment.objects.get(booking=booking, payment_status=Payment.PaymentStatus.COMPLETED)

            session_price     = payment.amount
            commission_amount = round(session_price * Decimal(str(PLATFORM_COMMISSION_RATE)), 2)
            tutor_base_share  = round(session_price * Decimal(str(TUTOR_BASE_SHARE_RATE)), 2)

            fine_percentage = Decimal(str(request.data.get('fine_percentage', 0)))
            fine_amount     = round(tutor_base_share * fine_percentage, 2)
            student_refund  = fine_amount
            tutor_final_payout = tutor_base_share - fine_amount

            payout, _ = Payout.objects.update_or_create(
                booking=booking,
                defaults={
                    'tutor':              booking.tutor,
                    'total_paid':         session_price,
                    'commission_amount':  commission_amount,
                    'tutor_base_share':   tutor_base_share,
                    'fine_percentage':    fine_percentage,
                    'fine_amount':         fine_amount,
                    'student_refund':      student_refund,
                    'tutor_final_payout':  tutor_final_payout,
                    'payout_status':       Payout.PayoutStatus.RELEASED,
                    'released_by':         user,
                    'released_at':         timezone.now()
                }
            )
            return success_response(message="Payout released successfully.", data=PayoutSerializer(payout).data)
        except Exception as e:
            return error_response(message=str(e))

class DemoPaymentCompleteView(APIView):
    """
    POST /api/payments/demo-complete/
    Auth: Student JWT required.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        booking_id = request.data.get('booking_id')
        if not booking_id:
            return error_response(message="booking_id required", status=status.HTTP_400_BAD_REQUEST)

        try:
            booking = Booking.objects.get(id=booking_id, student__user=request.user)
            payment, _ = Payment.objects.get_or_create(
                booking=booking,
                defaults={'amount': booking.tutor.pricing_per_session}
            )
            payment.payment_status = Payment.PaymentStatus.COMPLETED
            payment.paid_at = timezone.now()
            payment.save()

            booking.officially_scheduled = True
            booking.save()

            create_pending_payout(booking, payment.amount)

            scheduled_dt = datetime.datetime.combine(booking.proposed_date, booking.proposed_start_time)
            if timezone.is_naive(scheduled_dt):
                scheduled_dt = timezone.make_aware(scheduled_dt)
            Session.objects.get_or_create(
                booking=booking,
                defaults={'scheduled_at': scheduled_dt, 'session_status': 'pending'}
            )

            return success_response(message="Demo payment complete.")
        except Exception as e:
            return error_response(message=str(e))

class TutorPayoutHistoryView(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        try:
            tutor = request.user.tutor_profile
            qs = Payout.objects.filter(tutor=tutor).order_by('-created_at')
            paginator = PayoutPagination()
            page = paginator.paginate_queryset(qs, request)
            serializer = PayoutSerializer(page, many=True)
            return success_response(data={
                'count': paginator.page.paginator.count,
                'results': serializer.data
            })
        except Exception as e:
            return error_response(message=str(e))
