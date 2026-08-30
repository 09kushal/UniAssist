import logging
from django.contrib import admin
from django.utils import timezone
from .models import Payment, Payout
from notifications.services import notify_payout_released

logger = logging.getLogger(__name__)

@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ['id', 'booking', 'amount', 'payment_status', 'paid_at']

@admin.register(Payout)
class PayoutAdmin(admin.ModelAdmin):
    list_display = ['id', 'booking', 'tutor', 'total_paid', 'commission_amount',
                     'tutor_base_share', 'payout_status', 'released_at']
    actions = ['release_payouts']

    @admin.action(description='Release selected payouts to tutor')
    def release_payouts(self, request, queryset):
        count = 0
        for payout in queryset.filter(payout_status=Payout.PayoutStatus.HELD):
            payout.payout_status = Payout.PayoutStatus.RELEASED
            payout.released_by = request.user
            payout.released_at = timezone.now()
            payout.save(update_fields=['payout_status', 'released_by', 'released_at'])

            # Reusing notification logic from AdminPayoutReleaseView
            try:
                notify_payout_released(payout)
            except Exception as e:
                logger.warning('Failed to notify tutor for payout %s: %s', payout.id, e)

            count += 1
        self.message_user(request, f'{count} payout(s) released successfully.')
