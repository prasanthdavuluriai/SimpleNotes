package com.simplenotes;

import android.content.Intent;
import android.os.Bundle;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NoteActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextContent;

    private TextInputLayout layoutTitle;
    private TextInputLayout layoutContent;

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

        setupMagicFetch();
    }

    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);

        layoutTitle = findViewById(R.id.layoutTitle);
        layoutContent = findViewById(R.id.layoutContent);
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

    private void setupMagicFetch() {
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
        com.simplenotes.api.ApiClient.getService().getVerse(reference)
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
        String currentText = editTextContent.getText().toString();
        // Remove the trailing space that triggered the fetch
        // and append the verse
        String newText = currentText.trim() + "\n\"" + verseText.trim() + "\"\n";
        editTextContent.setText(newText);
        editTextContent.setSelection(newText.length());
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

        // Save to DB (Update only, since insert happened at creation)
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().update(currentNote);
        });
    }
}