package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.kushal.uniassist.models.NotificationResponse;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationResponse> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationResponse notification);
        void onNotificationDelete(NotificationResponse notification, int position);
    }

    public NotificationAdapter(List<NotificationResponse> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    public NotificationResponse getNotificationAt(int position) {
        return notifications.get(position);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationResponse notification = notifications.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View vUnreadIndicatorLine;
        LinearLayout llRoot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            vUnreadIndicatorLine = itemView.findViewById(R.id.vUnreadIndicatorLine);
            llRoot = itemView.findViewById(R.id.llNotificationRoot);
        }

        public void bind(NotificationResponse notification, OnNotificationClickListener listener) {
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());
            tvTime.setText(notification.getCreatedAt());

            if (notification.isRead()) {
                llRoot.setBackgroundResource(R.drawable.bg_notification_read);
                vUnreadIndicatorLine.setVisibility(View.GONE);
            } else {
                llRoot.setBackgroundResource(R.drawable.bg_notification_unread);
                vUnreadIndicatorLine.setVisibility(View.VISIBLE);
            }

            // Icon logic based on type
            String type = notification.getNotificationType();
            if ("booking".equalsIgnoreCase(type)) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_today);
                ivIcon.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.accent));
            } else if ("payment".equalsIgnoreCase(type)) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_send);
                ivIcon.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.success));
            } else if ("warning".equalsIgnoreCase(type)) {
                ivIcon.setImageResource(android.R.drawable.stat_sys_warning);
                ivIcon.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.warning));
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_popup_reminder);
                ivIcon.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.primary));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
        }
    }
}
