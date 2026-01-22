package com.simplenotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.graphics.drawable.GradientDrawable;

public class ColorBottomSheet extends BottomSheetDialogFragment {

    private String title;
    private int[] colors;
    private int selectedIndex = -1;
    private ColorListener listener;

    public interface ColorListener {
        void onColorSelected(int colorIndex);
    }

    public static ColorBottomSheet newInstance(String title, int[] colors, int selectedIndex) {
        ColorBottomSheet fragment = new ColorBottomSheet();
        fragment.title = title;
        fragment.colors = colors;
        fragment.selectedIndex = selectedIndex;
        return fragment;
    }

    public void setListener(ColorListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textViewTitle = view.findViewById(R.id.textViewTitle);
        View buttonClose = view.findViewById(R.id.buttonClose);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewColors);

        if (title != null) {
            textViewTitle.setText(title);
        }

        buttonClose.setOnClickListener(v -> dismiss());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        ColorAdapter adapter = new ColorAdapter();
        recyclerView.setAdapter(adapter);
    }

    private class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_swatch, parent, false);
            return new ColorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            int color = colors[position];
            holder.bind(color, position == selectedIndex);
        }

        @Override
        public int getItemCount() {
            return colors != null ? colors.length : 0;
        }

        class ColorViewHolder extends RecyclerView.ViewHolder {
            View viewColorSwatch;
            ImageView imageViewCheck;

            public ColorViewHolder(@NonNull View itemView) {
                super(itemView);
                viewColorSwatch = itemView.findViewById(R.id.viewColorSwatch);
                imageViewCheck = itemView.findViewById(R.id.imageViewCheck);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        selectedIndex = pos;
                        notifyDataSetChanged(); // Simple redraw to update checkmark
                        if (listener != null) {
                            listener.onColorSelected(pos);
                        }
                        // dismiss(); // Optional: Dismiss immediately or keep open?
                        // User design shows "X" button, implies manual close. But selecting usually
                        // closes in simple apps.
                        // I'll keep it open for now as per design pattern often seen in detailed
                        // pickers, or dismiss.
                        // Actually, screenshot shows checkmark. If checkmark is persistent, it might
                        // stay open.
                        // But usually simpler to close. I'll dismiss for efficiency.
                        // Wait, user said "change the color selection window...".
                        // If I dismiss immediately, I can't see the checkmark.
                        // I will NOT dismiss immediately. I'll let them click X or click outside.
                        // But I need to apply valid selection.
                        // I will apply immediately (listener.onColorSelected) but keep dialog open?
                        // If I apply immediately, the text changes behind the sheet. That's good
                        // feedback.
                    }
                });
            }

            void bind(int color, boolean isSelected) {
                // Create circle drawable
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(color);
                viewColorSwatch.setBackground(drawable);

                imageViewCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
        }
    }
}
