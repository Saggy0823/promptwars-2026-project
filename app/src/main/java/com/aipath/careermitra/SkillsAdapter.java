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

public class SkillsAdapter extends RecyclerView.Adapter<SkillsAdapter.ViewHolder> {

    private ArrayList<String> skillsList;
    private ArrayList<String> selectedSkills;

    // UI: tracks which rows already played their entrance animation
    private int lastAnimatedPosition = -1;

    public SkillsAdapter(ArrayList<String> skillsList, ArrayList<String> preSelectedSkills) {
        this.skillsList = skillsList;
        this.selectedSkills = new ArrayList<>(preSelectedSkills);
    }

    public ArrayList<String> getSelectedSkills() {
        return selectedSkills;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_skill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String skill = skillsList.get(position);
        holder.checkBox.setText(skill);

        // UI/safety: detach the old listener first. A recycled row still holds
        // the previous skill's listener, which would otherwise fire during
        // setChecked() below and change the wrong skill.
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedSkills.contains(skill));
        applySelectedLook(holder.itemRoot, holder.checkBox.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !selectedSkills.contains(skill)) selectedSkills.add(skill);
            else selectedSkills.remove(skill);

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

        // UI: staggered slide-in the first time a row appears (also plays for newly added skills)
        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(80f);
            holder.itemView.animate()
                    .alpha(1f).translationX(0f)
                    .setStartDelay(Math.min(position, 6) * 70L)
                    .setDuration(450L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
            lastAnimatedPosition = position;
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationX(0f);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        // UI: stop pending animations so recycled rows don't flicker
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationX(0f);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return skillsList.size();
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
            checkBox = itemView.findViewById(R.id.checkBoxSkill);
            itemRoot = itemView.findViewById(R.id.skillItemRoot);
        }
    }
}