package com.simplenotes;

import java.io.Serializable;

public class Note implements Serializable {
    private long id;
    private String title;
    private String content;
    private long timestamp;

    public Note() {
        this.id = System.currentTimeMillis();
        this.title = "";
        this.content = "";
        this.timestamp = System.currentTimeMillis();
    }

    public Note(String title, String content) {
        this.id = System.currentTimeMillis();
        this.title = title;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

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