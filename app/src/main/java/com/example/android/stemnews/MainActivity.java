package com.example.android.stemnews;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<NewsArticle>> {

    private final String LOG_TAG = MainActivity.class.getName();

    /**
     * The query URL for news article data from The Guardian web API.
     * <p>
     * I use my personal API key from the project gradle.properties file, but this is not included
     * in the GitHub repo, so other users will need to get their own API Key from:
     * https://open-platform.theguardian.com/access/on (or use the "test" key).
     * <p>
     * To include your personal key in the app, add it to the project's gradle.properties file
     * and use the following link as a guide to include it in your build.gradle (Module:app) file
     * under the name GuardianAPIKey:
     * https://medium.com/code-better/hiding-api-keys-from-your-android-repository-b23f5598b906
     * <p>
     * For test purposes, you can simply replace the GuardianAPIKey call in the build.gradle file
     * with the String "test", but this only gives you a limited number of calls to the servers.
     */
    private static final String GUARDIAN_API_URL = "http://content.guardianapis.com/search";

    private ListView articleListView;
    private ArticleAdapter articleAdapter;
    private TextView emptyStateView;
    private SwipeRefreshLayout articleRefresh;
    private View loadingIndicator;

    private int currentPage;
    private boolean articlesLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Replace the standard action bar for this activity with a custom toolbar
        Toolbar customToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(customToolbar);

        // Retrieve the loading indicator circle view
        loadingIndicator = findViewById(R.id.progress_circle);

        /* Retrieve the current page value on an orientation change. Otherwise, set it to its
        default initial value */
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(getString(R.string.current_page_key));
        } else {
            currentPage = 1;
        }

        // Retrieve the swipe-refresh view and add a listener to update the loader data on a refresh
        articleRefresh = findViewById(R.id.swipe_refresh);
        articleRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Set the page parameter value back to 1 and refresh the article list
                currentPage = 1;
                articleListView.setEnabled(false);
                updateArticles(true);
            }
        });

        /* Retrieve the {@link ListView} in the layout and then add the empty state {@link TextView}
        to it for when there is no data to display */
        articleListView = findViewById(R.id.article_list);
        emptyStateView = findViewById(R.id.empty_state_text);
        articleListView.setEmptyView(emptyStateView);

        /* Create a new adapter that takes an empty list of earthquakes as its input and then link
        the adapter to the {@link ListView} */
        articleAdapter = new ArticleAdapter(this, new ArrayList<NewsArticle>());
        articleListView.setAdapter(articleAdapter);

        /* Set an item click listener on the {@link ListView} that sends an intent to any
        available web browser to open the full selected article */
        articleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                NewsArticle clickedArticle = articleAdapter.getItem(position);

                if (clickedArticle != null) {
                    // Prepare a browser-opening {@link Intent} by parsing the website URL into a URI
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedArticle.getWebURL()));

                    /* Check that there is an application that can receive the intent and then
                    start the activity if there is. If there isn't one available, indicate this
                    with a {@link Toast} message */
                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_browser_app), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        /* Add a scroll listener to the {@link ListView} that loads in additional pages of data
        if they are available when the user nears the bottom of the current list */
        articleListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean userScrolled = false;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Check that the scroll event was a user controlled one
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        userScrolled = true;
                        break;
                    default:
                        userScrolled = false;
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                /* If the last visible item in the list is the last element AND no articles are
                currently being loaded in AND the scroll event was user controlled, prepare to
                load the next page of results into the adapter */
                if (absListView.getLastVisiblePosition() == (totalItemCount - 1) && !articlesLoading && userScrolled) {
                    /* Increase the results page number value by 1 and update the article list. It
                    does not matter if the currentPage value exceeds the query page size, as
                    this will just return no data and it will be handled by the
                    {@link LoaderManager} callbacks */
                    currentPage++;

                    /* Whilst the list is updating, show the loading indicator and prevent
                    user interaction with the {@link ListView} underneath */
                    loadingIndicator.setVisibility(View.VISIBLE);
                    articleListView.setEnabled(false);

                    updateArticles(true);
                }
            }
        });

        // Use a new {@link Loader} to create the first instance of the list
        updateArticles(false);
    }

    /**
     * Check that the device is connected to the internet before using a {@link ArticleLoader} to
     * load in the news data to the {@link ListView}. If the list is being refreshed, destroy the
     * old {@link ArticleLoader} and create a new one with the same ID.
     *
     * @param listRefresh is a boolean that indicates whether the list already exists and is being
     *                    refreshed by the user.
     */
    private void updateArticles(boolean listRefresh) {
        final int LOADER_ID = 1;

        /* Get a reference to the app's {@link ConnectivityManager} to check the state of
        the device's network connectivity */
        ConnectivityManager deviceConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (deviceConnectivity != null) {
            // Get details on the currently active network connection
            NetworkInfo deviceNetworkInfo = deviceConnectivity.getActiveNetworkInfo();

            /* If there is a network connection and information about it is available, fetch the
            news data using the {@link ArticleLoader} */
            if (deviceNetworkInfo != null && deviceNetworkInfo.isConnected()) {
                // Indicate that the load has started
                articlesLoading = true;

                /* Initialise the {@link ArticleLoader}. If we are refreshing the list instead of
                creating it for the first time, use the restartLoader method instead to destroy
                the existing one and create a new instance */
                if (listRefresh) {
                    getLoaderManager().restartLoader(LOADER_ID, null, this);
                } else {
                    getLoaderManager().initLoader(LOADER_ID, null, this);
                }
            } else {
                /* Otherwise, display a no internet connection error. Hide the loading indicator
                as well so that the error message is clearer */
                loadingIndicator.setVisibility(View.GONE);

                // Update empty state with no connection error message
                emptyStateView.setText(getString(R.string.no_internet_connection));
            }
        } else {
            loadingIndicator.setVisibility(View.GONE);

            emptyStateView.setText(getString(R.string.no_internet_connection));
            Log.e(LOG_TAG, "Error with connectivity services");
        }
    }

    @Override
    public Loader<List<NewsArticle>> onCreateLoader(int i, Bundle bundle) {//
        // Create the full URL String
        String fullUrl = createUrlString();

        /* If the article list is refreshing or it is empty, create a new {@link ArticleLoader} with
        no initial data. Otherwise, pass the existing data set to the loader so that it can be
        added to any new data that is obtained in the next HTTP request. Always force a new load
        of data if we are creating a new instance of {@link ArticleLoader} as well */
        if (articleRefresh.isRefreshing() || articleAdapter.isEmpty()) {
            return new ArticleLoader(this, fullUrl, null, true);
        } else {
            return new ArticleLoader(this, fullUrl, new ArrayList<>(articleAdapter.getAdapterItems()), true);
        }
    }

    /**
     * Create a full URL String for a HTTP request by using the domain/host String as a base and
     * then appending the relevant parameters onto this.
     */
    private String createUrlString() {
        // Get the shared preference keys and values for the app
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* Retrieve the String value or String {@link Set} for each parameter from the preferences.
         The second parameter is the default value for the preference if one has not already been
         loaded into the app */
        String orderBy = defaultPreferences.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));

        Set<String> searchCategories = defaultPreferences.getStringSet(
                getString(R.string.settings_search_categories_key),
                new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.settings_search_categories_values))));

        /* Combine all of the search category values into a single String, where they are
         separated with the logical OR String */
        StringBuilder categoriesParameter = new StringBuilder();
        Iterator<String> categoriesIterator = searchCategories.iterator();
        while (categoriesIterator.hasNext()) {
            categoriesParameter.append(categoriesIterator.next());
            if (categoriesIterator.hasNext()) {
                String combiner = " " + getString(R.string.logical_OR) + " ";
                categoriesParameter.append(combiner);
            }
        }

        // Parse the base URL string into an {@link Uri} object
        Uri baseUri = Uri.parse(GUARDIAN_API_URL);

        // Prepare the base URI so that we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append the query parameters and their values
        uriBuilder.appendQueryParameter("q", categoriesParameter.toString());
        uriBuilder.appendQueryParameter("order-by", orderBy);
        uriBuilder.appendQueryParameter("page-size", "10");
        uriBuilder.appendQueryParameter("page", String.valueOf(currentPage));
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        uriBuilder.appendQueryParameter("api-key", BuildConfig.API_KEY);

        return uriBuilder.toString();
    }

    @Override
    public void onLoadFinished(Loader<List<NewsArticle>> loader, List<NewsArticle> articlesList) {
        /* Hide the loading indicator once the data has been loaded and set the empty state text to
        display that no articles were found if it is used */
        loadingIndicator.setVisibility(View.GONE);
        emptyStateView.setText(R.string.no_articles);

        /* Store a temporary pointer to the articleList so that it is not lost when we clear the
        articleAdapter */
        List<NewsArticle> tempArticleList = (articlesList != null) ? new ArrayList<>(articlesList) : null;

        /* Store the current first visible list item index position and its offset from the top of the
        screen so that we can move the list back to the same exact position after new data has been
        loaded into the adapter */
        int firstItemIndex = articleListView.getFirstVisiblePosition();
        View firstItemView = articleListView.getChildAt(0);
        int topOffset = (firstItemView == null) ? 0 : (firstItemView.getTop() - articleListView.getPaddingTop());

        // Clear the adapter of any previous data and notify the {@link ListView}
        articleAdapter.clear();
        articleListView.requestLayout();

        /* If there is a valid list of {@link NewsArticle}s, then add them to the adapter's
        data set. Notify the {@link ListView} of this change again */
        if (tempArticleList != null && !tempArticleList.isEmpty()) {
            articleAdapter.addAll(tempArticleList);
            articleListView.requestLayout();
        }

        // Re-enable the {@link ListView} if it was previously disabled
        if (!articleListView.isEnabled()) {
            articleListView.setEnabled(true);
        }

        /* Restore the list scroll position unless the list has been refreshed, in which case it
        should go back to the top of the list */
        if (articleRefresh.isRefreshing()) {
            articleListView.setSelection(0);

            // Indicate that the refresh is complete
            articleRefresh.setRefreshing(false);
        } else {
            articleListView.setSelectionFromTop(firstItemIndex, topOffset);
        }

        // Indicate that the load has finished
        articlesLoading = false;
    }

    @Override
    public void onLoaderReset(Loader<List<NewsArticle>> loader) {
        articleAdapter.clear();
        articleListView.requestLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                /* Set the page parameter back to 1 and refresh the article list, disabling
                interactions with the list whilst the load is taking place */
                currentPage = 1;
                articleRefresh.setRefreshing(true);
                articleListView.setEnabled(false);
                updateArticles(true);
                return true;
            case R.id.open_settings:
                /* Set the default preference values the first time the user opens the settings
                 screen. Use true for the last parameter when testing to reset the defaults each
                 time the app is run and use false for the final build to keep the user's
                 preferences after first installation */
                PreferenceManager.setDefaultValues(this, R.xml.settings_main, false);

                // Open the Settings activity
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Save the current page number for the HTTP requests on an orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(getString(R.string.current_page_key), currentPage);
        super.onSaveInstanceState(outState);
    }
}
