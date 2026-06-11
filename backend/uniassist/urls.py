"""
UniAssist — Root URL Configuration

URL prefix mapping:
  /admin/         → Django admin panel
  /api/auth/      → accounts app  (registration, OTP, login, logout, password reset)
  /api/tutors/    → tutors app    (profile setup, discovery, listing)

Media files are served in development only (DEBUG=True).
"""

from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/', admin.site.urls),

    # Authentication & Accounts
    path('api/auth/', include('accounts.urls', namespace='accounts')),

    # Tutor Discovery Module
    path('api/tutors/', include('tutors.urls', namespace='tutors')),

    # Booking System Module
    path('api/booking/', include('booking.urls', namespace='booking')),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
