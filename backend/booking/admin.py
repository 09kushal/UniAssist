from django.contrib import admin
from booking.models import Booking, Session


@admin.register(Booking)
class BookingAdmin(admin.ModelAdmin):
    list_display = (
        'id',
        'student',
        'tutor',
        'subject_or_skill',
        'proposed_date',
        'booking_status',
        'officially_scheduled',
    )
    list_filter = ('booking_status', 'officially_scheduled', 'proposed_date')
    search_fields = (
        'student__user__full_name',
        'student__user__email',
        'tutor__user__full_name',
        'tutor__user__email',
        'subject_or_skill',
    )
    readonly_fields = ('created_at',)


@admin.register(Session)
class SessionAdmin(admin.ModelAdmin):
    list_display = (
        'id',
        'booking',
        'scheduled_at',
        'tutor_start_time',
        'session_status',
        'officially_scheduled',
    )
    list_filter = ('session_status', 'officially_scheduled', 'scheduled_at')
    search_fields = (
        'booking__student__user__full_name',
        'booking__tutor__user__full_name',
    )
    readonly_fields = ('created_at',)
