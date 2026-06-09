"""
UniAssist — accounts/views.py

Complete Authentication Module:
  1. StudentRegisterView        POST /api/auth/register/student/
  2. TutorRegisterView          POST /api/auth/register/tutor/
  3. OTPVerifyView              POST /api/auth/verify-otp/
  4. LoginView                  POST /api/auth/login/
  5. LogoutView                 POST /api/auth/logout/
  6. PasswordResetRequestView   POST /api/auth/password-reset/request/
  7. PasswordResetConfirmView   POST /api/auth/password-reset/confirm/

Response envelope (API_RULES.md):
  Success → {"success": true, "message": "...", "data": {...}}
  Error   → {"success": false, "message": "..."}

OTP Rules (CONTEXT.md / API_RULES.md):
  - OTP expires in exactly 10 minutes.
  - One OTP per email — old record deleted before inserting new.
  - OTP sent via Django SMTP (Gmail).
"""

import random
import string
import logging

logger = logging.getLogger(__name__)

from django.conf import settings
from django.contrib.auth import authenticate
from django.core.mail import send_mail
from django.utils import timezone
from datetime import timedelta

from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.exceptions import TokenError

from .models import OTPRecord, Student, Tutor, User
from .serializers import (
    LoginSerializer,
    LogoutSerializer,
    OTPVerifySerializer,
    PasswordResetConfirmSerializer,
    PasswordResetRequestSerializer,
    StudentRegisterSerializer,
    TutorRegisterSerializer,
    ResendOTPSerializer,
)
from uniassist.utils import error_response, success_response


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _generate_otp():
    """Return a 6-digit numeric OTP string."""
    return ''.join(random.choices(string.digits, k=6))


def _create_otp(email):
    """
    Delete any existing OTP for this email, then create a fresh one
    that expires exactly 10 minutes from now (OTP_EXPIRY_MINUTES setting).
    Returns the OTPRecord instance.
    """
    expiry_minutes = getattr(settings, 'OTP_EXPIRY_MINUTES', 10)
    OTPRecord.objects.filter(email=email).delete()   # One OTP per email
    otp = OTPRecord.objects.create(
        email=email,
        otp_code=_generate_otp(),
        expires_at=timezone.now() + timedelta(minutes=expiry_minutes),
    )
    return otp


def _send_otp_email(email, otp_code, purpose='verification'):
    """Send the OTP to the given email via Django SMTP."""
    subject = 'UniAssist — Your OTP Code'
    if purpose == 'password_reset':
        subject = 'UniAssist — Password Reset OTP'

    message = (
        f'Your UniAssist OTP code is: {otp_code}\n\n'
        f'This code is valid for {getattr(settings, "OTP_EXPIRY_MINUTES", 10)} minutes.\n'
        f'Do not share this code with anyone.\n\n'
        f'If you did not request this, please ignore this email.'
    )

    send_mail(
        subject=subject,
        message=message,
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[email],
        fail_silently=False,
    )


# ─── 1. Student Registration ──────────────────────────────────────────────────

