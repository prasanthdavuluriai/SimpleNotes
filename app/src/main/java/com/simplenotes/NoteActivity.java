package com.simplenotes;

import android.content.Intent;
import android.os.Bundle;

import android.widget.Toast;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NoteActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView editTextContent;

    private TextInputLayout layoutTitle;
    private TextInputLayout layoutContent;

    // Version Switcher
    private android.widget.TextView textViewVersion;
    private android.widget.ImageButton buttonVersion;
    private String currentTranslation = "web"; // Default
    private java.util.Map<String, String> bibleVersions;

    private Note currentNote;
    private boolean isNewNote = true;

    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        database = AppDatabase.getDatabase(this);

        initViews();
        checkIntentData();

        initializeBibleVersions();
        setupVersionSwitcher();
        setupMagicFetch();
    }

    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);

        layoutTitle = findViewById(R.id.layoutTitle);
        layoutContent = findViewById(R.id.layoutContent);

        textViewVersion = findViewById(R.id.textViewVersion);
        buttonVersion = findViewById(R.id.buttonVersion);
    }

    private void checkIntentData() {
        Intent intent = getIntent();

        if (intent != null) {
            isNewNote = intent.getBooleanExtra("isNewNote", true);

            if (!isNewNote) {
                currentNote = (Note) intent.getSerializableExtra("note");
                if (currentNote != null) {
                    editTextTitle.setText(currentNote.getTitle());
                    editTextContent.setText(currentNote.getContent());
                    applyVerseStyling();
                    setTitle(R.string.edit_note);
                }
            } else {
                setTitle(R.string.new_note);
                currentNote = new Note();
                // Immediate save for new notes
                AppExecutors.getInstance().diskIO().execute(() -> {
                    database.noteDao().insert(currentNote);
                });
            }
        }
    }

    private void initializeBibleVersions() {
        bibleVersions = new java.util.LinkedHashMap<>();
        bibleVersions.put("Cherokee New Testament", "cherokee");
        bibleVersions.put("Chinese Union Version", "cuv");
        bibleVersions.put("Czech Bible kralick", "bkr"); // Note: encoding might need care, using simple chars if needed
                                                         // or unicode
        bibleVersions.put("American Standard (1901)", "asv");
        bibleVersions.put("Bible in Basic English", "bbe");
        bibleVersions.put("Darby Bible", "darby");
        bibleVersions.put("Douay-Rheims 1899", "dra");
        bibleVersions.put("King James Version", "kjv");
        bibleVersions.put("World English Bible", "web");
        bibleVersions.put("Young's Literal (NT)", "ylt");
        bibleVersions.put("Open English (Commonwealth)", "oeb-cw");
        bibleVersions.put("Open English (US)", "oeb-us");
        bibleVersions.put("World English (British)", "webbe");
        bibleVersions.put("Latin Vulgate", "clementine");
        bibleVersions.put("Portuguese Almeida", "almeida");
        bibleVersions.put("Romanian Corrected", "rccv");
    }

    private void setupVersionSwitcher() {
        buttonVersion.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
            int index = 0;
            for (String name : bibleVersions.keySet()) {
                popup.getMenu().add(0, index++, 0, name);
            }

            popup.setOnMenuItemClickListener(item -> {
                String selectedName = item.getTitle().toString();
                currentTranslation = bibleVersions.get(selectedName);
                textViewVersion.setText("Bible Version: " + selectedName); // Update text
                Toast.makeText(this, "Set to: " + selectedName, Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        // Restore default text
        textViewVersion.setText("Bible Version: World English Bible");
    }

    private void setupMagicFetch() {
        // Setup Autocomplete
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                R.layout.item_autocomplete, BibleData.BOOKS);
        editTextContent.setAdapter(adapter);
        editTextContent.setTokenizer(new BibleTokenizer());

        editTextContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() > 0 && s.charAt(s.length() - 1) == ' ') {
                    checkForBibleReference(s.toString());
                }
            }
        });
    }

    private static class BibleTokenizer
            implements androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView.Tokenizer {
        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != '@') {
                i--;
            }
            if (i > 0 && text.charAt(i - 1) == '@') {
                return i;
            }
            return cursor; // No @ found, default behavior (won't trigger)
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == ' ' || text.charAt(i) == '\n') {
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            if (i > 0 && Character.isLetterOrDigit(text.charAt(i - 1))) {
                return text + " ";
            } else {
                if (text instanceof android.text.Spanned) {
                    SpannableStringBuilder sp = new SpannableStringBuilder(text);
                    sp.append(" ");
                    return sp;
                } else {
                    return text + " ";
                }
            }
        }
    }

    private void checkForBibleReference(String text) {
        // Regex to find @Book Chapter:Verse pattern (e.g., @John 3:16 or @1 Samuel 1:1)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9\\s]+ \\d+:\\d+) $");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String reference = matcher.group(1).trim();
            fetchVerse(reference);
        }
    }

    private void fetchVerse(String reference) {
        com.simplenotes.api.ApiClient.getService().getVerse(reference, currentTranslation)
                .enqueue(new retrofit2.Callback<com.simplenotes.api.BibleResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.simplenotes.api.BibleResponse> call,
                            retrofit2.Response<com.simplenotes.api.BibleResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String verseText = response.body().getText();
                            if (verseText != null) {
                                appendVerseToContent(verseText);
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.simplenotes.api.BibleResponse> call, Throwable t) {
                        Toast.makeText(NoteActivity.this, "Failed to fetch verse", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void appendVerseToContent(String verseText) {
        String currentContent = editTextContent.getText().toString();

        SpannableStringBuilder ssb = new SpannableStringBuilder(currentContent);

        if (!currentContent.isEmpty() && !currentContent.endsWith("\n")) {
            ssb.append("\n");
        }

        // Invisible marker to identify Bible verses for styling persistence
        String marker = "\u200B";
        String formattedVerse = marker + "\"" + verseText + "\"" + marker;

        ssb.append(formattedVerse);

        editTextContent.setText(ssb);
        editTextContent.setSelection(ssb.length());

        applyVerseStyling();
    }

    private void applyVerseStyling() {
        android.text.Editable text = editTextContent.getText();
        String content = text.toString();

        // Regex to find content wrapped in invisible markers: \u200B"..."\u200B
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\u200B(.*?)\u200B");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        int textColor = ContextCompat.getColor(this, R.color.bible_gold);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Apply Bold and Gold
            text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote();
    }

    private void saveNote() {
        if (currentNote == null)
            return;

        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        // Update note data in memory
        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setTimestamp(System.currentTimeMillis());

        // Save to DB (Using insert with REPLACE acts as Upsert, ensuring it saves even
        // if initial insert missed)
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().insert(currentNote);
        });
    }
}