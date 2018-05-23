package com.example.android.stemnews;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving news data from The Guardian web API.
 */
public final class QueryUtils {

    private static final String LOG_TAG = QueryUtils.class.getName();
    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int OK_RESPONSE = 200;

    private QueryUtils() { // Cannot instantiate this class
    }

    /**
     * Use a URL in String form to make a HTTP request, parse the JSON response and then create
     * a list of {@link NewsArticle} objects.
     *
     * @param requestUrl is the HTTP request URL in String form.
     * @param appContext is the {@link Context} used to access application resources.
     * @return a list of news articles to display to the user.
     */
    public static List<NewsArticle> fetchLatestNews(String requestUrl, Context appContext) {
        URL newsUrl = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(newsUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Issue making the HTTP request", e);
        }

        // Extract relevant fields from the JSON response and return a list of {@link NewsArticle}s
        return extractFeatureFromJson(jsonResponse, appContext);
    }

    /**
     * Convert a URL in String form to an {@link URL} object.
     *
     * @param stringUrl is the URL in String form.
     * @return is the converted {@link URL} object.
     */
    private static URL createUrl(String stringUrl) {
        URL convertedUrl = null;

        try {
            convertedUrl = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Issue building the URL.", e);
        }

        return convertedUrl;
    }

    /**
     * Use the {@link URL} object to make the HTTP request and retrieve the JSON response.
     *
     * @param inputUrl is the the {@link URL} object.
     * @return the JSON response as a single String.
     */
    private static String makeHttpRequest(URL inputUrl) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early
        if (inputUrl == null) {
            return jsonResponse;
        }

        // Otherwise, proceed with creating the connection for the HTTP request
        HttpURLConnection serverConnection = null;
        InputStream responseStream = null;

        try {
            serverConnection = (HttpURLConnection) inputUrl.openConnection();

            /*
            Set the time limits in milliseconds for starting to read data and for establishing
            the connection
            */
            serverConnection.setReadTimeout(READ_TIMEOUT);
            serverConnection.setConnectTimeout(CONNECT_TIMEOUT);

            // Indicate that we want to receive data and then make the connection
            serverConnection.setRequestMethod("GET");
            serverConnection.connect();

            /*
            Get the input stream and then parse it into the JSON response String, checking for
            the successful response code
            */
            if (serverConnection.getResponseCode() == OK_RESPONSE) {
                responseStream = serverConnection.getInputStream();
                jsonResponse = readFromStream(responseStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + serverConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Issue retrieving the JSON results.", e);
        } finally {
            /*
            Once the date is loaded, close the connection and input stream. Closing the input
            stream could throw an IOException, which is indicated by the method signature
            */
            if (serverConnection != null) {
                serverConnection.disconnect();
            }

            if (responseStream != null) {
                responseStream.close();
            }
        }

        return jsonResponse;
    }

    /**
     * Convert the response input stream from the server into a single JSON response String.
     *
     * @param responseStream is the input stream response from the HTTP request to the servers.
     * @return the JSON response as a single String.
     */
    private static String readFromStream(InputStream responseStream) throws IOException {
        StringBuilder outputString = new StringBuilder();

        if (responseStream != null) {
            // Convert the raw input data into characters and then into whole Strings
            InputStreamReader characterStream = new InputStreamReader(responseStream, Charset.forName("UTF-8"));
            BufferedReader stringStream = new BufferedReader(characterStream);

            /*
            Read each of the Strings in the {@link BufferedReader} object and concatenate them
            all together using the {@link StringBuilder} object
            */
            String currentLine = stringStream.readLine();
            while (currentLine != null) {
                outputString.append(currentLine);
                currentLine = stringStream.readLine();
            }
        }

        return outputString.toString();
    }

    /**
     * Extract the news article data from the JSON response String and use it to create a list of
     * {@link NewsArticle} objects to display to the user.
     *
     * @param newsArticleJson is the JSON response as a single String.
     * @param appContext      is the {@link Context} used to access application resources.
     * @return the list of {@link NewsArticle} objects to be displayed in the app.
     */
    private static List<NewsArticle> extractFeatureFromJson(String newsArticleJson, Context appContext) {
        // If the JSON string is empty or null, then return early
        if (TextUtils.isEmpty(newsArticleJson)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding articles to
        List<NewsArticle> articleList = new ArrayList<>();

        try {
            /*
            Extract the root {@link JSONObject} from the JSON response String and then obtain
            the "status" String to find out the status of the response
            */
            JSONObject rootJsonObject = new JSONObject(newsArticleJson).getJSONObject(appContext.getString(R.string.response_key));
            String responseStatus = rootJsonObject.getString(appContext.getString(R.string.status_key));

            /*
            If the status String is "ok", try to parse the rest of the JSON response to get
            the list of news articles. If there's a problem with the way the JSON is formatted,
            a JSONException object will be thrown
            */
            if (responseStatus.equals(appContext.getString(android.R.string.ok).toLowerCase())) {
                /*
                Extract the JSONArray associated with the key called "results", which represents
                the list of article objects
                */
                JSONArray articleArray = rootJsonObject.getJSONArray(appContext.getString(R.string.results_key));

                // For each article in the articleArray, create a new {@link NewsArticle} object
                for (int i = 0; i < articleArray.length(); i++) {
                    /*
                    Get a single article object at the current index position within the list of
                    news articles
                    */
                    JSONObject currentArticle = articleArray.getJSONObject(i);

                    // Extract the article title String
                    String articleTitle = currentArticle.getString(appContext.getString(R.string.webTitle_key));

                    // Extract the section name String
                    String sectionName = currentArticle.getString(appContext.getString(R.string.sectionName_key));

                    // Extract the first author's name in the list of authors if any are given. If
                    // there are multiple authors, indicate this with an ampersand and ellipses
                    JSONArray authorsArray = currentArticle.getJSONArray(appContext.getString(R.string.tags_key));
                    String authorString = null;
                    if (authorsArray.length() > 0) {
                        JSONObject author = authorsArray.getJSONObject(0);
                        authorString = author.getString(appContext.getString(R.string.webTitle_key));

                        if (authorsArray.length() > 1) {
                            authorString += appContext.getString(R.string.multiple_authors);
                        }
                    }

                    // Extract the publication date String
                    String datePublished = currentArticle.getString(appContext.getString(R.string.webPublicationDate_key));

                    // Extract the web URL String
                    String articleUrl = currentArticle.getString(appContext.getString(R.string.webUrl_key));

                    // Create a new {@link NewsArticle} object with the outputs from the parsing
                    NewsArticle article = new NewsArticle(articleTitle, sectionName, authorString, datePublished, articleUrl);

                    // Add the new {@link NewsArticle} to the list of articles
                    articleList.add(article);
                }
            } else {
                Log.e(LOG_TAG, responseStatus + ": " + rootJsonObject.getString(appContext.getString(R.string.message_key)));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the JSON results", e);
        }

        // Return the list of articles
        return articleList;
    }
}