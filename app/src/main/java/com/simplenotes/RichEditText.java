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
        // Force the Action Mode to be at the TOP (Primary) instead of Floating.
        // This prevents the menu from covering the formatting toolbar which sits just
        // above the text.
        return super.startActionMode(callback, android.view.ActionMode.TYPE_PRIMARY);
    }
}
