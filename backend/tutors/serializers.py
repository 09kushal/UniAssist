"""
UniAssist — tutors/serializers.py

Serializers for the Tutor Discovery Module (Phase 3):

  • TutorProfileSetupSerializer      — PATCH /api/tutors/profile/setup/
  • SubjectSerializer                — Subject read/write
  • SkillSerializer                  — Skill read/write
  • TutorAvailabilitySerializer      — Availability slot read/write
  • TutorListSerializer              — Public tutor listing (GET /api/tutors/list/)
  • TutorDetailSerializer            — Full tutor profile (GET /api/tutors/<id>/profile/)
  • MyProfileSerializer              — Own profile for logged-in tutor

All file upload validation enforces JPG/PNG only and 5 MB max size.
No models defined here — all imported from accounts app.
"""

import os
from rest_framework import serializers
from accounts.models import Tutor, Subject, Skill, TutorAvailability


# ─── Constants ────────────────────────────────────────────────────────────────

ALLOWED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png']
MAX_PHOTO_SIZE_MB = 5
MAX_PHOTO_SIZE_BYTES = MAX_PHOTO_SIZE_MB * 1024 * 1024   # 5 MB in bytes


# ─── Shared image validator ────────────────────────────────────────────────────

def validate_profile_photo(image):
    """
    Validates that an uploaded image is JPG or PNG and does not exceed 5 MB.
    Called by TutorProfileSetupSerializer.validate_profile_photo().
    """
    ext = os.path.splitext(image.name)[1].lower()
    if ext not in ALLOWED_IMAGE_EXTENSIONS:
        raise serializers.ValidationError(
            'Only JPG and PNG images are accepted.'
        )
    if image.size > MAX_PHOTO_SIZE_BYTES:
        raise serializers.ValidationError(
            f'Image size must not exceed {MAX_PHOTO_SIZE_MB} MB.'
        )
    return image


# ─── Tutor Profile Setup Serializer ───────────────────────────────────────────

class TutorProfileSetupSerializer(serializers.Serializer):
    """
    PATCH /api/tutors/profile/setup/
    All fields are optional (partial update).
    """
    bio                 = serializers.CharField(
        required=False,
        allow_blank=True,
        max_length=2000,
    )
    pricing_per_session = serializers.DecimalField(
        required=False,
        max_digits=10,
        decimal_places=2,
        min_value=0,
    )
    profile_photo       = serializers.ImageField(required=False, allow_null=True)

    def validate_profile_photo(self, image):
        return validate_profile_photo(image)

    def validate_pricing_per_session(self, value):
        if value < 0:
            raise serializers.ValidationError('Pricing must be a positive value.')
        return value


# ─── Subject Serializer ───────────────────────────────────────────────────────

class SubjectSerializer(serializers.ModelSerializer):
    """Read/write serializer for Subject (academic domain)."""

    class Meta:
        model  = Subject
        fields = ['id', 'name']

    def validate_name(self, value):
        return value.strip()


# ─── Skill Serializer ─────────────────────────────────────────────────────────

class SkillSerializer(serializers.ModelSerializer):
    """Read/write serializer for Skill (skill-based domain)."""

    class Meta:
        model  = Skill
        fields = ['id', 'name']

    def validate_name(self, value):
        return value.strip()


# ─── Add Subject Input Serializer ─────────────────────────────────────────────

class AddSubjectSerializer(serializers.Serializer):
    """Validates subject name for POST /api/tutors/subjects/add/"""
    name = serializers.CharField(max_length=255, required=True)

    def validate_name(self, value):
        return value.strip()


# ─── Add Skill Input Serializer ───────────────────────────────────────────────

class AddSkillSerializer(serializers.Serializer):
    """Validates skill name for POST /api/tutors/skills/add/"""
    name = serializers.CharField(max_length=255, required=True)

    def validate_name(self, value):
        return value.strip()


# ─── TutorAvailability Serializer ─────────────────────────────────────────────

class TutorAvailabilitySerializer(serializers.ModelSerializer):
    """Read/write serializer for TutorAvailability slots."""

    class Meta:
        model  = TutorAvailability
        fields = ['id', 'day_of_week', 'start_time', 'end_time']

    def validate(self, attrs):
        start = attrs.get('start_time')
        end   = attrs.get('end_time')
        if start and end and start >= end:
            raise serializers.ValidationError(
                'start_time must be earlier than end_time.'
            )
        return attrs


# ─── Add Availability Slot Input Serializer ───────────────────────────────────

