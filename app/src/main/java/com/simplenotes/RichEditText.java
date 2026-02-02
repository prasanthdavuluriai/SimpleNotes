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
                // Add "Refer to.." option
                menu.add(android.view.Menu.NONE, 9991, android.view.Menu.NONE, "Refer to..");
                return callback.onCreateActionMode(mode, menu);
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return callback.onPrepareActionMode(mode, menu);
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                if (item.getItemId() == 9991) {
                    if (referToListener != null) { // referToListener
                        int start = getSelectionStart();
                        int end = getSelectionEnd();
                        if (start >= 0 && end > start) {
                            String selectedText = getText().subSequence(start, end).toString();
                            referToListener.onReferToRequested(selectedText, start, end);
                            mode.finish(); // Check if we want to close menu
                            return true;
                        }
                    }
                }
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

                // 2. Top Boundary Logic (Force "Internal" Positioning)
                // The system often refuses to render "Below" the selection, preferring "Above".
                // Instead of trying to force a "Below" flip (which failed), we offset the
                // anchor DOWN.
                // If selection is at the top (< 150), we say it starts at 150.
                // The system will render the menu ABOVE 150 (e.g. at 100).
                // This keeps the menu INSIDE the view (Below the Red Line), protecting the
                // header.
                // Side effect: It covers the first few lines of text, but strictly obeys the
                // boundary.
                if (outRect.top < 150) {
                    outRect.top = 150;
                    // Ensure bottom is valid relative to new top
                    if (outRect.bottom < outRect.top) {
                        outRect.bottom = outRect.top + 1;
                    }
                }
            }
        };

        return super.startActionMode(wrappedCallback, type);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        // [FIX] REVERTED: Do NOT clean text here.
        // Cleaning here shortens the text, but super.performFiltering() uses
        // getSelectionEnd()
        // which returns the cursor position in the ORIGINAL (raw) text.
        // behavior: cursor > text.length() -> IndexOutOfBoundsException -> CRASH.
        super.performFiltering(text, keyCode);
    }

    @Override
    protected void performFiltering(CharSequence text, int start, int end, int keyCode) {
        // [FIX] Clean invisible markers from the token range before filtering
        // We do this HERE because 'start' and 'end' are valid indices into the RAW
        // 'text'.
        try {
            if (start >= 0 && end <= text.length() && start <= end) {
                CharSequence rawToken = text.subSequence(start, end);
                String cleanToken = rawToken.toString().replaceAll("\u200C|\\{\\d+\\}|\u200D", "");
                getFilter().filter(cleanToken, this);
            } else {
                getFilter().filter("", this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fail safely
            getFilter().filter("", this);
        }
    }

    public interface OnReferToListener {
        void onReferToRequested(String text, int start, int end);
    }

    private OnReferToListener referToListener;

    public void setOnReferToListener(OnReferToListener listener) {
        this.referToListener = listener;
    }
}
