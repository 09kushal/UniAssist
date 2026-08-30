package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.RescheduleRequestResponse;

import java.util.ArrayList;
import java.util.List;

public class RescheduleRequestAdapter extends RecyclerView.Adapter<RescheduleRequestAdapter.ViewHolder> {

    private List<RescheduleRequestResponse> requests = new ArrayList<>();

    public void updateList(List<RescheduleRequestResponse> newList) {
        this.requests.clear();
        if (newList != null) {
            this.requests.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reschedule_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RescheduleRequestResponse request = requests.get(position);
        
        holder.tvRequesterName.setText(request.getStudentName());
        holder.tvStatus.setText(request.getStatus().toUpperCase());
        holder.tvReason.setText(request.getReason());
        holder.tvCreatedAt.setText(request.getCreatedAt());

        if ("pending".equalsIgnoreCase(request.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge);
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge);
            holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.error)));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_verified_badge);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequesterName, tvStatus, tvReason, tvCreatedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequesterName = itemView.findViewById(R.id.tvRequesterName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
