package com.devss.tmdbdemo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TvShowServices {

    @GET("tv/popular")
    Call<PopularTvShows> getPopularTvShows(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int pageIndex
    );
}
