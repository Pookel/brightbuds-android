package com.example.familymodulegame;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private final List<GalleryItem> items;
    private final Context context;

    public static final String EXTRA_IMAGE_RES = "extra_image_res";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_RELATION = "extra_relation";

    public GalleryAdapter(Context context, List<GalleryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final GalleryItem item = items.get(position);
        holder.imageView.setImageResource(item.getImageResId());
        holder.textViewName.setText(item.getName());
        holder.textViewRelation.setText(item.getRelationship());

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, DetailActivity.class);
            i.putExtra(EXTRA_IMAGE_RES, item.getImageResId());
            i.putExtra(EXTRA_NAME, item.getName());
            i.putExtra(EXTRA_RELATION, item.getRelationship());
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolder must be INNER class of the Adapter ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewName;
        TextView textViewRelation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.galleryThumbnail);
            textViewName = itemView.findViewById(R.id.galleryName);
            textViewRelation = itemView.findViewById(R.id.galleryRelation);
        }
    }
}
