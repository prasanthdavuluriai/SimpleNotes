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