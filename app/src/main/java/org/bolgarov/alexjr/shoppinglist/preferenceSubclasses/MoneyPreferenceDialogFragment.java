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

package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.os.Bundle;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
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
