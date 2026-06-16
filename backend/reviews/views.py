"""
UniAssist — reviews/views.py

Implements 4 Review & Rating Module API endpoints (Phase 6):

  1. POST /api/reviews/submit/
       Auth: Student JWT required
       → Validate booking ownership and completion status.
       → Prevent duplicate reviews (OneToOne booking constraint).
       → Save Review; post_save signal auto-updates tutor punctuality_score.

  2. GET /api/reviews/tutor/<tutor_id>/
       Auth: None (public)
       → All reviews for a tutor, newest first, paginated.
       → Includes live-computed average_rating and total_reviews in response.

  3. GET /api/reviews/my-reviews/
       Auth: Student JWT required
       → All reviews submitted by the logged-in student, paginated.
       → Each result includes booking summary (subject, date, status).

  4. GET /api/reviews/check/<booking_id>/
       Auth: Student JWT required
       → Returns has_reviewed: true/false and the review object if it exists.
       → Used by Android to show/hide the Review button.

Response envelope (API_RULES.md):
  Success → { "success": true,  "message": "...", "data": { } }
  Error   → { "success": false, "message": "...", "errors": { } }
"""

import logging

from django.db.models import Avg
from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.views import APIView

from accounts.models import Student, Tutor
from booking.models import Booking
from reviews.models import Review
from reviews.serializers import (
    SubmitReviewSerializer,
    ReviewSerializer,
    ReviewWithBookingSerializer,
)
from uniassist.utils import success_response, error_response

logger = logging.getLogger(__name__)


# ─── Custom Pagination ────────────────────────────────────────────────────────

class ReviewPagination(PageNumberPagination):
    """Standard paginator for review list endpoints."""
    page_size             = 20
    page_size_query_param = 'page_size'
    max_page_size         = 100
    page_query_param      = 'page'


# ─── 1. Submit Review API ─────────────────────────────────────────────────────

