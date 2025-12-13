package com.simplenotes.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BibleApiService {
    @GET("/{reference}")
    Call<BibleResponse> getVerse(@Path("reference") String reference,
            @retrofit2.http.Query("translation") String translation);
}
