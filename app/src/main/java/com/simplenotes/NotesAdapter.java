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
    private OnNoteActionListener onNoteActionListener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteActionListener {
        void onAction(Note note, Action action);
    }

    public enum Action {
        DELETE,
        PIN,
        UNPIN,
        SHARE
    }

    public NotesAdapter(List<Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.onNoteClickListener = listener;
    }

    public void setOnNoteActionListener(OnNoteActionListener listener) {
        this.onNoteActionListener = listener;
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes.clear();
        if (newNotes != null) {
            this.notes.addAll(newNotes);
        }
        notifyDataSetChanged();
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
        private android.widget.ImageView imageViewPin;
        private android.widget.ImageButton buttonMore;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            imageViewPin = itemView.findViewById(R.id.imageViewPin);
            buttonMore = itemView.findViewById(R.id.buttonMore);

            itemView.setOnClickListener(v -> {
                if (onNoteClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onNoteClickListener.onNoteClick(notes.get(getAdapterPosition()));
                }
            });

            buttonMore.setOnClickListener(v -> showPopupMenu(v, notes.get(getAdapterPosition())));
        }

        private void showPopupMenu(View view, Note note) {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
            popup.getMenu().add(0, 1, 0, note.isPinned() ? "Unpin" : "Pin");
            popup.getMenu().add(0, 2, 0, "Share");
            popup.getMenu().add(0, 3, 0, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                if (onNoteActionListener == null)
                    return false;

                switch (item.getItemId()) {
                    case 1:
                        onNoteActionListener.onAction(note, note.isPinned() ? Action.UNPIN : Action.PIN);
                        return true;
                    case 2:
                        onNoteActionListener.onAction(note, Action.SHARE);
                        return true;
                    case 3:
                        onNoteActionListener.onAction(note, Action.DELETE);
                        return true;
                    default:
                        return false;
                }
            });
            popup.show();
        }

        public void bind(Note note) {
            textViewTitle.setText(note.getTitle().isEmpty() ? "Untitled" : note.getTitle());
            if (note.getContent().isEmpty()) {
                textViewContent.setText("No content");
            } else {
                textViewContent.setText(
                        android.text.Html.fromHtml(note.getContent(), android.text.Html.FROM_HTML_MODE_LEGACY));
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
            textViewTimestamp.setText(formattedDate);

            imageViewPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
        }
    }
}