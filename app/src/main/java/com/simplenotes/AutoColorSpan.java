package com.simplenotes;

import android.text.style.ForegroundColorSpan;

/**
 * A marker subclass of ForegroundColorSpan used to distinguish
 * programmatically applied colors (like Bible verses or Highlight text)
 * from user-applied Rich Text colors.
 * 
 * This allows applyStyling() to clear only its own spans while preserving
 * user formating.
 */
public class AutoColorSpan extends ForegroundColorSpan {
    public AutoColorSpan(int color) {
        super(color);
    }
}
