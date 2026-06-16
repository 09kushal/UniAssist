"""
UniAssist — payments/urls.py

URL patterns for the Payment Module (Phase 5).

Mounted at: /api/payments/ (see uniassist/urls.py)

Endpoints:
  POST   /api/payments/initiate/                        → InitiatePaymentView
  POST   /api/payments/callback/                        → PaymentCallbackView
  POST   /api/payments/failed/                          → PaymentFailedView
  GET    /api/payments/failed/                          → PaymentFailedView  (eSewa GET redirect)
  GET    /api/payments/<booking_id>/status/             → PaymentStatusView
  POST   /api/payments/admin/payout/<booking_id>/release/ → AdminPayoutReleaseView
  GET    /api/payments/tutor/payouts/                   → TutorPayoutHistoryView
"""

from django.urls import path

from payments.views import (
    InitiatePaymentView,
    PaymentCallbackView,
    PaymentFailedView,
    PaymentStatusView,
    AdminPayoutReleaseView,
    TutorPayoutHistoryView,
)

app_name = 'payments'

urlpatterns = [
    # ── Student payment initiation ────────────────────────────────────────────
    path('initiate/', InitiatePaymentView.as_view(), name='initiate'),

    # ── eSewa callback (no auth — called by eSewa server) ─────────────────────
    path('callback/', PaymentCallbackView.as_view(), name='callback'),

    # ── eSewa failure redirect (no auth — eSewa redirects here) ──────────────
    path('failed/', PaymentFailedView.as_view(), name='failed'),

    # ── Payment status for a specific booking (Student or Tutor) ─────────────
    path('<int:booking_id>/status/', PaymentStatusView.as_view(), name='status'),

    # ── Admin: release payout for a booking ───────────────────────────────────
    path(
        'admin/payout/<int:booking_id>/release/',
        AdminPayoutReleaseView.as_view(),
        name='admin-payout-release',
    ),

    # ── Tutor: paginated payout history ───────────────────────────────────────
    path('tutor/payouts/', TutorPayoutHistoryView.as_view(), name='tutor-payouts'),
]
