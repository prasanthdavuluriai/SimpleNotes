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

    static {
        // English
        VERSION_URLS.put("kjv", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_kjv.json");
        VERSION_URLS.put("bbe", "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_bbe.json");

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
                                    versionId,
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

    // JSON Mapping Classes
    private static class BookJson {
        String abbrev;
        List<List<String>> chapters;
        String name;
    }
}
