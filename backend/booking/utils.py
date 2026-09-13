import datetime
from django.utils import timezone
from django.db.models import Q
from .models import Booking, Session

def expire_old_bookings():
    """
    Finds 'accepted' or 'scheduled' bookings that were scheduled more than 24 hours ago
    but never completed, and moves them to 'expired' status.

    Self-healing logic triggered on dashboard/list loads.
    """
    now = timezone.now()
    threshold = now - datetime.timedelta(hours=24)

    # 1. Bookings WITH sessions
    # Find accepted/scheduled bookings where the latest session's scheduled_at is past threshold
    # and no session is 'completed'.
    # We include 'scheduled' for legacy compatibility.
    bookings_with_sessions = Booking.objects.filter(
        Q(booking_status=Booking.BookingStatus.ACCEPTED) | Q(booking_status='scheduled'),
        sessions__isnull=False
    ).distinct()

    expired_count = 0

    for booking in bookings_with_sessions:
        # Check if any session is completed - if so, this booking shouldn't expire
        if booking.sessions.filter(session_status=Session.SessionStatus.COMPLETED).exists():
            continue

        # Get the latest scheduled session
        latest_session = booking.sessions.order_by('-scheduled_at').first()
        if latest_session and latest_session.scheduled_at < threshold:
            booking.booking_status = Booking.BookingStatus.EXPIRED
            booking.save(update_fields=['booking_status'])
            expired_count += 1

    # 2. Bookings WITHOUT sessions (fallback to proposed_date + proposed_start_time)
    bookings_without_sessions = Booking.objects.filter(
        Q(booking_status=Booking.BookingStatus.ACCEPTED) | Q(booking_status='scheduled'),
        sessions__isnull=True
    )

    for booking in bookings_without_sessions:
        # Combine date and time
        scheduled_dt = datetime.datetime.combine(
            booking.proposed_date,
            booking.proposed_start_time
        )
        # Make aware if necessary (assume same timezone as now)
        if timezone.is_naive(scheduled_dt):
            scheduled_dt = timezone.make_aware(scheduled_dt, timezone.get_current_timezone())

        if scheduled_dt < threshold:
            booking.booking_status = Booking.BookingStatus.EXPIRED
            booking.save(update_fields=['booking_status'])
            expired_count += 1

    return expired_count
