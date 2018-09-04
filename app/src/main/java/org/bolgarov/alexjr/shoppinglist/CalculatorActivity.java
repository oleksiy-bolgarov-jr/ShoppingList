package org.bolgarov.alexjr.shoppinglist;

import android.os.Bundle;
import android.support.constraint.Group;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;

public class CalculatorActivity extends AppCompatActivity {
    private static final String TAG = CalculatorActivity.class.getSimpleName();

    private static final BigDecimal POUNDS_PER_KILOGRAM = new BigDecimal("2.20462262185");
    private static final BigDecimal KILOGRAMS_PER_POUND = new BigDecimal("0.45359237");
    private static final BigDecimal OUNCES_PER_POUND = new BigDecimal(16);

    private RadioGroup mFromRadioGroup, mToRadioGroup;
    private int mFromWhichRadioButtonChecked, mToWhichRadioButtonChecked;
    private RadioButton mToUnitsRb, mToKgRb, mToLbRb, mToLitresRb;
    private EditText mTotalPriceEditText;
    private Group mIfFromUnitsSelected;
    private LinearLayout mIfFromKgSelected, mIfFromLbSelected, mIfFromLitresSelected;
    private TextView mQuantityOrWeightTitle;
    private ImageButton mDecreaseQuantityButton, mIncreaseQuantityButton;
    private TextView mQuantityTextView;
    private EditText mKilogramsEditText;
    private EditText mPoundsEditText, mOuncesEditText;
    private EditText mLitresEditText;
    private TextView mResultsTextView;
    private RecyclerView mSavedCalculationsRecyclerView;

    private int mQuantity;
    private BigDecimal mResultPrice;

    private SavedCalculationAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        mFromRadioGroup = findViewById(R.id.rg_calculator_from);
        mToRadioGroup = findViewById(R.id.rg_calculator_to);
        mFromWhichRadioButtonChecked = R.id.rb_from_units;
        mToWhichRadioButtonChecked = R.id.rb_to_units;
        mToUnitsRb = findViewById(R.id.rb_to_units);
        mToKgRb = findViewById(R.id.rb_to_kg);
        mToLbRb = findViewById(R.id.rb_to_lb);
        mToLitresRb = findViewById(R.id.rb_to_litres);
        mTotalPriceEditText = findViewById(R.id.et_calculator_price);
        mIfFromUnitsSelected = findViewById(R.id.if_from_units_selected);
        mIfFromKgSelected = findViewById(R.id.if_from_kg_selected);
        mIfFromLbSelected = findViewById(R.id.if_from_lb_selected);
        mIfFromLitresSelected = findViewById(R.id.if_from_litres_selected);
        mQuantityOrWeightTitle = findViewById(R.id.tv_calculator_quantity_or_weight_title);
        mDecreaseQuantityButton = findViewById(R.id.btn_calculator_decrease_quantity);
        mIncreaseQuantityButton = findViewById(R.id.btn_calculator_increase_quantity);
        mQuantityTextView = findViewById(R.id.tv_calculator_quantity);
        mKilogramsEditText = findViewById(R.id.et_calculator_kg);
        mPoundsEditText = findViewById(R.id.et_calculator_lb);
        mOuncesEditText = findViewById(R.id.et_calculator_oz);
        mLitresEditText = findViewById(R.id.et_calculator_litres);
        mResultsTextView = findViewById(R.id.tv_calculator_result);
        mSavedCalculationsRecyclerView = findViewById(R.id.rv_saved_calculations);

        mQuantity = 1;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mSavedCalculationsRecyclerView.setLayoutManager(layoutManager);
        mSavedCalculationsRecyclerView.setHasFixedSize(true);

