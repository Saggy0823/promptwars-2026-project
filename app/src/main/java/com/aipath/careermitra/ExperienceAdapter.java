package com.aipath.careermitra;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ExperienceAdapter extends RecyclerView.Adapter<ExperienceAdapter.ViewHolder> {

    private ArrayList<String> experienceList;

    // UI: tracks which rows already played their entrance animation
    private int lastAnimatedPosition = -1;

    public ExperienceAdapter(ArrayList<String> experienceList) {
        this.experienceList = experienceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_experience, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(formatExperience(experienceList.get(position)));

        // UI: always reset the INNER card first, so a row can never get stuck offset
        holder.card.animate().cancel();
        holder.card.setAlpha(1f);
        holder.card.setTranslationX(0f);
        holder.card.setTranslationY(0f);

        // UI: animate only the inner card (RecyclerView never touches it)
        if (position > lastAnimatedPosition) {
            holder.card.setAlpha(0f);
            holder.card.setTranslationY(50f);
            holder.card.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(450L)
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
        return experienceList.size();
    }

    // UI: role in bold on the first line, "duration • company" on the second.
    // Display only; the stored string is not changed.
    private CharSequence formatExperience(String exp) {
        String[] parts = exp.split(" \\| ");
        if (parts.length != 3) return exp;

        SpannableStringBuilder sb = new SpannableStringBuilder();
        int roleEnd = parts[0].length();
        sb.append(parts[0]);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, roleEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new AbsoluteSizeSpan(17, true), 0, roleEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        sb.append("\n");
        int detailStart = sb.length();
        sb.append(parts[2]).append("  •  ").append(parts[1]);
        sb.setSpan(new ForegroundColorSpan(0xFF6E6E73), detailStart, sb.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new AbsoluteSizeSpan(14, true), detailStart, sb.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        View card; // UI: the inner card that gets animated

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewExperience);
            card = itemView.findViewById(R.id.cardExperienceItem);
        }
    }
}