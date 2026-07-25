package com.kushal.uniassist;

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
        private final MaterialButton btnAccept, btnReject;

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
        }

        public void bind(BookingResponse booking, OnBookingActionListener listener) {
            if (booking.getStudent() != null) {
                tvStudentName.setText(booking.getStudent().getFullName());
            } else {
                tvStudentName.setText("Unknown Student");
            }

            tvStatus.setText(booking.getBookingStatus());
            tvSubject.setText(booking.getSubjectOrSkill());
            tvDateTime.setText(booking.getProposedDate() + " • " + booking.getProposedStartTime());
            tvMessage.setText(booking.getMessage());

            String status = booking.getBookingStatus();
            if ("pending".equalsIgnoreCase(status)) {
                layoutActions.setVisibility(View.VISIBLE);
            } else {
                layoutActions.setVisibility(View.GONE);
            }

            btnAccept.setOnClickListener(v -> listener.onAccept(booking));
            btnReject.setOnClickListener(v -> listener.onReject(booking));
        }
    }
}
