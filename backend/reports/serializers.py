"""
UniAssist — reports/serializers.py

Serializers for Phase 7 — Reports & Admin Module:
  • LatenessReportSerializer          — full report details
  • FileLatenessReportSerializer       — student files lateness report (input)
  • FileTutorReportSerializer          — tutor files student report (input)
  • AdminReportActionSerializer        — admin takes action on report
  • FinePreviewSerializer              — input for fine live preview
  • FineConfirmSerializer              — input for confirming fine
  • RescheduleRequestSerializer        — full reschedule request details
  • CreateRescheduleRequestSerializer  — student creates reschedule request (input)
  • AdminRescheduleActionSerializer    — admin approves/rejects reschedule request
  • TutorVerificationSerializer        — tutor pending verification list
  • AdminTutorRejectSerializer         — admin rejects tutor with reason
"""

from rest_framework import serializers

from accounts.models import Student, Tutor, TutorDocument, User
from booking.models import Booking, Session
from payments.models import Payout
from reports.models import LatenessReport, RescheduleRequest


# ─── Lateness Report Serializers ──────────────────────────────────────────────

class LatenessReportSerializer(serializers.ModelSerializer):
    """Full read-only serializer for LatenessReport — used in admin list."""

    session_id          = serializers.IntegerField(source='session.id', read_only=True)
    booking_id          = serializers.IntegerField(source='session.booking.id', read_only=True)
    reported_by_name    = serializers.CharField(source='reported_by.full_name', read_only=True)
    reported_by_email   = serializers.CharField(source='reported_by.email', read_only=True)
    reported_against_name  = serializers.CharField(source='reported_against.full_name', read_only=True)
    reported_against_email = serializers.CharField(source='reported_against.email', read_only=True)

    class Meta:
        model  = LatenessReport
        fields = [
            'id',
            'session_id',
            'booking_id',
            'reported_by_name',
            'reported_by_email',
            'reported_against_name',
            'reported_against_email',
            'reporter_role',
            'delay_range',
            'description',
            'admin_action',
            'created_at',
        ]


class FileLatenessReportSerializer(serializers.Serializer):
    """Input serializer — student files a lateness report against tutor."""
    session_id  = serializers.IntegerField()
    delay_range = serializers.ChoiceField(choices=LatenessReport.DelayRange.choices)
    description = serializers.CharField()


class FileTutorReportSerializer(serializers.Serializer):
    """Input serializer — tutor files a lateness report against student."""
    session_id  = serializers.IntegerField()
    delay_range = serializers.ChoiceField(choices=LatenessReport.DelayRange.choices)
    description = serializers.CharField()


class AdminReportActionSerializer(serializers.Serializer):
    """Input serializer — admin takes action on a report."""
    admin_action = serializers.ChoiceField(
        choices=[
            ('no_action', 'No Action'),
            ('warning',   'Warning'),
            ('fined',     'Fined'),
            ('suspended', 'Suspended'),
            ('removed',   'Removed'),
        ]
    )


# ─── Fine Serializers ─────────────────────────────────────────────────────────

VALID_FINE_PERCENTAGES = [5, 10, 15, 20, 30, 50]


class FinePreviewSerializer(serializers.Serializer):
    """Input serializer — admin requests a fine live preview."""
    booking_id      = serializers.IntegerField()
    fine_percentage = serializers.IntegerField()

    def validate_fine_percentage(self, value):
        if value not in VALID_FINE_PERCENTAGES:
            raise serializers.ValidationError(
                f'fine_percentage must be one of {VALID_FINE_PERCENTAGES}.'
            )
        return value


class FineConfirmSerializer(serializers.Serializer):
    """Input serializer — admin confirms and applies a fine."""
    booking_id      = serializers.IntegerField()
    fine_percentage = serializers.IntegerField()
    fine_reason     = serializers.CharField()

    def validate_fine_percentage(self, value):
        if value not in VALID_FINE_PERCENTAGES:
            raise serializers.ValidationError(
                f'fine_percentage must be one of {VALID_FINE_PERCENTAGES}.'
            )
        return value


# ─── Reschedule Request Serializers ──────────────────────────────────────────

class RescheduleRequestSerializer(serializers.ModelSerializer):
    """Full read serializer for RescheduleRequest — used in admin list."""

    student_name  = serializers.CharField(source='requested_by.user.full_name', read_only=True)
    student_email = serializers.CharField(source='requested_by.user.email', read_only=True)
    booking_id    = serializers.IntegerField(source='booking.id', read_only=True)

    class Meta:
        model  = RescheduleRequest
        fields = [
            'id',
            'booking_id',
            'student_name',
            'student_email',
            'reason',
            'status',
            'created_at',
        ]


class CreateRescheduleRequestSerializer(serializers.Serializer):
    """Input serializer — student requests a booking reschedule."""
    booking_id = serializers.IntegerField()
    reason     = serializers.CharField()


class AdminRescheduleActionSerializer(serializers.Serializer):
    """Input serializer — admin approves or rejects a reschedule request."""
    status = serializers.ChoiceField(choices=['approved', 'rejected'])


# ─── Tutor Verification Serializers ──────────────────────────────────────────

class TutorDocumentMiniSerializer(serializers.ModelSerializer):
    """Minimal document info for tutor verification panel."""

    class Meta:
        model  = TutorDocument
        fields = ['id', 'doc_type', 'uploaded_at']


class TutorVerificationSerializer(serializers.ModelSerializer):
    """Serializer for admin's tutor verification panel — unverified tutors only."""

    full_name     = serializers.CharField(source='user.full_name', read_only=True)
    email         = serializers.CharField(source='user.email', read_only=True)
    registered_at = serializers.DateTimeField(source='user.created_at', read_only=True)
    documents     = TutorDocumentMiniSerializer(many=True, read_only=True)

    class Meta:
        model  = Tutor
        fields = [
            'id',
            'full_name',
            'email',
            'domain',
            'is_verified',
            'documents',
            'registered_at',
        ]


class AdminTutorRejectSerializer(serializers.Serializer):
    """Input serializer — admin rejects a tutor with a reason."""
    rejection_reason = serializers.CharField()
