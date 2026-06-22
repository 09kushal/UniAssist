from rest_framework import serializers
from notifications.models import Notification

class NotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notification
        fields = ['id', 'title', 'message', 'notification_type', 'is_read', 'created_at']

class DeviceTokenSerializer(serializers.Serializer):
    device_token = serializers.CharField(max_length=255)
