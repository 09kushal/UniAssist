package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kushal.uniassist.models.BookingResponse;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<BookingResponse> bookings = new ArrayList<>();
    private OnBookingClickListener listener;

    public interface OnBookingClickListener {
        void onBookingClick(BookingResponse booking);
    }

    public BookingAdapter(OnBookingClickListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookings.get(position);
        holder.tvSubject.setText(booking.getSubjectOrSkill());
        
        if (booking.getTutor() != null) {
            holder.tvTutor.setText("Tutor: " + booking.getTutor().getFullName());
        } else {
            holder.tvTutor.setText("Tutor: Unknown");
        }

        holder.tvDate.setText(booking.getProposedDate());
        holder.tvStatus.setText(booking.getBookingStatus().toUpperCase());
        
        holder.itemView.setOnClickListener(v -> listener.onBookingClick(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTutor, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvBookingSubject);
            tvTutor = itemView.findViewById(R.id.tvBookingTutor);
            tvDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
        }
    }
}
