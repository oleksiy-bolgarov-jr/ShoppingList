package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import org.bolgarov.alexjr.shoppinglist.R;

public class PercentPreference extends DialogPreference {
    private String mPercentage;

    public PercentPreference(Context context) {
        this(context, null);
    }

    public PercentPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public PercentPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public PercentPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.percentage_dialog_preference);
        setNegativeButtonText(R.string.cancel);
        setPositiveButtonText(R.string.confirm);
    }

    public String getPercentage() {
        return mPercentage;
    }

    public void setPercentage(String percentage) {
        mPercentage = percentage;
        persistString(percentage);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setPercentage(restorePersistedValue ?
                getPersistedString(mPercentage) :
                (String) defaultValue);
    }
}
