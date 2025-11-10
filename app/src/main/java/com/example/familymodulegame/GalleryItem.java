package com.example.familymodulegame;

public class GalleryItem {
    // Use drawable resource id for local images
    private final int imageResId;
    private final String name;
    private final String relationship;

    public GalleryItem(int imageResId, String name, String relationship) {
        this.imageResId = imageResId;
        this.name = name;
        this.relationship = relationship;
    }

    public int getImageResId() { return imageResId; }
    public String getName() { return name; }
    public String getRelationship() { return relationship; }
}
