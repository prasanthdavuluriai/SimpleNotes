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
    private List<Note> notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

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
        notes = new ArrayList<>();
        notesAdapter = new NotesAdapter(notes);
        notesAdapter.setOnNoteClickListener(this::onNoteClicked);
        
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(notesAdapter);
    }

    private void setupFab() {
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("isNewNote", true);
            startActivityForResult(intent, 100);
        });
    }

    private void loadNotes() {
        // For now, load from shared preferences or create some sample data
        // In a real app, this would come from a database
        updateEmptyState();
    }

    private void updateEmptyState() {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Note newNote = (Note) data.getSerializableExtra("note");
            if (newNote != null) {
                notes.add(0, newNote);
                notesAdapter.notifyItemInserted(0);
                updateEmptyState();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the notes list when returning from NoteActivity
        updateEmptyState();
    }
}