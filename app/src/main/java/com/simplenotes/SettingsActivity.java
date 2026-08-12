package com.simplenotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.simplenotes.utils.GoogleDriveBackupManager;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "note_style_prefs";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_TEXT_BOLD = "text_bold";
    public static final String KEY_TEXT_ITALIC = "text_italic";
    public static final String KEY_TEXT_UNDERLINE = "text_underline";
    public static final String KEY_TEXT_COLOR_INDEX = "text_color_index";
    public static final String KEY_MAGIC_COLOR_INDEX = "magic_color_index";

    private SwitchCompat switchNoteStyle;
    private LinearLayout layoutSettingsContainer;
    private ImageButton btnBold, btnItalic, btnUnderline;
    private View viewFontColorPreview;
    private View viewMagicFontColorPreview;

    // Google Drive Backup UI
    private TextView textViewDriveAccountStatus;
    private MaterialButton btnGoogleSignIn;
    private LinearLayout layoutDriveActions;
    private MaterialButton btnBackupDrive;
    private MaterialButton btnRestoreDrive;
    private ProgressBar progressBarDrive;

    private boolean isBold, isItalic, isUnderline;
    private int textColorIndex = 0;
    private int magicColorIndex = 0;

    private int[] highlightColors;
    private int[] textColors;

    private ActivityResultLauncher<Intent> driveSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initSignInLauncher();
        initColors();
        initViews();
        loadPreferences();
        setupListeners();
        updateUI();
        updateDriveUI();
    }

    private void initSignInLauncher() {
        driveSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                Toast.makeText(this, "Connected: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
                    }
                    updateDriveUI();
                }
        );
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

        // Drive Backup Views
        textViewDriveAccountStatus = findViewById(R.id.textViewDriveAccountStatus);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        layoutDriveActions = findViewById(R.id.layoutDriveActions);
        btnBackupDrive = findViewById(R.id.btnBackupDrive);
        btnRestoreDrive = findViewById(R.id.btnRestoreDrive);
        progressBarDrive = findViewById(R.id.progressBarDrive);

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

        // Drive Backup Listeners
        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = GoogleDriveBackupManager.getGoogleSignInClient(this).getSignInIntent();
            driveSignInLauncher.launch(signInIntent);
        });

        btnBackupDrive.setOnClickListener(v -> performDriveBackup());
        btnRestoreDrive.setOnClickListener(v -> confirmDriveRestore());
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

    private void updateDriveUI() {
        boolean signedIn = GoogleDriveBackupManager.isSignedIn(this);
        String email = GoogleDriveBackupManager.getSignedInAccountEmail(this);

        if (signedIn && email != null) {
            textViewDriveAccountStatus.setText("Connected as: " + email);
            btnGoogleSignIn.setText("Switch Google Account");
            layoutDriveActions.setVisibility(View.VISIBLE);
        } else {
            textViewDriveAccountStatus.setText("Not connected");
            btnGoogleSignIn.setText("Connect Google Account");
            layoutDriveActions.setVisibility(View.GONE);
        }
    }

    private void performDriveBackup() {
        setDriveLoading(true);
        GoogleDriveBackupManager.backupNotes(this, new GoogleDriveBackupManager.BackupCallback() {
            @Override
            public void onSuccess(int noteCount) {
                setDriveLoading(false);
                Toast.makeText(SettingsActivity.this,
                        "Successfully backed up " + noteCount + " notes to Google Drive!",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                setDriveLoading(false);
                Toast.makeText(SettingsActivity.this,
                        "Backup failed: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDriveRestore() {
        new AlertDialog.Builder(this)
                .setTitle("Restore Notes from Google Drive?")
                .setMessage("This will download notes from your Google Drive backup and merge them into your local notes. Continue?")
                .setPositiveButton("Restore", (dialog, which) -> performDriveRestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDriveRestore() {
        setDriveLoading(true);
        GoogleDriveBackupManager.restoreNotes(this, new GoogleDriveBackupManager.RestoreCallback() {
            @Override
            public void onSuccess(int noteCount) {
                setDriveLoading(false);
                Toast.makeText(SettingsActivity.this,
                        "Successfully restored " + noteCount + " notes from Google Drive!",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                setDriveLoading(false);
                Toast.makeText(SettingsActivity.this,
                        "Restore failed: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setDriveLoading(boolean isLoading) {
        progressBarDrive.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnBackupDrive.setEnabled(!isLoading);
        btnRestoreDrive.setEnabled(!isLoading);
        btnGoogleSignIn.setEnabled(!isLoading);
    }

    private void updatePreview(View view, int[] colors, int index) {
        int color = (index > 0 && index <= colors.length) ? colors[index - 1] : 0;

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);

        if (index == 0) {
            shape.setColor(0x00000000);
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
        result[0] = 0;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }
}
