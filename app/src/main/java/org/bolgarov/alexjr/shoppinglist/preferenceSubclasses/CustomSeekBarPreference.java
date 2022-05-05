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
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.R;

@SuppressWarnings({
        "unused",       // Actually used in pref_general.xml
        "WeakerAccess"  // Don't want to make constructors private in case it breaks the XML
})
public class CustomSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private final Context mContext;

    private TextView mTitleTextView, mSummaryTextView;
    private SeekBar mSeekBar;
    private EditText mSeekBarValueEditText;

    private int mPercentage;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                   int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        setLayoutResource(R.layout.preference_custom_seekbar);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekBarPreference(Context context) {
        this(context, null);
    }

    public int getPercentage() {
        return mPercentage;
    }

    public void setPercentage(int percentage) {
        mPercentage = percentage;
        persistInt(percentage);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mTitleTextView = (TextView) holder.findViewById(R.id.title_text_view);
        mSummaryTextView = (TextView) holder.findViewById(R.id.summary_text_view);
        mSeekBar = (SeekBar) holder.findViewById(R.id.seek_bar);
        mSeekBarValueEditText = (EditText) holder.findViewById(R.id.seekbar_value_edit_text);

        mTitleTextView.setText(getTitle());
        mSummaryTextView.setText(getSummary());
        mSeekBar.setProgress(mPercentage);
        String editTextValue = mContext.getString(
                R.string.pref_placeholder_seek_bar_edit_text, mPercentage);
        mSeekBarValueEditText.setText(editTextValue);
        mSeekBarValueEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int percentage;
                try {
                    percentage = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    percentage = 0;
                }
                // Protect against illegal values
                if (percentage < 0) {
                    mSeekBar.setProgress(0);
                } else if (percentage > 100) {
                    mSeekBar.setProgress(100);
                } else {
                    mSeekBar.setProgress(percentage);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
        mSeekBarValueEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                String localEditTextValue = mContext.getString(
                        R.string.pref_placeholder_seek_bar_edit_text, mPercentage);
                mSeekBarValueEditText.setText(localEditTextValue);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setPercentage(progress);
        if (!mSeekBarValueEditText.hasFocus()) {
            mSeekBarValueEditText.setText(mContext.getString(R.string.pref_placeholder_seek_bar_edit_text, mPercentage));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Not used
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Not used
    }

    @Override
    public void setTitle(int titleResId) {
        super.setTitle(titleResId);
        mTitleTextView.setText(titleResId);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mTitleTextView.setText(title);
    }

    @Override
    public void setSummary(int summaryResId) {
        super.setSummary(summaryResId);
        mSummaryTextView.setText(summaryResId);
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        mSummaryTextView.setText(summary);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setPercentage(restorePersistedValue ? getPersistedInt(mPercentage) : (int) defaultValue);
    }
}