        mAdapter = new SavedCalculationAdapter();
        mSavedCalculationsRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = findViewById(R.id.fab_save_calculation);
        fab.setOnClickListener(view -> {
            try {
                mAdapter.addCalculation(getCalculationString());
            } catch (IllegalStateException e) {
                if (TextUtils.isEmpty(mTotalPriceEditText.getText())) {
                    showToastMessage(R.string.empty_price_calculation_error);
                } else if (mFromWhichRadioButtonChecked == R.id.rb_from_kg ||
                        mFromWhichRadioButtonChecked == R.id.rb_from_lb) {
                    showToastMessage(R.string.empty_weight_calculation_error);
                } else if (mFromWhichRadioButtonChecked == R.id.rb_from_litres) {
                    showToastMessage(R.string.empty_litres_calculation_error);
                } else {
                    showToastMessage(R.string.generic_error);
                }
            }
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        setUpRadioButtons();
        perUnitSetup();
        perKgSetup();
        perLbSetup();
        perLitreSetup();
        mTotalPriceEditText.addTextChangedListener(new SimpleUpdateViewsTextWatcher());
        updateViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calculator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_clear_saved:
                mAdapter.clearCalculations();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpRadioButtons() {
        mFromRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            mFromWhichRadioButtonChecked = checkedId;
            mIfFromUnitsSelected.setVisibility(
                    checkedId == R.id.rb_from_units ?
                            View.VISIBLE :
                            View.GONE
            );
            mIfFromKgSelected.setVisibility(
                    checkedId == R.id.rb_from_kg ?
                            View.VISIBLE :
                            View.GONE
            );
            mIfFromLbSelected.setVisibility(
                    checkedId == R.id.rb_from_lb ?
                            View.VISIBLE :
                            View.GONE
            );
            mIfFromLitresSelected.setVisibility(
                    checkedId == R.id.rb_from_litres ?
                            View.VISIBLE :
                            View.GONE
            );
            mQuantityOrWeightTitle.setText(
                    checkedId == R.id.rb_from_units || checkedId == R.id.rb_from_litres ?
                            R.string.calculator_quantity_title :
                            R.string.calculator_weight_title
            );
            updateViews();
        });

        mToRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            mToWhichRadioButtonChecked = checkedId;
            updateViews();
        });
    }

    private void updateViews() {
        String quantityString = Integer.toString(mQuantity);
        mQuantityTextView.setText(quantityString);

        // Disable incompatible radio buttons. That is, if "from units" is checked, disable
        // all "to" radio buttons except "to units" and automatically check "to units". If
        // "from kg" or "from lb" is checked, disable "to units" and "to litres", and if one
        // of "to kg" or "to lb" is checked, leave that one checked, otherwise, check the one
        // that corresponds to which "from" is checked. If "from litres" is checked, disable
        // all "to" radio buttons except "to litres" and automatically check "to litres".
        switch (mFromWhichRadioButtonChecked) {
            case R.id.rb_from_units:
                mToUnitsRb.setEnabled(true);
                mToUnitsRb.setChecked(true);
                mToKgRb.setEnabled(false);
                mToKgRb.setChecked(false);
                mToLbRb.setEnabled(false);
                mToLbRb.setChecked(false);
                mToLitresRb.setEnabled(false);
                mToLitresRb.setChecked(false);
                mToWhichRadioButtonChecked = R.id.rb_to_units;
                break;
            case R.id.rb_from_kg:
            case R.id.rb_from_lb:
                if (mToUnitsRb.isChecked() || mToLitresRb.isChecked()) {
                    if (mFromWhichRadioButtonChecked == R.id.rb_from_kg) {
                        mToKgRb.setChecked(true);
                        mToWhichRadioButtonChecked = R.id.rb_to_kg;
                    } else {
                        mToLbRb.setChecked(true);
                        mToWhichRadioButtonChecked = R.id.rb_to_lb;
                    }
                }
                mToUnitsRb.setEnabled(false);
                mToUnitsRb.setChecked(false);
                mToKgRb.setEnabled(true);
                mToLbRb.setEnabled(true);
                mToLitresRb.setEnabled(false);
                mToLitresRb.setChecked(false);
                break;
            case R.id.rb_from_litres:
                mToUnitsRb.setEnabled(false);
                mToUnitsRb.setChecked(false);
                mToKgRb.setEnabled(false);
                mToKgRb.setChecked(false);
                mToLbRb.setEnabled(false);
                mToLbRb.setChecked(false);
                mToLitresRb.setEnabled(true);
                mToLitresRb.setChecked(true);
                mToWhichRadioButtonChecked = R.id.rb_to_litres;
                break;
        }

        if (!TextUtils.isEmpty(mTotalPriceEditText.getText())) {
            BigDecimal price = new BigDecimal(mTotalPriceEditText.getText().toString());
            BigDecimal result = null;
            String resultString;
            switch (mFromWhichRadioButtonChecked) {
                case R.id.rb_from_units:
                    result = price.divide(new BigDecimal(mQuantity), 2, RoundingMode.HALF_UP);
                    resultString = getString(R.string.calculator_result_to_units_format, result);
                    mResultsTextView.setText(resultString);
                    break;
                case R.id.rb_from_kg:
                    if (!TextUtils.isEmpty(mKilogramsEditText.getText())) {
                        BigDecimal kilograms =
                                new BigDecimal(mKilogramsEditText.getText().toString());
                        if (mToWhichRadioButtonChecked == R.id.rb_to_lb) {
                            BigDecimal pounds = kilograms.multiply(POUNDS_PER_KILOGRAM);
                            result = price.divide(pounds, 2, RoundingMode.HALF_UP);
                            resultString =
                                    getString(R.string.calculator_result_to_lb_format, result);
                        } else {
                            result = price.divide(kilograms, 2, RoundingMode.HALF_UP);
                            resultString =
                                    getString(R.string.calculator_result_to_kg_format, result);
                        }
                        mResultsTextView.setText(resultString);
                    } else {
                        mResultsTextView.setText(R.string.calculator_result_placeholder);
                    }
                    break;
                case R.id.rb_from_lb:
                    if (!TextUtils.isEmpty(mPoundsEditText.getText())) {
                        BigDecimal pounds = new BigDecimal(mPoundsEditText.getText().toString());
                        if (!TextUtils.isEmpty(mOuncesEditText.getText())) {
                            BigDecimal ounces =
                                    new BigDecimal(mOuncesEditText.getText().toString());
                            // Suppressed because there is no way to get a non-terminating expansion
                            // by dividing an integer by 16.
                            // If this produces an error, it means that some idiot modified the code
                            // so that there are no longer 16 ounces per pound.
                            //noinspection BigDecimalMethodWithoutRoundingCalled
                            pounds = pounds.add(ounces.divide(OUNCES_PER_POUND));
                        }
                        if (mToWhichRadioButtonChecked == R.id.rb_to_kg) {
                            BigDecimal kilograms = pounds.multiply(KILOGRAMS_PER_POUND);
                            result = price.divide(kilograms, 2, RoundingMode.HALF_UP);
                            resultString =
                                    getString(R.string.calculator_result_to_kg_format, result);
                        } else {
                            result = price.divide(pounds, 2, RoundingMode.HALF_UP);
                            resultString =
                                    getString(R.string.calculator_result_to_lb_format, result);
                        }
                        mResultsTextView.setText(resultString);
                    } else {
                        mResultsTextView.setText(R.string.calculator_result_placeholder);
                    }
                    break;
                case R.id.rb_from_litres:
                    if (!TextUtils.isEmpty(mLitresEditText.getText())) {
                        BigDecimal litres = new BigDecimal(mLitresEditText.getText().toString());
                        result = price.divide(litres, 2, RoundingMode.HALF_UP);
                        resultString =
                                getString(R.string.calculator_result_to_litres_format, result);
                        mResultsTextView.setText(resultString);
                    } else {
                        mResultsTextView.setText(R.string.calculator_result_placeholder);
                    }
                    break;
            }
            mResultPrice = result;
        } else {
            mResultsTextView.setText(R.string.calculator_result_placeholder);
        }
    }

    private void perUnitSetup() {
        mDecreaseQuantityButton.setOnClickListener(v -> {
            mQuantity--;
            mDecreaseQuantityButton.setEnabled(mQuantity > 1);
            updateViews();
        });
        mIncreaseQuantityButton.setOnClickListener(v -> {
            mQuantity++;
            mIncreaseQuantityButton.setEnabled(mQuantity > 1);
            updateViews();
        });
    }

    private void perKgSetup() {
        mKilogramsEditText.addTextChangedListener(new SimpleUpdateViewsTextWatcher());
    }

    private void perLbSetup() {
        mPoundsEditText.addTextChangedListener(new SimpleUpdateViewsTextWatcher());
        mOuncesEditText.addTextChangedListener(new SimpleUpdateViewsTextWatcher());
    }

    private void perLitreSetup() {
        mLitresEditText.addTextChangedListener(new SimpleUpdateViewsTextWatcher());
    }

    private String getCalculationString() {
        if (TextUtils.isEmpty(mTotalPriceEditText.getText())) {
            throw new IllegalStateException("mTotalPriceEditText must not be empty.");
        }
        BigDecimal price = new BigDecimal(mTotalPriceEditText.getText().toString());

        String result;

        DecimalFormat df =
                new DecimalFormat(getString(R.string.saved_calculation_decimal_format_weight));
        switch (mFromWhichRadioButtonChecked) {
            case R.id.rb_from_units:
                result = getString(
                        R.string.saved_calculation_to_units, price, mQuantity, mResultPrice);
                break;
            case R.id.rb_from_kg:
                if (TextUtils.isEmpty(mKilogramsEditText.getText())) {
                    throw new IllegalStateException(
                            "mKilogramsEditText must not be empty when rb_from_kg is selected.");
                }
                BigDecimal kilograms = new BigDecimal(mKilogramsEditText.getText().toString());
                String kilogramsString = df.format(kilograms) + getString(R.string.kg);
                if (mToWhichRadioButtonChecked == R.id.rb_to_kg) {
                    result = getString(
                            R.string.saved_calculation_to_kg, price, kilogramsString, mResultPrice);
                } else {
                    result = getString(
                            R.string.saved_calculation_to_lb, price, kilogramsString, mResultPrice);
                }
                break;
            case R.id.rb_from_lb:
                if (TextUtils.isEmpty(mPoundsEditText.getText())) {
                    throw new IllegalStateException(
                            "mPoundsEditText must not be empty when rb_from_lb is selected.");
                }
                BigDecimal pounds = new BigDecimal(mPoundsEditText.getText().toString());
                String poundsString = df.format(pounds) + getString(R.string.lb);
                if (!TextUtils.isEmpty(mOuncesEditText.getText())) {
                    int ounces = Integer.parseInt(mOuncesEditText.getText().toString());
                    poundsString += " " + ounces + getString(R.string.oz);
                }
                if (mToWhichRadioButtonChecked == R.id.rb_to_lb) {
                    result = getString(
                            R.string.saved_calculation_to_lb, price, poundsString, mResultPrice);
                } else {
                    result = getString(
                            R.string.saved_calculation_to_kg, price, poundsString, mResultPrice);
                }
                break;
            case R.id.rb_from_litres:
                if (TextUtils.isEmpty(mLitresEditText.getText())) {
                    throw new IllegalStateException(
                            "mLitresEditText must not be empty when rb_from_litres is selected.");
                }
                BigDecimal litres = new BigDecimal(mLitresEditText.getText().toString());
                String litresString = df.format(litres) + getString(R.string.litres);
                result = getString(
                        R.string.saved_calculation_to_litres, price, litresString, mResultPrice);
                break;
            default:
                result = getString(R.string.generic_error);
                break;
        }
        return result;
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToastMessage(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private class SimpleUpdateViewsTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateViews();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
