package com.kushal.uniassist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.BookingResponse;

import java.util.ArrayList;
import java.util.List;

public class BookingDetailAdapter extends RecyclerView.Adapter<BookingDetailAdapter.ViewHolder> {

    private List<BookingResponse> bookings = new ArrayList<>();
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onCancel(BookingResponse booking);
        void onJoin(BookingResponse booking);
        void onPay(BookingResponse booking);
    }

    public BookingDetailAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<BookingResponse> newList) {
        this.bookings.clear();
        if (newList != null) {
            this.bookings.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookings.get(position);
        holder.bind(booking, listener);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTutorName, tvTutorDomain, tvStatusChip, tvSubject, tvDate, tvTime;
        Button btnCancel, btnJoin;
        View llActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTutorName = itemView.findViewById(R.id.tvTutorName);
            tvTutorDomain = itemView.findViewById(R.id.tvTutorDomain);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            llActions = itemView.findViewById(R.id.llActions);
        }

        public void bind(BookingResponse booking, OnBookingActionListener listener) {
            Context context = itemView.getContext();
            
            if (booking.getTutor() != null) {
                tvTutorName.setText(booking.getTutor().getFullName());
                tvTutorDomain.setText(booking.getTutor().getDomain().toUpperCase());
            } else {
                tvTutorName.setText("Unknown Tutor");
                tvTutorDomain.setText("");
            }

            tvSubject.setText("Subject: " + booking.getSubjectOrSkill());
            tvDate.setText(booking.getProposedDate());
            tvTime.setText(booking.getProposedStartTime() + " - " + booking.getProposedEndTime());

            String status = booking.getBookingStatus().toLowerCase();
            tvStatusChip.setText(status.toUpperCase());
            
            int colorRes = R.color.text_hint;
            switch (status) {
                case "pending":
                    colorRes = android.R.color.holo_orange_dark;
                    break;
                case "accepted":
                    colorRes = android.R.color.holo_green_dark;
                    break;
                case "rejected":
                    colorRes = android.R.color.holo_red_dark;
                    break;
                case "completed":
                    colorRes = R.color.primary;
                    break;
            }
            tvStatusChip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));

            // Actions
            btnCancel.setVisibility(View.GONE);
            btnJoin.setVisibility(View.GONE);
            llActions.setVisibility(View.GONE);

            if ("pending".equals(status)) {
                btnCancel.setVisibility(View.VISIBLE);
                btnJoin.setVisibility(View.GONE);
                llActions.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> listener.onCancel(booking));
            } else if ("accepted".equals(status)) {
                llActions.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.GONE);
                btnJoin.setVisibility(View.VISIBLE);
                if (booking.isOfficiallyScheduled()) {
                    btnJoin.setText("Join Session");
                    btnJoin.setOnClickListener(v -> listener.onJoin(booking));
                } else {
                    btnJoin.setText("Pay Now");
                    btnJoin.setOnClickListener(v -> listener.onPay(booking));
                }
            }
        }
    }
}