class SubmitReviewView(APIView):
    """
    POST /api/reviews/submit/
    Auth: Student JWT required.

    Body:
      {
        "booking_id":           <int>,      ← required
        "rating":               <1-5>,      ← required
        "review_text":          "<str>",    ← required
        "knowledge_rating":     <1-5>,      ← optional
        "teaching_rating":      <1-5>,      ← optional
        "communication_rating": <1-5>,      ← optional
        "punctuality_rating":   <1-5>       ← optional
      }

    Validation rules:
      - Caller must be a student.
      - Booking must belong to this student.
      - Booking must have booking_status = 'completed'.
      - Duplicate reviews are rejected (OneToOne).
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user

        # Role gate
        if not user.is_student:
            return error_response(
                message='Only students can submit reviews.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Validate input body
        serializer = SubmitReviewSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))
            if isinstance(first_error, list):
                first_error = first_error[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data       = serializer.validated_data
        booking_id = data['booking_id']

        # Fetch booking
        try:
            booking = Booking.objects.select_related(
                'student__user', 'tutor__user'
            ).get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Ownership check
        if booking.student != student:
            return error_response(
                message='You can only review bookings that belong to you.',
                status=status.HTTP_403_FORBIDDEN,
            )

        # Booking must be completed before a review is allowed
        if booking.booking_status != Booking.BookingStatus.COMPLETED:
            return error_response(
                message=(
                    'You can only review a completed session. '
                    f'Current booking status: {booking.booking_status}.'
                ),
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Duplicate review prevention
        if hasattr(booking, 'review'):
            return error_response(
                message='You have already submitted a review for this booking.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Create review
        review = Review.objects.create(
            booking              = booking,
            student              = student,
            tutor                = booking.tutor,
            rating               = data['rating'],
            review_text          = data['review_text'],
            knowledge_rating     = data.get('knowledge_rating'),
            teaching_rating      = data.get('teaching_rating'),
            communication_rating = data.get('communication_rating'),
            punctuality_rating   = data.get('punctuality_rating'),
        )
        # post_save signal fires here — auto-updates tutor.punctuality_score

        logger.info(
            'Review #%s submitted by student %s for tutor %s (booking #%s). '
            'Rating: %s/5.',
            review.id,
            student.user.email,
            booking.tutor.user.email,
            booking.id,
            review.rating,
        )

        return success_response(
            message='Review submitted successfully.',
            data=ReviewSerializer(review).data,
            status=status.HTTP_201_CREATED,
        )


# ─── 2. Get Tutor Reviews API (public) ───────────────────────────────────────

class TutorReviewsView(APIView):
    """
    GET /api/reviews/tutor/<tutor_id>/
    Auth: None — public endpoint.

    Query params: page, page_size
    Returns paginated list of reviews for a tutor with live average_rating.
    """
    permission_classes = [AllowAny]

    def get(self, request, tutor_id):
        try:
            tutor = Tutor.objects.select_related('user').get(id=tutor_id)
        except Tutor.DoesNotExist:
            return error_response(
                message='Tutor not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = (
            Review.objects
            .filter(tutor=tutor)
            .select_related('student__user', 'tutor__user', 'booking')
            .order_by('-created_at')
        )

        # Compute aggregate stats before paginating
        stats         = qs.aggregate(avg=Avg('rating'))
        total_reviews = qs.count()
        average_rating = round(stats['avg'], 2) if stats['avg'] else 0.0

        paginator  = ReviewPagination()
        page       = paginator.paginate_queryset(qs, request)
        serializer = ReviewSerializer(page, many=True)

        return success_response(
            message='Reviews retrieved successfully.',
            data={
                'tutor_id':      tutor.id,
                'tutor_name':    tutor.user.full_name,
                'average_rating': average_rating,
                'total_reviews': total_reviews,
                'count':         paginator.page.paginator.count,
                'next':          paginator.get_next_link(),
                'previous':      paginator.get_previous_link(),
                'results':       serializer.data,
            },
        )


# ─── 3. My Reviews API ────────────────────────────────────────────────────────

class MyReviewsView(APIView):
    """
    GET /api/reviews/my-reviews/
    Auth: Student JWT required.

    Returns paginated list of all reviews the logged-in student has submitted.
    Each record includes booking summary (subject, date, status).
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user

        if not user.is_student:
            return error_response(
                message='Only students can access their review history.',
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            student = user.student_profile
        except Student.DoesNotExist:
            return error_response(
                message='Student profile not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = (
            Review.objects
            .filter(student=student)
            .select_related('student__user', 'tutor__user', 'booking')
            .order_by('-created_at')
        )

        paginator  = ReviewPagination()
        page       = paginator.paginate_queryset(qs, request)
        serializer = ReviewWithBookingSerializer(page, many=True)

        return success_response(
            message='Your reviews retrieved successfully.',
            data={
                'count':    paginator.page.paginator.count,
                'next':     paginator.get_next_link(),
                'previous': paginator.get_previous_link(),
                'results':  serializer.data,
            },
        )


# ─── 4. Check If Reviewed API ─────────────────────────────────────────────────

class CheckReviewedView(APIView):
    """
    GET /api/reviews/check/<booking_id>/
    Auth: Student JWT required.

    Returns:
      {
        "has_reviewed": true | false,
        "review": { review details } | null
      }

    Used by Android to decide whether to show or hide the Review button.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, booking_id):
        user = request.user

        if not user.is_student:
            return error_response(
                message='Only students can check review status.',
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
            booking = Booking.objects.select_related(
                'student__user', 'tutor__user'
            ).get(id=booking_id)
        except Booking.DoesNotExist:
            return error_response(
                message='Booking not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Ownership check — student can only check their own bookings
        if booking.student != student:
            return error_response(
                message='You are not authorized to check this booking.',
                status=status.HTTP_403_FORBIDDEN,
            )

        has_reviewed = hasattr(booking, 'review')
        review_data  = ReviewSerializer(booking.review).data if has_reviewed else None

        return success_response(
            message='Review status retrieved successfully.',
            data={
                'has_reviewed': has_reviewed,
                'review':       review_data,
            },
        )
