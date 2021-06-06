package com.devss.tmdbdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements PaginationAdapterCallback {

    private static final String TAG = "MainActivity";
    private static final int PAGE_START = 1;

    PaginationAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    RecyclerView rv;
    ProgressBar progressBar;
    LinearLayout errorLayout;
    Button btnRetry;
    TextView txtError;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 1002;
    private int currentPage = PAGE_START;

    private TvShowServices tvShowServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rv = findViewById(R.id.main_recycler);
        progressBar = findViewById(R.id.main_progress);
        errorLayout = findViewById(R.id.error_layout);
        btnRetry = findViewById(R.id.error_btn_retry);
        txtError = findViewById(R.id.error_txt_cause);

        // Creating Pagination Adapter
        adapter = new PaginationAdapter(this);

        // Setting layout manager on the recycler view
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(linearLayoutManager);
        rv.setItemAnimator(new DefaultItemAnimator()); // Setting default animator

        rv.setAdapter(adapter); // setting adapter

        // Adding scroll Listner
        rv.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;

                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    private void loadFirstPage() {
        Log.d(TAG, "loadFirstPage: ");

        hideErrorView();

        callPopulartvShowsApi().enqueue(new Callback<PopularTvShows>() {
            @Override
            public void onResponse(Call<PopularTvShows> call, Response<PopularTvShows> response) {

                hideErrorView();

                List<Result> results = fetchResults(response);
                progressBar.setVisibility(View.GONE);
                adapter.addAll(results);

                if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(Call<PopularTvShows> call, Throwable t) {
                showErrorView(t);
            }
        });
    }

    private List<Result> fetchResults(Response<PopularTvShows> response) {
        PopularTvShows popularTvShows = response.body();
        return popularTvShows.getResults();
    }

    private void loadNextPage() {
        Log.d(TAG, "loadNextPage: " + currentPage);

        callPopulartvShowsApi().enqueue(new Callback<PopularTvShows>() {
            @Override
            public void onResponse(Call<PopularTvShows> call, Response<PopularTvShows> response) {
                adapter.removeLoadingFooter();
                isLoading = false;

                List<Result> results = fetchResults(response);
                adapter.addAll(results);

                if (currentPage != TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(Call<PopularTvShows> call, Throwable t) {
                adapter.showRetry(true, fetchErrorMessage(t));
            }
        });
    }

    private Call<PopularTvShows> callPopulartvShowsApi() {
        return tvShowServices.getPopularTvShows(
                getString(R.string.my_api_key),
                "en_US",
                currentPage
        );
    }

    @Override
    public void retryPageLoad() {
        loadNextPage();
    }

    private void showErrorView(Throwable throwable) {

        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            txtError.setText(fetchErrorMessage(throwable));
        }
    }

    private String fetchErrorMessage(Throwable throwable) {
        String errorMsg = getResources().getString(R.string.error_msg_unknown);

        if (!isNetworkConnected()) {
            errorMsg = getResources().getString(R.string.error_msg_no_internet);
        } else if (throwable instanceof TimeoutException) {
            errorMsg = getResources().getString(R.string.error_msg_timeout);
        }

        return errorMsg;
    }

    private void hideErrorView() {
        if (errorLayout.getVisibility() == View.VISIBLE) {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE); // retrieve a ConnectivityManager for handling management of network connections

        return cm.getActiveNetworkInfo() != null; // returns currently active default data network
    }
}