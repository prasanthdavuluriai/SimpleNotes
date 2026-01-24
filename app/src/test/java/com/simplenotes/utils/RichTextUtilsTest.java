package com.simplenotes.utils;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 33)
public class RichTextUtilsTest {

    @Test
    public void testRoundTrip_Bold() {
        SpannableString input = new SpannableString("Hello World");
        input.setSpan(new StyleSpan(Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String json = RichTextUtils.toJson(input);
        Spannable output = RichTextUtils.fromJson(json);

        assertEquals("Hello World", output.toString());
        StyleSpan[] spans = output.getSpans(0, output.length(), StyleSpan.class);
        assertEquals(1, spans.length);
        assertEquals(Typeface.BOLD, spans[0].getStyle());
        assertEquals(0, output.getSpanStart(spans[0]));
        assertEquals(5, output.getSpanEnd(spans[0]));
    }

    @Test
    public void testRoundTrip_Color() {
        SpannableString input = new SpannableString("Red Text");
        input.setSpan(new ForegroundColorSpan(0xFFFF0000), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String json = RichTextUtils.toJson(input);
        Spannable output = RichTextUtils.fromJson(json);

        assertEquals("Red Text", output.toString());
        ForegroundColorSpan[] spans = output.getSpans(0, output.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(0xFFFF0000, spans[0].getForegroundColor());
    }

    @Test
    public void testRoundTrip_Complex() {
        SpannableString input = new SpannableString("Bold and Italic");
        input.setSpan(new StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        input.setSpan(new StyleSpan(Typeface.ITALIC), 9, 15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String json = RichTextUtils.toJson(input);
        Spannable output = RichTextUtils.fromJson(json);

        assertEquals("Bold and Italic", output.toString());

        StyleSpan[] spans = output.getSpans(0, output.length(), StyleSpan.class);
        assertEquals(2, spans.length); // Bold + Italic
    }
}
