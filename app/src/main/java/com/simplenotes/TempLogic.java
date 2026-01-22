
// Setup Formatting Listeners
private void setupFormattingButtons() {
    btnBold.setOnClickListener(v -> toggleStyle(Typeface.BOLD));
    btnItalic.setOnClickListener(v -> toggleStyle(Typeface.ITALIC));
    btnUnderline.setOnClickListener(v -> toggleUnderline());
    btnTextColor.setOnClickListener(v -> showTextColorPicker());
    btnBackendColor.setOnClickListener(v -> {
        // Trigger the same highlight color picker logic
        // Use current selection
        int start = editTextContent.getSelectionStart();
        int end = editTextContent.getSelectionEnd();
        if (start == end) { // If no selection, select word
            selectCurrentWord();
            start = editTextContent.getSelectionStart();
            end = editTextContent.getSelectionEnd();
        }
        if (start != end) {
            // Pseudo-action mode not needed, just show dialog
            showHighlightColorPicker(null);
        } else {
            Toast.makeText(this, "Select text to highlight", Toast.LENGTH_SHORT).show();
        }
    });
}

private void selectCurrentWord() {
    // Simple word selection helper
    int cursor = editTextContent.getSelectionStart();
    if (cursor < 0)
        return;
    String text = editTextContent.getText().toString();
    if (text.isEmpty())
        return;

    int start = cursor;
    int end = cursor;

    while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1)))
        start--;
    while (end < text.length() && Character.isLetterOrDigit(text.charAt(end)))
        end++;

    if (start < end) {
        editTextContent.setSelection(start, end);
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
                // Span extends outside selection, careful removal not trivial here
                // For simplicity in quick editor, standard toggle logic:
                // If any part is styled, remove style from selection.
                // Android's setSpan usually merges.
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
    if (start == end)
        return;

    // Load colors from XML logic or hardcode array mapping from colors.xml
    // Using the color names added to strings or just arrays
    final int[] textColors = new int[] {
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

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Text Color");

    // Custom Grid or List? List is easier for now.
    String[] names = { "Black", "Grey", "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Indigo", "Purple", "Pink",
            "Brown" };

    // Simple list adapter for colors
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this,
            android.R.layout.select_dialog_item, names) {
        @NonNull
        @Override
        public View getView(int position, @androidx.annotation.Nullable View convertView,
                @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            android.widget.TextView tv = (android.widget.TextView) v;
            tv.setTextColor(textColors[position]);
            tv.setTypeface(null, Typeface.BOLD);
            return v;
        }
    };

    builder.setAdapter(adapter, (dialog, which) -> {
        // Apply Standard ForegroundColorSpan (UserColor)
        // We do NOT use AutoColorSpan here because we want this to persist
        editTextContent.getText().setSpan(new ForegroundColorSpan(textColors[which]), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    });
    builder.show();
}
