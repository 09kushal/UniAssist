"""
UniAssist — reports/views.py

Phase 7 — Reports & Admin Module

Endpoints implemented:
  1.  POST   /api/reports/lateness/file/                    — Student files lateness report
  2.  POST   /api/reports/student/file/                     — Tutor files student report
  3.  GET    /api/reports/admin/all/                        — Admin views all reports (paginated)
  4.  PATCH  /api/reports/admin/<report_id>/action/         — Admin takes action on report
  5.  POST   /api/reports/admin/fine-preview/               — Admin live fine preview
  6.  POST   /api/reports/admin/fine-confirm/               — Admin confirms and applies fine
  7.  POST   /api/reports/reschedule/request/               — Student requests reschedule
  8.  GET    /api/reports/admin/reschedule/                  — Admin views all reschedule requests
  9.  PATCH  /api/reports/admin/reschedule/<id>/action/     — Admin acts on reschedule request
  10. GET    /api/admin/dashboard/summary/                  — Admin dashboard summary
  11. GET    /api/reports/admin/tutor-verification/         — Admin views unverified tutors
  12. PATCH  /api/reports/admin/tutor/<tutor_id>/approve/   — Admin approves tutor
  13. PATCH  /api/reports/admin/tutor/<tutor_id>/reject/    — Admin rejects tutor

Commission & Fine formula (EXACT — per DATABASE_SCHEMA.md & CONTEXT.md):
  commission          = total_paid * 0.30
  tutor_base_share    = total_paid * 0.70
  fine_amount         = tutor_base_share * (fine_percentage / 100)
  tutor_final_payout  = tutor_base_share - fine_amount
  student_refund      = fine_amount
  admin_commission    = commission  (always unchanged)

Rules:
  - NO automatic fine/suspension — admin acts manually only.
  - All amounts in NPR.
  - JWT role-based access enforced on every endpoint.
  - Standard response envelope: {success, message, data} / {success, message, errors}.
"""

from decimal import Decimal

from django.core.mail import send_mail
from django.core.paginator import EmptyPage, PageNotAnInteger, Paginator
from django.db.models import Sum
from django.conf import settings

from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Student, Tutor, User
from booking.models import Booking, Session
from payments.models import Payment, Payout
from reports.models import LatenessReport, RescheduleRequest
from reports.serializers import (
    AdminReportActionSerializer,
    AdminRescheduleActionSerializer,
    AdminTutorRejectSerializer,
    CreateRescheduleRequestSerializer,
    FileLatenessReportSerializer,
    FileTutorReportSerializer,
    FineConfirmSerializer,
    FinePreviewSerializer,
    LatenessReportSerializer,
    RescheduleRequestSerializer,
    TutorVerificationSerializer,
)


# ─── Permission Helpers ───────────────────────────────────────────────────────

