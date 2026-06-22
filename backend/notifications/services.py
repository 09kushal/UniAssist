from notifications.models import Notification
from notifications.firebase import send_fcm_notification

def create_notification(user, title, message, notification_type):
    notification = Notification.objects.create(
        user=user,
        title=title,
        message=message,
        notification_type=notification_type
    )
    if user.device_token:
        send_fcm_notification(user.device_token, title, message)
    return notification

def notify_booking_accepted(booking):
    create_notification(
        user=booking.student.user,
        title="Booking Accepted",
        message=f"Your booking with {booking.tutor.user.full_name} has been accepted.",
        notification_type=Notification.NotificationType.BOOKING_ACCEPTED
    )

def notify_booking_rejected(booking):
    create_notification(
        user=booking.student.user,
        title="Booking Rejected",
        message=f"Your booking with {booking.tutor.user.full_name} has been rejected.",
        notification_type=Notification.NotificationType.BOOKING_REJECTED
    )

def notify_payment_confirmed(booking):
    # Notify student
    create_notification(
        user=booking.student.user,
        title="Payment Confirmed",
        message=f"Payment confirmed. Your session with {booking.tutor.user.full_name} is officially scheduled.",
        notification_type=Notification.NotificationType.PAYMENT_CONFIRMED
    )
    # Notify tutor
    create_notification(
        user=booking.tutor.user,
        title="Payment Confirmed",
        message=f"Payment confirmed. Your session with {booking.student.user.full_name} is officially scheduled.",
        notification_type=Notification.NotificationType.PAYMENT_CONFIRMED
    )

def notify_payout_released(payout):
    create_notification(
        user=payout.tutor.user,
        title="Payout Released",
        message=f"Your payout of NPR {payout.tutor_final_payout} has been released.",
        notification_type=Notification.NotificationType.PAYOUT_RELEASED
    )

def notify_warning_issued(user, reason):
    create_notification(
        user=user,
        title="Warning Issued",
        message=f"A warning has been issued on your account: {reason}",
        notification_type=Notification.NotificationType.WARNING_ISSUED
    )

def notify_fine_applied(tutor, fine_amount, reason):
    create_notification(
        user=tutor.user,
        title="Fine Applied",
        message=f"A fine of NPR {fine_amount} has been applied: {reason}",
        notification_type=Notification.NotificationType.FINE_APPLIED
    )

def notify_suspension(user):
    create_notification(
        user=user,
        title="Account Suspended",
        message="Your account has been temporarily suspended.",
        notification_type=Notification.NotificationType.SUSPENSION_NOTICE
    )

def notify_tutor_approved(tutor):
    create_notification(
        user=tutor.user,
        title="Profile Approved",
        message="Congratulations! Your tutor profile has been approved. You are now live on UniAssist.",
        notification_type=Notification.NotificationType.TUTOR_APPROVED
    )

def notify_tutor_rejected(tutor, reason):
    create_notification(
        user=tutor.user,
        title="Profile Rejected",
        message=f"Your tutor profile was rejected: {reason}",
        notification_type=Notification.NotificationType.TUTOR_REJECTED
    )
