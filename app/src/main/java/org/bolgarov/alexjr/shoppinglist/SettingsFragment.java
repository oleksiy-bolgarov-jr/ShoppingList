package org.bolgarov.alexjr.shoppinglist;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.MoneyPreference;
import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.MoneyPreferenceDialogFragment;

public class SettingsFragment
        extends
        PreferenceFragmentCompat
        implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {
    private final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        setupPreference(sharedPreferences, preferenceScreen);
    }

    private void setupPreference(SharedPreferences sp, Preference p) {
        if (p == null) {
            // Base case 1, should never happen but just in case
            // Do nothing
        } else if (!(p instanceof PreferenceGroup)) {
            // Base case 2
            if (p instanceof MoneyPreference) {
                String value = sp.getString(p.getKey(), "");
                p.setSummary("$" + value);
                if (p.getKey().equals(getString(R.string.key_maximum_budget_edit_text))) {
                    boolean budgetEnabled = sp.getBoolean(
                            getString(R.string.key_set_maximum_budget_checkbox),
                            false
                    );
                    p.setEnabled(budgetEnabled);
                }
            }
        } else {
            // Recursive case
            PreferenceGroup group = (PreferenceGroup) p;
            int count = group.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                Preference child = group.getPreference(i);
                setupPreference(sp, child);
            }
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof MoneyPreference) {
            dialogFragment = MoneyPreferenceDialogFragment.newInstance(preference.getKey());
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference" +
                    ".PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            if (key.equals(getString(R.string.key_set_maximum_budget_checkbox))) {
                Preference budgetPreference =
                        findPreference(getString(R.string.key_maximum_budget_edit_text));
                boolean budgetEnabled = sharedPreferences.getBoolean(key, false);
                budgetPreference.setEnabled(budgetEnabled);
            } else if (key.equals(getString(R.string.key_maximum_budget_edit_text))) {
                String value = sharedPreferences.getString(key, "");
                preference.setSummary("$" + value);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String setBudgetCheckboxKey = getString(R.string.key_set_maximum_budget_checkbox);
        String budgetKey = getString(R.string.key_maximum_budget_edit_text);
        if (preference.getKey().equals(setBudgetCheckboxKey)) {
            try {
                boolean checkboxIsChecked = (Boolean) newValue;
                Log.d(TAG, "checkboxIsChecked is " + checkboxIsChecked);
                Preference budgetPreference = findPreference(budgetKey);
                budgetPreference.setEnabled(checkboxIsChecked);
            } catch (Exception e) { // TODO: Find out what exception is raised if boolean casting fails
                showToastMessage("An error occurred.");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void showToastMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
