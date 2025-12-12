package com.simplenotes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;


public class NoteActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextContent;
    private Button buttonSave;
    private TextInputLayout layoutTitle;
    private TextInputLayout layoutContent;
    
    private Note currentNote;
    private boolean isNewNote = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        initViews();
        checkIntentData();
        setupSaveButton();
    }

    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);
        buttonSave = findViewById(R.id.buttonSave);
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
            }
        }
    }

    private void setupSaveButton() {
        buttonSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        // Validate input
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter a title or content", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update note data
        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setTimestamp(System.currentTimeMillis());

        // Return result to NotesListActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("note", currentNote);
        setResult(RESULT_OK, resultIntent);

        // Close this activity
        finish();
    }

    @Override
    public void onBackPressed() {
        // Check if there are unsaved changes
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();
        
        if (!title.isEmpty() || !content.isEmpty()) {
            // There are unsaved changes - you could show a dialog here
            // For now, we'll just save automatically
            saveNote();
        } else {
            super.onBackPressed();
        }
    }
}