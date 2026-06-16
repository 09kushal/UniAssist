"""
UniAssist — reviews/urls.py

URL patterns for the Review & Rating Module (Phase 6).

Mounted at: /api/reviews/ (see uniassist/urls.py)

Endpoints:
  POST  /api/reviews/submit/                → SubmitReviewView
  GET   /api/reviews/tutor/<tutor_id>/      → TutorReviewsView    (public)
  GET   /api/reviews/my-reviews/            → MyReviewsView
  GET   /api/reviews/check/<booking_id>/    → CheckReviewedView
"""

from django.urls import path

from reviews.views import (
    SubmitReviewView,
    TutorReviewsView,
    MyReviewsView,
    CheckReviewedView,
)

app_name = 'reviews'

urlpatterns = [
    # ── Student: submit a review for a completed booking ──────────────────────
    path('submit/', SubmitReviewView.as_view(), name='submit'),

    # ── Public: all reviews for a specific tutor ──────────────────────────────
    path('tutor/<int:tutor_id>/', TutorReviewsView.as_view(), name='tutor-reviews'),

    # ── Student: list all reviews the student has written ─────────────────────
    path('my-reviews/', MyReviewsView.as_view(), name='my-reviews'),

    # ── Student: check if a specific booking has been reviewed ─────────────────
    path('check/<int:booking_id>/', CheckReviewedView.as_view(), name='check-reviewed'),
]
