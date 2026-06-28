"""
UniAssist — accounts/serializers.py

Serializers for the complete Authentication Module:
  • StudentRegisterSerializer   — POST /api/auth/register/student/
  • TutorRegisterSerializer     — POST /api/auth/register/tutor/
  • OTPVerifySerializer         — POST /api/auth/verify-otp/
  • LoginSerializer             — POST /api/auth/login/
  • LogoutSerializer            — POST /api/auth/logout/
  • PasswordResetRequestSerializer  — POST /api/auth/password-reset/request/
  • PasswordResetConfirmSerializer  — POST /api/auth/password-reset/confirm/

All inputs validated server-side (API_RULES.md).
Passwords are never returned in any response.
"""

from django.contrib.auth import authenticate
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

from tutors.serializers import validate_profile_photo
from .models import User, Tutor


# ─── Student Registration ─────────────────────────────────────────────────────

class StudentRegisterSerializer(serializers.Serializer):
    """
    Validates student registration payload.
    Fields: full_name, email, password
    """
    full_name = serializers.CharField(max_length=255, required=True)
    email     = serializers.EmailField(required=True)
    password  = serializers.CharField(
        write_only=True,
        required=True,
        style={'input_type': 'password'},
    )

    def validate_email(self, value):
        """Ensure email is not already registered."""
        value = value.lower().strip()
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError('An account with this email already exists.')
        return value

    def validate_password(self, value):
        """Run Django's built-in password validators."""
        validate_password(value)
        return value

    def validate_full_name(self, value):
        return value.strip()


# ─── Tutor Registration ───────────────────────────────────────────────────────

class TutorRegisterSerializer(serializers.Serializer):
    """
    Validates tutor registration payload.
    Fields: full_name, email, password, domain (academic/skill/both)
    """
    DOMAIN_CHOICES = [
        ('academic', 'Academic'),
        ('skill',    'Skill-Based'),
        ('both',     'Both'),
    ]

    full_name = serializers.CharField(max_length=255, required=True)
    email     = serializers.EmailField(required=True)
    password  = serializers.CharField(
        write_only=True,
        required=True,
        style={'input_type': 'password'},
    )
    domain    = serializers.ChoiceField(choices=DOMAIN_CHOICES, required=True)

    def validate_email(self, value):
        """Ensure email is not already registered."""
        value = value.lower().strip()
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError('An account with this email already exists.')
        return value

    def validate_password(self, value):
        """Run Django's built-in password validators."""
        validate_password(value)
        return value

    def validate_full_name(self, value):
        return value.strip()


# ─── OTP Verification ─────────────────────────────────────────────────────────

class OTPVerifySerializer(serializers.Serializer):
    """
    Validates OTP verification payload.
    Fields: email, otp_code
    """
    email    = serializers.EmailField(required=True)
    otp_code = serializers.CharField(
        min_length=6,
        max_length=6,
        required=True,
    )

    def validate_email(self, value):
        return value.lower().strip()


# ─── Login ────────────────────────────────────────────────────────────────────

class LoginSerializer(serializers.Serializer):
    """
    Validates login credentials.
    Fields: email, password
    """
    email    = serializers.EmailField(required=True)
    password = serializers.CharField(
        write_only=True,
        required=True,
        style={'input_type': 'password'},
    )

    def validate_email(self, value):
        return value.lower().strip()


# ─── Logout ───────────────────────────────────────────────────────────────────

class LogoutSerializer(serializers.Serializer):
    """
    Validates logout payload.
    Fields: refresh (JWT refresh token to blacklist)
    """
    refresh = serializers.CharField(required=True)


# ─── Password Reset — Request ─────────────────────────────────────────────────

class PasswordResetRequestSerializer(serializers.Serializer):
    """
    Validates password reset OTP request.
    Fields: email
    """
    email = serializers.EmailField(required=True)

    def validate_email(self, value):
        return value.lower().strip()


# ─── Password Reset — Confirm ─────────────────────────────────────────────────

class PasswordResetConfirmSerializer(serializers.Serializer):
    """
    Validates password reset confirmation.
    Fields: email, otp_code, new_password
    """
    email        = serializers.EmailField(required=True)
    otp_code     = serializers.CharField(min_length=6, max_length=6, required=True)
    new_password = serializers.CharField(
        write_only=True,
        required=True,
        style={'input_type': 'password'},
    )

    def validate_email(self, value):
        return value.lower().strip()

    def validate_new_password(self, value):
        """Run Django's built-in password validators."""
        validate_password(value)
        return value


# ─── Resend OTP ───────────────────────────────────────────────────────────────

class ResendOTPSerializer(serializers.Serializer):
    """
    Validates email for resending OTP.
    Fields: email
    """
    email = serializers.EmailField(required=True)

    def validate_email(self, value):
        return value.lower().strip()


# ─── Student Profile Setup ────────────────────────────────────────────────────

class StudentProfileSetupSerializer(serializers.Serializer):
    """
    PATCH /api/auth/student/profile/setup/
    All fields are optional (partial update).
    """
    grade_or_university  = serializers.CharField(
        required=False,
        allow_blank=True,
        max_length=255,
    )
    subjects_of_interest = serializers.CharField(
        required=False,
        allow_blank=True,
        max_length=500,
    )
    profile_photo        = serializers.ImageField(required=False, allow_null=True)

    def validate_profile_photo(self, image):
        return validate_profile_photo(image)
