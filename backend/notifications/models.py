from django.db import models
from accounts.models import User

class Notification(models.Model):
    class NotificationType(models.TextChoices):
        BOOKING_ACCEPTED = 'booking_accepted', 'Booking Accepted'
        BOOKING_REJECTED = 'booking_rejected', 'Booking Rejected'
        PAYMENT_CONFIRMED = 'payment_confirmed', 'Payment Confirmed'
        SESSION_REMINDER = 'session_reminder', 'Session Reminder'
        REPORT_FILED = 'report_filed', 'Report Filed'
        WARNING_ISSUED = 'warning_issued', 'Warning Issued'
        FINE_APPLIED = 'fine_applied', 'Fine Applied'
        PAYOUT_RELEASED = 'payout_released', 'Payout Released'
        SUSPENSION_NOTICE = 'suspension_notice', 'Suspension Notice'
        TUTOR_APPROVED = 'tutor_approved', 'Tutor Approved'
        TUTOR_REJECTED = 'tutor_rejected', 'Tutor Rejected'
        GENERAL = 'general', 'General'

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='notifications')
    title = models.CharField(max_length=255)
    message = models.TextField()
    notification_type = models.CharField(max_length=30, choices=NotificationType.choices, default=NotificationType.GENERAL)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'notification'
        verbose_name = 'Notification'
        verbose_name_plural = 'Notifications'

    def __str__(self):
        return f'{self.title} to {self.user.email}'