class AddAvailabilitySerializer(serializers.Serializer):
    """
    Validates availability slot data for POST /api/tutors/availability/add/
    Overlap checking is done in the view.
    """

    DAY_CHOICES = [
        ('Mon', 'Monday'),
        ('Tue', 'Tuesday'),
        ('Wed', 'Wednesday'),
        ('Thu', 'Thursday'),
        ('Fri', 'Friday'),
        ('Sat', 'Saturday'),
        ('Sun', 'Sunday'),
    ]

    day_of_week = serializers.ChoiceField(choices=DAY_CHOICES, required=True)
    start_time  = serializers.TimeField(required=True)
    end_time    = serializers.TimeField(required=True)

    def validate(self, attrs):
        start = attrs.get('start_time')
        end   = attrs.get('end_time')
        if start and end and start >= end:
            raise serializers.ValidationError(
                'start_time must be earlier than end_time.'
            )
        return attrs


# ─── Tutor List / Public Profile Serializers ──────────────────────────────────

class PublicSubjectSerializer(serializers.ModelSerializer):
    """Lightweight subject serializer for public tutor listings."""
    class Meta:
        model  = Subject
        fields = ['id', 'name']


class PublicSkillSerializer(serializers.ModelSerializer):
    """Lightweight skill serializer for public tutor listings."""
    class Meta:
        model  = Skill
        fields = ['id', 'name']


class PublicAvailabilitySerializer(serializers.ModelSerializer):
    """Lightweight availability serializer for public tutor listings."""
    class Meta:
        model  = TutorAvailability
        fields = ['id', 'day_of_week', 'start_time', 'end_time']


class TutorListSerializer(serializers.ModelSerializer):
    """
    Serializer for public tutor listing (GET /api/tutors/list/).
    Includes summary info, subjects, skills, availability, and computed stats.
    """
    full_name         = serializers.CharField(source='user.full_name', read_only=True)
    subjects          = PublicSubjectSerializer(many=True, read_only=True)
    skills            = PublicSkillSerializer(many=True, read_only=True)
    availability_slots = PublicAvailabilitySerializer(many=True, read_only=True)
    average_rating    = serializers.SerializerMethodField()
    review_count      = serializers.SerializerMethodField()
    is_verified_badge = serializers.BooleanField(source='is_verified', read_only=True)
    profile_photo_url = serializers.SerializerMethodField()

    class Meta:
        model = Tutor
        fields = [
            'id',
            'full_name',
            'domain',
            'bio',
            'pricing_per_session',
            'profile_photo_url',
            'punctuality_score',
            'total_sessions_done',
            'average_rating',
            'review_count',
            'is_verified_badge',
            'subjects',
            'skills',
            'availability_slots',
        ]

    def get_average_rating(self, obj):
        """
        Compute average rating from reviews.
        Reviews app may not exist yet — gracefully returns None if unavailable.
        """
        try:
            from django.db.models import Avg
            result = obj.reviews_received.aggregate(avg=Avg('rating'))
            avg = result.get('avg')
            return round(float(avg), 2) if avg is not None else None
        except Exception:
            return None

    def get_review_count(self, obj):
        try:
            return obj.reviews_received.count()
        except Exception:
            return 0

    def get_profile_photo_url(self, obj):
        """Return absolute URL for profile photo, or None."""
        request = self.context.get('request')
        if obj.profile_photo and request:
            return request.build_absolute_uri(obj.profile_photo.url)
        return None


# ─── Tutor Detail Serializer ──────────────────────────────────────────────────

class TutorDetailSerializer(serializers.ModelSerializer):
    """
    Full public tutor profile (GET /api/tutors/<id>/profile/ and my-profile).
    Returns all subjects, skills, availability, and computed rating stats.
    """
    full_name          = serializers.CharField(source='user.full_name', read_only=True)
    email              = serializers.EmailField(source='user.email', read_only=True)
    subjects           = PublicSubjectSerializer(many=True, read_only=True)
    skills             = PublicSkillSerializer(many=True, read_only=True)
    availability_slots = PublicAvailabilitySerializer(many=True, read_only=True)
    average_rating     = serializers.SerializerMethodField()
    review_count       = serializers.SerializerMethodField()
    is_verified_badge  = serializers.BooleanField(source='is_verified', read_only=True)
    profile_photo_url  = serializers.SerializerMethodField()

    class Meta:
        model = Tutor
        fields = [
            'id',
            'full_name',
            'email',
            'domain',
            'bio',
            'pricing_per_session',
            'profile_photo_url',
            'is_verified',
            'is_verified_badge',
            'is_suspended',
            'punctuality_score',
            'total_sessions_done',
            'average_rating',
            'review_count',
            'subjects',
            'skills',
            'availability_slots',
        ]

    def get_average_rating(self, obj):
        try:
            from django.db.models import Avg
            result = obj.reviews_received.aggregate(avg=Avg('rating'))
            avg = result.get('avg')
            return round(float(avg), 2) if avg is not None else None
        except Exception:
            return None

    def get_review_count(self, obj):
        try:
            return obj.reviews_received.count()
        except Exception:
            return 0

    def get_profile_photo_url(self, obj):
        request = self.context.get('request')
        if obj.profile_photo and request:
            return request.build_absolute_uri(obj.profile_photo.url)
        return None
