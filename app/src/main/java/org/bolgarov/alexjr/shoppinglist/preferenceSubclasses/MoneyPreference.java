package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import org.bolgarov.alexjr.shoppinglist.R;

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
        setDialogLayoutResource(R.layout.price_dialog_preference);
        setNegativeButtonText(R.string.cancel);
        setPositiveButtonText(R.string.confirm);
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
