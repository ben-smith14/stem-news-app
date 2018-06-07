package com.example.android.stemnews;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add up navigation arrow to the action bar
        ActionBar settingsActionBar = getActionBar();
        if (settingsActionBar != null) {
            settingsActionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content in the activity
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Respond to the action bar's Up/Home button
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

            /* When the settings activity is first opened, we want to display the summary text
             for each preference underneath its title and set up its change listeners. Therefore,
             we need to retrieve each preference and call the bindPreferenceSummaryToValue method
             for it */
            Preference orderBy = findPreference(getString(R.string.settings_order_by_key));
            bindPreferenceSummaryToValue(orderBy);

            Preference searchCategories = findPreference(getString(R.string.settings_search_categories_key));
            bindPreferenceSummaryToValue(searchCategories);
        }

        private void bindPreferenceSummaryToValue(Preference currentPreference) {
            /* Set the preference change listener for the current preference to this fragment and
             retrieve the shared preferences for this app */
            currentPreference.setOnPreferenceChangeListener(this);
            SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(currentPreference.getContext());

            /* If the current preference is an instance of {@link CategoryMultiSelectPreference}, retrieve
             the String {@link Set} and pass it to the onPreferenceChange call-back. Otherwise,
             just retrieve the single String and do the same */
            if (currentPreference instanceof CategoryMultiSelectPreference) {
                Set<String> preferenceValue = appPreferences.getStringSet(currentPreference.getKey(), new HashSet<String>());
                onPreferenceChange(currentPreference, preferenceValue);
            } else {
                String preferenceValue = appPreferences.getString(currentPreference.getKey(), "");
                onPreferenceChange(currentPreference, preferenceValue);
            }
        }

        /* The code in this method takes care of updating the displayed preference summary
         after it has been changed */
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String preferenceValue = value.toString();

            /* If the preference was an instance of the custom {@link CategoryMultiSelectPreference} class,
             retrieve the set of values and output them as one String in the summary */
            if (preference instanceof CategoryMultiSelectPreference) {
                /* Remove the square braces from the toString representation of the values and
                 split them into the individual Strings */
                preferenceValue = preferenceValue.replace("[", "");
                preferenceValue = preferenceValue.replace("]", "");
                String[] separatedValues = preferenceValue.split(", ");

                /* Get the labels for all the values in the multi-select preference and prepare a
                 {@link StringBuilder} to combine the ones that were selected */
                CategoryMultiSelectPreference multiSelect = (CategoryMultiSelectPreference) preference;
                CharSequence[] labels = multiSelect.getEntries();
                StringBuilder summaryString = new StringBuilder();

                /* For every value that was selected in the preference, find its label in the
                 String array and append this to the output String */
                for (int i = 0; i < separatedValues.length; i++) {
                    int prefIndex = multiSelect.findIndexOfValue(separatedValues[i]);
                    if (prefIndex >= 0) {
                        summaryString.append(labels[prefIndex]);
                    }

                    // If the current label is not the last, add a comma separator as well
                    if (i < separatedValues.length - 1) {
                        summaryString.append(", ");
                    }
                }

                preference.setSummary(summaryString);
            } else if (preference instanceof ListPreference) {
                /* If the preference is an instance of {@link ListPreference}, find the index of its
                 selected value in the list of options, retrieve that value's label and set this as
                 the summary text */
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(preferenceValue);
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    preference.setSummary(labels[prefIndex]);
                }
            } else {
                // Otherwise, set the actual value itself as the summary text
                preference.setSummary(preferenceValue);
            }

            return true;
        }
    }
}