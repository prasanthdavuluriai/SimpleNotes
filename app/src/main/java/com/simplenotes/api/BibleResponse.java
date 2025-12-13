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

    public String getTranslationName() {
        return translation_name;
    }
}
