/*
 * Copyright (c) 2018 Oleksiy Bolgarov.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.bolgarov.alexjr.shoppinglist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.MoneyPreference;
import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.MoneyPreferenceDialogFragment;
import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.PercentPreference;
import org.bolgarov.alexjr.shoppinglist.preferenceSubclasses.PercentPreferenceDialogFragment;

import java.math.BigDecimal;
import java.util.Objects;

public class SettingsFragment
        extends
        PreferenceFragmentCompat
        implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {
    private static final String ROOT_KEY = "root_key";
    @SuppressWarnings("unused")
    private final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //addPreferencesFromResource(R.xml.pref_general);
        if (getArguments() != null) {
            String key = getArguments().getString(ROOT_KEY);
            setPreferencesFromResource(R.xml.pref_general, key);
        } else {
            setPreferencesFromResource(R.xml.pref_general, rootKey);
        }

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        setupPreference(sharedPreferences, preferenceScreen);

        findPreference(getString(R.string.key_maximum_budget_edit_text))
                .setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.key_warning_fixed_edit_text))
                .setOnPreferenceChangeListener(this);
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    private void setupPreference(SharedPreferences sp, Preference p) {
        if (p != null) {
            if (!(p instanceof PreferenceGroup)) { // Base case 1
                if (p instanceof CheckBoxPreference) {
                    // Enable and disable preferences and preference groups based on the
                    // corresponding checkboxes
                    String key = p.getKey();
                    if (key.equals(getString(R.string.key_set_maximum_budget_checkbox))) {
                        findPreference(getString(R.string.key_budget_subscreen))
                                .setEnabled(((CheckBoxPreference) p).isChecked());
                    } else if (key.equals(getString(R.string.key_include_tax_checkbox))) {
                        findPreference(getString(R.string.key_tax_rate_edit_text))
                                .setEnabled(((CheckBoxPreference) p).isChecked());
                    }
                } else if (p instanceof MoneyPreference) {
                    String value = sp.getString(p.getKey(), "");
                    p.setSummary("$" + value);
                } else if (p instanceof ListPreference) {
                    String key = p.getKey();
                    ListPreference lp = (ListPreference) p;
                    // For all ListPreferences, set the summary
                    String value = sp.getString(key, "");
                    int index = lp.findIndexOfValue(value);
                    if (index >= 0) {
                        lp.setSummary(lp.getEntries()[index]);
                    }
                    // Actions specific to each ListPreference
                    if (key.equals(getString(R.string.key_warning_type_list))) {
                        Preference fixedPriceWarn =
                                findPreference(getString(R.string.key_warning_fixed_edit_text));
                        Preference percentageWarn =
                                findPreference(getString(R.string.key_warning_percentage_seek_bar));
                        if (value.equals(getString(R.string.pref_value_warn_fixed_price))) {
                            fixedPriceWarn.setVisible(true);
                            percentageWarn.setVisible(false);
                        } else {
                            fixedPriceWarn.setVisible(false);
                            percentageWarn.setVisible(true);
                        }
                    }
                } else if (p instanceof PercentPreference) {
                    String value = sp.getString(p.getKey(), "");
                    p.setSummary(value + "%");
                }
            } else if (p.getKey() != null &&
                    p.getKey().equals(getString(R.string.key_autocomplete_dictionary_intent))) {
                // Base case 2, where the preference is the autocomplete dictionary preference
                Intent intent = new Intent(getContext(), AutocompleteDictionaryActivity.class);
                p.setIntent(intent);
            } else {    // Recursive case
                PreferenceGroup group = (PreferenceGroup) p;
                int count = group.getPreferenceCount();
                for (int i = 0; i < count; i++) {
                    Preference child = group.getPreference(i);
                    setupPreference(sp, child);
                }
            }
        }
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ROOT_KEY, preferenceScreen.getKey());
        fragment.setArguments(args);
        FragmentManager manager = getFragmentManager();
        assert manager != null;
        manager.beginTransaction()
                .replace(R.id.activity_settings, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof MoneyPreference) {
            dialogFragment = MoneyPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof PercentPreference) {
            dialogFragment = PercentPreferenceDialogFragment.newInstance(preference.getKey());
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(Objects.requireNonNull(this.getFragmentManager()),
                    "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        setupPreference(sharedPreferences, preference);
    }

    private void showToastMessage(int messageResId) {
        Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.key_maximum_budget_edit_text))) {
            if (TextUtils.isEmpty((CharSequence) newValue)) {
                return false;
            }
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            BigDecimal budget = new BigDecimal((String) newValue);
            BigDecimal warningValue =
                    new BigDecimal(
                            sp.getString(getString(R.string.key_warning_fixed_edit_text), "0"));
            if (warningValue.compareTo(budget) > 0) {
                boolean warningSet =
                        sp.getBoolean(getString(R.string.key_set_warning_checkbox), false);
                String warningType = sp.getString(getString(R.string.key_warning_type_list),
                        getString(R.string.pref_value_warn_fixed_price));

                if (!warningSet ||
                        warningType.equals(getString(R.string.pref_value_warn_percentage))) {
                    return true;    // No point trying to enforce anything if fixed warning value
                    // not shown or desired
                    // I know this means that the user will be able to exploit this to set warning
                    // higher than budget, but there is no reason to do that anyway, and it won't
                    // break the app
                }
                showToastMessage(R.string.settings_fragment_toast_invalid_budget);
                return false;
            }
        } else if (preference.getKey().equals(getString(R.string.key_warning_fixed_edit_text))) {
            if (TextUtils.isEmpty((CharSequence) newValue)) {
                return false;
            }
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            BigDecimal budget =
                    new BigDecimal(
                            sp.getString(getString(R.string.key_maximum_budget_edit_text), "0"));
            BigDecimal warningValue = new BigDecimal((String) newValue);
            if (warningValue.compareTo(budget) > 0) {
                showToastMessage(R.string.settings_fragment_toast_invalid_warning);
                return false;
            }
        }
        return true;
    }
}
