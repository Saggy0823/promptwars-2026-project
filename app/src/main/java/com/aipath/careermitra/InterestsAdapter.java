package com.aipath.careermitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class InterestsAdapter extends RecyclerView.Adapter<InterestsAdapter.ViewHolder> {

    private ArrayList<String> interestsList;
    private int lastAnimatedPosition = -1; // UI

    public InterestsAdapter(ArrayList<String> interestsList) {
        this.interestsList = interestsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interest, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(interestsList.get(position));

        // UI: reset the inner view first so it can never stay shifted
        holder.textView.animate().cancel();
        holder.textView.setAlpha(1f);
        holder.textView.setTranslationX(0f);
        holder.textView.setTranslationY(0f);

        if (position > lastAnimatedPosition) {
            holder.textView.setAlpha(0f);
            holder.textView.setTranslationY(50f); // vertical, so it can't look "right-sided"
            holder.textView.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(450L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        holder.textView.animate().cancel();
        holder.textView.setAlpha(1f);
        holder.textView.setTranslationX(0f);
        holder.textView.setTranslationY(0f);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return interestsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewInterest);
        }
    }
}