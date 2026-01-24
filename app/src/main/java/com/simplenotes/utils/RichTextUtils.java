package com.simplenotes.utils;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import com.simplenotes.RoundedHighlighterSpan;
import org.json.JSONArray;
import org.json.JSONObject;

public class RichTextUtils {

    private static final String KEY_TEXT = "text";
    private static final String KEY_SPANS = "spans";

    private static final String TYPE_BOLD = "bold";
    private static final String TYPE_ITALIC = "italic";
    private static final String TYPE_UNDERLINE = "underline";
    private static final String TYPE_COLOR = "color";
    private static final String TYPE_HIGHLIGHT = "highlight";

    public static String toJson(Spanned text) {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_TEXT, text.toString());

            JSONArray spansArray = new JSONArray();

            // Bold & Italic
            StyleSpan[] styleSpans = text.getSpans(0, text.length(), StyleSpan.class);
            for (StyleSpan span : styleSpans) {
                JSONObject spanJson = new JSONObject();
                int start = text.getSpanStart(span);
                int end = text.getSpanEnd(span);

                if (span.getStyle() == Typeface.BOLD) {
                    spanJson.put("type", TYPE_BOLD);
                } else if (span.getStyle() == Typeface.ITALIC) {
                    spanJson.put("type", TYPE_ITALIC);
                } else if (span.getStyle() == Typeface.BOLD_ITALIC) {
                    // Split into two? Or handle specific?
                    // Simplest: Just save as two entries usually, but StyleSpan is one object.
                    // Actually, if it's BOLD_ITALIC, we can save two entries or one combined.
                    // Let's simpler: save as "bold" and "italic" separately?
                    // No, let's just create 2 JSON entries for simplicity in deserializer.
                    JSONObject s1 = new JSONObject();
                    s1.put("type", TYPE_BOLD);
                    s1.put("start", start);
                    s1.put("end", end);
                    spansArray.put(s1);

                    JSONObject s2 = new JSONObject();
                    s2.put("type", TYPE_ITALIC);
                    s2.put("start", start);
                    s2.put("end", end);
                    spansArray.put(s2);
                    continue;
                } else {
                    continue;
                }
                spanJson.put("start", start);
                spanJson.put("end", end);
                spansArray.put(spanJson);
            }

            // Underline
            UnderlineSpan[] underlineSpans = text.getSpans(0, text.length(), UnderlineSpan.class);
            for (UnderlineSpan span : underlineSpans) {
                JSONObject spanJson = new JSONObject();
                spanJson.put("type", TYPE_UNDERLINE);
                spanJson.put("start", text.getSpanStart(span));
                spanJson.put("end", text.getSpanEnd(span));
                spansArray.put(spanJson);
            }

            // Text Color
            ForegroundColorSpan[] colorSpans = text.getSpans(0, text.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : colorSpans) {
                JSONObject spanJson = new JSONObject();
                spanJson.put("type", TYPE_COLOR);
                spanJson.put("start", text.getSpanStart(span));
                spanJson.put("end", text.getSpanEnd(span));
                spanJson.put("value", span.getForegroundColor());
                spansArray.put(spanJson);
            }

            // Highlights
            RoundedHighlighterSpan[] highlightSpans = text.getSpans(0, text.length(), RoundedHighlighterSpan.class);
            for (RoundedHighlighterSpan span : highlightSpans) {
                JSONObject spanJson = new JSONObject();
                spanJson.put("type", TYPE_HIGHLIGHT);
                spanJson.put("start", text.getSpanStart(span));
                spanJson.put("end", text.getSpanEnd(span));
                spanJson.put("value", span.getBackgroundColor());
                spansArray.put(spanJson);
            }

            json.put(KEY_SPANS, spansArray);
            return json.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return text.toString(); // Fallback to raw text
        }
    }

    public static Spannable fromJson(String jsonString) {
        if (jsonString == null)
            return new SpannableString("");

        try {
            JSONObject json = new JSONObject(jsonString);
            String rawText = json.optString(KEY_TEXT, "");
            SpannableStringBuilder ssb = new SpannableStringBuilder(rawText);

            JSONArray spansArray = json.optJSONArray(KEY_SPANS);
            if (spansArray != null) {
                for (int i = 0; i < spansArray.length(); i++) {
                    JSONObject spanJson = spansArray.getJSONObject(i);
                    String type = spanJson.optString("type");
                    int start = spanJson.optInt("start");
                    int end = spanJson.optInt("end");

                    // Boundary checks
                    start = Math.max(0, start);
                    end = Math.min(rawText.length(), end);
                    if (start >= end)
                        continue;

                    Object spanObj = null;
                    if (TYPE_BOLD.equals(type)) {
                        spanObj = new StyleSpan(Typeface.BOLD);
                    } else if (TYPE_ITALIC.equals(type)) {
                        spanObj = new StyleSpan(Typeface.ITALIC);
                    } else if (TYPE_UNDERLINE.equals(type)) {
                        spanObj = new UnderlineSpan();
                    } else if (TYPE_COLOR.equals(type)) {
                        spanObj = new ForegroundColorSpan(spanJson.optInt("value"));
                    } else if (TYPE_HIGHLIGHT.equals(type)) {
                        spanObj = new RoundedHighlighterSpan(spanJson.optInt("value"), 12f);
                    }

                    if (spanObj != null) {
                        ssb.setSpan(spanObj, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            return ssb;

        } catch (Exception e) {
            // If parsing fails (e.g. it's old HTML or plain text), return as is
            // But usually the caller handles the fallback logic.
            // If it's not JSON, assume raw text?
            return new SpannableString(jsonString); // Dangerous if it WAS HTML.
        }
    }

    public static boolean isJson(String content) {
        return content != null && content.trim().startsWith("{") && content.trim().endsWith("}");
    }
}
