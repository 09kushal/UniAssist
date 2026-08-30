from django.contrib import admin
from payments.models import Payment, Payout

@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ['id', 'booking', 'amount', 'payment_status', 'paid_at']
    list_filter = ['payment_status', 'paid_at']
    search_fields = ['booking__id', 'esewa_ref_id']
    ordering = ['-paid_at']

@admin.register(Payout)
class PayoutAdmin(admin.ModelAdmin):
    list_display = ['id', 'booking', 'total_paid', 'commission_amount', 'tutor_base_share', 'payout_status', 'released_at']
    list_filter = ['payout_status', 'released_at']
    search_fields = ['booking__id', 'tutor__user__full_name']
    ordering = ['-created_at']
