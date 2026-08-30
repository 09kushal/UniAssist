package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.PayoutResponse;

import java.util.ArrayList;
import java.util.List;

public class PayoutAdapter extends RecyclerView.Adapter<PayoutAdapter.ViewHolder> {

    private List<PayoutResponse> payouts = new ArrayList<>();

    public void updateList(List<PayoutResponse> newList) {
        this.payouts.clear();
        if (newList != null) {
            this.payouts.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PayoutResponse payout = payouts.get(position);
        
        holder.tvSessionName.setText("Booking #" + payout.getBookingId());
        holder.tvTotalPaid.setText("NPR " + payout.getTotalPaid());
        holder.tvNetPayout.setText("NPR " + payout.getTutorFinalPayout());
        
        String status = payout.getPayoutStatus();
        holder.tvPayoutStatus.setText(status.toUpperCase());
        
        if ("released".equalsIgnoreCase(status)) {
            holder.tvPayoutStatus.setBackgroundResource(R.drawable.bg_verified_badge);
            holder.tvPayoutDate.setText("Released at: " + (payout.getReleasedAt() != null ? payout.getReleasedAt() : "N/A"));
            holder.tvPayoutDate.setVisibility(View.VISIBLE);
        } else if ("fined".equalsIgnoreCase(status)) {
            holder.tvPayoutStatus.setBackgroundResource(R.drawable.bg_status_badge); // using pending orange as base for now or just error
            holder.tvPayoutStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.error)));
            holder.tvPayoutDate.setVisibility(View.GONE);
        } else {
            holder.tvPayoutStatus.setBackgroundResource(R.drawable.bg_status_badge);
            holder.tvPayoutDate.setVisibility(View.GONE);
        }

        if (payout.getFineReason() != null && !payout.getFineReason().isEmpty()) {
            holder.tvFineReason.setVisibility(View.VISIBLE);
            holder.tvFineReason.setText("Fine Reason: " + payout.getFineReason());
        } else {
            holder.tvFineReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return payouts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionName, tvPayoutStatus, tvPayoutDate, tvTotalPaid, tvNetPayout, tvFineReason;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionName = itemView.findViewById(R.id.tvSessionName);
            tvPayoutStatus = itemView.findViewById(R.id.tvPayoutStatus);
            tvPayoutDate = itemView.findViewById(R.id.tvPayoutDate);
            tvTotalPaid = itemView.findViewById(R.id.tvTotalPaid);
            tvNetPayout = itemView.findViewById(R.id.tvNetPayout);
            tvFineReason = itemView.findViewById(R.id.tvFineReason);
        }
    }
}
