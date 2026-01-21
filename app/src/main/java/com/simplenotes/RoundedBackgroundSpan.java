package com.simplenotes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;
import androidx.annotation.NonNull;

public class RoundedBackgroundSpan extends ReplacementSpan {

    private final int backgroundColor;
    private final int textColor;
    private final float cornerRadius;
    private final float padding;

    public RoundedBackgroundSpan(int backgroundColor, int textColor, float cornerRadius, float padding) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.cornerRadius = cornerRadius;
        this.padding = padding;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + (padding * 2));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
            @NonNull Paint paint) {
        float width = paint.measureText(text, start, end);
        RectF rect = new RectF(x, top, x + width + (padding * 2), bottom);

        paint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        paint.setColor(textColor);
        canvas.drawText(text, start, end, x + padding, y, paint);
    }
}
