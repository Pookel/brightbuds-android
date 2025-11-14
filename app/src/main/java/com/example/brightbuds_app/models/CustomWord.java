package com.example.brightbuds_app.models;

/**
 * CustomWord model
 */
public class CustomWord {

    private String word;
    private String imageUrl;
    private long createdAt;
    private int plays;
    private String parentId;
    private String childId;

    // Default constructor (required for Firebase)
    public CustomWord() {
    }

    public CustomWord(String word, String parentId, long createdAt, int plays) {
        this.word = word;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.plays = plays;
    }

    // Getters and Setters
    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getPlays() {
        return plays;
    }

    public void setPlays(int plays) {
        this.plays = plays;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    @Override
    public String toString() {
        return "CustomWord{" +
                "word='" + word + '\'' +
                ", parentId='" + parentId + '\'' +
                ", childId='" + childId + '\'' +
                '}';
    }
}