package com.simplenotes;

import android.content.Intent;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.List;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ScaleXSpan;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Html;
import android.text.style.UnderlineSpan;
import android.text.Spanned;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.app.AlertDialog;
import android.graphics.Color;

public class NoteActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private RichEditText editTextContent;

    // Version Switcher
    private android.widget.TextView textViewVersion;
    private android.widget.ImageButton buttonVersion;
    private String currentTranslation = "web"; // Default
    private java.util.Map<String, String> bibleVersions;

    // Highlighting
    private int[] highlightColors;
    private String[] highlightColorNames;
    private int[] textColors; // [NEW] Class field

    private Note currentNote;
    private boolean isNewNote = true;

    private AppDatabase database;

    // Sticky Formatting State
    private boolean pendingBold = false;
    private boolean pendingItalic = false;
    private boolean pendingUnderline = false;
    private Integer pendingTextColor = null;
    private Integer pendingHighlightColor = null;
    private boolean isTyping = false; // Prevent recursion

    // Rich Text Toolbar
    private ImageButton btnBold, btnItalic, btnUnderline, btnTextColor, btnBackendColor;
    private int manualOverridePosition = -1; // [NEW] Track where user manually toggled styles

    // Text Watcher State
    private int lastChangeStart = 0;
    private int lastChangeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        database = AppDatabase.getDatabase(this);

        initViews();
        initializeHighlightColors(); // Initialize colors

        if (savedInstanceState != null && savedInstanceState.containsKey("current_note")) {
            currentNote = (Note) savedInstanceState.getSerializable("current_note");
            isNewNote = false; // Restored state means it's not a fresh "new" note
        } else {
            checkIntentData();
        }

        initializeBibleVersions();
        setupVersionSwitcher();
        setupMagicFetch();
    }

    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);

        // Dynamic Hint Logic: Hide explanation when user clicks to type
        editTextContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editTextContent.setHint("");
            } else {
                if (editTextContent.getText().length() == 0) {
                    editTextContent.setHint(R.string.magic_fetch_guide);
                }
            }
        });

        // Initialize hint state
        if (editTextContent.getText().length() == 0) {
            editTextContent.setHint(R.string.magic_fetch_guide);
        }

        textViewVersion = findViewById(R.id.textViewVersion);
        buttonVersion = findViewById(R.id.buttonVersion);

        // Rich Text Toolbar
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnTextColor = findViewById(R.id.btnTextColor);
        btnBackendColor = findViewById(R.id.btnBackendColor);

        setupFormattingButtons();
        setupStickyFormatting();
        setupSelectionListener();
    }

    private void setupSelectionListener() {
        editTextContent.setOnSelectionChangedListener((start, end) -> {
            if (start == end) { // Cursor move (no selection)
                checkStylesAtCursor(start);
            } else {
                // If text is selected, maybe update UI to show common style?
                // For now, let's just focus on cursor for sticky behavior
                // Or maybe check start position?
                checkStylesAtCursor(start);
            }
        });
    }

    private void checkStylesAtCursor(int position) {
        android.text.Editable editable = editTextContent.getText();
        if (editable == null)
            return;
        if (position < 0)
            return;

        // Prevent race conditions: Do not check styles while we are programmatically
        // modifying text
        if (isTyping)
            return;

        // [NEW] If user manually toggled styles at this exact position, DO NOT override
        // them
        // This prevents the auto-detector from immediately undoing the user's click
        if (position == manualOverridePosition) {
            return;
        } else {
            // Cursor moved, so we are back to auto-sensing mode
            manualOverridePosition = -1;
        }

        // Reset all first (unless we want 'additive' inheritance, but usually cursor
        // status is absolute)
        // But wait, if user JUST toggled Bold, we don't want to clear it if they
        // haven't typed yet.
        // Issue: Toggling Bold doesn't move cursor. So this won't fire.
        // Issue: Typing 'A' moves cursor. 'A' has style. listener fires. detects bold.
        // sets pending=true. OK.
        // Issue: Moving cursor to plain text. listener fires. detects nothing. sets
        // pending=false. OK.

        // So we can aggressively set flags based on spans.

        pendingBold = false;
        pendingItalic = false;
        pendingUnderline = false;
        pendingTextColor = null;
        pendingHighlightColor = null;

        if (position > 0) {
            // Check character BEFORE cursor (inheritance usually comes from prev char)
            // Or check span covering the point.
            // Spans are usually Start <= pos < End (Exclusive) or <= End (Inclusive)

            Object[] spans = editable.getSpans(position - 1, position, Object.class);

            for (Object span : spans) {
                int flags = editable.getSpanFlags(span);
                if ((flags & Spanned.SPAN_COMPOSING) != 0)
                    continue; // Ignore temporary composing spans

                if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    if ((style & Typeface.BOLD) != 0)
                        pendingBold = true;
                    if ((style & Typeface.ITALIC) != 0)
                        pendingItalic = true;
                }
                if (span instanceof UnderlineSpan) {
                    pendingUnderline = true;
                }
                if (span instanceof ForegroundColorSpan && !(span instanceof AutoColorSpan)) {
                    pendingTextColor = ((ForegroundColorSpan) span).getForegroundColor();
                }
                if (span instanceof RoundedHighlighterSpan) {
                    // Need to find the color index
                    // This span stores direct color, not index.
                    // But we only have buttons for specific indices.
                    // Let's reverse lookup the color?
                    int color = ((RoundedHighlighterSpan) span).getBackgroundColor();
                    for (int i = 0; i < highlightColors.length; i++) {
                        if (highlightColors[i] == color) {
                            pendingHighlightColor = i;
                            break;
                        }
                    }
                }
            }
        }

        updateToolbarUI();
    }

    private void setupStickyFormatting() {
        editTextContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isTyping)
                    return;
                lastChangeStart = start;
                lastChangeCount = count;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isTyping)
                    return;

                // Apply styles for insertions
                if (lastChangeCount > 0) {
                    applyPendingStyles(lastChangeStart, lastChangeCount);
                    lastChangeCount = 0; // Reset
                }

                // Previously existing logic (Bible Fetch, etc)
                // NOTE: We need to merge this with setupMagicFetch or move it?
                // Wait, setupMagicFetch adds *another* listener.
                // That listener runs separately.
                // This listener is DEDICATED to sticky formatting.
            }
        });
    }

    private void applyPendingStyles(int start, int count) {
        isTyping = true;
        try {
            android.text.Editable editable = editTextContent.getText();
            int end = start + count;

            // 1. Highlight is special (Needs markers for persistence)
            if (pendingHighlightColor != null) {
                // We must wrap the new text with markers: \u200C{idx} TEXT \u200D
                // This requires modifying text, which triggers onTextChanged again (recursion
                // guarded by isTyping)
                String newText = editable.subSequence(start, end).toString();
                String wrapped = "\u200C{" + pendingHighlightColor + "}" + newText + "\u200D";
                editable.replace(start, end, wrapped);

                // Update count to reflect the new length of the inserted text + markers
                count = wrapped.length();
            }

            // 2. Standard Styles (Apply to the range)
            // Note: If we inserted highlights, the range 'start' to 'end' is now
            // markers+text
            // But usually we want bold inside the text.
            // Simplified: Just apply to the whole inserted block (including markers is
            // fine, they are hidden)
            // Or better: Re-calculate the range of the *visible* content if highlight
            // changed it.

            // Actually, if we use applyStyling() for highlights, it hides markers.
            // Let's rely on applyStyling() for highlights and standard spans for others.

            if (pendingHighlightColor != null) {
                applyStyling(); // This parses markers and applies ResolvedHighlighterSpan
                // The markers are now hidden. We need to find where our text went?
                // Complicated.
                // Alternative: Don't use markers for sticky char-by-char. Use markers only for
                // block selection?
                // But user wants persistence.

                // Let's stick to: Apply Pending Highlight = Insert Markers.
                // Reset flag? No, sticky implies it stays.
            }

            // Re-get (in case replaced)
            editable = editTextContent.getText();
            // If we replaced text, 'end' is stale. But for Bold/Italic, we just span the
            // range.
            // If Highlight was applied, the text length changed.
            // However, typical sticky bold is applied to the typed character.

            if (pendingBold)
                editable.setSpan(new StyleSpan(Typeface.BOLD), start, start + count, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                removeStyle(editable, start, start + count, Typeface.BOLD);

            if (pendingItalic)
                editable.setSpan(new StyleSpan(Typeface.ITALIC), start, start + count,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                removeStyle(editable, start, start + count, Typeface.ITALIC);

            if (pendingUnderline)
                editable.setSpan(new UnderlineSpan(), start, start + count, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                removeUnderline(editable, start, start + count);

            if (pendingTextColor != null)
                editable.setSpan(new ForegroundColorSpan(pendingTextColor), start, start + count,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                removeTextColor(editable, start, start + count);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isTyping = false;
        }
    }

    private void removeStyle(android.text.Editable editable, int start, int end, int style) {
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                int spanStart = editable.getSpanStart(span);
                int spanEnd = editable.getSpanEnd(span);
                editable.removeSpan(span);

                if (spanStart < start) {
                    editable.setSpan(new StyleSpan(style), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (spanEnd > end) {
                    editable.setSpan(new StyleSpan(style), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private void removeUnderline(android.text.Editable editable, int start, int end) {
        UnderlineSpan[] spans = editable.getSpans(start, end, UnderlineSpan.class);
        for (UnderlineSpan span : spans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            editable.removeSpan(span);

            if (spanStart < start) {
                editable.setSpan(new UnderlineSpan(), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (spanEnd > end) {
                editable.setSpan(new UnderlineSpan(), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void removeTextColor(android.text.Editable editable, int start, int end) {
        ForegroundColorSpan[] spans = editable.getSpans(start, end, ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            int color = span.getForegroundColor();
            editable.removeSpan(span);

            if (spanStart < start) {
                editable.setSpan(new ForegroundColorSpan(color), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (spanEnd > end) {
                editable.setSpan(new ForegroundColorSpan(color), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // reset pending?
    private void resetPendingStyles() {
        pendingBold = false;
        pendingItalic = false;
        pendingUnderline = false;
        pendingTextColor = null;
        pendingHighlightColor = null;
        updateToolbarUI();
    }

    private void updateToolbarUI() {
        // Visual feedback (Primitive: Alpha or Tint)
        // Active = ColorAccent or Darker
        int activeColor = ContextCompat.getColor(this, R.color.bible_gold);
        int inactiveColor = ContextCompat.getColor(this, R.color.bible_cream);

        btnBold.setColorFilter(pendingBold ? activeColor : inactiveColor);
        btnItalic.setColorFilter(pendingItalic ? activeColor : inactiveColor);
        btnUnderline.setColorFilter(pendingUnderline ? activeColor : inactiveColor);

        if (pendingTextColor != null)
            btnTextColor.setColorFilter(pendingTextColor);
        else
            btnTextColor.setColorFilter(inactiveColor);

        if (pendingHighlightColor != null)
            btnBackendColor.setColorFilter(highlightColors[pendingHighlightColor]);
        else
            btnBackendColor.setColorFilter(inactiveColor);
    }

    private void checkIntentData() {
        Intent intent = getIntent();

        if (intent != null) {
            isNewNote = intent.getBooleanExtra("isNewNote", true);

            if (!isNewNote) {
                currentNote = (Note) intent.getSerializableExtra("note");
                if (currentNote != null) {
                    editTextTitle.setText(currentNote.getTitle());
                    // Load Rich Text from HTML
                    String content = currentNote.getContent();
                    if (content != null) {
                        if (content.contains("<") && content.contains(">")) {
                            editTextContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            editTextContent.setText(content);
                        }
                    }
                    applyVerseStyling();
                    setTitle(R.string.edit_note);
                }
            } else {
                setTitle(R.string.new_note);
                currentNote = new Note();
                // Immediate save for new notes
                AppExecutors.getInstance().diskIO().execute(() -> {
                    database.noteDao().insert(currentNote);
                });
            }
        }
    }

    private void initializeBibleVersions() {
        bibleVersions = new java.util.LinkedHashMap<>();
        bibleVersions.put("Cherokee New Testament", "cherokee");
        bibleVersions.put("Chinese Union Version", "cuv");
        bibleVersions.put("Czech Bible kralick", "bkr"); // Note: encoding might need care, using simple chars if needed
                                                         // or unicode
        bibleVersions.put("American Standard (1901)", "asv");
        bibleVersions.put("Bible in Basic English", "bbe");
        bibleVersions.put("Darby Bible", "darby");
        bibleVersions.put("Douay-Rheims 1899", "dra");

        bibleVersions.put("King James Version", "kjv");
        bibleVersions.put("New International Version", "niv");
        bibleVersions.put("New Living Translation", "nlt");
        bibleVersions.put("World English Bible", "web");
        bibleVersions.put("Young's Literal (NT)", "ylt");
        bibleVersions.put("Open English (Commonwealth)", "oeb-cw");
        bibleVersions.put("Open English (US)", "oeb-us");
        bibleVersions.put("World English (British)", "webbe");
        bibleVersions.put("Latin Vulgate", "clementine");
        bibleVersions.put("Portuguese Almeida", "almeida");
        bibleVersions.put("Romanian Corrected", "rccv");
        bibleVersions.put("Standard Telugu Bible (Old Version)", "tel");
    }

    private void setupVersionSwitcher() {
        buttonVersion.setOnClickListener(v -> {
            BibleVersionSheet sheet = new BibleVersionSheet();
            sheet.setListener(version -> {
                currentTranslation = version.getId();
                textViewVersion.setText("Bible Version: " + version.getName());

                // Save preference
                getSharedPreferences("bible_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("selected_version", currentTranslation)
                        .apply();

                Toast.makeText(this, "Set to: " + version.getName(), Toast.LENGTH_SHORT).show();
            });
            sheet.show(getSupportFragmentManager(), "BibleVersionSheet");
        });

        // Restore default text or fetch current from DB preference if we had one
        // Load saved preference
        currentTranslation = getSharedPreferences("bible_prefs", MODE_PRIVATE)
                .getString("selected_version", "web"); // Default to web

        // Find name for the ID
        String versionName = "World English Bible"; // Default name
        for (java.util.Map.Entry<String, String> entry : bibleVersions.entrySet()) {
            if (entry.getValue().equals(currentTranslation)) {
                versionName = entry.getKey();
                break;
            }
        }
        textViewVersion.setText("Bible Version: " + versionName);
    }

    private void setupMagicFetch() {
        // Setup Autocomplete
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                R.layout.item_autocomplete, BibleData.BOOKS);
        editTextContent.setAdapter(adapter);
        editTextContent.setTokenizer(new BibleTokenizer());

        editTextContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() > 0) {
                    int cursorPos = editTextContent.getSelectionStart();
                    if (cursorPos > 0) {
                        char lastChar = s.charAt(cursorPos - 1);
                        if (Character.isWhitespace(lastChar)) {
                            // Check text only up to the cursor
                            checkForBibleReference(s.subSequence(0, cursorPos).toString(), cursorPos);
                        }
                    }
                }

                // Dynamic Dropdown Positioning
                // Only adjust if the user types '@' (start of trigger) or tokens
                if (editTextContent.getLayout() != null) {
                    int pos = editTextContent.getSelectionStart();
                    int line = editTextContent.getLayout().getLineForOffset(pos);
                    int bottom = editTextContent.getLayout().getLineBottom(line);

                    // The default behavior anchors to the bottom of the View.
                    // We need a negative offset to bring it up to the cursor line.
                    int height = editTextContent.getHeight();
                    int scrollY = editTextContent.getScrollY();

                    // Offset = (Cursor Line Bottom - Scroll Position) - View Height
                    // Effectively moves the anchor point from bottom of view to the cursor line
                    int offset = (bottom - scrollY) - height;

                    editTextContent.setDropDownVerticalOffset(offset);
                }
            }
        });
    }

    private static class BibleTokenizer
            implements androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView.Tokenizer {
        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != '@') {
                i--;
            }
            if (i > 0 && text.charAt(i - 1) == '@') {
                return i;
            }
            return cursor; // No @ found, default behavior (won't trigger)
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == ' ' || text.charAt(i) == '\n') {
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            if (i > 0 && Character.isLetterOrDigit(text.charAt(i - 1))) {
                return text + " ";
            } else {
                if (text instanceof android.text.Spanned) {
                    SpannableStringBuilder sp = new SpannableStringBuilder(text);
                    sp.append(" ");
                    return sp;
                } else {
                    return text + " ";
                }
            }
        }
    }

    private void checkForBibleReference(String text, int cursorPos) {
        // Regex to find @Book Chapter:Verse pattern (e.g., @John 3:16 or @1 Samuel 1:1)
        // followed by whitespace
        // Matches "@Book Chapter:Verse" followed by one or more whitespace characters
        // at the end
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("(@([a-zA-Z0-9\\s]+ \\d+:\\d+(?:-\\d+)?))\\s+$");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String fullTrigger = matcher.group(1); // The whole "@Luke 1:1 "
            String reference = matcher.group(2).trim(); // "Luke 1:1"

            // Calculate start index based on match length and cursor position
            // Since we matched against the substring ending at cursorPos, the end of match
            // is effectively cursorPos (minus trailing whitespace potentially caught by
            // regex but group 1 captures strict trigger part usually, let's correspond)
            // Actually group 0 is the whole match including waiting whitespace.
            // We want to replace group 0.

            int matchEnd = cursorPos; // Because we anchored to $ of substring(0, cursorPos)
            int matchStart = matchEnd - matcher.group(0).length();

            fetchVerse(reference, matchStart, matchEnd);
        }
    }

    private void fetchVerse(String reference, int startIdx, int endIdx) {
        // Parse reference to find Book, Chapter, Verse for DB lookup
        // Ref format: "Book Name Chapter:Verse" or "Book Name Chapter:Start-End"
        // e.g. "John 3:16" or "1 Samuel 2:10"

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // 1. Check Local DB
                BibleDao dao = database.bibleDao();
                String refTrimmed = reference.trim();
                int lastSpace = refTrimmed.lastIndexOf(' ');

                if (lastSpace != -1) {
                    String book = refTrimmed.substring(0, lastSpace).trim();
                    String versePart = refTrimmed.substring(lastSpace + 1).trim();
                    String[] cv = versePart.split(":");

                    if (cv.length == 2) {
                        int chapter = Integer.parseInt(cv[0]);
                        String verseNumStr = cv[1];

                        StringBuilder sb = new StringBuilder();
                        boolean foundLocally = false;

                        if (verseNumStr.contains("-")) {
                            // Range lookup
                            String[] range = verseNumStr.split("-");
                            int startV = Integer.parseInt(range[0]);
                            int endV = Integer.parseInt(range[1]);
                            List<Verse> verses = dao.getVersesInRange(currentTranslation, book, chapter, startV, endV);

                            if (verses != null && !verses.isEmpty() && verses.size() == (endV - startV + 1)) {
                                foundLocally = true;
                                for (Verse v : verses) {
                                    sb.append(v.getBook()).append(" ").append(v.getChapter()).append(":")
                                            .append(v.getVerse()).append("\n");
                                    sb.append("\u200B\"").append(v.getText()).append("\"\u200B\n\n");
                                }
                            }
                        } else {
                            // Single verse lookup
                            int verseNum = Integer.parseInt(verseNumStr);
                            Verse v = dao.getVerse(currentTranslation, book, chapter, verseNum);
                            if (v != null) {
                                foundLocally = true;
                                sb.append(reference).append("\n");
                                sb.append("\u200B\"").append(v.getText()).append("\"\u200B"); // Single verse format
                                                                                              // matches existing logic
                            }
                        }

                        if (foundLocally) {
                            if (sb.length() > 2 && sb.toString().endsWith("\n\n")) {
                                sb.setLength(sb.length() - 2);
                            }
                            String finalInfo = sb.toString();

                            AppExecutors.getInstance().mainThread().execute(() -> {
                                replaceTextWithVerse(startIdx, endIdx, "", finalInfo);
                            });
                            return; // Done
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback to network on error
            }

            // 2. Fetch from Network if not found or parsing failed
            AppExecutors.getInstance().mainThread().execute(() -> {
                fetchVerseNetwork(reference, startIdx, endIdx);
            });
        });
    }

    private void fetchVerseNetwork(String reference, int startIdx, int endIdx) {
        com.simplenotes.api.ApiClient.getService().getVerse(reference, currentTranslation)
                .enqueue(new retrofit2.Callback<com.simplenotes.api.BibleResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.simplenotes.api.BibleResponse> call,
                            retrofit2.Response<com.simplenotes.api.BibleResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.simplenotes.api.BibleResponse body = response.body();

                            // Save to Cache (Background)
                            AppExecutors.getInstance().diskIO().execute(() -> {
                                if (body.getVerses() != null) {
                                    for (com.simplenotes.api.BibleResponse.Verse apiVerse : body.getVerses()) {
                                        database.bibleDao().insertVerse(new Verse(
                                                currentTranslation,
                                                apiVerse.getBookName(),
                                                apiVerse.getChapter(),
                                                apiVerse.getVerse(),
                                                apiVerse.getText()));
                                    }
                                } else if (body.getText() != null) {
                                    // Single verse response might not give broken down details easily if API
                                    // doesn't return list
                                    // But Bible-api.com usually returns 'verses' array even for single.
                                    // We'll skip caching complex partials for simplicity if list is missing
                                }
                            });

                            // UI Update
                            if (body.getVerses() != null && !body.getVerses().isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (com.simplenotes.api.BibleResponse.Verse v : body.getVerses()) {
                                    String ref = v.getBookName() + " " + v.getChapter() + ":" + v.getVerse();
                                    sb.append(ref).append("\n");
                                    sb.append("\u200B\"").append(v.getText()).append("\"\u200B\n\n");
                                }
                                if (sb.length() > 2)
                                    sb.setLength(sb.length() - 2);
                                replaceTextWithVerse(startIdx, endIdx, "", sb.toString());

                            } else {
                                String verseText = body.getText();
                                if (verseText != null) {
                                    replaceTextWithVerse(startIdx, endIdx, reference, verseText);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.simplenotes.api.BibleResponse> call, Throwable t) {
                        Toast.makeText(NoteActivity.this, "Failed to fetch verse", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void replaceTextWithVerse(int start, int end, String reference, String verseText) {
        isTyping = true;
        try {
            android.text.Editable editable = editTextContent.getText();

            // Safety check indices
            if (start < 0 || end > editable.length() || start > end) {
                return;
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder();

            if (!reference.isEmpty()) {
                // Format: Reference (Newline) "Verse"
                ssb.append(reference).append("\n");

                // Invisible marker to identify Bible verses for styling persistence
                String marker = "\u200B";
                String formattedVerse = marker + "\"" + verseText + "\"" + marker;
                ssb.append(formattedVerse);
            } else {
                // Pre-formatted block (Multi-verse)
                ssb.append(verseText);
            }

            // Apply Pending Styles to the new content (Sticky Formatting for Magic Fetch)
            if (pendingBold)
                ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, ssb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (pendingItalic)
                ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), 0, ssb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (pendingUnderline)
                ssb.setSpan(new android.text.style.UnderlineSpan(), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (pendingTextColor != null)
                ssb.setSpan(new android.text.style.ForegroundColorSpan(pendingTextColor), 0, ssb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (pendingHighlightColor != null) {
                // Highlighting is tricky because it usually involves markers.
                // For now, let's skip manual marker insertion here and rely on applyStyling if
                // we set the span?
                // Actually, sticky highlight inserts markers.
                // But here we are inserting a VERSE which relies on \u200B markers.
                // Nesting markers might be complex. Let's just stick to font styles for now.
            }

            // Replace the trigger characters
            editable.replace(start, end, ssb);

            // Re-apply styling (will handle the new content)
            applyVerseStyling();
        } finally {
            isTyping = false;
        }
    }

    private void appendVerseToContentFallback(String verseText) {
        String currentContent = editTextContent.getText().toString();
        SpannableStringBuilder ssb = new SpannableStringBuilder(currentContent);
        if (!currentContent.isEmpty() && !currentContent.endsWith("\n")) {
            ssb.append("\n");
        }
        String marker = "\u200B";
        String formattedVerse = marker + "\"" + verseText + "\"" + marker;
        ssb.append(formattedVerse);
        editTextContent.setText(ssb);
        editTextContent.setSelection(ssb.length());
        applyVerseStyling();
    }

    private void initializeHighlightColors() {
        highlightColors = new int[] {
                ContextCompat.getColor(this, R.color.highlight_gold),
                ContextCompat.getColor(this, R.color.highlight_blue),
                ContextCompat.getColor(this, R.color.highlight_green),
                ContextCompat.getColor(this, R.color.highlight_pink),
                ContextCompat.getColor(this, R.color.highlight_purple),
                ContextCompat.getColor(this, R.color.highlight_peach)
        };
        highlightColorNames = new String[] { "Gold", "Blue", "Green", "Pink", "Purple", "Peach" };

        textColors = new int[] {
                ContextCompat.getColor(this, R.color.text_black),
                ContextCompat.getColor(this, R.color.text_grey),
                ContextCompat.getColor(this, R.color.text_red),
                ContextCompat.getColor(this, R.color.text_orange),
                ContextCompat.getColor(this, R.color.text_yellow),
                ContextCompat.getColor(this, R.color.text_green),
                ContextCompat.getColor(this, R.color.text_teal),
                ContextCompat.getColor(this, R.color.text_blue),
                ContextCompat.getColor(this, R.color.text_indigo),
                ContextCompat.getColor(this, R.color.text_purple),
                ContextCompat.getColor(this, R.color.text_pink),
                ContextCompat.getColor(this, R.color.text_brown)
        };
    }

    private void showHighlightColorPicker(android.view.ActionMode mode) {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();

        if (start == end && mode != null) {
            // mode implies we were triggered from selection menu, but selection is empty?
            // Should not happen, but safe guard.
            return;
        }

        // We don't easily know the CURRENT highlight color of the selection without
        // parsing.
        // Default to -1 (none selected) or 0?
        // Current selection color? Hard to determine. Default to 0 (None)
        int selectedIndex = 0; // Default to "None"

        int[] displayColors = prependZero(highlightColors);

        ColorBottomSheet sheet = ColorBottomSheet.newInstance("Highlight color", displayColors, selectedIndex);
        sheet.setListener(index -> {
            if (index == 0) {
                removeHighlight(start, end);
            } else {
                applyHighlight(index - 1, start, end); // Map back to 0-based
            }
            if (mode != null)
                mode.finish();
            sheet.dismiss();
        });
        sheet.show(getSupportFragmentManager(), "HighlightSheet");
    }

    // Sticky Highlight Picker
    private void showStickyHighlightPicker() {
        // Map pending (null/0-based) to UI (0=None, 1..N=Colors)
        int selectedIndex = (pendingHighlightColor == null) ? 0 : pendingHighlightColor + 1;

        int[] displayColors = prependZero(highlightColors);

        ColorBottomSheet sheet = ColorBottomSheet.newInstance("Highlight color", displayColors, selectedIndex);
        sheet.setListener(index -> {
            if (index == 0)
                pendingHighlightColor = null;
            else
                pendingHighlightColor = index - 1;

            updateToolbarUI();
            sheet.dismiss();
        });
        sheet.show(getSupportFragmentManager(), "StickyHighlightSheet");
    }

    // Setup Formatting Listeners
    private void setupFormattingButtons() {
        // [VERIFICATION] Binding toolbar actions to rich text logic
        btnBold.setOnClickListener(v -> {
            if (hasSelection())
                toggleStyle(Typeface.BOLD);
            else {
                pendingBold = !pendingBold;
                manualOverridePosition = editTextContent.getSelectionStart(); // Lock this choice
                updateToolbarUI();
            }
        });
        btnItalic.setOnClickListener(v -> {
            if (hasSelection())
                toggleStyle(Typeface.ITALIC);
            else {
                pendingItalic = !pendingItalic;
                manualOverridePosition = editTextContent.getSelectionStart(); // Lock this choice
                updateToolbarUI();
            }
        });
        btnUnderline.setOnClickListener(v -> {
            if (hasSelection())
                toggleUnderline();
            else {
                pendingUnderline = !pendingUnderline;
                manualOverridePosition = editTextContent.getSelectionStart(); // Lock this choice
                updateToolbarUI();
            }
        });
        btnTextColor.setOnClickListener(v -> showTextColorPicker());
        btnBackendColor.setOnClickListener(v -> showHighlightPickerLogic());
    }

    private boolean hasSelection() {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();
        return start != end && start >= 0;
    }

    private void showHighlightPickerLogic() {
        if (hasSelection()) {
            // Use current selection
            int start = editTextContent.getSelectionStart();
            int end = editTextContent.getSelectionEnd();
            showHighlightColorPicker(null);
        } else {
            // Sticky Mode
            // Show picker to SET pending color
            showStickyHighlightPicker();
        }
    }

    private void toggleStyle(int style) {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();
        if (start == end)
            return;

        Spannable str = editTextContent.getText();
        StyleSpan[] spans = str.getSpans(start, end, StyleSpan.class);
        boolean exists = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                if (str.getSpanStart(span) < start || str.getSpanEnd(span) > end) {
                    // Span extends outside selection
                }
                exists = true;
                str.removeSpan(span);
            }
        }

        if (!exists) {
            str.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    private void toggleUnderline() {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();
        if (start == end)
            return;

        Spannable str = editTextContent.getText();
        UnderlineSpan[] spans = str.getSpans(start, end, UnderlineSpan.class);
        boolean exists = false;
        for (UnderlineSpan span : spans) {
            exists = true;
            str.removeSpan(span);
        }

        if (!exists) {
            str.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    private void showTextColorPicker() {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();

        // Find selected index logic
        // UI Index: 0 = Default, 1..N = Colors
        int selectedIndex = 0; // Default to "None"
        if (pendingTextColor != null) {
            for (int i = 0; i < textColors.length; i++) {
                if (textColors[i] == pendingTextColor) {
                    selectedIndex = i + 1; // Map to UI
                    break;
                }
            }
        }

        int[] displayColors = prependZero(textColors);

        ColorBottomSheet sheet = ColorBottomSheet.newInstance("Font color", displayColors, selectedIndex);
        sheet.setListener(index -> {
            if (hasSelection()) {
                if (index == 0) {
                    // Clear Color Spans
                    removeTextColor(start, end);
                } else {
                    editTextContent.getText().setSpan(new ForegroundColorSpan(textColors[index - 1]), start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                if (index == 0)
                    pendingTextColor = null;
                else
                    pendingTextColor = textColors[index - 1];
                updateToolbarUI();
            }
            sheet.dismiss();
        });
        sheet.show(getSupportFragmentManager(), "TextColorSheet");
    }

    private void removeTextColor(int start, int end) {
        if (start >= end)
            return;
        Spannable str = editTextContent.getText();
        ForegroundColorSpan[] spans = str.getSpans(start, end, ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            int s = str.getSpanStart(span);
            int e = str.getSpanEnd(span);
            str.removeSpan(span);

            // Restore parts outside selection
            if (s < start)
                str.setSpan(new ForegroundColorSpan(span.getForegroundColor()), s, start,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (e > end)
                str.setSpan(new ForegroundColorSpan(span.getForegroundColor()), end, e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyHighlight(int colorIndex, int start, int end) {
        isTyping = true;
        try {
            android.text.Editable text = editTextContent.getText();
            String str = text.toString();

            // Robust Smart Expansion using Regex
            // 1. Expand START to include preceding OR overlapping marker \u200C{digits}
            // Scan for all markers. If start falls INSIDE or AT THE END of a marker, expand
            // to marker start.
            java.util.regex.Matcher startMatcher = java.util.regex.Pattern.compile("\u200C\\{\\d+\\}").matcher(str);
            while (startMatcher.find()) {
                int mStart = startMatcher.start();
                int mEnd = startMatcher.end();
                // If selection starts inside the marker (mStart < start < mEnd)
                // OR exactly at the end (start == mEnd)
                if (start > mStart && start <= mEnd) {
                    start = mStart;
                    break;
                }
            }

            // 2. Expand END to include succeeding marker \u200D (or legacy \u200C)
            // Check immediate char at 'end'
            if (end < str.length()) {
                char c = str.charAt(end);
                if (c == '\u200D' || c == '\u200C') {
                    end++;
                }
            }

            if (start < 0 || end < 0 || start >= end)
                return;

            // Use SpannableStringBuilder to preserve existing spans (Bold, Italic, Color)
            // while stripping out old markers.
            android.text.SpannableStringBuilder content = new android.text.SpannableStringBuilder(
                    text.subSequence(start, end));

            // Remove existing markers patterns from the content
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\u200C|\u200D|\\{\\d+\\}");
            java.util.regex.Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                content.delete(matcher.start(), matcher.end());
                matcher.reset(content);
            }

            // Also remove any existing RoundedHighlighterSpan or HiddenSpan
            Object[] oldSpans = content.getSpans(0, content.length(), Object.class);
            for (Object span : oldSpans) {
                if (span instanceof RoundedHighlighterSpan || span instanceof HiddenSpan) {
                    content.removeSpan(span);
                }
            }

            // Construct final wrapped text
            android.text.SpannableStringBuilder finalSSB = new android.text.SpannableStringBuilder();
            finalSSB.append("\u200C{" + colorIndex + "}");
            finalSSB.append(content);
            finalSSB.append("\u200D");

            text.replace(start, end, finalSSB);
            applyStyling();
        } finally {
            isTyping = false;
        }
    }

    private void removeHighlight(int start, int end) {
        isTyping = true;
        try {
            android.text.Editable text = editTextContent.getText();
            String str = text.toString();

            // Mirror START expansion
            java.util.regex.Matcher startMatcher = java.util.regex.Pattern.compile("\u200C\\{\\d+\\}").matcher(str);
            while (startMatcher.find()) {
                int mStart = startMatcher.start();
                int mEnd = startMatcher.end();
                if (start > mStart && start <= mEnd) {
                    start = mStart;
                    break;
                }
            }

            // Mirror END expansion
            if (end < str.length()) {
                char c = str.charAt(end);
                if (c == '\u200D' || c == '\u200C') {
                    end++;
                }
            }

            if (start < 0 || end < 0 || start >= end)
                return;

            // Use SpannableStringBuilder to preserve existing spans (Bold, Italic, Color)
            // while stripping out old markers.
            android.text.SpannableStringBuilder content = new android.text.SpannableStringBuilder(
                    text.subSequence(start, end));

            // Remove existing markers patterns from the content
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\u200C|\u200D|\\{\\d+\\}");
            java.util.regex.Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                content.delete(matcher.start(), matcher.end());
                matcher.reset(content);
            }

            // Also remove any existing RoundedHighlighterSpan or HiddenSpan
            Object[] oldSpans = content.getSpans(0, content.length(), Object.class);
            for (Object span : oldSpans) {
                if (span instanceof RoundedHighlighterSpan || span instanceof HiddenSpan) {
                    content.removeSpan(span);
                }
            }

            text.replace(start, end, content);
            applyStyling();
        } finally {
            isTyping = false;
        }
    }

    private void applyStyling() {
        android.text.Editable text = editTextContent.getText();
        String content = text.toString();

        // 0. AUTO-MIGRATION: Fix Legacy Closers
        java.util.regex.Matcher migrationMatcher = java.util.regex.Pattern.compile("\u200C(?![{])").matcher(content);
        if (migrationMatcher.find()) {
            String cleanContent = migrationMatcher.replaceAll("\u200D");
            if (!cleanContent.equals(content)) {
                int selectionStart = editTextContent.getSelectionStart();
                int selectionEnd = editTextContent.getSelectionEnd();

                editTextContent.setText(cleanContent);
                try {
                    int len = editTextContent.length();
                    editTextContent.setSelection(Math.min(selectionStart, len), Math.min(selectionEnd, len));
                } catch (Exception e) {
                }
                applyStyling();
                return;
            }
        }

        // 1. Clear ONLY Auto Spans (Leave user's Bold/Italic/Color intact)
        AutoColorSpan[] autoSpans = text.getSpans(0, text.length(), AutoColorSpan.class);
        for (AutoColorSpan span : autoSpans)
            text.removeSpan(span);

        RoundedHighlighterSpan[] roundedSpans = text.getSpans(0, text.length(), RoundedHighlighterSpan.class);
        for (RoundedHighlighterSpan span : roundedSpans)
            text.removeSpan(span);

        HiddenSpan[] hiddenSpans = text.getSpans(0, text.length(), HiddenSpan.class);
        for (HiddenSpan span : hiddenSpans)
            text.removeSpan(span);

        // 2. Hide ALL Markers Globally (Robustness)
        java.util.regex.Pattern markerPattern = java.util.regex.Pattern.compile("(\u200C(\\{\\d+\\})?)|\u200D");
        java.util.regex.Matcher markerMatcher = markerPattern.matcher(content);

        while (markerMatcher.find()) {
            text.setSpan(new HiddenSpan(), markerMatcher.start(), markerMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. Bible Verse Styling
        java.util.regex.Pattern versePattern = java.util.regex.Pattern.compile("\u200B(.*?)\u200B",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher verseMatcher = versePattern.matcher(content);
        int goldColor = ContextCompat.getColor(this, R.color.bible_gold);

        while (verseMatcher.find()) {
            // Hide the \u200B markers
            text.setSpan(new HiddenSpan(), verseMatcher.start(), verseMatcher.start(1),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new HiddenSpan(), verseMatcher.end(1), verseMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Check if user has already colored this text
            ForegroundColorSpan[] userColors = text.getSpans(verseMatcher.start(1), verseMatcher.end(1),
                    ForegroundColorSpan.class);
            boolean hasUserColor = false;
            for (ForegroundColorSpan span : userColors) {
                if (!(span instanceof AutoColorSpan)) {
                    hasUserColor = true;
                    break;
                }
            }

            // Only apply default gold if no user color is present
            if (!hasUserColor) {
                text.setSpan(new AutoColorSpan(goldColor), verseMatcher.start(1), verseMatcher.end(1),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // 4. Highlight Coloring
        // [FIX] Make leading \u200C optional to handle cases where it might be stripped
        // or missing
        // This prevents {0} from being visible if \u200C is lost.
        java.util.regex.Pattern highlightPattern = java.util.regex.Pattern.compile("(?:\u200C)?\\{(\\d+)\\}(.*?)\u200D",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher highlightMatcher = highlightPattern.matcher(content);

        int textColor = ContextCompat.getColor(this, R.color.black);

        while (highlightMatcher.find()) {
            try {
                int colorIndex = Integer.parseInt(highlightMatcher.group(1));

                // Explicitly hide markers with HiddenSpan
                text.setSpan(new HiddenSpan(), highlightMatcher.start(), highlightMatcher.start(2),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new HiddenSpan(), highlightMatcher.end(2), highlightMatcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (colorIndex >= 0 && colorIndex < highlightColors.length) {
                    int bgColor = highlightColors[colorIndex];

                    // Apply Rounded Highlighter
                    text.setSpan(new RoundedHighlighterSpan(bgColor, 12f),
                            highlightMatcher.start(2),
                            highlightMatcher.end(2),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                }
            } catch (Exception e) {
            }
        }
    }

    private void applyVerseStyling() {
        applyStyling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentNote != null) {
            // Ensure object is up-to-date with UI before saving
            currentNote.setTitle(editTextTitle.getText().toString().trim());
            // Save as HTML to persist Rich Text
            currentNote.setContent(Html.toHtml(editTextContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
            outState.putSerializable("current_note", currentNote);
        }
    }

    private void saveNote() {
        if (currentNote == null)
            return;

        String title = editTextTitle.getText().toString().trim();

        // [FIX] Prepare content for HTML conversion
        // Html.toHtml ignores text inside ReplacementSpans (like HiddenSpan).
        // We MUST remove HiddenSpans so the hidden markers (which are just text) are
        // saved.
        // We also remove AutoColorSpan so we don't hardcode dynamic colors (Gold).
        SpannableStringBuilder ssb = new SpannableStringBuilder(editTextContent.getText());

        HiddenSpan[] hiddenSpans = ssb.getSpans(0, ssb.length(), HiddenSpan.class);
        for (HiddenSpan span : hiddenSpans) {
            ssb.removeSpan(span);
        }

        AutoColorSpan[] autoSpans = ssb.getSpans(0, ssb.length(), AutoColorSpan.class);
        for (AutoColorSpan span : autoSpans) {
            ssb.removeSpan(span);
        }

        // Save as HTML to persist Rich Text
        String content = Html.toHtml(ssb, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        // Update note data in memory
        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setTimestamp(System.currentTimeMillis());

        // Save to DB (Using insert with REPLACE acts as Upsert, ensuring it saves even
        // if initial insert missed)
        // Save to DB
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().insert(currentNote);
        });
    }

    private int[] prependZero(int[] original) {
        int[] result = new int[original.length + 1];
        result[0] = 0; // The "None" value
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }
}