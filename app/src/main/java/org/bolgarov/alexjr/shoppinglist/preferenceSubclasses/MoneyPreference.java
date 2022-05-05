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

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

import org.bolgarov.alexjr.shoppinglist.R;

@SuppressWarnings({
        "unused",       // Actually used in pref_general.xml
        "WeakerAccess"  // Don't want to make constructors private in case it breaks the XML
})
public class MoneyPreference extends DialogPreference {

    private String mMoney;

    public MoneyPreference(Context context) {
        this(context, null);
    }

    public MoneyPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public MoneyPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public MoneyPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.preference_dialog_price);
        setNegativeButtonText(R.string.money_pref_dialog_negative);
        setPositiveButtonText(R.string.money_pref_dialog_positive);
    }

    public String getMoney() {
        return mMoney;
    }

    public void setMoney(String money) {
        mMoney = money;
        persistString(money);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setMoney(restorePersistedValue ? getPersistedString(mMoney) : (String) defaultValue);
    }
}
