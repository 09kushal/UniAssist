"""
UniAssist — booking/serializers.py

Serializers for Phase 4 Booking System Module:
  • StudentBriefSerializer      — Helper to serialize student details.
  • TutorBriefSerializer        — Helper to serialize tutor details.
  • BookingSerializer           — Full serialized booking output.
  • BookingRequestSerializer    — Input validator for POST /api/booking/request/
  • BookingRespondSerializer    — Input validator for PATCH /api/booking/<id>/respond/
"""

from django.utils import timezone
from rest_framework import serializers

from accounts.models import Student, Tutor, TutorAvailability
from booking.models import Booking


class StudentBriefSerializer(serializers.ModelSerializer):
    """Brief representation of a Student for nested booking responses."""
    full_name = serializers.CharField(source='user.full_name', read_only=True)
    email = serializers.EmailField(source='user.email', read_only=True)
    profile_photo_url = serializers.SerializerMethodField()

    class Meta:
        model = Student
        fields = ['id', 'full_name', 'email', 'profile_photo_url']

    def get_profile_photo_url(self, obj):
        request = self.context.get('request')
        if obj.profile_photo and request:
            return request.build_absolute_uri(obj.profile_photo.url)
        return None


class TutorBriefSerializer(serializers.ModelSerializer):
    """Brief representation of a Tutor for nested booking responses."""
    full_name = serializers.CharField(source='user.full_name', read_only=True)
    email = serializers.EmailField(source='user.email', read_only=True)
    profile_photo_url = serializers.SerializerMethodField()

    class Meta:
        model = Tutor
        fields = [
            'id',
            'full_name',
            'email',
            'domain',
            'pricing_per_session',
            'profile_photo_url',
            'is_verified',
        ]

    def get_profile_photo_url(self, obj):
        request = self.context.get('request')
        if obj.profile_photo and request:
            return request.build_absolute_uri(obj.profile_photo.url)
        return None


class BookingAvailabilitySerializer(serializers.ModelSerializer):
    """Brief representation of selected availability slot."""
    class Meta:
        model = TutorAvailability
        fields = ['id', 'day_of_week', 'start_time', 'end_time']


class BookingSerializer(serializers.ModelSerializer):
    """
    Serializer to represent a Booking instance fully.
    Conforms to the success response data envelope structure.
    """
    student = StudentBriefSerializer(read_only=True)
    tutor = TutorBriefSerializer(read_only=True)
    selected_slot = BookingAvailabilitySerializer(read_only=True)

    class Meta:
        model = Booking
        fields = [
            'id',
            'student',
            'tutor',
            'selected_slot',
            'subject_or_skill',
            'proposed_date',
            'proposed_start_time',
            'proposed_end_time',
            'message',
            'booking_status',
            'rejection_reason',
            'officially_scheduled',
            'created_at',
        ]


class BookingRequestSerializer(serializers.Serializer):
    """
    Validates booking request payload from a student.
    Fields: tutor_id, subject_or_skill, proposed_date, proposed_start_time, proposed_end_time, message
    """
    tutor_id = serializers.IntegerField(required=True)
    subject_or_skill = serializers.CharField(max_length=255, required=True)
    proposed_date = serializers.DateField(required=True)
    proposed_start_time = serializers.TimeField(required=True)
    proposed_end_time = serializers.TimeField(required=True)
    message = serializers.CharField(max_length=2000, required=False, allow_blank=True, default='')

    def validate_tutor_id(self, value):
        """Ensure tutor exists and is verified."""
        try:
            tutor = Tutor.objects.get(id=value)
        except Tutor.DoesNotExist:
            raise serializers.ValidationError('Tutor not found.')

        if not tutor.is_verified:
            raise serializers.ValidationError('Tutor is not verified yet.')

        if tutor.is_suspended:
            raise serializers.ValidationError('Tutor is currently suspended.')

        return value

    def validate_proposed_date(self, value):
        """Ensure proposed date is not in the past."""
        today = timezone.localdate()
        if value < today:
            raise serializers.ValidationError('Proposed date cannot be in the past.')
        return value

    def validate(self, attrs):
        """Cross-field validations (e.g. start time before end time)."""
        start = attrs.get('proposed_start_time')
        end = attrs.get('proposed_end_time')

        if start and end and start >= end:
            raise serializers.ValidationError({
                'proposed_start_time': 'Proposed start time must be earlier than end time.'
            })

        # Also validate if today is selected, start time must not be in the past.
        # However, let's keep it simple or strictly enforce:
        proposed_date = attrs.get('proposed_date')
        if proposed_date == timezone.localdate():
            current_time = timezone.localtime().time()
            if start < current_time:
                raise serializers.ValidationError({
                    'proposed_start_time': 'Proposed start time cannot be in the past for today.'
                })

        return attrs


class BookingRespondSerializer(serializers.Serializer):
    """
    Validates tutor response action (accept/reject).
    Fields: action, rejection_reason
    """
    ACTION_CHOICES = [
        ('accept', 'Accept'),
        ('reject', 'Reject'),
    ]
    action = serializers.ChoiceField(choices=ACTION_CHOICES, required=True)
    rejection_reason = serializers.CharField(
        max_length=1000,
        required=False,
        allow_blank=True,
        default='',
    )

    def validate(self, attrs):
        action = attrs.get('action')
        rejection_reason = attrs.get('rejection_reason', '')

        if action == 'reject' and not rejection_reason.strip():
            raise serializers.ValidationError({
                'rejection_reason': 'Rejection reason is required when rejecting a booking.'
            })

        return attrs
