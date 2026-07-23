package com.kushal.uniassist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.SkillResponse;
import com.kushal.uniassist.models.SubjectResponse;
import com.kushal.uniassist.models.TutorResponse;

import java.util.ArrayList;
import java.util.List;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.TutorViewHolder> {

    private List<TutorResponse> tutors;
    private OnTutorClickListener listener;

    public interface OnTutorClickListener {
        void onTutorClick(TutorResponse tutor);
        void onBookNowClick(TutorResponse tutor);
    }

    public TutorAdapter(OnTutorClickListener listener) {
        this.tutors = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutor_card, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        TutorResponse tutor = tutors.get(position);
        if (tutor != null) {
            holder.bind(tutor, listener);
        }
    }

    @Override
    public int getItemCount() {
        return tutors.size();
    }

    public void updateList(List<TutorResponse> newList) {
        this.tutors.clear();
        if (newList != null) {
            this.tutors.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class TutorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, ivVerified;
        TextView tvName, tvDomainChip, tvSubjects, tvRating, tvPrice;
        RatingBar ratingBar;
        Button btnBookNow;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            tvName = itemView.findViewById(R.id.tvName);
            tvDomainChip = itemView.findViewById(R.id.tvDomainChip);
            tvSubjects = itemView.findViewById(R.id.tvSubjects);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }

        public void bind(TutorResponse tutor, OnTutorClickListener listener) {
            Context context = itemView.getContext();
            tvName.setText(tutor.getFullName());
            
            tvDomainChip.setText(tutor.getDomain());
            int colorRes = "academic".equalsIgnoreCase(tutor.getDomain()) ? R.color.accent : R.color.primary;
            tvDomainChip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));
            
            String subjectsText = "";
            if (tutor.getSubjects() != null && !tutor.getSubjects().isEmpty()) {
                List<String> names = new ArrayList<>();
                for (SubjectResponse s : tutor.getSubjects()) {
                    names.add(s.getName());
                }
                subjectsText = TextUtils.join(", ", names);
            } else if (tutor.getSkills() != null && !tutor.getSkills().isEmpty()) {
                List<String> names = new ArrayList<>();
                for (SkillResponse s : tutor.getSkills()) {
                    names.add(s.getName());
                }
                subjectsText = TextUtils.join(", ", names);
            }
            
            if (subjectsText.isEmpty()) {
                tvSubjects.setText("No subjects listed");
            } else {
                tvSubjects.setText(subjectsText);
            }
            
            tvRating.setText(tutor.getAverageRating());
            ratingBar.setRating(tutor.getAverageRatingFloat());
            
            tvPrice.setText("NPR " + tutor.getPricingPerSession() + "/session");
            
            ivVerified.setVisibility(tutor.isVerifiedBadge() ? View.VISIBLE : View.GONE);

            if (tutor.getProfilePhotoUrl() != null && !tutor.getProfilePhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(tutor.getProfilePhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_tutor_placeholder)
                        .error(R.drawable.ic_tutor_placeholder)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.ic_tutor_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTutorClick(tutor);
            });

            if (btnBookNow != null) {
                btnBookNow.setOnClickListener(v -> {
                    if (listener != null) listener.onBookNowClick(tutor);
                });
            }
        }
    }
}
