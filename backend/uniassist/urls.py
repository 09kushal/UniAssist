"""
UniAssist — Root URL Configuration

URL prefix mapping:
  /admin/                 → Django admin panel
  /api/auth/              → accounts app  (registration, OTP, login, logout, password reset)
  /api/tutors/            → tutors app    (profile setup, discovery, listing)
  /api/booking/           → booking app   (request, respond, cancel, history)
  /api/payments/          → payments app  (eSewa initiation, callback, payout)
  /api/reviews/           → reviews app   (submit review, tutor reviews, my reviews, check)
  /api/reports/           → reports app   (lateness reports, reschedule, tutor verification, fines)
  /api/admin/dashboard/   → admin dashboard summary (per API_RULES.md)

Media files are served in development only (DEBUG=True).
"""

from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

from reports.views import AdminDashboardSummaryView

urlpatterns = [
    path('admin/', admin.site.urls),

    # Authentication & Accounts
    path('api/auth/', include('accounts.urls', namespace='accounts')),

    # Tutor Discovery Module
    path('api/tutors/', include('tutors.urls', namespace='tutors')),

    # Booking System Module
    path('api/booking/', include('booking.urls', namespace='booking')),

    # Payment Module (Phase 5)
    path('api/payments/', include('payments.urls', namespace='payments')),

    # Review & Rating Module (Phase 6)
    path('api/reviews/', include('reviews.urls', namespace='reviews')),

    # Reports & Admin Module (Phase 7)
    path('api/reports/', include('reports.urls', namespace='reports')),

    # Admin Dashboard Summary — exact URL from API_RULES.md:
    # GET /api/admin/dashboard/summary/
    path('api/admin/dashboard/summary/', AdminDashboardSummaryView.as_view(), name='admin-dashboard-summary'),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
