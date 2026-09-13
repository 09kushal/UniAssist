package com.kushal.uniassist;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kushal.uniassist.models.BookingResponse;

import java.util.ArrayList;
import java.util.List;

public class TutorBookingAdapter extends RecyclerView.Adapter<TutorBookingAdapter.BookingViewHolder> {

    private List<BookingResponse> bookings = new ArrayList<>();
    private final OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onAccept(BookingResponse booking);
        void onReject(BookingResponse booking);
        void onJoin(BookingResponse booking);
    }

    public TutorBookingAdapter(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<BookingResponse> newList) {
        this.bookings = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<BookingResponse> getBookings() {
        return bookings;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutor_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingResponse booking = bookings.get(position);
        holder.bind(booking, listener);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStudentName, tvStatus, tvSubject, tvDateTime, tvMessage;
        private final LinearLayout layoutActions;
        private final MaterialButton btnAccept, btnReject, btnJoin;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }

        public void bind(BookingResponse booking, OnBookingActionListener listener) {
            if (booking.getStudent() != null) {
                tvStudentName.setText(booking.getStudent().getFullName());
            } else {
                tvStudentName.setText("Unknown Student");
            }

            String status = booking.getBookingStatus().toLowerCase();
            tvStatus.setText(status);
            
            // Fix status chip colors
            int color;
            switch (status) {
                case "accepted":
                    color = Color.parseColor("#22C55E");
                    break;
                case "rejected":
                    color = Color.parseColor("#EF4444");
                    break;
                case "expired":
                    color = Color.parseColor("#9CA3AF");
                    break;
                case "pending":
                default:
                    color = Color.parseColor("#F59E0B");
                    break;
            }
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(color));
            tvStatus.setTextColor(Color.WHITE);

            // Fix subject chip
            String subject = booking.getSubjectOrSkill();
            if (subject != null && !subject.isEmpty()) {
                tvSubject.setText(subject);
                tvSubject.setVisibility(View.VISIBLE);
            } else {
                tvSubject.setVisibility(View.GONE);
            }

            tvDateTime.setText(booking.getProposedDate() + " • " + booking.getProposedStartTime());
            tvMessage.setText(booking.getMessage());

            if ("pending".equals(status)) {
                layoutActions.setVisibility(View.VISIBLE);
                btnJoin.setVisibility(View.GONE);
            } else if ("accepted".equals(status)) {
                layoutActions.setVisibility(View.GONE);
                btnJoin.setVisibility(View.VISIBLE);
            } else {
                layoutActions.setVisibility(View.GONE);
                btnJoin.setVisibility(View.GONE);
            }

            btnAccept.setOnClickListener(v -> listener.onAccept(booking));
            btnReject.setOnClickListener(v -> listener.onReject(booking));
            btnJoin.setOnClickListener(v -> listener.onJoin(booking));
        }
    }
}
