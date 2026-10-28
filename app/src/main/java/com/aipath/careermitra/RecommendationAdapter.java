package com.aipath.careermitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private List<Recommendation> recommendations;

    // UI: tracks which rows already played their entrance animation
    private int lastAnimatedPosition = -1;

    public RecommendationAdapter(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommendation rec = recommendations.get(position);

        // UI: split "Title - reason" for display only
        String full = rec.careerPath;
        int split = full.indexOf(" - ");
        if (split > 0 && split < full.length() - 3) {
            holder.textViewCareerPath.setText(full.substring(0, split).trim());
            holder.textViewReason.setText(full.substring(split + 3).trim());
            holder.textViewReason.setVisibility(View.VISIBLE);
        } else {
            holder.textViewCareerPath.setText(full);
            holder.textViewReason.setVisibility(View.GONE);
        }
        holder.tvRank.setText(String.valueOf(position + 1));

        // UI: reset the INNER card first so a row can never get stuck offset
        holder.card.animate().cancel();
        holder.card.setAlpha(1f);
        holder.card.setTranslationX(0f);
        holder.card.setTranslationY(0f);

        // UI: staggered slide-up, animating only the inner card
        if (position > lastAnimatedPosition) {
            holder.card.setAlpha(0f);
            holder.card.setTranslationY(60f);
            holder.card.animate()
                    .alpha(1f).translationY(0f)
                    .setStartDelay(position * 120L)
                    .setDuration(500L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        holder.card.animate().cancel();
        holder.card.setAlpha(1f);
        holder.card.setTranslationX(0f);
        holder.card.setTranslationY(0f);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCareerPath;
        TextView textViewReason; // UI
        TextView tvRank;         // UI
        View card;               // UI: the inner card that gets animated

        ViewHolder(View itemView) {
            super(itemView);
            textViewCareerPath = itemView.findViewById(R.id.textViewCareerPath);
            textViewReason = itemView.findViewById(R.id.textViewReason);
            tvRank = itemView.findViewById(R.id.tvRank);
            card = itemView.findViewById(R.id.cardRecommendationItem);
        }
    }
}