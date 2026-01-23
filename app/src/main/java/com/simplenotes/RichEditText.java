package com.simplenotes;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

public class RichEditText extends AppCompatMultiAutoCompleteTextView {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }

    private OnSelectionChangedListener selectionListener;

    public RichEditText(Context context) {
        super(context);
    }

    public RichEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RichEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback callback, int type) {
        // User prefers Floating mode, but we need to prevent it from overlapping the
        // bottom toolbar.
        // We wrap the callback to override onGetContentRect.
        android.view.ActionMode.Callback2 wrappedCallback = new android.view.ActionMode.Callback2() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return callback.onCreateActionMode(mode, menu);
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return callback.onPrepareActionMode(mode, menu);
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return callback.onActionItemClicked(mode, item);
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                callback.onDestroyActionMode(mode);
            }

            @Override
            public void onGetContentRect(android.view.ActionMode mode, android.view.View view,
                    android.graphics.Rect outRect) {
                if (callback instanceof android.view.ActionMode.Callback2) {
                    ((android.view.ActionMode.Callback2) callback).onGetContentRect(mode, view, outRect);
                } else {
                    super.onGetContentRect(mode, view, outRect);
                }

                // Smart Positioning:
                // Only modify the rect if the selection is near the bottom of the view.
                // This prevents the menu from appearing far below the text when checking
                // middle-screen content.
                // We define a threshold (e.g., 200px) that represents the danger zone near the
                // toolbar.
                int viewHeight = view.getHeight();
                int threshold = 200;

                // If the bottom of the selection is within the threshold distance of the view
                // bottom...
                if (outRect.bottom > viewHeight - threshold) {
                    // ...we extend the rect to the very bottom of the view.
                    // This tells the system "There is no usable space below this selection inside
                    // the view",
                    // effectively forcing it to render the menu ABOVE the text.
                    outRect.bottom = viewHeight;
                }
            }
        };

        return super.startActionMode(wrappedCallback, type);
    }
}
