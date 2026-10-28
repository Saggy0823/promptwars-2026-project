package com.aipath.careermitra;

import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    /** Explanation shown when a card is expanded. */
    public static class Detail {
        public String why = "";
        public String steps = "";
        public String careers = "";
    }

    private List<Recommendation> recommendations;
    private List<Detail> details = new ArrayList<>();
    private final Set<Integer> expanded = new HashSet<>();

    // UI: tracks which rows already played their entrance animation
    private int lastAnimatedPosition = -1;

    public RecommendationAdapter(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    /** Call this BEFORE notifyDataSetChanged() when new results arrive. */
    public void setDetails(List<Detail> newDetails) {
        details = newDetails != null ? newDetails : new ArrayList<>();
        expanded.clear();
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
        Detail detail = position < details.size() ? details.get(position) : null;
        boolean hasDetail = detail != null && detail.why != null && !detail.why.isEmpty();

        // Split "Title - 95% Match" for display only
        String full = rec.careerPath;
        String subLine = null;
        int split = full.indexOf(" - ");
        if (split > 0 && split < full.length() - 3) {
            holder.textViewCareerPath.setText(full.substring(0, split).trim());
            subLine = full.substring(split + 3).trim();
        } else {
            holder.textViewCareerPath.setText(full);
        }

        if (subLine != null) {
            holder.textViewReason.setText(hasDetail ? subLine + "  •  Tap for details" : subLine);
            holder.textViewReason.setVisibility(View.VISIBLE);
        } else {
            holder.textViewReason.setVisibility(View.GONE);
        }
        holder.tvRank.setText(String.valueOf(position + 1));

        // Expandable details
        if (hasDetail) {
            holder.tvWhy.setText(detail.why);
            holder.tvSteps.setText(detail.steps);
            holder.tvCareers.setText(detail.careers);
            holder.tvArrow.setVisibility(View.VISIBLE);

            boolean open = expanded.contains(position);
            holder.layoutDetails.setVisibility(open ? View.VISIBLE : View.GONE);
            holder.tvArrow.setRotation(open ? 180f : 0f);

            holder.card.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                ViewGroup parent = (ViewGroup) holder.itemView.getParent();
                if (parent != null) {
                    TransitionManager.beginDelayedTransition(parent);
                }

                if (expanded.contains(pos)) {
                    expanded.remove(pos);
                    holder.layoutDetails.setVisibility(View.GONE);
                    holder.tvArrow.animate().rotation(0f).setDuration(200L).start();
                } else {
                    expanded.add(pos);
                    holder.layoutDetails.setVisibility(View.VISIBLE);
                    holder.tvArrow.animate().rotation(180f).setDuration(200L).start();
                }
            });
        } else {
            holder.layoutDetails.setVisibility(View.GONE);
            holder.tvArrow.setVisibility(View.GONE);
            holder.card.setOnClickListener(null);
            holder.card.setClickable(false);
        }

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
        TextView textViewReason;
        TextView tvRank;
        TextView tvArrow;
        TextView tvWhy;
        TextView tvSteps;
        TextView tvCareers;
        View layoutDetails;
        View card;

        ViewHolder(View itemView) {
            super(itemView);
            textViewCareerPath = itemView.findViewById(R.id.textViewCareerPath);
            textViewReason = itemView.findViewById(R.id.textViewReason);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvArrow = itemView.findViewById(R.id.tvArrow);
            tvWhy = itemView.findViewById(R.id.tvWhy);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvCareers = itemView.findViewById(R.id.tvCareers);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            card = itemView.findViewById(R.id.cardRecommendationItem);
        }
    }
}