"""
UniAssist — reviews/serializers.py

Serializers for the Review & Rating Module (Phase 6):

  • SubmitReviewSerializer   — validates POST /api/reviews/submit/ body
  • ReviewSerializer         — read representation of a Review record
  • ReviewWithBookingSerializer — extended read representation that also
                                  exposes booking summary (for my-reviews)
"""

from rest_framework import serializers

from reviews.models import Review


# ─── Submit (input) serializer ────────────────────────────────────────────────

class SubmitReviewSerializer(serializers.Serializer):
    """
    Validates the request body for POST /api/reviews/submit/.

    Required fields : booking_id, rating, review_text
    Optional fields : knowledge_rating, teaching_rating,
                      communication_rating, punctuality_rating
    All rating fields must be integers between 1 and 5.
    """

    booking_id            = serializers.IntegerField(min_value=1)
    rating                = serializers.IntegerField(min_value=1, max_value=5)
    review_text           = serializers.CharField(
        min_length=1,
        max_length=5000,
        error_messages={
            'blank': 'Review text cannot be empty.',
            'min_length': 'Review text cannot be empty.',
        },
    )

    # Optional sub-ratings
    knowledge_rating      = serializers.IntegerField(
        min_value=1, max_value=5, required=False, allow_null=True,
    )
    teaching_rating       = serializers.IntegerField(
        min_value=1, max_value=5, required=False, allow_null=True,
    )
    communication_rating  = serializers.IntegerField(
        min_value=1, max_value=5, required=False, allow_null=True,
    )
    punctuality_rating    = serializers.IntegerField(
        min_value=1, max_value=5, required=False, allow_null=True,
    )


# ─── Read serializer ──────────────────────────────────────────────────────────

class ReviewSerializer(serializers.ModelSerializer):
    """
    Full read representation of a Review.
    Exposes student name and tutor name as flat fields.
    """

    student_name = serializers.SerializerMethodField()
    tutor_name   = serializers.SerializerMethodField()

    class Meta:
        model  = Review
        fields = [
            'id',
            'booking_id',
            'student_name',
            'tutor_name',
            'rating',
            'review_text',
            'knowledge_rating',
            'teaching_rating',
            'communication_rating',
            'punctuality_rating',
            'created_at',
        ]
        read_only_fields = fields

    def get_student_name(self, obj):
        return obj.student.user.full_name

    def get_tutor_name(self, obj):
        return obj.tutor.user.full_name


# ─── Read serializer WITH booking summary ─────────────────────────────────────

class ReviewWithBookingSerializer(serializers.ModelSerializer):
    """
    Extended read representation that also exposes a booking summary.
    Used by GET /api/reviews/my-reviews/ so the student can see which
    booking each review belongs to without a second API call.
    """

    student_name         = serializers.SerializerMethodField()
    tutor_name           = serializers.SerializerMethodField()
    booking_subject      = serializers.SerializerMethodField()
    booking_date         = serializers.SerializerMethodField()
    booking_status       = serializers.SerializerMethodField()

    class Meta:
        model  = Review
        fields = [
            'id',
            'booking_id',
            'student_name',
            'tutor_name',
            'booking_subject',
            'booking_date',
            'booking_status',
            'rating',
            'review_text',
            'knowledge_rating',
            'teaching_rating',
            'communication_rating',
            'punctuality_rating',
            'created_at',
        ]
        read_only_fields = fields

    def get_student_name(self, obj):
        return obj.student.user.full_name

    def get_tutor_name(self, obj):
        return obj.tutor.user.full_name

    def get_booking_subject(self, obj):
        return obj.booking.subject_or_skill

    def get_booking_date(self, obj):
        return obj.booking.proposed_date

    def get_booking_status(self, obj):
        return obj.booking.booking_status
