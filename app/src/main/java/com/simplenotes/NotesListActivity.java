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
    private android.view.Menu menu;

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
        notesAdapter.setOnNoteLongClickListener(this::onNoteLongClicked);

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
                notesAdapter.clearSelection();
                updateDeleteMenuState();
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
        if (notesAdapter.getSelectedCount() > 0) {
            notesAdapter.toggleSelection(note);
            updateDeleteMenuState();
        } else {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("note", note);
            intent.putExtra("isNewNote", false);
            startActivity(intent);
        }
    }

    private void onNoteLongClicked(Note note) {
        notesAdapter.toggleSelection(note);
        updateDeleteMenuState();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes_list, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDeleteMenuState() {
        if (menu != null) {
            android.view.MenuItem deleteItem = menu.findItem(R.id.action_delete);
            if (deleteItem != null) {
                boolean hasSelection = notesAdapter.getSelectedCount() > 0;
                deleteItem.setEnabled(hasSelection);

                // Update icon appearance (simple alpha change or tint can be used)
                android.graphics.drawable.Drawable icon = deleteItem.getIcon();
                if (icon != null) {
                    icon.setAlpha(hasSelection ? 255 : 130);
                }
            }
        }
    }

    private void deleteSelectedNotes() {
        List<Note> selectedNotes = notesAdapter.getSelectedNotes();
        AppExecutors.getInstance().diskIO().execute(() -> {
            for (Note note : selectedNotes) {
                database.noteDao().delete(note);
            }
            AppExecutors.getInstance().mainThread().execute(this::loadNotes);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data every time we come back
        loadNotes();
    }
}