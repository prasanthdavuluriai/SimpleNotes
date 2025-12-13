package com.simplenotes;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "notes")
public class Note implements Serializable {
    @PrimaryKey
    private long id;
    private String title;
    private String content;
    private long timestamp;
    private boolean isPinned;

    public Note() {
        this.id = System.currentTimeMillis();
        this.title = "";
        this.content = "";
        this.timestamp = System.currentTimeMillis();
    }

    @Ignore
    public Note(String title, String content) {
        this.id = System.currentTimeMillis();
        this.title = title;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    @Ignore
    public Note(long id, String title, String content, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}