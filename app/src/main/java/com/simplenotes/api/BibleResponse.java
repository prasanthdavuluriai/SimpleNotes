package com.simplenotes.api;

public class BibleResponse {
    private String reference;
    private String text;
    private String translation_name;

    public String getReference() {
        return reference;
    }

    public String getText() {
        return text;
    }

    private java.util.List<Verse> verses;

    public java.util.List<Verse> getVerses() {
        return verses;
    }

    public String getTranslationName() {
        return translation_name;
    }

    public static class Verse {
        private String book_name;
        private int chapter;
        private int verse;
        private String text;

        public String getBookName() {
            return book_name;
        }

        public int getChapter() {
            return chapter;
        }

        public int getVerse() {
            return verse;
        }

        public String getText() {
            return text;
        }
    }
}
