package com.simplenotes.api;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simplenotes.AppDatabase;
import com.simplenotes.Verse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BibleDownloader {
    private static final Map<String, String> VERSION_URLS = new HashMap<>();

    // ... URLs ...
    static {
        // English
        VERSION_URLS.put("kjv", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_kjv.json");
        VERSION_URLS.put("bbe", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_bbe.json");
        VERSION_URLS.put("niv",
                "https://raw.githubusercontent.com/jadenzaleski/bible-translations/41a2e9212da9f3daf8419b3d1a06194914b711eb/NIV/NIV_bible.json");
        VERSION_URLS.put("nlt",
                "https://raw.githubusercontent.com/jadenzaleski/bible-translations/41a2e9212da9f3daf8419b3d1a06194914b711eb/NLT/NLT_bible.json");

        // Other Languages
        VERSION_URLS.put("ar_svd", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/ar_svd.json");
        VERSION_URLS.put("zh_cuv", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/zh_cuv.json");
        VERSION_URLS.put("zh_ncv", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/zh_ncv.json");
        VERSION_URLS.put("eo_esperanto",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/eo_esperanto.json");
        VERSION_URLS.put("fi_finnish",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/fi_finnish.json");
        VERSION_URLS.put("fi_pr", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/fi_pr.json");
        VERSION_URLS.put("fr_apee", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/fr_apee.json");
        VERSION_URLS.put("de_schlachter",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/de_schlachter.json");
        VERSION_URLS.put("el_greek", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/el_greek.json");
        VERSION_URLS.put("ko_ko", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/ko_ko.json");
        VERSION_URLS.put("pt_aa", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/pt_aa.json");
        VERSION_URLS.put("pt_acf", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/pt_acf.json");
        VERSION_URLS.put("pt_nvi", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/pt_nvi.json");
        VERSION_URLS.put("ro_cornilescu",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/ro_cornilescu.json");
        VERSION_URLS.put("ru_synodal",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/ru_synodal.json");
        VERSION_URLS.put("es_rvr", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/es_rvr.json");
        VERSION_URLS.put("vi_vietnamese",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/vi_vietnamese.json");
    }

    public interface DownloadCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public static boolean isVersionSupported(String versionId) {
        return VERSION_URLS.containsKey(versionId);
    }

    /**
     * @deprecated Use downloadVersion instead
     */
    @Deprecated
    public static void downloadKJV(Context context, DownloadCallback callback) {
        downloadVersion(context, "kjv", callback);
    }

    public static void downloadVersion(Context context, String versionId, DownloadCallback callback) {
        if (versionId.equals("tel")) {
            downloadTelugu(context, callback);
            return;
        }

        String url = VERSION_URLS.get(versionId);
        if (url == null) {
            callback.onFailure("Version not supported for offline download: " + versionId);
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to download file: " + response.code());
                }

                String jsonData = response.body().string();
                Gson gson = new Gson();
                List<Verse> versesToInsert = new ArrayList<>();

                if (versionId.equals("niv") || versionId.equals("nlt")) {
                    // Handle Map Format (Book -> Chapter -> Verse -> Text)
                    java.lang.reflect.Type type = new TypeToken<Map<String, Map<String, Map<String, String>>>>() {
                    }.getType();
                    Map<String, Map<String, Map<String, String>>> bibleMap = gson.fromJson(jsonData, type);

                    // Use Canonical Order from BibleData
                    for (String bookName : com.simplenotes.BibleData.BOOKS) {
                        Map<String, Map<String, String>> bookData = bibleMap.get(bookName);
                        if (bookData == null)
                            continue; // Book not found in JSON (or name mismatch)

                        // Iterate Chapters (Keys are "1", "2", etc.)
                        List<String> chapterKeys = new ArrayList<>(bookData.keySet());
                        // Sort chapters numerically to be safe
                        chapterKeys.sort((a, b) -> {
                            try {
                                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                            } catch (NumberFormatException e) {
                                return a.compareTo(b);
                            }
                        });

                        for (String chapterKey : chapterKeys) {
                            Map<String, String> verseData = bookData.get(chapterKey);
                            if (verseData == null)
                                continue;

                            List<String> verseKeys = new ArrayList<>(verseData.keySet());
                            // Sort verses numerically
                            verseKeys.sort((a, b) -> {
                                try {
                                    return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                                } catch (NumberFormatException e) {
                                    return a.compareTo(b);
                                }
                            });

                            for (String verseKey : verseKeys) {
                                String text = verseData.get(verseKey);
                                try {
                                    int chapter = Integer.parseInt(chapterKey);
                                    int verse = Integer.parseInt(verseKey);
                                    versesToInsert.add(new Verse(versionId, bookName, chapter, verse, text));
                                } catch (NumberFormatException e) {
                                    // Skip malformed keys
                                }
                            }
                        }
                    }

                } else {
                    // Handle List Format (List<BookJson>) - Existing Logic
                    List<BookJson> books = gson.fromJson(jsonData, new TypeToken<List<BookJson>>() {
                    }.getType());

                    for (BookJson book : books) {
                        for (int c = 0; c < book.chapters.size(); c++) {
                            List<String> verses = book.chapters.get(c);
                            for (int v = 0; v < verses.size(); v++) {
                                // c is 0-indexed index, chapter is c+1
                                // v is 0-indexed index, verse is v+1
                                versesToInsert.add(new Verse(
                                        versionId,
                                        book.name,
                                        c + 1,
                                        v + 1,
                                        verses.get(v)));
                            }
                        }
                    }
                }

                // Insert into DB
                AppDatabase db = AppDatabase.getDatabase(context);
                db.bibleDao().insertVerses(versesToInsert);
                db.bibleDao().markVersionDownloaded(versionId);

                // Notify Main Thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onFailure(e.getMessage()));
            }
        }).start();
    }

    private static void downloadTelugu(Context context, DownloadCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Gson gson = new Gson();
                List<Verse> allVerses = new ArrayList<>();
                AppDatabase db = AppDatabase.getDatabase(context);

                int successCount = 0;
                // Base URL for aruljohn/Bible-telugu
                String baseUrl = "https://raw.githubusercontent.com/aruljohn/Bible-telugu/master/";

                for (String bookName : com.simplenotes.BibleData.BOOKS) {
                    // Handle URL encoding for filenames with spaces (e.g. "1 Samuel" ->
                    // "1%20Samuel")
                    // URLEncoder encodes spaces as '+' which might fail on raw.github, so we
                    // manually swap
                    String encodedName = bookName.replace(" ", "%20");
                    String url = baseUrl + encodedName + ".json";

                    Request request = new Request.Builder().url(url).build();
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            System.err.println("Failed to download book: " + bookName);
                            continue; // Skip failed book but try others (or could fail hard)
                        }

                        String jsonData = response.body().string();
                        // Parse individual book structure
                        TeluguBookResponse bookData = gson.fromJson(jsonData, TeluguBookResponse.class);

                        if (bookData != null && bookData.chapters != null) {
                            for (TeluguChapter chap : bookData.chapters) {
                                if (chap.verses == null)
                                    continue;
                                int chapterNum = Integer.parseInt(chap.chapter);

                                for (TeluguVerse v : chap.verses) {
                                    int verseNum = Integer.parseInt(v.verse);
                                    allVerses.add(new Verse(
                                            "tel",
                                            bookName, // Use canonical name from BibleData
                                            chapterNum,
                                            verseNum,
                                            v.text));
                                }
                            }
                            successCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (successCount == 0) {
                    throw new IOException("Failed to download any Telugu books.");
                }

                // Insert into DB in one transaction
                db.bibleDao().insertVerses(allVerses);
                db.bibleDao().markVersionDownloaded("tel");

                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onFailure(e.getMessage()));
            }
        }).start();
    }

    // --- Telugu JSON Mapping Classes ---
    private static class TeluguBookResponse {
        // "book": { "english": "Genesis", ... }
        // We don't strictly need the book object if we use our loop's bookName,
        // but removing it might break GSON matching if not careful.
        // GSON ignores missing fields, so we just match "chapters".
        List<TeluguChapter> chapters;
    }

    private static class TeluguChapter {
        String chapter;
        List<TeluguVerse> verses;
    }

    private static class TeluguVerse {
        String verse;
        String text;
    }
    // -----------------------------------

    // JSON Mapping Classes
    private static class BookJson {
        String abbrev;
        List<List<String>> chapters;
        String name;
    }
}
