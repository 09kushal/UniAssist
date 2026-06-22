"""
UniAssist — accounts/urls.py

Authentication URL routes.
All endpoints are prefixed with /api/auth/ (set in uniassist/urls.py).

Endpoints:
  POST  register/student/          → StudentRegisterView
  POST  register/tutor/            → TutorRegisterView
  POST  verify-otp/                → OTPVerifyView
  POST  login/                     → LoginView
  POST  logout/                    → LogoutView
  POST  password-reset/request/    → PasswordResetRequestView
  POST  password-reset/confirm/    → PasswordResetConfirmView
"""

from django.urls import path

from . import views

app_name = 'accounts'

urlpatterns = [
    # Registration
    path('register/student/',       views.StudentRegisterView.as_view(),      name='register-student'),
    path('register/tutor/',         views.TutorRegisterView.as_view(),        name='register-tutor'),

    # OTP Verification
    path('verify-otp/',             views.OTPVerifyView.as_view(),            name='verify-otp'),
    path('resend-otp/',             views.ResendOTPView.as_view(),            name='resend-otp'),

    # Login / Logout
    path('login/',                  views.LoginView.as_view(),                name='login'),
    path('logout/',                 views.LogoutView.as_view(),               name='logout'),

    # Password Reset
    path('password-reset/request/', views.PasswordResetRequestView.as_view(), name='password-reset-request'),
    path('password-reset/confirm/', views.PasswordResetConfirmView.as_view(), name='password-reset-confirm'),

    # Device Token
    path('device-token/', __import__('notifications.views').views.DeviceTokenUpdateView.as_view(), name='device-token'),
]
