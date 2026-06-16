"""
UniAssist — payments/serializers.py

Serializers for the Payment and Payout models.

Covers:
  - PaymentSerializer       : read-only representation of a Payment record
  - InitiatePaymentSerializer: validates booking_id from student input
  - PayoutSerializer        : read-only representation of a Payout record
"""

from rest_framework import serializers

from payments.models import Payment, Payout


# ─── Payment Serializer (read) ────────────────────────────────────────────────

class PaymentSerializer(serializers.ModelSerializer):
    """
    Read-only serializer for the Payment model.
    Used by payment status and callback response views.
    Never exposes passwords or tokens (per API_RULES.md).
    """

    booking_id    = serializers.IntegerField(source='booking.id', read_only=True)
    student_name  = serializers.CharField(
        source='booking.student.user.full_name',
        read_only=True,
    )
    tutor_name    = serializers.CharField(
        source='booking.tutor.user.full_name',
        read_only=True,
    )

    class Meta:
        model  = Payment
        fields = [
            'id',
            'booking_id',
            'student_name',
            'tutor_name',
            'amount',
            'payment_status',
            'esewa_ref_id',
            'paid_at',
        ]
        read_only_fields = fields


# ─── Initiate Payment Serializer (write) ─────────────────────────────────────

class InitiatePaymentSerializer(serializers.Serializer):
    """
    Validates input for POST /api/payments/initiate/
    Only field required from the student is the booking_id.
    """
    booking_id = serializers.IntegerField(
        help_text='ID of the accepted booking to pay for.',
    )


# ─── Payout Serializer (read) ─────────────────────────────────────────────────

class PayoutSerializer(serializers.ModelSerializer):
    """
    Read-only serializer for the Payout model.
    Includes full breakdown for admin and tutor payout history views.
    """

    tutor_name       = serializers.CharField(
        source='tutor.user.full_name',
        read_only=True,
    )
    booking_id       = serializers.IntegerField(source='booking.id', read_only=True)
    released_by_name = serializers.SerializerMethodField()
    fine_imposed_by_name = serializers.SerializerMethodField()

    class Meta:
        model  = Payout
        fields = [
            'id',
            'booking_id',
            'tutor_name',
            'total_paid',
            'commission_amount',
            'tutor_base_share',
            'fine_percentage',
            'fine_amount',
            'fine_reason',
            'fine_imposed_by_name',
            'student_refund',
            'tutor_final_payout',
            'payout_status',
            'released_by_name',
            'released_at',
        ]
        read_only_fields = fields

    def get_released_by_name(self, obj):
        """Return name of admin who released payout, or None."""
        if obj.released_by:
            return obj.released_by.full_name
        return None

    def get_fine_imposed_by_name(self, obj):
        """Return name of admin who imposed fine, or None."""
        if obj.fine_imposed_by:
            return obj.fine_imposed_by.full_name
        return None
