package com.example.android.stemnews;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class ArticleLoader extends AsyncTaskLoader<List<NewsArticle>> {

    private String queryUrl;
    private List<NewsArticle> existingList;
    private boolean forceLoadFlag;

    ArticleLoader(Context context, String queryUrl, List<NewsArticle> existingList, boolean forceLoadFlag) {
        super(context);
        this.queryUrl = queryUrl;
        this.existingList = existingList;
        this.forceLoadFlag = forceLoadFlag;
    }

    @Override
    protected void onStartLoading() {
        /*
        Use cached data if it exists and we aren't forcing a load. This prevents the Loader from
        reloading if the app is moved to the background
        */
        if (existingList != null && !forceLoadFlag) {
            deliverResult(existingList);
        } else {
            forceLoad();
        }
    }

    @Override
    public List<NewsArticle> loadInBackground() {
        if (queryUrl == null) {
            return null;
        }

        /*
        Perform the network request using the query URL, parse the response and extract the
        list of news articles created
        */
        return QueryUtils.fetchLatestNews(queryUrl, getContext());
    }

    @Override
    public void deliverResult(List<NewsArticle> data) {
        /*
        If the new list consists of acceptable values and there is a previous data set of news
        articles already in the loader, add the new list to the end of the existing list and
        output the combined result. Otherwise, just output the new data set
        */
        if (data != null && !data.isEmpty()) {
            if (existingList != null) {
                existingList.addAll(data);
            } else {
                existingList = new ArrayList<>(data);
            }

            /*
            Once we have existing data, flip the forceLoad flag for the rest of this loader's
            lifecycle so that it uses the cached data instead of carrying out a new request
            */
            forceLoadFlag = false;
            super.deliverResult(existingList);
        } else {
            super.deliverResult(data);
        }
    }
}
