package com.simplenotes.utils;

import android.content.Context;
import android.content.Intent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplenotes.AppDatabase;
import com.simplenotes.AppExecutors;
import com.simplenotes.Note;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class GoogleDriveBackupManager {

    private static final String BACKUP_FILE_NAME = "simplenotes_backup.json";
    private static final String MIME_TYPE_JSON = "application/json";

    public interface BackupCallback {
        void onSuccess(int noteCount);
        void onError(String error);
    }

    public interface RestoreCallback {
        void onSuccess(int noteCount);
        void onError(String error);
    }

    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    public static GoogleSignInAccount getSignedInAccount(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    public static String getSignedInAccountEmail(Context context) {
        GoogleSignInAccount account = getSignedInAccount(context);
        return account != null ? account.getEmail() : null;
    }

    public static boolean isSignedIn(Context context) {
        GoogleSignInAccount account = getSignedInAccount(context);
        return account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA));
    }

    private static Drive getDriveService(Context context) {
        GoogleSignInAccount account = getSignedInAccount(context);
        if (account == null) {
            return null;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("SimpleNotes")
                .build();
    }

    public static void backupNotes(Context context, BackupCallback callback) {
        GoogleSignInAccount account = getSignedInAccount(context);
        if (account == null) {
            callback.onError("Not signed in to Google");
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // 1. Fetch notes from local DB
                List<Note> notes = AppDatabase.getDatabase(context).noteDao().getAllNotes();
                if (notes == null) {
                    notes = Collections.emptyList();
                }

                // 2. Build Drive service
                Drive driveService = getDriveService(context);
                if (driveService == null) {
                    notifyBackupError(callback, "Failed to initialize Drive service");
                    return;
                }

                // 3. Serialize notes to JSON
                Gson gson = new Gson();
                String jsonContent = gson.toJson(notes);
                byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
                ByteArrayContent mediaContent = new ByteArrayContent(MIME_TYPE_JSON, contentBytes);

                // 4. Search for existing file in appDataFolder
                FileList fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .execute();

                if (fileList.getFiles() != null && !fileList.getFiles().isEmpty()) {
                    // Update existing backup file
                    String existingFileId = fileList.getFiles().get(0).getId();
                    driveService.files().update(existingFileId, null, mediaContent).execute();
                } else {
                    // Create new backup file in appDataFolder
                    File metadata = new File()
                            .setName(BACKUP_FILE_NAME)
                            .setParents(Collections.singletonList("appDataFolder"));
                    driveService.files().create(metadata, mediaContent).execute();
                }

                int count = notes.size();
                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(count));

            } catch (Exception e) {
                e.printStackTrace();
                notifyBackupError(callback, e.getMessage() != null ? e.getMessage() : "Backup failed");
            }
        });
    }

    public static void restoreNotes(Context context, RestoreCallback callback) {
        GoogleSignInAccount account = getSignedInAccount(context);
        if (account == null) {
            callback.onError("Not signed in to Google");
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Drive driveService = getDriveService(context);
                if (driveService == null) {
                    notifyRestoreError(callback, "Failed to initialize Drive service");
                    return;
                }

                // 1. Search for backup file in appDataFolder
                FileList fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .execute();

                if (fileList.getFiles() == null || fileList.getFiles().isEmpty()) {
                    notifyRestoreError(callback, "No backup file found in Google Drive");
                    return;
                }

                // 2. Download file content
                String fileId = fileList.getFiles().get(0).getId();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                String jsonContent = outputStream.toString("UTF-8");

                // 3. Deserialize JSON to Notes list
                Gson gson = new Gson();
                List<Note> restoredNotes = gson.fromJson(jsonContent, new TypeToken<List<Note>>() {}.getType());

                if (restoredNotes == null || restoredNotes.isEmpty()) {
                    notifyRestoreError(callback, "Backup file is empty");
                    return;
                }

                // 4. Upsert notes into Room DB
                for (Note note : restoredNotes) {
                    AppDatabase.getDatabase(context).noteDao().insert(note);
                }

                int count = restoredNotes.size();
                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(count));

            } catch (Exception e) {
                e.printStackTrace();
                notifyRestoreError(callback, e.getMessage() != null ? e.getMessage() : "Restore failed");
            }
        });
    }

    private static void notifyBackupError(BackupCallback callback, String error) {
        AppExecutors.getInstance().mainThread().execute(() -> callback.onError(error));
    }

    private static void notifyRestoreError(RestoreCallback callback, String error) {
        AppExecutors.getInstance().mainThread().execute(() -> callback.onError(error));
    }
}
