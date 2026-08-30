package com.kushal.uniassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.TutorResponse;

import java.util.ArrayList;
import java.util.List;

public class FeaturedTutorAdapter extends RecyclerView.Adapter<FeaturedTutorAdapter.ViewHolder> implements Filterable {

    private List<TutorResponse> tutors;
    private List<TutorResponse> tutorsFull;
    private OnTutorClickListener listener;

    public interface OnTutorClickListener {
        void onTutorClick(TutorResponse tutor);
    }

    public FeaturedTutorAdapter(List<TutorResponse> tutors, OnTutorClickListener listener) {
        this.tutors = tutors;
        this.tutorsFull = new ArrayList<>(tutors);
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
        
        String price = tutor.getPricingPerSession();
        if (price != null && !price.equals("0.00")) {
            holder.tvPrice.setText("NPR " + price + "/session");
        } else {
            holder.tvPrice.setText("Free");
        }

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

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<TutorResponse> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(tutorsFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (TutorResponse item : tutorsFull) {
                        if (item.getFullName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                tutors.clear();
                tutors.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
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
