"""
UniAssist — booking/models.py

Implements:
  • Booking — links Student ↔ Tutor with status tracking.

Schema ref: DATABASE_SCHEMA.md → Booking

Booking flow (STRICT ORDER per API_RULES.md):
  1. Student sends request   → booking_status = pending
  2. Tutor accepts/rejects   → booking_status = accepted | rejected
  3. Student pays via eSewa  → payment_status = completed      (Phase 5)
  4. officially_scheduled    → TRUE only after payment          (Phase 5)
  5. Sessions via Jitsi Meet                                     (Phase 9)

IMPORTANT:
  - officially_scheduled MUST default to False.
  - Never set officially_scheduled = True in Phase 4.
  - booking_status 'cancelled' added for student cancellation flow.
"""

from django.db import models

from accounts.models import Student, Tutor, TutorAvailability


class Booking(models.Model):
    """
    Core booking entity linking a Student to a Tutor for a learning session.

    Fields beyond DATABASE_SCHEMA.md baseline:
      - subject_or_skill    : the topic/subject/skill the student wants to learn
      - proposed_date       : the date the student proposes for the session
      - proposed_start_time : start time proposed by student
      - proposed_end_time   : end time proposed by student
      - message             : optional note from student to tutor
      - rejection_reason    : optional note from tutor when rejecting
    """

    class BookingStatus(models.TextChoices):
        PENDING   = 'pending',   'Pending'
        ACCEPTED  = 'accepted',  'Accepted'
        REJECTED  = 'rejected',  'Rejected'
        CANCELLED = 'cancelled', 'Cancelled'
        COMPLETED = 'completed', 'Completed'  # set in Phase 5 after session

    student  = models.ForeignKey(
        Student,
        on_delete=models.CASCADE,
        related_name='bookings',
        help_text='The student who made the booking request.',
    )
    tutor    = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='booking_requests',
        help_text='The tutor who received the booking request.',
    )
    selected_slot = models.ForeignKey(
        TutorAvailability,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='bookings',
        help_text='Optional reference to the tutor availability slot selected.',
    )

    # Session details provided by the student at booking time
    subject_or_skill     = models.CharField(
        max_length=255,
        help_text='The subject or skill the student wants to learn.',
    )
    proposed_date        = models.DateField(
        help_text='The date proposed by the student for the session.',
    )
    proposed_start_time  = models.TimeField(
        help_text='Proposed session start time.',
    )
    proposed_end_time    = models.TimeField(
        help_text='Proposed session end time.',
    )
    message              = models.TextField(
        blank=True,
        default='',
        help_text='Optional message from the student to the tutor.',
    )

    # Status tracking
    booking_status       = models.CharField(
        max_length=10,
        choices=BookingStatus.choices,
        default=BookingStatus.PENDING,
        help_text='Current status of the booking.',
    )
    rejection_reason     = models.TextField(
        blank=True,
        default='',
        help_text='Reason provided by the tutor when rejecting the booking.',
    )

    # Phase 5 flag — NEVER set to True in Phase 4
    officially_scheduled = models.BooleanField(
        default=False,
        help_text=(
            'Set to True ONLY after eSewa payment is confirmed (Phase 5). '
            'Must remain False throughout Phase 4.'
        ),
    )

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table      = 'booking'
        verbose_name  = 'Booking'
        verbose_name_plural = 'Bookings'
        ordering      = ['-created_at']

    def __str__(self):
        return (
            f'Booking #{self.id} — '
            f'{self.student.user.full_name} → {self.tutor.user.full_name} '
            f'[{self.booking_status}]'
        )


class Session(models.Model):
    """
    Session entity representing the actual scheduled tutoring class.
    Schema ref: DATABASE_SCHEMA.md → Session
    """
    class SessionStatus(models.TextChoices):
        PENDING   = 'pending',   'Pending'
        COMPLETED = 'completed', 'Completed'
        CANCELLED = 'cancelled', 'Cancelled'

    booking = models.ForeignKey(
        Booking,
        on_delete=models.CASCADE,
        related_name='sessions',
        help_text='The booking associated with this session.'
    )
    scheduled_at = models.DateTimeField(
        help_text='The date and time when this session is scheduled to start.'
    )
    tutor_start_time = models.DateTimeField(
        null=True,
        blank=True,
        help_text='The exact time the tutor started the session.'
    )
    session_status = models.CharField(
        max_length=10,
        choices=SessionStatus.choices,
        default=SessionStatus.PENDING,
        help_text='Status of the session.'
    )
    officially_scheduled = models.BooleanField(
        default=False,
        help_text='Set to True only after eSewa payment is completed (Phase 5).'
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'session'
        verbose_name = 'Session'
        verbose_name_plural = 'Sessions'
        ordering = ['-scheduled_at']

    def __str__(self):
        return f'Session #{self.id} (Booking #{self.booking.id}) — {self.session_status}'

