package com.simplenotes;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "bible_versions")
public class BibleVersion {
    @PrimaryKey
    @NonNull
    private String id; // e.g., "kjv"
    private String name;
    private boolean isDownloaded;

    public BibleVersion(@NonNull String id, String name, boolean isDownloaded) {
        this.id = id;
        this.name = name;
        this.isDownloaded = isDownloaded;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }
}
