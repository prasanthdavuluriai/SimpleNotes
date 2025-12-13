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
    // Removed local list 'notes' as we will fetch fresh from DB

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
        notesAdapter = new NotesAdapter(new ArrayList<>()); // Empty init
        notesAdapter.setOnNoteClickListener(this::onNoteClicked);

        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(notesAdapter);
    }

    private void setupFab() {
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("isNewNote", true);
            startActivity(intent); // No result needed, DB handles sync
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

    private void onNoteClicked(Note note) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isNewNote", false);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data every time we come back
        loadNotes();
    }
}