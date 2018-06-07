package com.example.android.stemnews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;

public class CategoryMultiSelectPreference extends MultiSelectListPreference {

    private Context passedContext;
    private Set<String> newValues = new HashSet<>();
    private boolean preferenceChanged;

    public CategoryMultiSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.passedContext = context;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        /* Add the current preference values to the {@link HashSet} member variable when the
         dialog is first created */
        newValues.clear();
        newValues.addAll(getValues());
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Retrieve the dialog object and find the {@link ListView} in the dialog
        final AlertDialog multiSelectDialog = (AlertDialog) getDialog();
        final ListView categoryListView = multiSelectDialog.getListView();

        /* Set a click listener for the items in the list view that checks at least one is
         selected at all times. If the user deselects all options, make the OK button not clickable
         so that they cannot save this option. Re-enable it when the user then selects at least
         one item */
        categoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /* This click listener seems to prevent the inherited dialog multi-choice click
                 listener from working correctly, so I have moved that functionality to this
                 sub-class */
                CheckedTextView clickedItem = (CheckedTextView) parent.getChildAt(position);
                if (clickedItem.isChecked()) {
                    preferenceChanged |= newValues.add(getEntryValues()[position].toString());
                } else {
                    preferenceChanged |= newValues.remove(getEntryValues()[position].toString());
                }

                /* For all the items in the {@link ListView}, identify whether they are currently
                 checked or not and increase the checked count by one for each item that is */
                int numberChecked = 0;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    CheckedTextView currentListItem = (CheckedTextView) parent.getChildAt(i);
                    if (currentListItem.isChecked()) {
                        numberChecked++;
                    }
                }

                /* If no items are selected, disable the ok button so that the user cannot save
                 this option. If at least one item is selected, re-enable the ok button so that
                 they can */
                Button okButton = multiSelectDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (numberChecked == 0) {
                    okButton.setClickable(false);
                    okButton.setTextColor(ContextCompat.getColor(passedContext, android.R.color.darker_gray));
                } else {
                    okButton.setClickable(true);
                    okButton.setTextColor(ContextCompat.getColor(passedContext, R.color.colorAccent));
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        /* This code is a replica of the source code in the parent class, but it works with the
         specific member variables of this sub-class instead so that it can still record the
         preference change when using the new list item click listener */
        if (positiveResult && preferenceChanged) {
            final Set<String> values = newValues;
            if (callChangeListener(values)) {
                setValues(values);
            }
        }

        preferenceChanged = false;
    }
}