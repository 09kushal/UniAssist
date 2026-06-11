"""
UniAssist — booking/views.py

Implements 6 core Booking System API endpoints (Phase 4):

  1. POST  /api/booking/request/         → BookingRequestView (Student only)
  2. PATCH /api/booking/<id>/respond/    → BookingRespondView (Tutor only)
  3. GET   /api/booking/<id>/status/     → BookingStatusView (Student or Tutor involved)
  4. GET   /api/booking/my-bookings/     → StudentBookingHistoryView (Student only, paginated)
  5. GET   /api/booking/my-requests/     → TutorBookingHistoryView (Tutor only, paginated)
  6. PATCH /api/booking/<id>/cancel/      → CancelBookingView (Student only, pending only)

Response envelope:
  Success → { "success": true,  "message": "...", "data": { ... } }
  Error   → { "success": false, "message": "...", "errors": { ... } }
"""

import logging
from rest_framework import status
from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination

from accounts.models import Student, Tutor, TutorAvailability
from booking.models import Booking
from booking.serializers import (
    BookingSerializer,
    BookingRequestSerializer,
    BookingRespondSerializer,
)
from uniassist.utils import success_response, error_response

logger = logging.getLogger(__name__)


# ─── Custom Pagination ────────────────────────────────────────────────────────

class BookingPagination(PageNumberPagination):
    """
    Paginator for booking list/history views.
    Supports ?page=<n>&page_size=<n> query params.
    """
    page_size = 20
    page_size_query_param = 'page_size'
    max_page_size = 100
    page_query_param = 'page'


# ─── 1. Booking Request API ───────────────────────────────────────────────────

