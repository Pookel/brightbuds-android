package com.example.brightbuds_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.FamilyMember;

import java.io.File;
import java.util.List;

public class FamilyMembersAdapter extends RecyclerView.Adapter<FamilyMembersAdapter.ViewHolder> {

    // FIELDS
    private final List<FamilyMember> familyMembers;           // List of members to display
    private final OnFamilyMemberClickListener listener;       // Click listener callback

    // INTERFACE
    /** Callback interface used by activities to handle tap events. */
    public interface OnFamilyMemberClickListener {
        void onFamilyMemberClick(FamilyMember member);
    }

    // CONSTRUCTOR
    public FamilyMembersAdapter(List<FamilyMember> familyMembers,
                                OnFamilyMemberClickListener listener) {
        this.familyMembers = familyMembers;
        this.listener = listener;
    }

    // Create a new ViewHolder (inflate item layout)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    // Bind data to each ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = familyMembers.get(position);

        // Set text fields
        holder.textName.setText(member.getName());
        holder.textRelationship.setText(member.getRelationship());

        // Load photo source
        if (member.getLocalPath() != null && !member.getLocalPath().isEmpty()) {
            File localFile = new File(member.getLocalPath());
            if (localFile.exists()) {
                // Load from internal storage — secure, offline, fast
                Glide.with(holder.itemView.getContext())
                        .load(localFile)
                        .placeholder(getDefaultImageResource(member.getRelationship()))
                        .error(getDefaultImageResource(member.getRelationship()))
                        .into(holder.imagePhoto);
            } else {
                // File missing locally — fallback to placeholder
                holder.imagePhoto.setImageResource(
                        getDefaultImageResource(member.getRelationship()));
            }

        } else if (member.getImageUrl() != null && !member.getImageUrl().isEmpty()) {
            // Optional legacy support: load from Firebase URL if provided
            Glide.with(holder.itemView.getContext())
                    .load(member.getImageUrl())
                    .placeholder(getDefaultImageResource(member.getRelationship()))
                    .error(getDefaultImageResource(member.getRelationship()))
                    .into(holder.imagePhoto);

        } else {
            // Default built-in drawable if nothing available
            holder.imagePhoto.setImageResource(
                    getDefaultImageResource(member.getRelationship()));
        }

        // Handle click events (speak name, etc.)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFamilyMemberClick(member);
        });
    }

    // Utility: choose correct default placeholder by relationship type
    private int getDefaultImageResource(String relationship) {
        if (relationship == null) return R.drawable.default_family_member;

        switch (relationship.toLowerCase()) {
            case "mother":
            case "mom":
                return R.drawable.default_mom;
            case "father":
            case "dad":
                return R.drawable.default_dad;
            case "grandmother":
            case "grandma":
                return R.drawable.default_grandma;
            case "grandfather":
            case "grandpa":
                return R.drawable.default_grandpa;
            case "sister":
                return R.drawable.default_sister;
            case "brother":
                return R.drawable.default_brother;
            default:
                return R.drawable.default_family_member;
        }
    }

    // Required Adapter Methods
    @Override
    public int getItemCount() {
        return familyMembers.size();
    }

    // ViewHolder Inner Class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePhoto;
        TextView textName;
        TextView textRelationship;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imageFamilyMember);
            textName = itemView.findViewById(R.id.textFamilyMemberName);
            textRelationship = itemView.findViewById(R.id.textFamilyMemberRelationship);
        }
    }
}
