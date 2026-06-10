"""
UniAssist — tutors/views.py

Tutor Discovery Module — All 10 API endpoints:

  1.  PATCH  /api/tutors/profile/setup/          → TutorProfileSetupView
  2.  POST   /api/tutors/subjects/add/            → AddSubjectView
  3.  DELETE /api/tutors/subjects/<id>/remove/    → RemoveSubjectView
  4.  POST   /api/tutors/skills/add/              → AddSkillView
  5.  DELETE /api/tutors/skills/<id>/remove/      → RemoveSkillView
  6.  POST   /api/tutors/availability/add/        → AddAvailabilityView
  7.  DELETE /api/tutors/availability/<id>/remove/→ RemoveAvailabilityView
  8.  GET    /api/tutors/list/                    → TutorListView  (public)
  9.  GET    /api/tutors/<id>/profile/            → TutorProfileDetailView (public)
  10. GET    /api/tutors/my-profile/              → MyProfileView

Response envelope (API_RULES.md):
  Success → {"success": true,  "message": "...", "data": {...}}
  Error   → {"success": false, "message": "...", "errors": {...}}

Role enforcement:
  - Only tutors can access profile-edit endpoints.
  - Listing and detail endpoints are fully public.
"""

import logging

from django.db.models import Avg, Q
from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.views import APIView

from accounts.models import Tutor, Subject, Skill, TutorAvailability
from uniassist.utils import error_response, success_response

from .serializers import (
    TutorProfileSetupSerializer,
    AddSubjectSerializer,
    AddSkillSerializer,
    AddAvailabilitySerializer,
    SubjectSerializer,
    SkillSerializer,
    TutorAvailabilitySerializer,
    TutorListSerializer,
    TutorDetailSerializer,
)

logger = logging.getLogger(__name__)


# ─── Custom Permission Helper ─────────────────────────────────────────────────

def _get_tutor_or_error(request):
    """
    Verify the request user is a tutor and return their Tutor profile.
    Returns (tutor, None) on success or (None, Response) on failure.
    """
    if not request.user.is_authenticated:
        return None, error_response(
            message='Authentication required.',
            status=status.HTTP_401_UNAUTHORIZED,
        )
    if not request.user.is_tutor:
        return None, error_response(
            message='Only tutors can access this endpoint.',
            status=status.HTTP_403_FORBIDDEN,
        )
    try:
        tutor = request.user.tutor_profile
    except Tutor.DoesNotExist:
        return None, error_response(
            message='Tutor profile not found.',
            status=status.HTTP_404_NOT_FOUND,
        )
    return tutor, None


# ─── Pagination ───────────────────────────────────────────────────────────────

class TutorPagination(PageNumberPagination):
    """
    Paginator for public tutor listing.
    Supports ?page=<n>&page_size=<n> query params.
    """
    page_size              = 20
    page_size_query_param  = 'page_size'
    max_page_size          = 100
    page_query_param       = 'page'


# ─── 1. Tutor Profile Setup ───────────────────────────────────────────────────

