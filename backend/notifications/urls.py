from django.urls import path
from notifications.views import (
    NotificationListView,
    DeviceTokenUpdateView,
    MarkNotificationReadView,
    MarkAllNotificationsReadView,
    UnreadNotificationCountView
)

urlpatterns = [
    path('', NotificationListView.as_view(), name='notification-list'),
    path('read-all/', MarkAllNotificationsReadView.as_view(), name='mark-all-read'),
    path('unread-count/', UnreadNotificationCountView.as_view(), name='unread-count'),
    path('<int:pk>/read/', MarkNotificationReadView.as_view(), name='mark-read'),
]
