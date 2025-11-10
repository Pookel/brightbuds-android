package com.example.familymodulegame;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FamilyAdapter extends RecyclerView.Adapter<FamilyAdapter.ViewHolder> {

    Context context;
    String[] names;
    String[] relations;
    int[] images;

    public FamilyAdapter(Context context, String[] names, String[] relations, int[] images) {
        this.context = context;
        this.names = names;
        this.relations = relations;
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.family_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.familyName.setText(names[position]);
        holder.familyRelation.setText(relations[position]);
        holder.familyImage.setImageResource(images[position]);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView familyImage;
        TextView familyName, familyRelation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            familyImage = itemView.findViewById(R.id.familyImage);
            familyName = itemView.findViewById(R.id.familyName);
            familyRelation = itemView.findViewById(R.id.familyRelation);
        }
    }
}
