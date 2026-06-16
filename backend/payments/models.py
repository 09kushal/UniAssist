"""
UniAssist — payments/models.py

Implements:
  • Payment — tracks eSewa payment for a booking.
  • Payout  — admin-controlled payout to tutor after session completion.

Schema ref: DATABASE_SCHEMA.md → Payment, Payout

Commission & Fine formula (EXACT, per DATABASE_SCHEMA.md):
  Platform commission  = session_price * 0.30
  Tutor base share     = session_price * 0.70
  Fine amount          = tutor_base_share * fine_percentage
  Tutor final payout   = tutor_base_share - fine_amount
  Student refund       = fine_amount  (equals tutor fine by schema)

Rules (from CONTEXT.md & API_RULES.md):
  - officially_scheduled = TRUE only after payment_status = completed.
  - Payout held until admin manually releases (never automatic).
  - 30% platform commission always deducted first.
  - Fine always calculated from tutor's 70% share only.
"""

from django.db import models

from accounts.models import Tutor, User
from booking.models import Booking


# ─── Payment ──────────────────────────────────────────────────────────────────

class Payment(models.Model):
    """
    Records the eSewa payment attempt for a booking.
    Schema ref: DATABASE_SCHEMA.md → Payment

    State machine:
      pending   → completed (on eSewa callback success)
      pending   → failed    (on eSewa callback failure)
    """

    class PaymentStatus(models.TextChoices):
        PENDING   = 'pending',   'Pending'
        COMPLETED = 'completed', 'Completed'
        FAILED    = 'failed',    'Failed'

    booking = models.OneToOneField(
        Booking,
        on_delete=models.CASCADE,
        related_name='payment',
        help_text='The booking this payment belongs to.',
    )
    amount = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        help_text='Amount charged to student (NPR). Equals tutor pricing_per_session.',
    )
    payment_status = models.CharField(
        max_length=10,
        choices=PaymentStatus.choices,
        default=PaymentStatus.PENDING,
        help_text='Current payment state.',
    )
    esewa_ref_id = models.CharField(
        max_length=255,
        blank=True,
        null=True,
        help_text='Transaction reference ID returned by eSewa on success.',
    )
    paid_at = models.DateTimeField(
        blank=True,
        null=True,
        help_text='Timestamp when payment was confirmed by eSewa.',
    )

    class Meta:
        db_table = 'payment'
        verbose_name = 'Payment'
        verbose_name_plural = 'Payments'
        ordering = ['-id']

    def __str__(self):
        return (
            f'Payment #{self.id} — Booking #{self.booking_id} '
            f'NPR {self.amount} [{self.payment_status}]'
        )


# ─── Payout ───────────────────────────────────────────────────────────────────

class Payout(models.Model):
    """
    Admin-controlled payout record for a tutor after a completed session.
    Schema ref: DATABASE_SCHEMA.md → Payout

    Payout is held until admin manually calls the release API.
    Formula is applied at release time using current fine data.

    Fields that match schema exactly:
      total_paid           = booking session price (what student paid)
      commission_amount    = total_paid * 0.30
      tutor_base_share     = total_paid * 0.70
      fine_percentage      = admin-set fine percentage (e.g. 0.20 for 20%)
      fine_amount          = tutor_base_share * fine_percentage
      student_refund       = fine_amount (equals tutor fine)
      tutor_final_payout   = tutor_base_share - fine_amount
    """

    class PayoutStatus(models.TextChoices):
        HELD     = 'held',     'Held'
        RELEASED = 'released', 'Released'
        FINED    = 'fined',    'Fined'

    tutor = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='payouts',
        help_text='The tutor receiving this payout.',
    )
    booking = models.OneToOneField(
        Booking,
        on_delete=models.CASCADE,
        related_name='payout',
        help_text='The booking this payout is linked to.',
    )

    # Financial breakdown (all NPR)
    total_paid = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        help_text='Total amount paid by student (NPR). Source of all calculations.',
    )
    commission_amount = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        help_text='Platform commission = total_paid * 0.30',
    )
    tutor_base_share = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        help_text='Tutor base share = total_paid * 0.70',
    )
    fine_percentage = models.DecimalField(
        max_digits=5,
        decimal_places=2,
        default=0.00,
        help_text='Fine percentage applied to tutor base share (e.g. 0.20 = 20%).',
    )
    fine_amount = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        default=0.00,
        help_text='Fine amount = tutor_base_share * fine_percentage.',
    )
    fine_reason = models.CharField(
        max_length=500,
        blank=True,
        null=True,
        help_text='Admin-provided reason for the fine.',
    )
    fine_imposed_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='fines_imposed',
        help_text='Admin user who imposed the fine.',
    )
    student_refund = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        default=0.00,
        help_text='Amount to refund to student. Equals fine_amount per schema.',
    )
    tutor_final_payout = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        help_text='Net payout to tutor = tutor_base_share - fine_amount.',
    )

    payout_status = models.CharField(
        max_length=10,
        choices=PayoutStatus.choices,
        default=PayoutStatus.HELD,
        help_text='Current payout state. Admin must explicitly release.',
    )
    released_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='payouts_released',
        help_text='Admin user who released the payout.',
    )
    released_at = models.DateTimeField(
        blank=True,
        null=True,
        help_text='Timestamp when payout was released by admin.',
    )

    class Meta:
        db_table = 'payout'
        verbose_name = 'Payout'
        verbose_name_plural = 'Payouts'
        ordering = ['-id']

    def __str__(self):
        return (
            f'Payout #{self.id} — {self.tutor.user.full_name} '
            f'NPR {self.tutor_final_payout} [{self.payout_status}]'
        )
