package com.devss.tmdbdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

public class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private static final int HERO = 2;

    private static final String BASE_URL_IMG = "https://image.tmdb.org/t/p/w300";
    private static final String BASE_URL_IMG_BACKGROUND = "https://image.tmdb.org/t/p/w780";

    private ArrayList<Result> tvShowResults;
    private Context context;

    private boolean isLoadingAdded = false;
    private boolean retryPageLoad = false;

    private PaginationAdapterCallback mCallback;

    private String errorMsg;

    public PaginationAdapter(Context context) {
        this.context = context;
        this.mCallback = (PaginationAdapterCallback) context;
        tvShowResults = new ArrayList<>();
    }

    public PaginationAdapter(ArrayList<Result> tvShowResults) {
        this.tvShowResults = tvShowResults;
    }

    public ArrayList<Result> getTvShows() {
        return  tvShowResults;
    }

    public void setTvShows(ArrayList<Result> tvShowResults) {
        this.tvShowResults = tvShowResults;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case ITEM:
                View viewItem = inflater.inflate(R.layout.item_list, parent, false);
                viewHolder = new TvShowViewHolder(viewItem);
                break;
            case LOADING:
                View viewLoading = inflater.inflate(R.layout.item_progress, parent, false);
                viewHolder = new LoadingViewH(viewLoading);
                break;
            case HERO:
                View viewHero = inflater.inflate(R.layout.item_hero, parent, false);
                viewHolder = new HeaderViewH(viewHero);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Result result = tvShowResults.get(position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();

                Intent intent = new Intent(context, DetailTvShowActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(Intent.EXTRA_TEXT, tvShowResults);
                bundle.putInt("POSITION", holder.getAdapterPosition());
                intent.putExtras(bundle);

                context.startActivity(intent);
            }
        });

        switch (getItemViewType(position)) {

            case HERO:
                final HeaderViewH heroVh = (HeaderViewH) holder;

                heroVh.mTvShowTitle.setText(result.getTitle());
                heroVh.mTvShowDesc.setText(result.getOverview());

                loadImage(BASE_URL_IMG_BACKGROUND + result.getBackdropPath())
                        .into(heroVh.mPosterImg);
                break;

            case ITEM:
                final TvShowViewHolder tvShowViewHolder = (TvShowViewHolder) holder;

                tvShowViewHolder.mTvShowTitle.setText(result.getTitle());
                tvShowViewHolder.mTvShowDesc.setText(result.getOverview());

                GlideDrawableImageViewTarget imageViewPreview = new GlideDrawableImageViewTarget(tvShowViewHolder.mPosterImg);
                loadImage(BASE_URL_IMG + result.getPosterPath())
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        tvShowViewHolder.mProgress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        tvShowViewHolder.mProgress.setVisibility(View.GONE);
                        return false;
                    }
                }).into(imageViewPreview);
                break;

            case LOADING:
                LoadingViewH loadingViewH = (LoadingViewH) holder;

                if (retryPageLoad) {
                    loadingVH.mErrorLayout.setVisibility(View.VISIBLE);
                    loadingVH.mProgressBar.setVisibility(View.GONE);

                    loadingVH.mErrorText.setText(errorMsg != null ? errorMsg : context.getString(R.string.error_msg_unknown));
                } else {
                    loadingVH.mErrorLayout.setVisibility(View.GONE);
                    loadingVH.mProgressBar.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return tvShowResults == null ? 0 : tvShowResults.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HERO;
        } else {
            return (position == tvShowResults.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
        }
    }

    private DrawableRequestBuilder<String> loadImage(@NonNull String posterPath) {
        return Glide
                .with(context)
                .load(posterPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized image
                .centerCrop()
                .crossFade();
    }

    public void add(Result r) {
        tvShowResults.add(r);
        notifyItemInserted(tvShowResults.size() - 1);
    }

    public void addAll(List<Result> moveResults) {
        for (Result result : moveResults) {
            add(result);
        }
    }

    public void remove(Result r) {
        int position = tvShowResults.indexOf(r);
        if (position > -1) {
            tvShowResults.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Result());
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;

        int position = tvShowResults.size() - 1;
        Result result = getItem(position);

        if (result != null) {
            tvShowResults.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Result getItem(int position) {
        return tvShowResults.get(position);
    }

    public void showRetry(boolean show, @Nullable String errorMsg) {
        retryPageLoad = show;
        notifyItemChanged(tvShowResults.size() - 1);

        if (errorMsg != null) this.errorMsg = errorMsg;
    }

    protected class HeaderViewH extends RecyclerView.ViewHolder {
        private TextView mTvShowTitle;
        private TextView mTvShowDesc;
        private ImageView mPosterImg;

        public HeaderViewH(@NonNull View itemView) {
            super(itemView);

            mTvShowTitle = itemView.findViewById(R.id.tvshow_title);
            mTvShowDesc = itemView.findViewById(R.id.tvshow_desc);
            mPosterImg = itemView.findViewById(R.id.tvshow_poster);
        }
    }

    protected class TvShowViewHolder extends RecyclerView.ViewHolder {
        private TextView mTvShowTitle;
        private TextView mTvShowDesc;
        private ImageView mPosterImg;
        private ProgressBar mProgress;

        public TvShowViewHolder(@NonNull View itemView) {
            super(itemView);

            mTvShowTitle = itemView.findViewById(R.id.tvshow_title);
            mTvShowDesc = itemView.findViewById(R.id.tvshow_desc);
            mPosterImg = itemView.findViewById(R.id.tvshow_poster);
//            mProgress = itemView.findViewById(R.id.tvshow_progress);
        }
    }

    protected class LoadingViewH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ProgressBar mProgressBar;
        private ImageButton mRetryBtn;
        private TextView mErrorTxt;
        private LinearLayout mErrorLayout;

        public LoadingViewH(@NonNull View itemView) {
            super(itemView);

            mProgressBar = (ProgressBar) itemView.findViewById(R.id.loadmore_progress);
            mRetryBtn = (ImageButton) itemView.findViewById(R.id.loadmore_retry);
            mErrorTxt = (TextView) itemView.findViewById(R.id.loadmore_errortxt);
            mErrorLayout = (LinearLayout) itemView.findViewById(R.id.loadmore_errorlayout);

            mRetryBtn.setOnClickListener(this);
            mErrorLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.loadmore_retry:
                case R.id.loadmore_errorlayout:
                    showRetry(false, null);
                    mCallback.retryPageLoad();

                    break;
            }
        }
    }
}












