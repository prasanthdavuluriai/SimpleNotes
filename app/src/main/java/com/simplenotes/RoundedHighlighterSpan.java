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
            // Use smartMeasure to ignore hidden markers (ScaleXSpan == 0)
            float textOffset = smartMeasure(p, text, lineStart, start);
            float textWidth = smartMeasure(p, text, start, end);

            float rectLeft = left + textOffset - paddingHorizontal;
            float rectRight = rectLeft + textWidth + (paddingHorizontal * 2);

            float rectTop = top - paddingVertical;
            float rectBottom = bottom + paddingVertical;

            RectF rect = new RectF(rectLeft, rectTop, rectRight, rectBottom);

            int originalColor = p.getColor();
            p.setColor(backgroundColor);
            c.drawRoundRect(rect, cornerRadius, cornerRadius, p);
            p.setColor(originalColor);
        }
    }

    private float smartMeasure(@NonNull Paint p, CharSequence text, int start, int end) {
        if (start >= end)
            return 0f;
        if (!(text instanceof Spanned))
            return p.measureText(text, start, end);

        Spanned spanned = (Spanned) text;
        HiddenSpan[] spans = spanned.getSpans(start, end, HiddenSpan.class);

        // Optimization: if no hidden spans in range, fast path
        if (spans.length == 0)
            return p.measureText(text, start, end);

        // Slow path: measure character by character skipping hidden ones
        float width = 0f;
        for (int i = start; i < end; i++) {
            boolean hiddenChar = false;
            for (HiddenSpan span : spans) {
                int s = spanned.getSpanStart(span);
                int e = spanned.getSpanEnd(span);
                if (i >= s && i < e) {
                    hiddenChar = true;
                    break;
                }
            }
            if (!hiddenChar) {
                width += p.measureText(text, i, i + 1);
            }
        }
        return width;
    }
}
