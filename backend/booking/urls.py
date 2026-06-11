"""
UniAssist — booking/urls.py

Defines standard URL endpoints for booking module:
  - POST  /api/booking/request/         → booking-request
  - PATCH /api/booking/<id>/respond/    → booking-respond
  - GET   /api/booking/<id>/status/     → booking-status
  - GET   /api/booking/my-bookings/     → my-bookings (student history)
  - GET   /api/booking/my-requests/     → my-requests (tutor history)
  - PATCH /api/booking/<id>/cancel/      → booking-cancel
"""

from django.urls import path
from booking.views import (
    BookingRequestView,
    BookingRespondView,
    BookingStatusView,
    StudentBookingHistoryView,
    TutorBookingHistoryView,
    CancelBookingView,
)

app_name = 'booking'

urlpatterns = [
    # Creating booking request
    path('request/', BookingRequestView.as_view(), name='request'),

    # Responding to booking request (Accept/Reject)
    path('<int:booking_id>/respond/', BookingRespondView.as_view(), name='respond'),

    # Viewing individual booking status
    path('<int:booking_id>/status/', BookingStatusView.as_view(), name='status'),

    # History & lists
    path('my-bookings/', StudentBookingHistoryView.as_view(), name='my-bookings'),
    path('my-requests/', TutorBookingHistoryView.as_view(), name='my-requests'),

    # Cancelling booking request
    path('<int:booking_id>/cancel/', CancelBookingView.as_view(), name='cancel'),
]
