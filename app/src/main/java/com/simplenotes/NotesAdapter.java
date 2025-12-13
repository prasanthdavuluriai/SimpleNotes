package com.simplenotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteClickListener onNoteClickListener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NotesAdapter() {
        this.notes = new ArrayList<>();
    }

    public NotesAdapter(List<Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.onNoteClickListener = listener;
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes.clear();
        if (newNotes != null) {
            this.notes.addAll(newNotes);
        }
        notifyDataSetChanged();
    }

    public void addNote(Note note) {
        notes.add(0, note);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewContent;
        private TextView textViewTimestamp;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);

            itemView.setOnClickListener(v -> {
                if (onNoteClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onNoteClickListener.onNoteClick(notes.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Note note) {
            textViewTitle.setText(note.getTitle().isEmpty() ? "Untitled" : note.getTitle());
            textViewContent.setText(note.getContent().isEmpty() ? "No content" : note.getContent());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
            textViewTimestamp.setText(formattedDate);
        }
    }
}