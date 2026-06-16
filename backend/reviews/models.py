"""
UniAssist — reviews/models.py

Implements:
  • Review — one review per booking (OneToOne on booking)

Schema ref: DATABASE_SCHEMA.md → Review

Rules (from CONTEXT.md & API_RULES.md):
  - Only the student who made the booking can submit a review.
  - Booking must have booking_status = 'completed' before review is allowed.
  - One review per booking — enforced via OneToOneField on booking.
  - Rating 1–5 (INT). Sub-ratings optional but also 1–5 if provided.
  - After saving, tutor average_rating and punctuality_score are recalculated
    automatically via the post_save signal defined at the bottom of this file.
"""

from django.db import models
from django.db.models import Avg
from django.db.models.signals import post_save
from django.dispatch import receiver

from accounts.models import Student, Tutor
from booking.models import Booking


# ─── Validator helper ─────────────────────────────────────────────────────────

def _rating_validators():
    """Return MinValueValidator and MaxValueValidator for 1–5 range."""
    from django.core.validators import MinValueValidator, MaxValueValidator
    return [MinValueValidator(1), MaxValueValidator(5)]


# ─── Review ──────────────────────────────────────────────────────────────────

class Review(models.Model):
    """
    Student review of a tutor after a completed session.

    Constraints:
      • booking is OneToOne — one review per booking only.
      • rating is mandatory (1–5).
      • Sub-ratings (knowledge, teaching, communication, punctuality) are
        optional (null=True, blank=True) but must be 1–5 if supplied.
    """

    booking = models.OneToOneField(
        Booking,
        on_delete=models.CASCADE,
        related_name='review',
        help_text='The completed booking this review belongs to.',
    )
    student = models.ForeignKey(
        Student,
        on_delete=models.CASCADE,
        related_name='reviews_given',
        help_text='The student who wrote this review.',
    )
    tutor = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='reviews_received',
        help_text='The tutor being reviewed.',
    )

    # ── Main rating ───────────────────────────────────────────────────────────
    rating = models.IntegerField(
        validators=_rating_validators(),
        help_text='Overall rating from 1 (worst) to 5 (best).',
    )
    review_text = models.TextField(
        help_text='Written feedback from the student.',
    )

    # ── Sub-ratings (all optional) ────────────────────────────────────────────
    knowledge_rating = models.IntegerField(
        null=True,
        blank=True,
        validators=_rating_validators(),
        help_text='Subject knowledge rating (1–5, optional).',
    )
    teaching_rating = models.IntegerField(
        null=True,
        blank=True,
        validators=_rating_validators(),
        help_text='Teaching style rating (1–5, optional).',
    )
    communication_rating = models.IntegerField(
        null=True,
        blank=True,
        validators=_rating_validators(),
        help_text='Communication skills rating (1–5, optional).',
    )
    punctuality_rating = models.IntegerField(
        null=True,
        blank=True,
        validators=_rating_validators(),
        help_text='Punctuality rating (1–5, optional).',
    )

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'review'
        verbose_name = 'Review'
        verbose_name_plural = 'Reviews'
        ordering = ['-created_at']

    def __str__(self):
        return (
            f'Review #{self.id} — '
            f'{self.student.user.full_name} → {self.tutor.user.full_name} '
            f'[{self.rating}/5]'
        )


# ─── Signal: update tutor averages after every review save ───────────────────

@receiver(post_save, sender=Review)
def update_tutor_average_rating(sender, instance, **kwargs):
    """
    Recalculate and persist two Tutor aggregates whenever a Review is saved:
      1. punctuality_score  — average of all punctuality_rating values for
                              reviews where punctuality_rating is not null.
                              Stays at its current value if no ratings exist.
      2. (avg overall)      — stored externally; callers read it via annotation.

    Note: Tutor model does not have a dedicated average_rating field per
    DATABASE_SCHEMA.md.  The overall average is always computed live using
    Tutor.reviews_received.aggregate(Avg('rating')).  Only punctuality_score
    is persisted because it is an explicit field in the schema.
    """
    tutor = instance.tutor

    # Recalculate punctuality_score from all reviews that provided it
    punctuality_avg = (
        Review.objects
        .filter(tutor=tutor, punctuality_rating__isnull=False)
        .aggregate(avg=Avg('punctuality_rating'))['avg']
    )
    if punctuality_avg is not None:
        tutor.punctuality_score = round(punctuality_avg, 2)
        tutor.save(update_fields=['punctuality_score'])
