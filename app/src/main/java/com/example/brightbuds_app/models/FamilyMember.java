package com.example.brightbuds_app.models;
public class FamilyMember {

    // Fields
    private String name;

    private String relationship;

    private String imageUrl;

    private String localPath;

    private String parentId;

    private long createdAt;

    // Constructors
    /** Empty constructor required for Firestore or deserialization libraries. */
    public FamilyMember() {}

    public FamilyMember(String name, String relationship, int imageResource, String imageUrl) {
        this.name = name;
        this.relationship = relationship;
        this.imageUrl = imageUrl;
        this.localPath = null; // placeholder, no local file
    }

    public FamilyMember(String name, String relationship, String localPath) {
        this.name = name;
        this.relationship = relationship;
        this.localPath = localPath;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Utility / Debug
    @Override
    public String toString() {
        return "FamilyMember{" +
                "name='" + name + '\'' +
                ", relationship='" + relationship + '\'' +
                ", localPath='" + localPath + '\'' +
                ", parentId='" + parentId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
