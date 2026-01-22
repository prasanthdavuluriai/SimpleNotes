package com.simplenotes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import androidx.annotation.NonNull;

/**
 * A span that makes the covered text completely invisible and take up zero
 * width.
 * Used for hiding persistence markers like \u200C{0}.
 */
public class HiddenSpan extends ReplacementSpan {

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        // Return 0 width so it doesn't affect layout
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
            @NonNull Paint paint) {
        // Do nothing - render no text
    }
}
