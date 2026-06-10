"""
UniAssist — tutors/urls.py

Tutor Discovery Module URL routes.
All endpoints are prefixed with /api/tutors/ (set in uniassist/urls.py).

Endpoints:
  PATCH  profile/setup/              → TutorProfileSetupView     (tutor auth)
  POST   subjects/add/               → AddSubjectView             (tutor auth)
  DELETE subjects/<id>/remove/       → RemoveSubjectView          (tutor auth)
  POST   skills/add/                 → AddSkillView               (tutor auth)
  DELETE skills/<id>/remove/         → RemoveSkillView            (tutor auth)
  POST   availability/add/           → AddAvailabilityView        (tutor auth)
  DELETE availability/<id>/remove/   → RemoveAvailabilityView     (tutor auth)
  GET    list/                       → TutorListView              (public)
  GET    my-profile/                 → MyProfileView              (tutor auth)
  GET    <id>/profile/               → TutorProfileDetailView     (public)

Note: my-profile/ must come before <id>/profile/ to avoid URL conflicts.
"""

from django.urls import path

from . import views

app_name = 'tutors'

urlpatterns = [
    # ── Tutor Profile Setup ────────────────────────────────────────────────────
    path(
        'profile/setup/',
        views.TutorProfileSetupView.as_view(),
        name='profile-setup',
    ),

    # ── Subject Management ─────────────────────────────────────────────────────
    path(
        'subjects/add/',
        views.AddSubjectView.as_view(),
        name='subject-add',
    ),
    path(
        'subjects/<int:subject_id>/remove/',
        views.RemoveSubjectView.as_view(),
        name='subject-remove',
    ),

    # ── Skill Management ───────────────────────────────────────────────────────
    path(
        'skills/add/',
        views.AddSkillView.as_view(),
        name='skill-add',
    ),
    path(
        'skills/<int:skill_id>/remove/',
        views.RemoveSkillView.as_view(),
        name='skill-remove',
    ),

    # ── Availability Slot Management ───────────────────────────────────────────
    path(
        'availability/add/',
        views.AddAvailabilityView.as_view(),
        name='availability-add',
    ),
    path(
        'availability/<int:slot_id>/remove/',
        views.RemoveAvailabilityView.as_view(),
        name='availability-remove',
    ),

    # ── Public Listing ─────────────────────────────────────────────────────────
    path(
        'list/',
        views.TutorListView.as_view(),
        name='tutor-list',
    ),

    # ── Own Profile (must be before <id>/profile/ to avoid conflict) ───────────
    path(
        'my-profile/',
        views.MyProfileView.as_view(),
        name='my-profile',
    ),

    # ── Public Tutor Profile Detail ────────────────────────────────────────────
    path(
        '<int:tutor_id>/profile/',
        views.TutorProfileDetailView.as_view(),
        name='tutor-profile-detail',
    ),
]
