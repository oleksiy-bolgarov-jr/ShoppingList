package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;

import org.bolgarov.alexjr.shoppinglist.R;

public class MoneyPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private EditText mEditText;

    public static MoneyPreferenceDialogFragment newInstance(String key) {
        final MoneyPreferenceDialogFragment fragment = new MoneyPreferenceDialogFragment();
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
        String moneyString = null;
        if (preference instanceof MoneyPreference) {
            moneyString = ((MoneyPreference) preference).getMoney();
        }

        if (moneyString != null) {
            mEditText.setText(moneyString);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String money = mEditText.getText().toString();
            DialogPreference preference = getPreference();
            if (preference instanceof MoneyPreference) {
                MoneyPreference moneyPreference = (MoneyPreference) preference;
                if (moneyPreference.callChangeListener(money)) {
                    moneyPreference.setMoney(money);
                }
            }
        }
    }
}