class StudentRegisterView(APIView):
    """
    POST /api/auth/register/student/
    Fields: full_name, email, password

    Creates a User (role=student, is_active=False) + Student profile,
    generates a 6-digit OTP, and sends it to the provided email.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = StudentRegisterSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        # Create the User with is_active=False (activated after OTP)
        user = User.objects.create_user(
            email=data['email'],
            full_name=data['full_name'],
            password=data['password'],
            role=User.Role.STUDENT,
        )
        # is_active is already False by model default; create_user doesn't
        # override it because we don't pass is_active here explicitly.
        user.is_active = False
        user.save(update_fields=['is_active'])

        # Create the Student profile
        Student.objects.create(user=user)

        # Generate OTP and send email
        try:
            otp = _create_otp(user.email)
            _send_otp_email(user.email, otp.otp_code, purpose='verification')
        except Exception as e:
            logger.error(f"Failed to send OTP email to {user.email}: {str(e)}")
            return success_response(
                message='Registration successful, but failed to send OTP email. Please use the Resend OTP feature.',
                data={'email': user.email},
                status=status.HTTP_201_CREATED,
            )

        return success_response(
            message='Registration successful. Please check your email for the OTP to verify your account.',
            data={'email': user.email},
            status=status.HTTP_201_CREATED,
        )


# ─── 2. Tutor Registration ────────────────────────────────────────────────────

class TutorRegisterView(APIView):
    """
    POST /api/auth/register/tutor/
    Fields: full_name, email, password, domain (academic/skill/both)

    Creates a User (role=tutor, is_active=False) + Tutor profile,
    generates a 6-digit OTP, and sends it to the provided email.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = TutorRegisterSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        # Create the User with is_active=False (activated after OTP)
        user = User.objects.create_user(
            email=data['email'],
            full_name=data['full_name'],
            password=data['password'],
            role=User.Role.TUTOR,
        )
        user.is_active = False
        user.save(update_fields=['is_active'])

        # Create the Tutor profile with chosen domain
        Tutor.objects.create(user=user, domain=data['domain'])

        # Generate OTP and send email
        try:
            otp = _create_otp(user.email)
            _send_otp_email(user.email, otp.otp_code, purpose='verification')
        except Exception as e:
            logger.error(f"Failed to send OTP email to {user.email}: {str(e)}")
            return success_response(
                message='Registration successful, but failed to send OTP email. Please use the Resend OTP feature.',
                data={'email': user.email},
                status=status.HTTP_201_CREATED,
            )

        return success_response(
            message='Registration successful. Please check your email for the OTP to verify your account.',
            data={'email': user.email},
            status=status.HTTP_201_CREATED,
        )


# ─── 3. OTP Verification ──────────────────────────────────────────────────────

class OTPVerifyView(APIView):
    """
    POST /api/auth/verify-otp/
    Fields: email, otp_code

    - Checks OTP matches and is not expired.
    - If valid   → activates user (is_active=True), deletes OTP record.
    - If expired → returns "OTP expired".
    - If wrong   → returns "Invalid OTP".
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = OTPVerifySerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data
        email    = data['email']
        otp_code = data['otp_code']

        try:
            otp_record = OTPRecord.objects.get(email=email)
        except OTPRecord.DoesNotExist:
            return error_response(
                message='No OTP found for this email. Please request a new OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Check expiry first
        if not otp_record.is_valid():
            otp_record.delete()
            return error_response(
                message='OTP expired. Please request a new OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Check code match
        if otp_record.otp_code != otp_code:
            return error_response(
                message='Invalid OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Valid — activate the user
        try:
            user = User.objects.get(email=email)
            user.is_active = True
            user.save(update_fields=['is_active'])
        except User.DoesNotExist:
            return error_response(
                message='User not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Clean up OTP record
        otp_record.delete()

        return success_response(
            message='Email verified successfully. You can now log in.',
            data={'email': email},
        )


# ─── 4. Login ─────────────────────────────────────────────────────────────────

class LoginView(APIView):
    """
    POST /api/auth/login/
    Fields: email, password

    - Validates credentials.
    - Checks user is active (OTP verified).
    - Returns JWT access token + refresh token + role.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data  = serializer.validated_data
        email = data['email']
        password = data['password']

        # Check the user exists and is not inactive due to wrong credentials
        try:
            user_obj = User.objects.get(email=email)
        except User.DoesNotExist:
            return error_response(
                message='Invalid email or password.',
                status=status.HTTP_401_UNAUTHORIZED,
            )

        # Check OTP verification status before authenticating
        if not user_obj.is_active:
            return error_response(
                message='Account not verified. Please verify your email with the OTP sent during registration.',
                status=status.HTTP_401_UNAUTHORIZED,
            )

        # Authenticate — returns None if password is wrong
        user = authenticate(request, username=email, password=password)
        if user is None:
            return error_response(
                message='Invalid email or password.',
                status=status.HTTP_401_UNAUTHORIZED,
            )

        # Generate JWT tokens
        refresh = RefreshToken.for_user(user)

        return success_response(
            message='Login successful.',
            data={
                'access_token':  str(refresh.access_token),
                'refresh_token': str(refresh),
                'role':          user.role,
                'full_name':     user.full_name,
                'email':         user.email,
            },
        )


