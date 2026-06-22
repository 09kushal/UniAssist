from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated
from rest_framework import status
from rest_framework.pagination import PageNumberPagination
from uniassist.utils import success_response, error_response
from notifications.models import Notification
from notifications.serializers import NotificationSerializer, DeviceTokenSerializer

class NotificationPagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = 'page_size'
    max_page_size = 100
    page_query_param = 'page'

class NotificationListView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = Notification.objects.filter(user=request.user).order_by('-created_at')
        unread_count = Notification.objects.filter(user=request.user, is_read=False).count()
        
        paginator = NotificationPagination()
        page = paginator.paginate_queryset(qs, request)
        serializer = NotificationSerializer(page, many=True)
        
        return success_response(
            message='Notifications retrieved successfully.',
            data={
                'unread_count': unread_count,
                'count': paginator.page.paginator.count,
                'pages': paginator.page.paginator.num_pages,
                'results': serializer.data,
            }
        )

class DeviceTokenUpdateView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = DeviceTokenSerializer(data=request.data)
        if not serializer.is_valid():
            return error_response(
                message='Invalid input.',
                errors=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST
            )
        
        request.user.device_token = serializer.validated_data['device_token']
        request.user.save(update_fields=['device_token'])
        
        return success_response(
            message='Device token updated successfully.',
            data={}
        )

class MarkNotificationReadView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        try:
            notification = Notification.objects.get(id=pk, user=request.user)
        except Notification.DoesNotExist:
            return error_response(
                message='Notification not found.',
                status=status.HTTP_404_NOT_FOUND
            )
        
        notification.is_read = True
        notification.save(update_fields=['is_read'])
        
        return success_response(
            message='Notification marked as read.',
            data={}
        )

class MarkAllNotificationsReadView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request):
        updated = Notification.objects.filter(user=request.user, is_read=False).update(is_read=True)
        return success_response(
            message='All notifications marked as read.',
            data={'updated': updated}
        )

class UnreadNotificationCountView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        unread_count = Notification.objects.filter(user=request.user, is_read=False).count()
        return success_response(
            message='Unread count retrieved successfully.',
            data={'unread_count': unread_count}
        )
