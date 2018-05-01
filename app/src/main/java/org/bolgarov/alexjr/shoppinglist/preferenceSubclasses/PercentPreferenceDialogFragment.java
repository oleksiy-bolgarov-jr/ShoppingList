package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;

import org.bolgarov.alexjr.shoppinglist.R;

public class PercentPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    private EditText mEditText;

    public static PercentPreferenceDialogFragment newInstance(String key) {
        final PercentPreferenceDialogFragment fragment = new PercentPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditText = view.findViewById(R.id.edit);
        if (mEditText == null) {
            throw new IllegalStateException("View must contain an EditText with ID 'edit'");
        }

        DialogPreference preference = getPreference();
        String percentString = null;
        if (preference instanceof PercentPreference) {
            percentString = ((PercentPreference) preference).getPercentage();
        }

        if (percentString != null) {
            mEditText.setText(percentString);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String percentage = mEditText.getText().toString();
            DialogPreference preference = getPreference();
            if (preference instanceof PercentPreference) {
                PercentPreference percentPreference = (PercentPreference) preference;
                if (percentPreference.callChangeListener(percentage)) {
                    percentPreference.setPercentage(percentage);
                }
            }
        }
    }
}
