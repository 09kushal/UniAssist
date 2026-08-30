package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.AvailabilitySlot;

import java.util.List;

public class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.ViewHolder> {

    private List<AvailabilitySlot> slots;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int slotId);
    }

    public AvailabilityAdapter(List<AvailabilitySlot> slots, OnDeleteClickListener deleteListener) {
        this.slots = slots;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_availability_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvailabilitySlot slot = slots.get(position);
        holder.tvSlotInfo.setText(slot.getDayOfWeek() + ": " + slot.getStartTime() + " - " + slot.getEndTime());
        
        if (deleteListener == null) {
            holder.ivDelete.setVisibility(View.GONE);
        } else {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> deleteListener.onDeleteClick(slot.getId()));
        }
    }

    @Override
    public int getItemCount() {
        return slots != null ? slots.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotInfo;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotInfo = itemView.findViewById(R.id.tvSlotInfo);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}