# ─── 5. Logout ────────────────────────────────────────────────────────────────

class LogoutView(APIView):
    """
    POST /api/auth/logout/
    Fields: refresh (JWT refresh token)
    Requires: Bearer access token in Authorization header.

    Blacklists the provided refresh token to invalidate it.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = LogoutSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        refresh_token = serializer.validated_data['refresh']

        try:
            token = RefreshToken(refresh_token)
            token.blacklist()
        except TokenError:
            return error_response(
                message='Invalid or already blacklisted refresh token.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        return success_response(message='Logged out successfully.')


# ─── 6. Password Reset — Request ─────────────────────────────────────────────

class PasswordResetRequestView(APIView):
    """
    POST /api/auth/password-reset/request/
    Fields: email

    Sends a 6-digit OTP to the email for password reset.
    Returns a success message regardless of whether the email exists
    (security best practice — do not reveal registered emails).
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = PasswordResetRequestSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        email = serializer.validated_data['email']

        # Only send OTP if email is registered — but return the same message
        # either way to avoid user enumeration.
        if User.objects.filter(email=email).exists():
            try:
                otp = _create_otp(email)
                _send_otp_email(email, otp.otp_code, purpose='password_reset')
            except Exception as e:
                logger.error(f"Failed to send password reset OTP to {email}: {str(e)}")
                return error_response(
                    message='Failed to send OTP email. Please try again.',
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                )

        return success_response(
            message='If this email is registered, a password reset OTP has been sent.',
            data={'email': email},
        )


# ─── 7. Password Reset — Confirm ─────────────────────────────────────────────

class PasswordResetConfirmView(APIView):
    """
    POST /api/auth/password-reset/confirm/
    Fields: email, otp_code, new_password

    - Verifies OTP.
    - Updates the user's password.
    - Deletes the OTP record.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = PasswordResetConfirmSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data     = serializer.validated_data
        email    = data['email']
        otp_code = data['otp_code']
        new_pass = data['new_password']

        # Validate OTP
        try:
            otp_record = OTPRecord.objects.get(email=email)
        except OTPRecord.DoesNotExist:
            return error_response(
                message='No OTP found for this email. Please request a new OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not otp_record.is_valid():
            otp_record.delete()
            return error_response(
                message='OTP expired. Please request a new OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        if otp_record.otp_code != otp_code:
            return error_response(
                message='Invalid OTP.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Update password
        try:
            user = User.objects.get(email=email)
            user.set_password(new_pass)
            user.save(update_fields=['password'])
        except User.DoesNotExist:
            return error_response(
                message='User not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Clean up OTP record
        otp_record.delete()

        return success_response(
            message='Password reset successful. You can now log in with your new password.',
        )


# ─── 8. Resend OTP ────────────────────────────────────────────────────────────

class ResendOTPView(APIView):
    """
    POST /api/auth/resend-otp/
    Fields: email

    - Checks if user exists.
    - Prevents spam with a 60-second cooldown.
    - Deletes old OTP, generates new, and emails it.
    """
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = ResendOTPSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        email = serializer.validated_data['email']

        # Check if user exists
        try:
            user = User.objects.get(email=email)
        except User.DoesNotExist:
            return error_response(
                message='User not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        # Check cooldown
        try:
            old_otp = OTPRecord.objects.get(email=email)
            time_since_creation = timezone.now() - old_otp.created_at
            if time_since_creation.total_seconds() < 60:
                return error_response(
                    message='Please wait 60 seconds before requesting a new OTP.',
                    status=status.HTTP_429_TOO_MANY_REQUESTS,
                )
        except OTPRecord.DoesNotExist:
            pass

        # Generate OTP and send email
        try:
            otp = _create_otp(email)
            purpose = 'verification' if not user.is_active else 'password_reset'
            _send_otp_email(email, otp.otp_code, purpose=purpose)
        except Exception as e:
            logger.error(f"Failed to resend OTP email to {email}: {str(e)}")
            return error_response(
                message='Failed to send OTP email. Please try again later.',
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return success_response(
            message='A new OTP has been sent to your email.',
            data={'email': email},
        )
