package com.simplenotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "note_style_prefs";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_TEXT_BOLD = "text_bold";
    public static final String KEY_TEXT_ITALIC = "text_italic";
    public static final String KEY_TEXT_UNDERLINE = "text_underline";
    public static final String KEY_TEXT_COLOR_INDEX = "text_color_index";
    // REMOVED: KEY_TEXT_BG_COLOR_INDEX
    public static final String KEY_MAGIC_COLOR_INDEX = "magic_color_index";
    // REMOVED: KEY_MAGIC_BG_COLOR_INDEX

    private SwitchCompat switchNoteStyle;
    private LinearLayout layoutSettingsContainer;
    private ImageButton btnBold, btnItalic, btnUnderline;
    private View viewFontColorPreview;
    private View viewMagicFontColorPreview;

    private boolean isBold, isItalic, isUnderline;
    private int textColorIndex = 0;
    private int magicColorIndex = 0;

    private int[] highlightColors;
    private int[] textColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initColors();
        initViews();
        loadPreferences();
        setupListeners();
        updateUI();
    }

    private void initColors() {
        highlightColors = new int[] {
                ContextCompat.getColor(this, R.color.highlight_gold),
                ContextCompat.getColor(this, R.color.highlight_blue),
                ContextCompat.getColor(this, R.color.highlight_green),
                ContextCompat.getColor(this, R.color.highlight_pink),
                ContextCompat.getColor(this, R.color.highlight_purple),
                ContextCompat.getColor(this, R.color.highlight_peach)
        };

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

    private void initViews() {
        switchNoteStyle = findViewById(R.id.switchNoteStyle);
        layoutSettingsContainer = findViewById(R.id.layoutSettingsContainer);

        btnBold = findViewById(R.id.btnSettingBold);
        btnItalic = findViewById(R.id.btnSettingItalic);
        btnUnderline = findViewById(R.id.btnSettingUnderline);

        viewFontColorPreview = findViewById(R.id.viewSettingFontColorPreview);
        viewMagicFontColorPreview = findViewById(R.id.viewSettingMagicFontColorPreview);

        findViewById(R.id.layoutSettingFontColor)
                .setOnClickListener(v -> showColorPicker("Font Color", textColors, textColorIndex, index -> {
                    textColorIndex = index;
                    updatePreview(viewFontColorPreview, textColors, textColorIndex);
                    savePreferences();
                }));

        findViewById(R.id.layoutSettingMagicFontColor)
                .setOnClickListener(v -> showColorPicker("Magic Verse Color", textColors, magicColorIndex, index -> {
                    magicColorIndex = index;
                    updatePreview(viewMagicFontColorPreview, textColors, magicColorIndex);
                    savePreferences();
                }));

    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        switchNoteStyle.setChecked(enabled);
        layoutSettingsContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);

        isBold = prefs.getBoolean(KEY_TEXT_BOLD, false);
        isItalic = prefs.getBoolean(KEY_TEXT_ITALIC, false);
        isUnderline = prefs.getBoolean(KEY_TEXT_UNDERLINE, false);

        textColorIndex = prefs.getInt(KEY_TEXT_COLOR_INDEX, 0);
        magicColorIndex = prefs.getInt(KEY_MAGIC_COLOR_INDEX, 0);
    }

    private void setupListeners() {
        switchNoteStyle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutSettingsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            savePreferences();
        });

        View.OnClickListener toggleListener = v -> {
            if (v == btnBold)
                isBold = !isBold;
            else if (v == btnItalic)
                isItalic = !isItalic;
            else if (v == btnUnderline)
                isUnderline = !isUnderline;
            updateUI();
            savePreferences();
        };

        btnBold.setOnClickListener(toggleListener);
        btnItalic.setOnClickListener(toggleListener);
        btnUnderline.setOnClickListener(toggleListener);
    }

    private void updateUI() {
        int activeColor = ContextCompat.getColor(this, R.color.bible_gold);
        int inactiveColor = ContextCompat.getColor(this, R.color.bible_cream);

        btnBold.setColorFilter(isBold ? activeColor : inactiveColor);
        btnItalic.setColorFilter(isItalic ? activeColor : inactiveColor);
        btnUnderline.setColorFilter(isUnderline ? activeColor : inactiveColor);

        updatePreview(viewFontColorPreview, textColors, textColorIndex);
        updatePreview(viewMagicFontColorPreview, textColors, magicColorIndex);
    }

    private void updatePreview(View view, int[] colors, int index) {
        int color = (index > 0 && index <= colors.length) ? colors[index - 1] : 0; // 0 is transparent/default

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);

        if (index == 0) {
            // Default - maybe stroke only or X?
            shape.setColor(0x00000000); // Transparent
            shape.setStroke(2, ContextCompat.getColor(this, R.color.bible_cream));
        } else {
            shape.setColor(color);
            shape.setStroke(0, 0);
        }
        view.setBackground(shape);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_ENABLED, switchNoteStyle.isChecked());
        editor.putBoolean(KEY_TEXT_BOLD, isBold);
        editor.putBoolean(KEY_TEXT_ITALIC, isItalic);
        editor.putBoolean(KEY_TEXT_UNDERLINE, isUnderline);
        editor.putInt(KEY_TEXT_COLOR_INDEX, textColorIndex);
        editor.putInt(KEY_MAGIC_COLOR_INDEX, magicColorIndex);
        editor.apply();
    }

    private interface ColorPickerCallback {
        void onColorSelected(int index);
    }

    private void showColorPicker(String title, int[] colors, int selectedIndex, ColorPickerCallback callback) {
        int[] displayColors = prependZero(colors);
        ColorBottomSheet sheet = ColorBottomSheet.newInstance(title, displayColors, selectedIndex);
        sheet.setListener(callback::onColorSelected);
        sheet.show(getSupportFragmentManager(), "ColorSheet");
    }

    private int[] prependZero(int[] original) {
        int[] result = new int[original.length + 1];
        result[0] = 0; // The "None" value
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }
}
