package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.LatenessReportResponse;

import java.util.ArrayList;
import java.util.List;

public class LatenessReportAdapter extends RecyclerView.Adapter<LatenessReportAdapter.ViewHolder> {

    private List<LatenessReportResponse> reports = new ArrayList<>();

    public void updateList(List<LatenessReportResponse> newList) {
        this.reports.clear();
        if (newList != null) {
            this.reports.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lateness_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LatenessReportResponse report = reports.get(position);
        
        holder.tvReportRole.setText("Report filed as: " + report.getReporterRole().toUpperCase());
        holder.tvAdminAction.setText(report.getAdminAction().toUpperCase());
        holder.tvDelayRange.setText("Lateness: " + report.getDelayRange());
        holder.tvDescription.setText(report.getDescription());
        holder.tvCreatedAt.setText(report.getCreatedAt());

        if ("pending".equalsIgnoreCase(report.getAdminAction())) {
            holder.tvAdminAction.setBackgroundResource(R.drawable.bg_status_badge);
        } else if ("no_action".equalsIgnoreCase(report.getAdminAction())) {
            holder.tvAdminAction.setBackgroundResource(R.drawable.bg_status_badge);
            holder.tvAdminAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_hint)));
        } else {
            holder.tvAdminAction.setBackgroundResource(R.drawable.bg_verified_badge);
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportRole, tvAdminAction, tvDelayRange, tvDescription, tvCreatedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportRole = itemView.findViewById(R.id.tvReportRole);
            tvAdminAction = itemView.findViewById(R.id.tvAdminAction);
            tvDelayRange = itemView.findViewById(R.id.tvDelayRange);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
