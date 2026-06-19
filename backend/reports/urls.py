"""
UniAssist — reports/urls.py

URL patterns for Phase 7 — Reports & Admin Module.

All report endpoints are under /api/reports/
Dashboard summary is under /api/admin/  (as defined in API_RULES.md)
"""

from django.urls import path

from reports.views import (
    AdminAllReportsView,
    AdminApproveTutorView,
    AdminDashboardSummaryView,
    AdminFineConfirmView,
    AdminFinePreviewView,
    AdminRejectTutorView,
    AdminRescheduleActionView,
    AdminRescheduleListView,
    AdminReportActionView,
    AdminTutorVerificationView,
    StudentFileLatenessReportView,
    StudentRescheduleRequestView,
    TutorFileStudentReportView,
)

app_name = 'reports'

urlpatterns = [
    # ── Student endpoints ───────────────────────────────────────────────────
    # Student files lateness report against tutor
    path('lateness/file/', StudentFileLatenessReportView.as_view(), name='student-file-lateness'),

    # Student files report against student (tutor endpoint)
    path('student/file/', TutorFileStudentReportView.as_view(), name='tutor-file-student'),

    # Student requests reschedule
    path('reschedule/request/', StudentRescheduleRequestView.as_view(), name='student-reschedule-request'),

    # ── Admin report endpoints ───────────────────────────────────────────────
    # Admin views all lateness reports
    path('admin/all/', AdminAllReportsView.as_view(), name='admin-all-reports'),

    # Admin takes action on a specific report
    path('admin/<int:report_id>/action/', AdminReportActionView.as_view(), name='admin-report-action'),

    # Admin fine live preview
    path('admin/fine-preview/', AdminFinePreviewView.as_view(), name='admin-fine-preview'),

    # Admin confirms and applies fine
    path('admin/fine-confirm/', AdminFineConfirmView.as_view(), name='admin-fine-confirm'),

    # ── Admin reschedule endpoints ───────────────────────────────────────────
    # Admin views all reschedule requests
    path('admin/reschedule/', AdminRescheduleListView.as_view(), name='admin-reschedule-list'),

    # Admin approves / rejects a reschedule request
    path('admin/reschedule/<int:request_id>/action/', AdminRescheduleActionView.as_view(), name='admin-reschedule-action'),

    # ── Tutor verification endpoints ─────────────────────────────────────────
    # Admin views tutors pending verification
    path('admin/tutor-verification/', AdminTutorVerificationView.as_view(), name='admin-tutor-verification'),

    # Admin approves a tutor
    path('admin/tutor/<int:tutor_id>/approve/', AdminApproveTutorView.as_view(), name='admin-approve-tutor'),

    # Admin rejects a tutor
    path('admin/tutor/<int:tutor_id>/reject/', AdminRejectTutorView.as_view(), name='admin-reject-tutor'),
]