def _require_student(request):
    """Return (Student, None) or (None, Response) if not a student."""
    if request.user.role != 'student':
        return None, Response(
            {'success': False, 'message': 'Only students can access this endpoint.'},
            status=status.HTTP_403_FORBIDDEN,
        )
    try:
        return request.user.student_profile, None
    except Student.DoesNotExist:
        return None, Response(
            {'success': False, 'message': 'Student profile not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )


def _require_tutor(request):
    """Return (Tutor, None) or (None, Response) if not a tutor."""
    if request.user.role != 'tutor':
        return None, Response(
            {'success': False, 'message': 'Only tutors can access this endpoint.'},
            status=status.HTTP_403_FORBIDDEN,
        )
    try:
        return request.user.tutor_profile, None
    except Tutor.DoesNotExist:
        return None, Response(
            {'success': False, 'message': 'Tutor profile not found.'},
            status=status.HTTP_404_NOT_FOUND,
        )


def _require_admin(request):
    """Return error Response if not admin, else None."""
    if request.user.role != 'admin':
        return Response(
            {'success': False, 'message': 'Admin access required.'},
            status=status.HTTP_403_FORBIDDEN,
        )
    return None


def _paginate(queryset, request, serializer_class):
    """Paginate a queryset and return a Response with pagination metadata."""
    page      = request.query_params.get('page', 1)
    page_size = request.query_params.get('page_size', 20)
    try:
        page_size = int(page_size)
    except ValueError:
        page_size = 20

    paginator = Paginator(queryset, page_size)
    try:
        page_obj = paginator.page(page)
    except PageNotAnInteger:
        page_obj = paginator.page(1)
    except EmptyPage:
        page_obj = paginator.page(paginator.num_pages)

    serializer = serializer_class(page_obj.object_list, many=True)
    return Response({
        'success': True,
        'data': {
            'count':    paginator.count,
            'pages':    paginator.num_pages,
            'page':     page_obj.number,
            'results':  serializer.data,
        },
    }, status=status.HTTP_200_OK)


# ─── Fine Formula Helper ──────────────────────────────────────────────────────

def _calculate_fine(total_paid: Decimal, fine_percentage: int) -> dict:
    """
    Apply EXACT commission & fine formula from DATABASE_SCHEMA.md / CONTEXT.md.

    commission         = total_paid * 0.30
    tutor_base_share   = total_paid * 0.70
    fine_amount        = tutor_base_share * (fine_percentage / 100)
    tutor_final_payout = tutor_base_share - fine_amount
    student_refund     = fine_amount
    admin_commission   = commission  (unchanged)
    """
    commission         = total_paid * Decimal('0.30')
    tutor_base_share   = total_paid * Decimal('0.70')
    fine_amount        = tutor_base_share * (Decimal(fine_percentage) / Decimal('100'))
    tutor_final_payout = tutor_base_share - fine_amount
    student_refund     = fine_amount

    return {
        'total_paid':          round(total_paid, 2),
        'platform_commission': round(commission, 2),
        'tutor_base_share':    round(tutor_base_share, 2),
        'fine_percentage':     fine_percentage,
        'fine_amount':         round(fine_amount, 2),
        'tutor_final_payout':  round(tutor_final_payout, 2),
        'student_refund':      round(student_refund, 2),
    }


# ═══════════════════════════════════════════════════════════════════════════════
# 1. Student Files Lateness Report
# POST /api/reports/lateness/file/
# ═══════════════════════════════════════════════════════════════════════════════

class StudentFileLatenessReportView(APIView):
    """
    Student files a lateness / no-show report against a tutor.
    Auth: Student JWT required.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        student, err = _require_student(request)
        if err:
            return err

        serializer = FileLatenessReportSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        # Fetch session and verify it belongs to this student's booking
        try:
            session = Session.objects.select_related('booking__student').get(
                id=data['session_id']
            )
        except Session.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Session not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        if session.booking.student != student:
            return Response(
                {'success': False, 'message': 'You can only report on your own sessions.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        # One report per session per student
        if LatenessReport.objects.filter(
            session=session,
            reported_by=request.user,
            reporter_role='student',
        ).exists():
            return Response(
                {'success': False, 'message': 'You have already filed a report for this session.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # reported_against = tutor's user
        tutor_user = session.booking.tutor.user

        LatenessReport.objects.create(
            session          = session,
            reported_by      = request.user,
            reported_against = tutor_user,
            reporter_role    = LatenessReport.ReporterRole.STUDENT,
            delay_range      = data['delay_range'],
            description      = data['description'],
        )

        return Response(
            {'success': True, 'message': 'Lateness report filed successfully.'},
            status=status.HTTP_201_CREATED,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 2. Tutor Files Student Report
# POST /api/reports/student/file/
# ═══════════════════════════════════════════════════════════════════════════════

class TutorFileStudentReportView(APIView):
    """
    Tutor files a lateness / no-show report against a student.
    Auth: Tutor JWT required.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        tutor, err = _require_tutor(request)
        if err:
            return err

        serializer = FileTutorReportSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        # Fetch session and verify it belongs to this tutor's booking
        try:
            session = Session.objects.select_related('booking__tutor').get(
                id=data['session_id']
            )
        except Session.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Session not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        if session.booking.tutor != tutor:
            return Response(
                {'success': False, 'message': 'You can only report on your own sessions.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        # One report per session per tutor
        if LatenessReport.objects.filter(
            session=session,
            reported_by=request.user,
            reporter_role='tutor',
        ).exists():
            return Response(
                {'success': False, 'message': 'You have already filed a report for this session.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # reported_against = student's user
        student_user = session.booking.student.user

        LatenessReport.objects.create(
            session          = session,
            reported_by      = request.user,
            reported_against = student_user,
            reporter_role    = LatenessReport.ReporterRole.TUTOR,
            delay_range      = data['delay_range'],
            description      = data['description'],
        )

        return Response(
            {'success': True, 'message': 'Student report filed successfully.'},
            status=status.HTTP_201_CREATED,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 3. Admin View All Reports
# GET /api/reports/admin/all/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminAllReportsView(APIView):
    """
    Admin views all lateness reports with optional filters.
    Auth: Admin JWT required.
    Query params: reporter_role, admin_action, page, page_size
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        err = _require_admin(request)
        if err:
            return err

        qs = LatenessReport.objects.select_related(
            'session__booking',
            'reported_by',
            'reported_against',
        ).order_by('-created_at')

        reporter_role = request.query_params.get('reporter_role')
        if reporter_role:
            qs = qs.filter(reporter_role=reporter_role)

        admin_action = request.query_params.get('admin_action')
        if admin_action:
            qs = qs.filter(admin_action=admin_action)

        return _paginate(qs, request, LatenessReportSerializer)


# ═══════════════════════════════════════════════════════════════════════════════
# 4. Admin Take Action on Report
# PATCH /api/reports/admin/<report_id>/action/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminReportActionView(APIView):
    """
    Admin takes action on a lateness report.
    Auth: Admin JWT required.
    Actions: no_action | warning | fined | suspended | removed

    Side effects (manual — NOT automatic):
      warning   → increment warning_count on tutor/student
      suspended → set is_suspended = True on tutor/student
      removed   → set is_active = False on User
      no_action → only update the report
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, report_id):
        err = _require_admin(request)
        if err:
            return err

        try:
            report = LatenessReport.objects.select_related(
                'reported_against',
                'session__booking__tutor__user',
                'session__booking__student__user',
            ).get(id=report_id)
        except LatenessReport.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Report not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = AdminReportActionSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        action        = serializer.validated_data['admin_action']
        target_user   = report.reported_against

        # Determine if target is student or tutor
        target_student = None
        target_tutor   = None

        if report.reporter_role == LatenessReport.ReporterRole.STUDENT:
            # Student reported the tutor → action affects the tutor
            try:
                target_tutor = target_user.tutor_profile
            except Tutor.DoesNotExist:
                pass
        else:
            # Tutor reported the student → action affects the student
            try:
                target_student = target_user.student_profile
            except Student.DoesNotExist:
                pass

        # Apply side effects based on action
        if action == 'warning':
            if target_tutor:
                target_tutor.warning_count += 1
                target_tutor.save(update_fields=['warning_count'])
            elif target_student:
                target_student.warning_count += 1
                target_student.save(update_fields=['warning_count'])
                
            from notifications.services import notify_warning_issued
            notify_warning_issued(target_user, report.description)

        elif action == 'suspended':
            if target_tutor:
                target_tutor.is_suspended = True
                target_tutor.save(update_fields=['is_suspended'])
            elif target_student:
                target_student.is_suspended = True
                target_student.save(update_fields=['is_suspended'])

            from notifications.services import notify_suspension
            notify_suspension(target_user)

        elif action == 'removed':
            target_user.is_active = False
            target_user.save(update_fields=['is_active'])

        # Update the report
        report.admin_action = action
        report.save(update_fields=['admin_action'])

        return Response({
            'success': True,
            'message': f'Action "{action}" applied successfully.',
            'data':    LatenessReportSerializer(report).data,
        }, status=status.HTTP_200_OK)


# ═══════════════════════════════════════════════════════════════════════════════
# 5. Admin Fine Live Preview
# POST /api/reports/admin/fine-preview/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminFinePreviewView(APIView):
    """
    Admin gets a live fine preview for a booking.
    Auth: Admin JWT required.
    fine_percentage options: 5, 10, 15, 20, 30, 50
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        err = _require_admin(request)
        if err:
            return err

        serializer = FinePreviewSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        try:
            payment = Payment.objects.get(
                booking_id=data['booking_id'],
                payment_status='completed',
            )
        except Payment.DoesNotExist:
            return Response(
                {'success': False, 'message': 'No completed payment found for this booking.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        breakdown = _calculate_fine(payment.amount, data['fine_percentage'])

        return Response(
            {'success': True, 'data': breakdown},
            status=status.HTTP_200_OK,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 6. Admin Confirm Fine
# POST /api/reports/admin/fine-confirm/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminFineConfirmView(APIView):
    """
    Admin confirms and applies a fine to the payout record.
    Auth: Admin JWT required.

    Updates Payout fields:
      fine_percentage, fine_amount, fine_reason, fine_imposed_by,
      student_refund, tutor_final_payout, payout_status = fined
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        err = _require_admin(request)
        if err:
            return err

        serializer = FineConfirmSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        try:
            payment = Payment.objects.get(
                booking_id=data['booking_id'],
                payment_status='completed',
            )
        except Payment.DoesNotExist:
            return Response(
                {'success': False, 'message': 'No completed payment found for this booking.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        try:
            payout = Payout.objects.get(booking_id=data['booking_id'])
        except Payout.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Payout record not found for this booking.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        breakdown = _calculate_fine(payment.amount, data['fine_percentage'])

        # Apply fine to payout — fine_percentage stored as decimal fraction
        payout.fine_percentage    = Decimal(data['fine_percentage']) / Decimal('100')
        payout.fine_amount        = breakdown['fine_amount']
        payout.fine_reason        = data['fine_reason']
        payout.fine_imposed_by    = request.user
        payout.student_refund     = breakdown['student_refund']
        payout.tutor_final_payout = breakdown['tutor_final_payout']
        payout.payout_status      = Payout.PayoutStatus.FINED
        payout.save(update_fields=[
            'fine_percentage', 'fine_amount', 'fine_reason', 'fine_imposed_by',
            'student_refund', 'tutor_final_payout', 'payout_status',
        ])
        
        from notifications.services import notify_fine_applied
        notify_fine_applied(payout.tutor, payout.fine_amount, payout.fine_reason)

        # Update any existing fined reports for this booking's sessions
        LatenessReport.objects.filter(
            session__booking_id=data['booking_id']
        ).update(admin_action=LatenessReport.AdminAction.FINED)

        return Response({
            'success': True,
            'message': 'Fine applied successfully.',
            'data': {
                'payout_id':           payout.id,
                'total_paid':          float(payout.total_paid),
                'platform_commission': float(payout.commission_amount),
                'tutor_base_share':    float(payout.tutor_base_share),
                'fine_percentage':     data['fine_percentage'],
                'fine_amount':         float(payout.fine_amount),
                'tutor_final_payout':  float(payout.tutor_final_payout),
                'student_refund':      float(payout.student_refund),
                'payout_status':       payout.payout_status,
            },
        }, status=status.HTTP_200_OK)


# ═══════════════════════════════════════════════════════════════════════════════
# 7. Student Creates Reschedule Request
# POST /api/reports/reschedule/request/
# ═══════════════════════════════════════════════════════════════════════════════

class StudentRescheduleRequestView(APIView):
    """
    Student requests a reschedule for an accepted booking.
    Auth: Student JWT required.
    Rules:
      - Booking must be in 'accepted' status.
      - One pending reschedule request per booking at a time.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        student, err = _require_student(request)
        if err:
            return err

        serializer = CreateRescheduleRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data

        try:
            booking = Booking.objects.get(id=data['booking_id'], student=student)
        except Booking.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Booking not found or does not belong to you.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        if booking.booking_status != Booking.BookingStatus.ACCEPTED:
            return Response(
                {'success': False, 'message': 'Reschedule can only be requested for accepted bookings.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # One pending request per booking at a time
        if RescheduleRequest.objects.filter(
            booking=booking,
            status=RescheduleRequest.Status.PENDING,
        ).exists():
            return Response(
                {'success': False, 'message': 'A pending reschedule request already exists for this booking.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        RescheduleRequest.objects.create(
            booking      = booking,
            requested_by = student,
            reason       = data['reason'],
        )

        return Response(
            {'success': True, 'message': 'Reschedule request submitted successfully.'},
            status=status.HTTP_201_CREATED,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 8. Admin View Reschedule Requests
# GET /api/reports/admin/reschedule/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminRescheduleListView(APIView):
    """
    Admin views all reschedule requests (paginated).
    Auth: Admin JWT required.
    Query params: status (pending/approved/rejected), page, page_size
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        err = _require_admin(request)
        if err:
            return err

        qs = RescheduleRequest.objects.select_related(
            'booking', 'requested_by__user',
        ).order_by('-created_at')

        status_filter = request.query_params.get('status')
        if status_filter:
            qs = qs.filter(status=status_filter)

        return _paginate(qs, request, RescheduleRequestSerializer)


# ═══════════════════════════════════════════════════════════════════════════════
# 9. Admin Action on Reschedule Request
# PATCH /api/reports/admin/reschedule/<request_id>/action/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminRescheduleActionView(APIView):
    """
    Admin approves or rejects a reschedule request.
    Auth: Admin JWT required.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, request_id):
        err = _require_admin(request)
        if err:
            return err

        try:
            rr = RescheduleRequest.objects.select_related(
                'booking', 'requested_by__user',
            ).get(id=request_id)
        except RescheduleRequest.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Reschedule request not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = AdminRescheduleActionSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        rr.status = serializer.validated_data['status']
        rr.save(update_fields=['status'])

        return Response({
            'success': True,
            'message': f'Reschedule request {rr.status}.',
            'data':    RescheduleRequestSerializer(rr).data,
        }, status=status.HTTP_200_OK)


# ═══════════════════════════════════════════════════════════════════════════════
# 10. Admin Dashboard Summary
# GET /api/admin/dashboard/summary/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminDashboardSummaryView(APIView):
    """
    Returns platform-wide summary statistics for the admin dashboard.
    Auth: Admin JWT required.

    Response exactly as defined in API_RULES.md:
      total_students, total_tutors, active_bookings, total_platform_earnings
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        err = _require_admin(request)
        if err:
            return err

        total_students = Student.objects.count()
        total_tutors   = Tutor.objects.count()

        # Active bookings = bookings in pending or accepted status
        active_bookings = Booking.objects.filter(
            booking_status__in=[
                Booking.BookingStatus.PENDING,
                Booking.BookingStatus.ACCEPTED,
            ]
        ).count()

        # Total platform earnings = sum of all commission_amount from all payouts
        earnings_result = Payout.objects.aggregate(
            total=Sum('commission_amount')
        )
        total_platform_earnings = earnings_result['total'] or Decimal('0.00')

        return Response({
            'success': True,
            'data': {
                'total_students':          total_students,
                'total_tutors':            total_tutors,
                'active_bookings':         active_bookings,
                'total_platform_earnings': float(round(total_platform_earnings, 2)),
            },
        }, status=status.HTTP_200_OK)


# ═══════════════════════════════════════════════════════════════════════════════
# 11. Tutor Verification Panel
# GET /api/reports/admin/tutor-verification/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminTutorVerificationView(APIView):
    """
    Admin views all tutors with is_verified = False (pending verification).
    Auth: Admin JWT required.
    Returns: full_name, email, domain, documents uploaded, registered_at
    Paginated.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        err = _require_admin(request)
        if err:
            return err

        qs = Tutor.objects.filter(is_verified=False).select_related(
            'user'
        ).prefetch_related('documents').order_by('-user__created_at')

        return _paginate(qs, request, TutorVerificationSerializer)


# ═══════════════════════════════════════════════════════════════════════════════
# 12. Admin Approve Tutor
# PATCH /api/reports/admin/tutor/<tutor_id>/approve/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminApproveTutorView(APIView):
    """
    Admin approves a tutor — sets is_verified = True.
    Sends email notification to the tutor.
    Auth: Admin JWT required.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, tutor_id):
        err = _require_admin(request)
        if err:
            return err

        try:
            tutor = Tutor.objects.select_related('user').get(id=tutor_id)
        except Tutor.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Tutor not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        tutor.is_verified = True
        tutor.save(update_fields=['is_verified'])

        from notifications.services import notify_tutor_approved
        notify_tutor_approved(tutor)

        # Send approval email notification
        try:
            send_mail(
                subject='UniAssist — Your Account Has Been Approved',
                message=(
                    f'Hello {tutor.user.full_name},\n\n'
                    'Congratulations! Your tutor profile on UniAssist has been '
                    'reviewed and approved by our admin team.\n\n'
                    'You can now start accepting bookings from students.\n\n'
                    'Welcome to UniAssist!\n\n'
                    '— The UniAssist Team'
                ),
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[tutor.user.email],
                fail_silently=True,
            )
        except Exception:
            pass  # Email failure must not break the API response

        return Response({
            'success': True,
            'message': 'Tutor approved and notified via email.',
            'data': {
                'tutor_id':    tutor.id,
                'full_name':   tutor.user.full_name,
                'email':       tutor.user.email,
                'domain':      tutor.domain,
                'is_verified': tutor.is_verified,
            },
        }, status=status.HTTP_200_OK)


# ═══════════════════════════════════════════════════════════════════════════════
# 13. Admin Reject Tutor
# PATCH /api/reports/admin/tutor/<tutor_id>/reject/
# ═══════════════════════════════════════════════════════════════════════════════

class AdminRejectTutorView(APIView):
    """
    Admin rejects a tutor — sets is_verified = False.
    Sends email notification with rejection reason.
    Auth: Admin JWT required.
    """
    permission_classes = [IsAuthenticated]

    def patch(self, request, tutor_id):
        err = _require_admin(request)
        if err:
            return err

        try:
            tutor = Tutor.objects.select_related('user').get(id=tutor_id)
        except Tutor.DoesNotExist:
            return Response(
                {'success': False, 'message': 'Tutor not found.'},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = AdminTutorRejectSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {'success': False, 'message': 'Invalid input.', 'errors': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST,
            )

        rejection_reason = serializer.validated_data['rejection_reason']

        tutor.is_verified = False
        tutor.save(update_fields=['is_verified'])

        from notifications.services import notify_tutor_rejected
        notify_tutor_rejected(tutor, rejection_reason)

        # Send rejection email with reason
        try:
            send_mail(
                subject='UniAssist — Tutor Application Update',
                message=(
                    f'Hello {tutor.user.full_name},\n\n'
                    'Thank you for applying to become a tutor on UniAssist. '
                    'After reviewing your profile and documents, our admin team '
                    'was unable to approve your application at this time.\n\n'
                    f'Reason: {rejection_reason}\n\n'
                    'Please update your profile or documents and reapply.\n\n'
                    '— The UniAssist Team'
                ),
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[tutor.user.email],
                fail_silently=True,
            )
        except Exception:
            pass  # Email failure must not break the API response

        return Response(
            {'success': True, 'message': 'Tutor rejected and notified via email.'},
            status=status.HTTP_200_OK,
        )