class BookingRequestView(APIView):
    """
    POST /api/booking/request/
    Auth: Student JWT required.

    Fields: tutor_id, subject_or_skill, proposed_date, proposed_start_time, proposed_end_time, message
    - booking_status must start as pending
    - Only student users can book
    - Tutor must be verified
    - Returns full booking details
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        if not user.is_student:
            return error_response(
                message='Only students can create booking requests.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        if student.is_suspended:
            return error_response(
                message='Your account is suspended. You cannot make bookings.',
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = BookingRequestSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))
            if isinstance(first_error, list):
                first_error = first_error[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data
        tutor_id = data['tutor_id']
        tutor = Tutor.objects.get(id=tutor_id)

        # Look up if tutor has a matching availability slot on that day of the week
        day_abbr = data['proposed_date'].strftime('%a')  # e.g., 'Mon', 'Tue'
        selected_slot = TutorAvailability.objects.filter(
            tutor=tutor,
            day_of_week=day_abbr,
            start_time__lte=data['proposed_start_time'],
            end_time__gte=data['proposed_end_time'],
        ).first()

        booking = Booking.objects.create(
            student=student,
            tutor=tutor,
            selected_slot=selected_slot,
            subject_or_skill=data['subject_or_skill'],
            proposed_date=data['proposed_date'],
            proposed_start_time=data['proposed_start_time'],
            proposed_end_time=data['proposed_end_time'],
            message=data.get('message', ''),
            booking_status=Booking.BookingStatus.PENDING,
            officially_scheduled=False,  # Phase 4 hard constraint
        )

        resp_serializer = BookingSerializer(booking, context={'request': request})
        return success_response(
            message='Booking request submitted successfully.',
            data=resp_serializer.data,
            status=status.HTTP_201_CREATED,
        )


# ─── 2. Tutor Accept/Reject Booking API ───────────────────────────────────────

class BookingRespondView(APIView):
    """
    PATCH /api/booking/<id>/respond/
    Auth: Tutor JWT required.

    Fields: action (accept / reject), rejection_reason (optional)
    - Only the tutor who received the booking can respond.
    - Only pending bookings can be responded to.
    - booking_status -> accepted or rejected.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, booking_id):
        user = request.user
        if not user.is_tutor:
            return error_response(
                message='Only tutors can respond to bookings.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            tutor = user.tutor_profile
        except Tutor.DoesNotExist:
            return error_response(
                message='Tutor profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        try:
            booking = Booking.objects.get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        if booking.tutor != tutor:
            return error_response(
                message='You are not authorized to respond to this booking request.',
                status=status.HTTP_403_FORBIDDEN,
            )

        if booking.booking_status != Booking.BookingStatus.PENDING:
            return error_response(
                message=f'Cannot respond to a booking that is already {booking.booking_status}.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = BookingRespondSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))
            if isinstance(first_error, list):
                first_error = first_error[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        action = serializer.validated_data['action']
        rejection_reason = serializer.validated_data.get('rejection_reason', '')

        if action == 'accept':
            booking.booking_status = Booking.BookingStatus.ACCEPTED
            booking.rejection_reason = ''
        elif action == 'reject':
            booking.booking_status = Booking.BookingStatus.REJECTED
            booking.rejection_reason = rejection_reason

        booking.save()

        resp_serializer = BookingSerializer(booking, context={'request': request})
        return success_response(
            message=f'Booking successfully {booking.booking_status}.',
            data=resp_serializer.data,
        )


# ─── 3. Booking Status API ────────────────────────────────────────────────────

class BookingStatusView(APIView):
    """
    GET /api/booking/<id>/status/
    Auth: Student or Tutor JWT required.

    - Only the student or tutor involved can view.
    - Returns full booking details including status.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, booking_id):
        try:
            booking = Booking.objects.get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        user = request.user

        # Access check: user must be the student or tutor involved
        is_involved = False
        if user.is_student:
            try:
                is_involved = (booking.student == user.student_profile)
            except Student.DoesNotExist:
                pass
        elif user.is_tutor:
            try:
                is_involved = (booking.tutor == user.tutor_profile)
            except Tutor.DoesNotExist:
                pass
        elif user.is_admin_user:
            is_involved = True  # Admins can view any status

        if not is_involved:
            return error_response(
                message='You are not authorized to view the status of this booking.',
                status=status.HTTP_403_FORBIDDEN,
            )

        resp_serializer = BookingSerializer(booking, context={'request': request})
        return success_response(
            message='Booking details retrieved successfully.',
            data=resp_serializer.data,
        )


# ─── 4. Student Booking History API ───────────────────────────────────────────

class StudentBookingHistoryView(APIView):
    """
    GET /api/booking/my-bookings/
    Auth: Student JWT required.

    Query params:
      status    - optional filter (pending, accepted, rejected, cancelled, completed)
      page      - page number (default: 1)
      page_size - items per page (default: 20)
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        if not user.is_student:
            return error_response(
                message='Only students can access booking history.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = Booking.objects.filter(student=student).select_related(
            'student__user', 'tutor__user', 'selected_slot'
        ).order_by('-created_at')

        # Filter by status
        status_filter = request.query_params.get('status')
        if status_filter:
            status_filter = status_filter.lower().strip()
            # Validate status choice
            if status_filter not in [choice[0] for choice in Booking.BookingStatus.choices]:
                return error_response(
                    message=f"Invalid status filter. Allowed values: {', '.join([choice[0] for choice in Booking.BookingStatus.choices])}",
                    status=status.HTTP_400_BAD_REQUEST,
                )
            qs = qs.filter(booking_status=status_filter)

        # Pagination
        paginator = BookingPagination()
        page = paginator.paginate_queryset(qs, request)
        serializer = BookingSerializer(page, many=True, context={'request': request})

        return success_response(
            message='Booking history retrieved successfully.',
            data={
                'count': paginator.page.paginator.count,
                'next': paginator.get_next_link(),
                'previous': paginator.get_previous_link(),
                'results': serializer.data,
            },
        )


# ─── 5. Tutor Booking History API ─────────────────────────────────────────────

class TutorBookingHistoryView(APIView):
    """
    GET /api/booking/my-requests/
    Auth: Tutor JWT required.

    Query params:
      status    - optional filter (pending, accepted, rejected, cancelled, completed)
      page      - page number (default: 1)
      page_size - items per page (default: 20)
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        if not user.is_tutor:
            return error_response(
                message='Only tutors can view booking requests.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            tutor = user.tutor_profile
        except Tutor.DoesNotExist:
            return error_response(
                message='Tutor profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = Booking.objects.filter(tutor=tutor).select_related(
            'student__user', 'tutor__user', 'selected_slot'
        ).order_by('-created_at')

        # Filter by status
        status_filter = request.query_params.get('status')
        if status_filter:
            status_filter = status_filter.lower().strip()
            if status_filter not in [choice[0] for choice in Booking.BookingStatus.choices]:
                return error_response(
                    message=f"Invalid status filter. Allowed values: {', '.join([choice[0] for choice in Booking.BookingStatus.choices])}",
                    status=status.HTTP_400_BAD_REQUEST,
                )
            qs = qs.filter(booking_status=status_filter)

        # Pagination
        paginator = BookingPagination()
        page = paginator.paginate_queryset(qs, request)
        serializer = BookingSerializer(page, many=True, context={'request': request})

        return success_response(
            message='Booking requests retrieved successfully.',
            data={
                'count': paginator.page.paginator.count,
                'next': paginator.get_next_link(),
                'previous': paginator.get_previous_link(),
                'results': serializer.data,
            },
        )


# ─── 6. Cancel Booking API ────────────────────────────────────────────────────

class CancelBookingView(APIView):
    """
    PATCH /api/booking/<id>/cancel/
    Auth: Student JWT required.

    - Only the student who created the booking can cancel it.
    - Only allowed if booking_status is pending.
    - booking_status -> cancelled.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, booking_id):
        user = request.user
        if not user.is_student:
            return error_response(
                message='Only students can cancel bookings.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        try:
            booking = Booking.objects.get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        if booking.student != student:
            return error_response(
                message='You are not authorized to cancel this booking.',
                status=status.HTTP_403_FORBIDDEN,
            )

        if booking.booking_status != Booking.BookingStatus.PENDING:
            return error_response(
                message=f'Cannot cancel a booking that is already {booking.booking_status}.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        booking.booking_status = Booking.BookingStatus.CANCELLED
        booking.save()

        resp_serializer = BookingSerializer(booking, context={'request': request})
        return success_response(
            message='Booking cancelled successfully.',
            data=resp_serializer.data,
        )
