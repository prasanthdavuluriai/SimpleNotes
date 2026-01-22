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

public class NoteActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView editTextContent;

    private TextInputLayout layoutTitle;
    private TextInputLayout layoutContent;

    // Version Switcher
    private android.widget.TextView textViewVersion;
    private android.widget.ImageButton buttonVersion;
    private String currentTranslation = "web"; // Default
    private java.util.Map<String, String> bibleVersions;

    // Highlighting
    private int[] highlightColors;
    private String[] highlightColorNames;

    private Note currentNote;
    private boolean isNewNote = true;

    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        database = AppDatabase.getDatabase(this);

        initViews();
        initializeHighlightColors(); // Initialize colors
        setupSelectionMenu(); // Custom Selection Menu

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

        layoutTitle = findViewById(R.id.layoutTitle);
        layoutContent = findViewById(R.id.layoutContent);

        textViewVersion = findViewById(R.id.textViewVersion);
        buttonVersion = findViewById(R.id.buttonVersion);
    }

    private void checkIntentData() {
        Intent intent = getIntent();

        if (intent != null) {
            isNewNote = intent.getBooleanExtra("isNewNote", true);

            if (!isNewNote) {
                currentNote = (Note) intent.getSerializableExtra("note");
                if (currentNote != null) {
                    editTextTitle.setText(currentNote.getTitle());
                    editTextContent.setText(currentNote.getContent());
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

        // Replace the trigger characters
        editable.replace(start, end, ssb);

        // Re-apply styling (will handle the new content)
        applyVerseStyling();
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
    }

    private void setupSelectionMenu() {
        editTextContent.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // Add "Highlight" option
                menu.add(0, 101, 0, "Highlight")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                if (item.getItemId() == 101) {
                    showHighlightColorPicker(mode);
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        });
    }

    private void showHighlightColorPicker(android.view.ActionMode mode) {
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();

        if (start == end)
            return; // No selection

        // Check if already highlighted (simple check)
        String selectedText = editTextContent.getText().subSequence(start, end).toString();

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Choose Highlight Color");

        // Simple list adapter for colors
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this,
                android.R.layout.select_dialog_item, highlightColorNames) {
            @NonNull
            @Override
            public View getView(int position, @androidx.annotation.Nullable View convertView,
                    @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                android.widget.TextView tv = (android.widget.TextView) v;
                tv.setTextColor(highlightColors[position]);
                tv.setTypeface(null, Typeface.BOLD);
                return v;
            }
        };

        builder.setAdapter(adapter, (dialog, which) -> {
            applyHighlight(which, start, end);
            mode.finish(); // Close selection menu
        });

        builder.setNegativeButton("Remove Highlight", (dialog, which) -> {
            removeHighlight(start, end);
            mode.finish();
        });

        builder.show();
    }

    private void applyHighlight(int colorIndex, int start, int end) {
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

        String selected = text.subSequence(start, end).toString();

        // Remove existing markers inside selection to avoid nesting
        selected = selected.replace("\u200C", "").replace("\u200D", "");
        selected = selected.replaceAll("\\{\\d+\\}", "");

        // Use \u200D as Distinct Closer
        String newText = "\u200C{" + colorIndex + "}" + selected + "\u200D";

        text.replace(start, end, newText);
        applyStyling();
    }

    private void removeHighlight(int start, int end) {
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

        String selected = text.subSequence(start, end).toString();
        selected = selected.replace("\u200C", "").replace("\u200D", "").replaceAll("\\{\\d+\\}", "");
        text.replace(start, end, selected);
        applyStyling();
    }

    private void applyStyling() {
        android.text.Editable text = editTextContent.getText();
        String content = text.toString();

        // 0. AUTO-MIGRATION: Fix Legacy Closers
        // Replace any \u200C that is NOT followed by '{' with \u200D
        java.util.regex.Matcher migrationMatcher = java.util.regex.Pattern.compile("\u200C(?![{])").matcher(content);
        if (migrationMatcher.find()) {
            String cleanContent = migrationMatcher.replaceAll("\u200D");
            if (!cleanContent.equals(content)) {
                int selectionStart = editTextContent.getSelectionStart();
                int selectionEnd = editTextContent.getSelectionEnd();

                editTextContent.setText(cleanContent);
                try {
                    // Check bounds to prevents crashes if text length changed (unlikely here but
                    // safe)
                    int len = editTextContent.length();
                    editTextContent.setSelection(Math.min(selectionStart, len), Math.min(selectionEnd, len));
                } catch (Exception e) {
                }

                // Recurse to apply styles to clean text
                applyStyling();
                return;
            }
        }

        // 1. Clear existing spans
        ForegroundColorSpan[] fgSpans = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : fgSpans)
            text.removeSpan(span);

        RoundedBackgroundSpan[] bgSpans = text.getSpans(0, text.length(), RoundedBackgroundSpan.class);
        for (RoundedBackgroundSpan span : bgSpans)
            text.removeSpan(span);

        BackgroundColorSpan[] colorSpans = text.getSpans(0, text.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : colorSpans)
            text.removeSpan(span);

        RoundedHighlighterSpan[] roundedSpans = text.getSpans(0, text.length(), RoundedHighlighterSpan.class);
        for (RoundedHighlighterSpan span : roundedSpans)
            text.removeSpan(span);

        HiddenSpan[] hiddenSpans = text.getSpans(0, text.length(), HiddenSpan.class);
        for (HiddenSpan span : hiddenSpans)
            text.removeSpan(span);

        // 2. Hide ALL Markers Globally (Robustness)
        // Matches \u200C{digits} OR \u200D OR just \u200C (orphaned)
        java.util.regex.Pattern markerPattern = java.util.regex.Pattern.compile("(\u200C(\\{\\d+\\})?)|\u200D");
        java.util.regex.Matcher markerMatcher = markerPattern.matcher(content);

        int transparent = android.graphics.Color.TRANSPARENT;

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
            text.setSpan(new ForegroundColorSpan(goldColor), verseMatcher.start(1), verseMatcher.end(1),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 4. Highlight Coloring: \u200C{index} ... \u200D
        java.util.regex.Pattern highlightPattern = java.util.regex.Pattern.compile("\u200C\\{(\\d+)\\}(.*?)\u200D",
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

                    // Apply Rounded Highlighter (Widget Look)
                    text.setSpan(new RoundedHighlighterSpan(bgColor, 12f),
                            highlightMatcher.start(2), // Content Start
                            highlightMatcher.end(2), // Content End
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Force text color black
                    text.setSpan(new ForegroundColorSpan(textColor),
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
            currentNote.setContent(editTextContent.getText().toString().trim());
            outState.putSerializable("current_note", currentNote);
        }
    }

    private void saveNote() {
        if (currentNote == null)
            return;

        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        // Update note data in memory
        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setTimestamp(System.currentTimeMillis());

        // Save to DB (Using insert with REPLACE acts as Upsert, ensuring it saves even
        // if initial insert missed)
        AppExecutors.getInstance().diskIO().execute(() -> {
            database.noteDao().insert(currentNote);
        });
    }
}