package org.bolgarov.alexjr.shoppinglist.preferenceSubclasses;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.R;

public class CustomSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private TextView mTitleTextView, mSummaryTextView;
    private SeekBar mSeekbar;
    private EditText mSeekbarValueEditText;
    private TextView mSeekbarValueTextView; // TODO Remove

    private int mPercentage;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.custom_seekbar_preference);
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
        mSeekbar = (SeekBar) holder.findViewById(R.id.seek_bar);
        mSeekbarValueEditText = (EditText) holder.findViewById(R.id.seekbar_value_edit_text);

        mTitleTextView.setText(getTitle());
        mSummaryTextView.setText(getSummary());
        mSeekbar.setProgress(getPercentage());
        mSeekbarValueEditText.setText("" + getPercentage());
        mSeekbarValueEditText.addTextChangedListener(new TextWatcher() {
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
                    mSeekbar.setProgress(0);
                } else if (percentage > 100) {
                    mSeekbar.setProgress(100);
                } else {
                    mSeekbar.setProgress(percentage);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
        mSeekbarValueEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                mSeekbarValueEditText.setText("" + mPercentage);
            }
        });

        mSeekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setPercentage(progress);
        if (!mSeekbarValueEditText.hasFocus()) {
            mSeekbarValueEditText.setText("" + mPercentage);
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
        return a.getInt(index, 0);  // TODO: Set a default value instead of 0
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setPercentage(restorePersistedValue ? getPersistedInt(mPercentage) : (int) defaultValue);
    }
}