class TutorProfileSetupView(APIView):
    """
    PATCH /api/tutors/profile/setup/
    Auth: Tutor JWT required.

    Partial update of bio, pricing_per_session, and/or profile_photo.
    Supports multipart/form-data for file uploads.
    Photo validation: JPG/PNG only, max 5 MB.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        serializer = TutorProfileSetupSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        # Apply validated fields
        if 'bio' in data:
            tutor.bio = data['bio']
        if 'pricing_per_session' in data:
            tutor.pricing_per_session = data['pricing_per_session']
        if 'profile_photo' in data and data['profile_photo'] is not None:
            tutor.profile_photo = data['profile_photo']

        tutor.save()

        # Build response data
        profile_photo_url = None
        if tutor.profile_photo:
            try:
                profile_photo_url = request.build_absolute_uri(tutor.profile_photo.url)
            except Exception:
                pass

        return success_response(
            message='Profile updated successfully.',
            data={
                'id':                   tutor.id,
                'full_name':            tutor.user.full_name,
                'domain':               tutor.domain,
                'bio':                  tutor.bio,
                'pricing_per_session':  str(tutor.pricing_per_session),
                'profile_photo_url':    profile_photo_url,
                'is_verified':          tutor.is_verified,
            },
        )


# ─── 2. Add Subject ───────────────────────────────────────────────────────────

class AddSubjectView(APIView):
    """
    POST /api/tutors/subjects/add/
    Auth: Tutor JWT required.

    Only allowed if tutor domain is 'academic' or 'both'.
    Creates and returns the new Subject.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        # Domain check — academic or both only
        if tutor.domain not in [Tutor.Domain.ACADEMIC, Tutor.Domain.BOTH]:
            return error_response(
                message='Only tutors with academic or both domain can add subjects.',
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = AddSubjectSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        name = serializer.validated_data['name']
        subject = Subject.objects.create(tutor=tutor, name=name)

        return success_response(
            message='Subject added successfully.',
            data=SubjectSerializer(subject).data,
            status=status.HTTP_201_CREATED,
        )


# ─── 3. Remove Subject ────────────────────────────────────────────────────────

class RemoveSubjectView(APIView):
    """
    DELETE /api/tutors/subjects/<id>/remove/
    Auth: Tutor JWT required.

    Only the tutor who owns the subject can delete it.
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, subject_id):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        try:
            subject = Subject.objects.get(id=subject_id, tutor=tutor)
        except Subject.DoesNotExist:
            return error_response(
                message='Subject not found or you do not own it.',
                status=status.HTTP_404_NOT_FOUND,
            )

        subject.delete()
        return success_response(message='Subject removed successfully.')


# ─── 4. Add Skill ─────────────────────────────────────────────────────────────

class AddSkillView(APIView):
    """
    POST /api/tutors/skills/add/
    Auth: Tutor JWT required.

    Only allowed if tutor domain is 'skill' or 'both'.
    Creates and returns the new Skill.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        # Domain check — skill or both only
        if tutor.domain not in [Tutor.Domain.SKILL, Tutor.Domain.BOTH]:
            return error_response(
                message='Only tutors with skill-based or both domain can add skills.',
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = AddSkillSerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        name = serializer.validated_data['name']
        skill = Skill.objects.create(tutor=tutor, name=name)

        return success_response(
            message='Skill added successfully.',
            data=SkillSerializer(skill).data,
            status=status.HTTP_201_CREATED,
        )


# ─── 5. Remove Skill ──────────────────────────────────────────────────────────

class RemoveSkillView(APIView):
    """
    DELETE /api/tutors/skills/<id>/remove/
    Auth: Tutor JWT required.

    Only the tutor who owns the skill can delete it.
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, skill_id):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        try:
            skill = Skill.objects.get(id=skill_id, tutor=tutor)
        except Skill.DoesNotExist:
            return error_response(
                message='Skill not found or you do not own it.',
                status=status.HTTP_404_NOT_FOUND,
            )

        skill.delete()
        return success_response(message='Skill removed successfully.')


# ─── 6. Add Availability Slot ─────────────────────────────────────────────────

class AddAvailabilityView(APIView):
    """
    POST /api/tutors/availability/add/
    Auth: Tutor JWT required.

    Fields: day_of_week (Mon/Tue/Wed/Thu/Fri/Sat/Sun), start_time, end_time.
    Validates no overlapping slots on the same day.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        serializer = AddAvailabilitySerializer(data=request.data)
        if not serializer.is_valid():
            first_error = next(iter(serializer.errors.values()))[0]
            return error_response(
                message=str(first_error),
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST,
            )

        data       = serializer.validated_data
        day        = data['day_of_week']
        start_time = data['start_time']
        end_time   = data['end_time']

        # Overlap check: any existing slot on same day where ranges intersect.
        # Two intervals [A, B] and [C, D] overlap when A < D and C < B.
        overlapping = TutorAvailability.objects.filter(
            tutor=tutor,
            day_of_week=day,
        ).filter(
            # existing slot starts before new slot ends AND existing slot ends after new slot starts
            start_time__lt=end_time,
            end_time__gt=start_time,
        )

        if overlapping.exists():
            return error_response(
                message='This slot overlaps with an existing availability slot on the same day.',
                status=status.HTTP_400_BAD_REQUEST,
            )

        slot = TutorAvailability.objects.create(
            tutor=tutor,
            day_of_week=day,
            start_time=start_time,
            end_time=end_time,
        )

        return success_response(
            message='Availability slot added successfully.',
            data=TutorAvailabilitySerializer(slot).data,
            status=status.HTTP_201_CREATED,
        )


# ─── 7. Remove Availability Slot ─────────────────────────────────────────────

class RemoveAvailabilityView(APIView):
    """
    DELETE /api/tutors/availability/<id>/remove/
    Auth: Tutor JWT required.

    Only the tutor who owns the slot can delete it.
    """
    permission_classes = [IsAuthenticated]

    def delete(self, request, slot_id):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        try:
            slot = TutorAvailability.objects.get(id=slot_id, tutor=tutor)
        except TutorAvailability.DoesNotExist:
            return error_response(
                message='Availability slot not found or you do not own it.',
                status=status.HTTP_404_NOT_FOUND,
            )

        slot.delete()
        return success_response(message='Availability slot removed successfully.')


# ─── 8. Public Tutor Listing ──────────────────────────────────────────────────

class TutorListView(APIView):
    """
    GET /api/tutors/list/
    Auth: Not required (public endpoint).

    Query params:
      domain        — filter by domain (academic/skill/both)
      subject       — filter by subject name (case-insensitive contains)
      skill         — filter by skill name (case-insensitive contains)
      min_rating    — minimum average rating (float, applied in Python)
      available_day — filter by available day (Mon/Tue/Wed/Thu/Fri/Sat/Sun)
      page          — page number (default: 1)
      page_size     — results per page (default: 20, max: 100)

    Rules:
      - Only verified tutors (is_verified=True) are shown.
      - Ordered by average_rating descending (annotated), then by id for stability.
    """
    permission_classes = [AllowAny]

    def get(self, request):
        qs = Tutor.objects.filter(is_verified=True).select_related('user').prefetch_related(
            'subjects', 'skills', 'availability_slots'
        )

        # ── Filters ────────────────────────────────────────────────────────────
        domain = request.query_params.get('domain')
        if domain:
            qs = qs.filter(domain=domain)

        subject_name = request.query_params.get('subject')
        if subject_name:
            qs = qs.filter(subjects__name__icontains=subject_name).distinct()

        skill_name = request.query_params.get('skill')
        if skill_name:
            qs = qs.filter(skills__name__icontains=skill_name).distinct()

        available_day = request.query_params.get('available_day')
        if available_day:
            qs = qs.filter(availability_slots__day_of_week=available_day).distinct()

        # ── Annotate average rating and order ──────────────────────────────────
        try:
            qs = qs.annotate(avg_rating=Avg('reviews_received__rating'))
            qs = qs.order_by('-avg_rating', 'id')
        except Exception:
            # reviews_received may not exist before the reviews app is built
            qs = qs.order_by('id')

        # ── min_rating filter (applied after annotation) ───────────────────────
        min_rating = request.query_params.get('min_rating')
        if min_rating:
            try:
                min_rating_val = float(min_rating)
                qs = qs.filter(avg_rating__gte=min_rating_val)
            except (ValueError, TypeError):
                return error_response(
                    message='min_rating must be a valid number.',
                    status=status.HTTP_400_BAD_REQUEST,
                )

        # ── Paginate ───────────────────────────────────────────────────────────
        paginator = TutorPagination()
        page      = paginator.paginate_queryset(qs, request)

        serializer = TutorListSerializer(
            page,
            many=True,
            context={'request': request},
        )

        return success_response(
            message='Tutors retrieved successfully.',
            data={
                'count':    paginator.page.paginator.count,
                'next':     paginator.get_next_link(),
                'previous': paginator.get_previous_link(),
                'results':  serializer.data,
            },
        )


# ─── 9. Public Tutor Profile Detail ──────────────────────────────────────────

class TutorProfileDetailView(APIView):
    """
    GET /api/tutors/<id>/profile/
    Auth: Not required (public endpoint).

    Returns the full profile of a single verified tutor.
    """
    permission_classes = [AllowAny]

    def get(self, request, tutor_id):
        try:
            tutor = (
                Tutor.objects
                .select_related('user')
                .prefetch_related('subjects', 'skills', 'availability_slots')
                .get(id=tutor_id, is_verified=True)
            )
        except Tutor.DoesNotExist:
            return error_response(
                message='Tutor not found.',
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = TutorDetailSerializer(tutor, context={'request': request})
        return success_response(
            message='Tutor profile retrieved successfully.',
            data=serializer.data,
        )


# ─── 10. My Profile (logged-in tutor) ────────────────────────────────────────

class MyProfileView(APIView):
    """
    GET /api/tutors/my-profile/
    Auth: Tutor JWT required.

    Returns the authenticated tutor's full own profile.
    is_verified does NOT need to be True — tutors can view their own pending profile.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        tutor, err = _get_tutor_or_error(request)
        if err:
            return err

        tutor = (
            Tutor.objects
            .select_related('user')
            .prefetch_related('subjects', 'skills', 'availability_slots')
            .get(id=tutor.id)
        )

        serializer = TutorDetailSerializer(tutor, context={'request': request})
        return success_response(
            message='Your profile retrieved successfully.',
            data=serializer.data,
        )
