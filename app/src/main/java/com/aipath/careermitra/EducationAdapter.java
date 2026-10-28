package com.aipath.careermitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.ViewHolder> {

    private ArrayList<String> educationList;
    private ArrayList<String> selectedEducations;

    // UI: remembers which rows already played their entrance animation
    private int lastAnimatedPosition = -1;

    public EducationAdapter(ArrayList<String> educationList) {
        this.educationList = educationList;
        this.selectedEducations = new ArrayList<>();
    }

    public ArrayList<String> getSelectedEducations() {
        return selectedEducations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_education, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String edu = educationList.get(position);
        holder.checkBox.setText(edu);

        // UI: restore the visual state of a recycled row BEFORE attaching the
        // listener, so this does not trigger add/remove. Remove these two
        // lines if you want the exact original behaviour.
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedEducations.contains(edu));
        applySelectedLook(holder.itemRoot, holder.checkBox.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedEducations.add(edu);
            else selectedEducations.remove(edu);

            // UI: highlight and bounce
            applySelectedLook(holder.itemRoot, isChecked);
            holder.itemRoot.animate().scaleX(1.03f).scaleY(1.03f).setDuration(100L)
                    .withEndAction(() -> holder.itemRoot.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(180L)
                            .setInterpolator(new OvershootInterpolator(2f))
                            .start())
                    .start();
        });

        // UI: tapping anywhere on the row toggles the checkbox
        holder.itemRoot.setOnClickListener(v -> holder.checkBox.toggle());

        // UI: staggered slide-in the first time each row appears
        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(60f);
            holder.itemView.animate()
                    .alpha(1f).translationY(0f)
                    .setStartDelay(position * 70L)
                    .setDuration(450L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
            lastAnimatedPosition = position;
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        // UI: stop pending animations so recycled rows don't flicker
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return educationList.size();
    }

    // UI helper
    private void applySelectedLook(View row, boolean selected) {
        row.setBackgroundResource(selected
                ? R.drawable.bg_education_item_selected
                : R.drawable.bg_input_field);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        View itemRoot; // UI: the row container

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBoxEducation);
            itemRoot = itemView.findViewById(R.id.itemRoot);
        }
    }
}