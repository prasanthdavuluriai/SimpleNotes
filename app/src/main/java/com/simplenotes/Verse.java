package com.simplenotes;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "verses", indices = {
        @Index(value = { "translationId", "book", "chapter", "verse" }, unique = true) })
public class Verse {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String translationId; // e.g., "kjv"
    private String book; // e.g., "John"
    private int chapter; // e.g., 3
    private int verse; // e.g., 16
    private String text; // Content of the verse

    public Verse(String translationId, String book, int chapter, int verse, String text) {
        this.translationId = translationId;
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTranslationId() {
        return translationId;
    }

    public void setTranslationId(String translationId) {
        this.translationId = translationId;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public int getVerse() {
        return verse;
    }

    public void setVerse(int verse) {
        this.verse = verse;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
