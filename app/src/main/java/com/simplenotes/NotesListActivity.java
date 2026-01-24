package com.simplenotes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class NotesListActivity extends AppCompatActivity {
    private RecyclerView recyclerViewNotes;
    private FloatingActionButton fabAddNote;
    private TextView textViewEmpty;
    private NotesAdapter notesAdapter;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        database = AppDatabase.getDatabase(this);

        initViews();
        setupRecyclerView();
        setupFab();
        loadNotes();
    }

    private void initViews() {
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        fabAddNote = findViewById(R.id.fabAddNote);
        textViewEmpty = findViewById(R.id.textViewEmpty);
    }

    private void setupRecyclerView() {
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new NotesAdapter(null);
        notesAdapter.setOnNoteClickListener(this::openNoteActivity);
        notesAdapter.setOnNoteActionListener(this::handleNoteAction);
        recyclerViewNotes.setAdapter(notesAdapter);
    }

    private void openNoteActivity(Note note) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isNewNote", false);
        startActivity(intent);
    }

    private void setupFab() {
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("isNewNote", true);
            startActivity(intent); // No result needed, DB handles sync
        });
    }

    private void handleNoteAction(Note note, NotesAdapter.Action action) {
        switch (action) {
            case DELETE:
                deleteNote(note);
                break;
            case PIN:
                togglePin(note, true);
                break;
            case UNPIN:
                togglePin(note, false);
                break;
            case SHARE:
                shareNoteAsFile(note);
                break;
        }
    }

    private void shareNoteAsFile(Note note) {
        try {
            // Create shared_notes directory in cache if it doesn't exist
            File cachePath = new File(getCacheDir(), "shared_notes");
            cachePath.mkdirs();

            // Create the file: Title.html
            String fileName = (note.getTitle().isEmpty() ? "Untitled" : note.getTitle()) + ".html";
            // Sanitize filename to avoid issues with special chars
            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");

            File newFile = new File(cachePath, fileName);

            // Write content
            FileOutputStream stream = new FileOutputStream(newFile);
            String rawContent = note.getContent() == null ? "" : note.getContent();
            String finalContent = rawContent;

            if (com.simplenotes.utils.RichTextUtils.isJson(rawContent)) {
                try {
                    // JSON -> Spannable -> HTML
                    android.text.Spannable text = com.simplenotes.utils.RichTextUtils.fromJson(rawContent);
                    finalContent = android.text.Html.toHtml(text,
                            android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
                } catch (Exception e) {
                    finalContent = rawContent; // Fallback
                }
            } else {
                // Legacy HTML or Plain Text
                // If it looks like HTML, keep it for .html export
                finalContent = rawContent;
            }

            stream.write(finalContent.getBytes());
            stream.close();

            // Get URI
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);

            // Share Intent
            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, "text/html");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share Note via"));
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error sharing note", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void deleteNote(Note note) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().delete(note);
            loadNotes();
        });
    }

    private void togglePin(Note note, boolean pin) {
        note.setPinned(pin);
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().update(note);
            loadNotes();
        });
    }

    private void loadNotes() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Note> notes = database.noteDao().getAllNotes();
            AppExecutors.getInstance().mainThread().execute(() -> {
                notesAdapter.updateNotes(notes);
                updateEmptyState(notes);
            });
        });
    }

    private void updateEmptyState(List<Note> notes) {
        if (notes.isEmpty()) {
            recyclerViewNotes.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerViewNotes.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data every time we come back
        loadNotes();
    }
}