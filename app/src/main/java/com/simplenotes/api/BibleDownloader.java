package com.simplenotes.api;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplenotes.AppDatabase;
import com.simplenotes.Verse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BibleDownloader {
    private static final String KJV_URL = "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_kjv.json";

    public interface DownloadCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public static void downloadKJV(Context context, DownloadCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(KJV_URL).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to download file: " + response.code());
                }

                String jsonData = response.body().string();

                // Parse JSON
                // Structure expected: List of Books, where each Book has chapters (List of List
                // of Strings)
                Gson gson = new Gson();
                List<BookJson> books = gson.fromJson(jsonData, new TypeToken<List<BookJson>>() {
                }.getType());

                List<Verse> versesToInsert = new ArrayList<>();

                for (BookJson book : books) {
                    for (int c = 0; c < book.chapters.size(); c++) {
                        List<String> verses = book.chapters.get(c);
                        for (int v = 0; v < verses.size(); v++) {
                            // c is 0-indexed index, chapter is c+1
                            // v is 0-indexed index, verse is v+1
                            versesToInsert.add(new Verse(
                                    "kjv",
                                    book.name,
                                    c + 1,
                                    v + 1,
                                    verses.get(v)));
                        }
                    }
                }

                // Insert into DB
                AppDatabase db = AppDatabase.getDatabase(context);
                db.bibleDao().insertVerses(versesToInsert);
                db.bibleDao().markVersionDownloaded("kjv");

                // Notify Main Thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onFailure(e.getMessage()));
            }
        }).start();
    }

    // JSON Mapping Classes
    private static class BookJson {
        String abbrev;
        List<List<String>> chapters;
        String name;
    }
}
