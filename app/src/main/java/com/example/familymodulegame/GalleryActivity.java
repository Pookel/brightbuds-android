package com.example.familymodulegame;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GalleryAdapter adapter;
    List<GalleryItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        recyclerView = findViewById(R.id.recyclerGallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns

        items = new ArrayList<>();
        // --- ADD YOUR ITEMS HERE (sample items) ---
        // Replace sample1/sample2/sample3 with your own drawable names (in res/drawable).
        items.add(new GalleryItem(R.drawable.mother, "Megan", "Daughter"));
        items.add(new GalleryItem(R.drawable.father, "Uncle Bob", "Uncle"));
        items.add(new GalleryItem(R.drawable.sister, "Nana", "Grandmother"));
        // Add more items below:
        // items.add(new GalleryItem(R.drawable.my_aunt, "Aunt Liza", "Aunt"));
        // -------------------------------------------

        adapter = new GalleryAdapter(this, items);
        recyclerView.setAdapter(adapter);
    }
}

