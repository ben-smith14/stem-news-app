package com.example.android.stemnews;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticleAdapter extends ArrayAdapter<NewsArticle> {

    private final String LOG_TAG = ArticleAdapter.class.getName();

    private List<NewsArticle> adapterItems;

    ArticleAdapter(@NonNull Context context, @NonNull List<NewsArticle> objects) {
        super(context, 0, objects);
        this.adapterItems = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder articleViewHolder;

        /*
        If no recycled view is available, inflate a new list item from the layout file and
        obtain references to all the views in the list item so that they can be stored in the
        {@link ViewHolder}
        */
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.article_list_item, parent, false);
            articleViewHolder = new ViewHolder();
            articleViewHolder.titleTextView = convertView.findViewById(R.id.article_title);
            articleViewHolder.sectionName = convertView.findViewById(R.id.section_name);
            articleViewHolder.authorText = convertView.findViewById(R.id.article_author);
            articleViewHolder.separator = convertView.findViewById(R.id.seperator);
            articleViewHolder.dateTextView = convertView.findViewById(R.id.date);
            convertView.setTag(articleViewHolder);
        } else {
            articleViewHolder = (ViewHolder) convertView.getTag();
        }

        NewsArticle currentArticle = getItem(position);

        if (currentArticle != null) {
            /*
            Remove any additional text in the article title response by using the | character
            as a separator, then display it in the correct {@link TextView}
            */
            String titleText = currentArticle.getArticleTitle();
            if (titleText.contains("|")) {
                int endOfTitle = titleText.indexOf("|");
                titleText = titleText.substring(0, endOfTitle);
            }
            articleViewHolder.titleTextView.setText(titleText);

            // Set the text of the news section {@link TextView}
            articleViewHolder.sectionName.setText(currentArticle.getNewsSection());

            /*
            Retrieve the width dimension of the screen for use in limiting the size of the
            section name and author name {@link TextView}s
            */
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;

            /*
            If an author name was given, set the maximum width of both its {@link TextView} and
            that of the section name so that each can only take up half the screen width. If it
            was not given, simply hide the separator and the author's name {@link TextView}
            */
            String authorName = currentArticle.getAuthorName();
            if (authorName != null) {
                articleViewHolder.sectionName.setMaxWidth(screenWidth / 2);

                articleViewHolder.authorText.setMaxWidth(screenWidth / 2);
                articleViewHolder.authorText.setText(authorName);
                articleViewHolder.authorText.setVisibility(View.VISIBLE);

                articleViewHolder.separator.setVisibility(View.VISIBLE);
            } else {
                articleViewHolder.authorText.setVisibility(View.GONE);
                articleViewHolder.separator.setVisibility(View.GONE);
            }

            /*
            The response publication date is in the ISO 8601 format, which cannot be directly
            converted to a {@link Date} object using the {@link SimpleDateFormat} class. Therefore,
            the values in the String need to be rearranged and then it can be parsed
            */
            String ISO8601Date = currentArticle.getDatePublished();
            Date datePublished = null;
            try {
                String parsableDate = convertDateForParse(ISO8601Date);
                DateFormat ISO8601Format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                datePublished = ISO8601Format.parse(parsableDate);
            } catch (ParseException e) {
                Log.e(LOG_TAG, "Issue parsing the date", e);
            }

            /*
            If the date was successfully parsed, convert it into the desired format for displaying
            in the list item and link it to the appropriate {@link TextView}. Otherwise, remove
            the view from the current list item
            */
            if (datePublished != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                String dateString = displayFormat.format(datePublished);
                articleViewHolder.dateTextView.setText(dateString);
                articleViewHolder.dateTextView.setVisibility(View.VISIBLE);
            } else {
                articleViewHolder.dateTextView.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    // Object for holding view references when recycling list items
    private static class ViewHolder {
        private TextView titleTextView;
        private TextView sectionName;
        private TextView authorText;
        private View separator;
        private TextView dateTextView;
    }

    /**
     * A method to convert a date String in ISO 8601 format (which cannot be parsed by the
     * {@link SimpleDateFormat} class) to a format that can be parsed into a {@link Date} object
     *
     * @param ISO8601Date is a String that gives a UTC date and time in the ISO 8601 format,
     *                    for example: 2018-05-17T13:21:54Z is 1:21 pm (and 54secs) on 17th May
     *                    2018, GMT +00:00
     * @return the ISO 8601 date String in a form that can be parsed by the {@link SimpleDateFormat}
     * class
     */
    private String convertDateForParse(String ISO8601Date) {
        // Remove the UTC indicator (the Z character) at the end of the string
        int UTCIndicator = ISO8601Date.indexOf("Z");
        String removedUTCIndicator = ISO8601Date.substring(0, UTCIndicator);

        // Split the date and time strings, then split the date string up into year, month and day
        String[] splitDateTime = removedUTCIndicator.split("T");
        String[] splitDate = splitDateTime[0].split("-");

        // Swap the positions of the year and day in the string array
        String tempYearHolder = splitDate[0];
        splitDate[0] = splitDate[2];
        splitDate[2] = tempYearHolder;

        // Rebuild the date by adding hyphens between the elements
        StringBuilder parsableDate = new StringBuilder();
        for (int i = 0; i < splitDate.length; i++) {
            parsableDate.append(splitDate[i]);

            if (i < splitDate.length - 1) {
                parsableDate.append("-");
            }
        }

        // Return the newly parsable date and time as a single String
        return parsableDate + " " + splitDateTime[1];
    }

    public List<NewsArticle> getAdapterItems() {
        return adapterItems;
    }
}
