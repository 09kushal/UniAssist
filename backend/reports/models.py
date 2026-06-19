"""
UniAssist — reports/models.py

Implements:
  • LatenessReport   — filed by student or tutor against the other party
  • RescheduleRequest — filed by student to request booking reschedule

Schema ref: DATABASE_SCHEMA.md → LatenessReport, RescheduleRequest

Rules (from CONTEXT.md):
  - NO automatic suspension or fine system — admin acts manually.
  - Imports Session from booking.models, Booking from booking.models.
  - Imports User, Student from accounts.models.
  - __init__.py must remain empty (mysqlclient only — no pymysql).
"""

from django.db import models

from accounts.models import Student, User
from booking.models import Booking, Session


# ─── LatenessReport ──────────────────────────────────────────────────────────

class LatenessReport(models.Model):
    """
    A report filed by a student or tutor about lateness / no-show in a session.
    Schema ref: DATABASE_SCHEMA.md → LatenessReport

    reporter_role  : who filed the report (student or tutor)
    admin_action   : admin's response — default pending, set manually
    """

    class ReporterRole(models.TextChoices):
        STUDENT = 'student', 'Student'
        TUTOR   = 'tutor',   'Tutor'

    class DelayRange(models.TextChoices):
        FIVE_15     = '5_15min',   '5–15 Minutes'
        FIFTEEN_30  = '15_30min',  '15–30 Minutes'
        THIRTY_PLUS = '30_plus',   '30+ Minutes'
        NO_SHOW     = 'no_show',   'No Show'

    class AdminAction(models.TextChoices):
        PENDING    = 'pending',    'Pending'
        NO_ACTION  = 'no_action',  'No Action'
        WARNING    = 'warning',    'Warning Issued'
        FINED      = 'fined',      'Fined'
        SUSPENDED  = 'suspended',  'Suspended'
        REMOVED    = 'removed',    'Removed'

    session = models.ForeignKey(
        Session,
        on_delete=models.CASCADE,
        related_name='lateness_reports',
        help_text='The session this report is about.',
    )
    reported_by = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='reports_filed',
        help_text='The user who filed this report.',
    )
    reported_against = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='reports_received',
        help_text='The user being reported.',
    )
    reporter_role = models.CharField(
        max_length=10,
        choices=ReporterRole.choices,
        help_text='Role of the person filing the report.',
    )
    delay_range = models.CharField(
        max_length=10,
        choices=DelayRange.choices,
        help_text='How late was the other party?',
    )
    description = models.TextField(
        help_text='Detailed description of the lateness / no-show incident.',
    )
    admin_action = models.CharField(
        max_length=10,
        choices=AdminAction.choices,
        default=AdminAction.PENDING,
        help_text='Admin action taken on this report. Default: pending.',
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table            = 'lateness_report'
        verbose_name        = 'Lateness Report'
        verbose_name_plural = 'Lateness Reports'
        ordering            = ['-created_at']

    def __str__(self):
        return (
            f'Report #{self.id} — {self.reporter_role} filed against '
            f'{self.reported_against.full_name} [{self.admin_action}]'
        )


# ─── RescheduleRequest ────────────────────────────────────────────────────────

class RescheduleRequest(models.Model):
    """
    A reschedule request filed by a student for an accepted booking.
    Schema ref: DATABASE_SCHEMA.md → RescheduleRequest

    Rules:
      - Only students can create these.
      - Booking must be in 'accepted' status.
      - One pending request per booking at a time.
    """

    class Status(models.TextChoices):
        PENDING  = 'pending',  'Pending'
        APPROVED = 'approved', 'Approved'
        REJECTED = 'rejected', 'Rejected'

    booking = models.ForeignKey(
        Booking,
        on_delete=models.CASCADE,
        related_name='reschedule_requests',
        help_text='The booking for which reschedule is requested.',
    )
    requested_by = models.ForeignKey(
        Student,
        on_delete=models.CASCADE,
        related_name='reschedule_requests',
        help_text='The student requesting reschedule.',
    )
    reason = models.TextField(
        help_text='Reason provided by the student for the reschedule request.',
    )
    status = models.CharField(
        max_length=10,
        choices=Status.choices,
        default=Status.PENDING,
        help_text='Current status of the reschedule request.',
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table            = 'reschedule_request'
        verbose_name        = 'Reschedule Request'
        verbose_name_plural = 'Reschedule Requests'
        ordering            = ['-created_at']

    def __str__(self):
        return (
            f'RescheduleRequest #{self.id} — '
            f'Booking #{self.booking_id} [{self.status}]'
        )
