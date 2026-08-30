from django.contrib import admin
from .models import LatenessReport, RescheduleRequest

@admin.register(LatenessReport)
class LatenessReportAdmin(admin.ModelAdmin):
    list_display = ['id', 'session', 'reported_by', 'reported_against', 'reporter_role',
                     'delay_range', 'admin_action', 'created_at']

@admin.register(RescheduleRequest)
class RescheduleRequestAdmin(admin.ModelAdmin):
    list_display = ['id', 'booking', 'requested_by', 'reason', 'status', 'created_at']
