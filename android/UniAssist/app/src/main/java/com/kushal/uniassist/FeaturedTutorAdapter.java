package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.TutorResponse;

import java.util.List;

public class FeaturedTutorAdapter extends RecyclerView.Adapter<FeaturedTutorAdapter.ViewHolder> {

    private List<TutorResponse> tutors;
    private OnTutorClickListener listener;

    public interface OnTutorClickListener {
        void onTutorClick(TutorResponse tutor);
    }

    public FeaturedTutorAdapter(List<TutorResponse> tutors, OnTutorClickListener listener) {
        this.tutors = tutors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_tutor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorResponse tutor = tutors.get(position);
        holder.tvName.setText(tutor.getFullName());
        holder.tvRating.setText(tutor.getAverageRating());
        holder.tvPrice.setText("NPR " + tutor.getPricingPerSession() + "/session");

        Glide.with(holder.itemView.getContext())
                .load(tutor.getProfilePhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_tutor_placeholder)
                .into(holder.ivPhoto);

        holder.itemView.setOnClickListener(v -> listener.onTutorClick(tutor));
    }

    @Override
    public int getItemCount() {
        return tutors != null ? tutors.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvRating, tvPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvName = itemView.findViewById(R.id.tvName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
