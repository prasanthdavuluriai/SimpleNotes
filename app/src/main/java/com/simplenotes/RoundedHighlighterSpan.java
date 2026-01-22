package com.simplenotes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spanned;
import android.text.style.LineBackgroundSpan;
import androidx.annotation.NonNull;

public class RoundedHighlighterSpan implements LineBackgroundSpan {

    private final int backgroundColor;
    private final float cornerRadius;
    // Add extra padding for that "widget" feel
    private final float paddingHorizontal = 8f;
    private final float paddingVertical = 2f;

    public RoundedHighlighterSpan(int backgroundColor, float cornerRadius) {
        this.backgroundColor = backgroundColor;
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void drawBackground(@NonNull Canvas c, @NonNull Paint p,
            int left, int right, int top, int baseline, int bottom,
            @NonNull CharSequence text, int lineStart, int lineEnd, int lnum) {

        if (!(text instanceof Spanned))
            return;
        Spanned spanned = (Spanned) text;

        int spanStart = spanned.getSpanStart(this);
        int spanEnd = spanned.getSpanEnd(this);

        // Find the intersection of the current line and the span's range
        int start = Math.max(lineStart, spanStart);
        int end = Math.min(lineEnd, spanEnd);

        if (start < end) {
            // Calculate X coordinates relative to the line start
            // measureText gives the width of the substring.
            // We assume text starts at 'left' (standard LTR).

            float textOffset = p.measureText(text, lineStart, start);
            float textWidth = p.measureText(text, start, end);

            float rectLeft = left + textOffset - paddingHorizontal;
            float rectRight = rectLeft + textWidth + (paddingHorizontal * 2);

            // Adjust vertical bounds for a cleaner look (less tight to line height)
            float rectTop = top - paddingVertical;
            float rectBottom = bottom + paddingVertical;

            RectF rect = new RectF(rectLeft, rectTop, rectRight, rectBottom);

            int originalColor = p.getColor();
            p.setColor(backgroundColor);

            // Draw rounded rect
            c.drawRoundRect(rect, cornerRadius, cornerRadius, p);

            p.setColor(originalColor); // Restore
        }
    }
}
