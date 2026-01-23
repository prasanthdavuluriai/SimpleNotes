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

                int viewHeight = view.getHeight();
                int[] screenPos = new int[2];
                view.getLocationOnScreen(screenPos);
                int screenY = screenPos[1];

                // 1. Bottom Boundary Logic (Force UP)
                // If selection is near the bottom toolbar, extend rect to bottom to force menu
                // ABOVE.
                // Threshold: 200px (approx toolbar height + buffer)
                if (outRect.bottom > viewHeight - 200) {
                    outRect.bottom = viewHeight;
                }

                // 2. Top Boundary Logic (Force DOWN)
                // If selection is near the top of the view (e.g. first line), user wants menu
                // INSIDE the view.
                // We artificially extend the rect to the Top of the Screen (-screenY).
                // This makes the system think there is NO SPACE above, forcing it to render
                // BELOW the selection.
                // Threshold: 50px (approx first line height)
                if (outRect.top < 50) {
                    outRect.top = -screenY;
                }
            }
        };

        return super.startActionMode(wrappedCallback, type);
    }
}
