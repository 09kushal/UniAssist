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
        void onReport(BookingResponse booking);
        void onReschedule(BookingResponse booking);
        void onPaymentCompleted(); // For demo/testing refresh
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
        TextView tvTutorName, tvTutorDomain, tvStatusChip, tvSubject, tvDate, tvTime, tvReport, tvReschedule;
        Button btnCancel, btnJoin, btnDemoPayment;
        View llActions, llReportReschedule;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTutorName = itemView.findViewById(R.id.tvTutorName);
            tvTutorDomain = itemView.findViewById(R.id.tvTutorDomain);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvReport = itemView.findViewById(R.id.tvReport);
            tvReschedule = itemView.findViewById(R.id.tvReschedule);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnDemoPayment = itemView.findViewById(R.id.btnDemoPayment);
            llActions = itemView.findViewById(R.id.llActions);
            llReportReschedule = itemView.findViewById(R.id.llReportReschedule);
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
            llReportReschedule.setVisibility(View.GONE);

            if ("pending".equals(status)) {
                btnCancel.setVisibility(View.VISIBLE);
                btnJoin.setVisibility(View.GONE);
                llActions.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> listener.onCancel(booking));
            } else if ("accepted".equals(status)) {
                llActions.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.GONE);
                btnJoin.setVisibility(View.VISIBLE);
                llReportReschedule.setVisibility(View.VISIBLE);

                tvReport.setOnClickListener(v -> listener.onReport(booking));
                tvReschedule.setOnClickListener(v -> listener.onReschedule(booking));
                
                if (booking.isOfficiallyScheduled()) {
                    btnJoin.setText("Join Session");
                    btnJoin.setOnClickListener(v -> listener.onJoin(booking));
                    btnDemoPayment.setVisibility(View.GONE);
                } else {
                    btnJoin.setText("Pay Now");
                    btnJoin.setOnClickListener(v -> listener.onPay(booking));
                    
                    // Demo/Testing button
                    btnDemoPayment.setVisibility(View.VISIBLE);
                    btnDemoPayment.setOnClickListener(v -> {
                        new android.app.AlertDialog.Builder(context)
                                .setTitle("Demo Payment")
                                .setMessage("Simulate eSewa payment for: " + booking.getSubjectOrSkill() +
                                        "\nAmount: NPR " + (booking.getTutor() != null ? booking.getTutor().getPricingPerSession() : "1000"))
                                .setPositiveButton("Confirm", (d, w) -> {
                                    String token = "Bearer " + new SessionManager(context).getAccessToken();
                                    com.kushal.uniassist.models.PaymentInitiateRequest req = 
                                            new com.kushal.uniassist.models.PaymentInitiateRequest(booking.getId());
                                    
                                    com.kushal.uniassist.network.ApiClient.getClient().create(com.kushal.uniassist.network.ApiService.class)
                                            .demoCompletePayment(token, req)
                                            .enqueue(new retrofit2.Callback<com.kushal.uniassist.models.ApiResponse<Object>>() {
                                                @Override
                                                public void onResponse(retrofit2.Call<com.kushal.uniassist.models.ApiResponse<Object>> c, 
                                                                     retrofit2.Response<com.kushal.uniassist.models.ApiResponse<Object>> response) {
                                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()){
                                                        android.widget.Toast.makeText(context, "✅ Payment confirmed!\nYou can now join the session.", android.widget.Toast.LENGTH_LONG).show();
                                                        if (listener != null) listener.onPaymentCompleted();
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Failed to complete payment", android.widget.Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                                @Override
                                                public void onFailure(retrofit2.Call<com.kushal.uniassist.models.ApiResponse<Object>> c, Throwable t) {
                                                    android.widget.Toast.makeText(context, "Network error: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
            }
        }
    }
}
