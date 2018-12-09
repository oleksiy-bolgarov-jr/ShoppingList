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

package org.bolgarov.alexjr.shoppinglist.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;
import org.bolgarov.alexjr.shoppinglist.ShoppingListAdapter;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Objects;

public class OnCheckedSingleItemClickDialogFragment extends DialogFragment {

    private static final String TAG = OnCheckedSingleItemClickDialogFragment.class.getSimpleName();

    private static final BigDecimal OUNCES_PER_POUND = new BigDecimal(16);
    private static final BigDecimal POUNDS_PER_KILOGRAM = new BigDecimal("2.20462262185");
    private static final BigDecimal KILOGRAMS_PER_POUND = new BigDecimal("0.45359237");

    private static WeakReference<FragmentActivity> activityRef;

    private SingleShoppingListItem mItem;
    private ShoppingListAdapter mAdapter;
    private boolean mBudgetIsSet;
    private BigDecimal mBudget;

    private int mCurrentQuantity;
    private boolean mWeightWasChanged;

    private int mWhichRadioButtonChecked;
    private TextView mPriceTitleTextView;
    private LinearLayout mIfPerUnitSelectedLayout, mIfPerKgSelectedLayout, mIfPerLbSelectedLayout;
    private EditText mPriceEditText;
    private ImageButton mDecreaseQuantityButton, mIncreaseQuantityButton;
    private TextView mQuantityTextView;
    private EditText mKilogramsEditText;
    private EditText mPoundsEditText, mOuncesEditText;
    private TextView mPriceNoTaxTextView;
    private TextView mTaxTextView;
    private TextView mTotalPriceTextView;

    private Button mPositiveButton;

    /**
     * Creates a dialog warning the user that he is over budget.
     */
    private static void warnOverBudget() {
        AlertDialog.Builder warningDialogBuilder =
                new AlertDialog.Builder(Objects.requireNonNull(activityRef.get()));
        warningDialogBuilder.setMessage(R.string.over_budget_dialog_body)
                .setPositiveButton(
                        R.string.over_budget_dialog_dismiss_button,
                        (dialog, which) -> dialog.dismiss()
                );
        warningDialogBuilder.create().show();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            mPositiveButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
        }
        activityRef = new WeakReference<>(getActivity());
        updateDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCurrentQuantity = mItem.getQuantity();
        mWeightWasChanged = false;

        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_buying_item, null);

        RadioGroup pricingRadioGroup = view.findViewById(R.id.radio_group_pricing);
        RadioButton perUnitRadioButton = view.findViewById(R.id.rb_per_unit);
        RadioButton perKgRadioButton = view.findViewById(R.id.rb_per_kg);
        RadioButton perLbRadioButton = view.findViewById(R.id.rb_per_pound);
        mPriceTitleTextView = view.findViewById(R.id.tv_price_title);
        mIfPerUnitSelectedLayout = view.findViewById(R.id.ll_if_per_unit_selected);
        mIfPerKgSelectedLayout = view.findViewById(R.id.ll_if_per_kg_selected);
        mIfPerLbSelectedLayout = view.findViewById(R.id.ll_if_per_lb_selected);
        mPriceEditText = view.findViewById(R.id.et_price);
        mDecreaseQuantityButton = view.findViewById(R.id.btn_decrease_quantity);
        mIncreaseQuantityButton = view.findViewById(R.id.btn_increase_quantity);
        mQuantityTextView = view.findViewById(R.id.tv_quantity);
        mKilogramsEditText = view.findViewById(R.id.et_kg);
        mPoundsEditText = view.findViewById(R.id.et_lb);
        mOuncesEditText = view.findViewById(R.id.et_oz);
        mPriceNoTaxTextView = view.findViewById(R.id.tv_total_item_price_no_tax);
        mTaxTextView = view.findViewById(R.id.tv_tax);
        mTotalPriceTextView = view.findViewById(R.id.tv_total_item_price);

        builder.setTitle(mItem.getName())
                .setView(view)
                .setPositiveButton(
                        R.string.item_dialog_checked_positive,
                        (dialog, which) -> onPositive()
                )
                .setNegativeButton(
                        R.string.item_dialog_checked_negative,
                        (dialog, which) -> dialog.dismiss()
                )
                .setNeutralButton(
                        R.string.item_dialog_checked_neutral,
                        (dialog, which) -> onNeutral()
                );
        Dialog dialog = builder.create();

        if (mItem.isPerUnitOrPerWeight() == SingleShoppingListItem.PER_UNIT) {
            mWhichRadioButtonChecked = R.id.rb_per_unit;
            perKgRadioButton.setEnabled(false);
            perLbRadioButton.setEnabled(false);
            perUnitSetup();
        } else {
            mIfPerUnitSelectedLayout.setVisibility(View.GONE);
            mIfPerKgSelectedLayout.setVisibility(View.VISIBLE);

            perUnitRadioButton.setEnabled(false);
            perUnitRadioButton.setChecked(false);
            perKgRadioButton.setChecked(true);
            mWhichRadioButtonChecked = R.id.rb_per_kg;
            perKgSetup();
            perLbSetup();
        }

        pricingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            mWhichRadioButtonChecked = checkedId;
            switch (checkedId) {
                case R.id.rb_per_unit:
                    mIfPerUnitSelectedLayout.setVisibility(View.VISIBLE);
                    mIfPerKgSelectedLayout.setVisibility(View.GONE);
                    mIfPerLbSelectedLayout.setVisibility(View.GONE);
                    mPriceTitleTextView.setText(R.string.price_per_unit);
                    break;
                case R.id.rb_per_kg:
                    mIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    mIfPerKgSelectedLayout.setVisibility(View.VISIBLE);
                    mIfPerLbSelectedLayout.setVisibility(View.GONE);
                    mPriceTitleTextView.setText(R.string.price_per_kg);
                    if (!TextUtils.isEmpty(mPriceEditText.getText())) {
                        BigDecimal pricePerPound =
                                new BigDecimal(addLeadingZero(mPriceEditText.getText().toString()));
                        String prefill = getString(R.string.item_dialog_placeholder_price_edit_text,
                                pricePerPound.multiply(POUNDS_PER_KILOGRAM));
                        mPriceEditText.setText(prefill);
                    }
                    if (!TextUtils.isEmpty(mPoundsEditText.getText()) && mWeightWasChanged) {
                        BigDecimal pounds = new BigDecimal(mPoundsEditText.getText().toString());
                        BigDecimal kilograms = TextUtils.isEmpty(mOuncesEditText.getText()) ?
                                poundsToKilograms(pounds) :
                                poundsToKilograms(pounds, new BigDecimal(
                                        addLeadingZero(mOuncesEditText.getText().toString())));

                        // Strip trailing zeros from kilograms
                        String format =
                                getString(R.string.item_dialog_decimal_format_weight_edit_text);
                        DecimalFormat df = new DecimalFormat(format);
                        mKilogramsEditText.setText(df.format(kilograms));
                        mWeightWasChanged = false;
                    }
                    break;
                case R.id.rb_per_pound:
                    mIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    mIfPerKgSelectedLayout.setVisibility(View.GONE);
                    mIfPerLbSelectedLayout.setVisibility(View.VISIBLE);
                    mPriceTitleTextView.setText(R.string.price_per_lb);
                    if (!TextUtils.isEmpty(mPriceEditText.getText())) {
                        BigDecimal pricePerKilogram =
                                new BigDecimal(addLeadingZero(mPriceEditText.getText().toString()));
                        String prefill = getString(R.string.item_dialog_placeholder_price_edit_text,
                                pricePerKilogram.multiply(KILOGRAMS_PER_POUND));
                        mPriceEditText.setText(prefill);
                    }
                    if (!TextUtils.isEmpty(mKilogramsEditText.getText()) && mWeightWasChanged) {
                        BigDecimal kilograms = new BigDecimal(
                                addLeadingZero(mKilogramsEditText.getText().toString()));
                        BigDecimal pounds = kilograms.multiply(POUNDS_PER_KILOGRAM);

                        // Strip trailing zeros from pounds
                        String format =
                                getString(R.string.item_dialog_decimal_format_weight_edit_text);
                        DecimalFormat df = new DecimalFormat(format);
                        mPoundsEditText.setText(df.format(pounds));
                        mOuncesEditText.setText("");
                        mWeightWasChanged = false;
                    }
                    break;
            }
            updateDialog();
        });

        mPriceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            ItemClickDialogListener listener =
                    (ItemClickDialogListener) context;
            mAdapter = listener.getAdapter();
            mBudgetIsSet = listener.isBudgetSet();
            mBudget = listener.getBudget();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement ItemClickDialogListener");
        }
    }

    public void setItem(SingleShoppingListItem item) {
        mItem = item;
    }

    private void updateDialog() {
        String quantityString = Integer.toString(mCurrentQuantity);
        mQuantityTextView.setText(quantityString);
        if (!TextUtils.isEmpty(mPriceEditText.getText())) {
            BigDecimal basePrice = new BigDecimal(
                    addLeadingZero(mPriceEditText.getText().toString()));
            BigDecimal priceWithoutTax = null, tax = null, totalPrice = null;
            switch (mWhichRadioButtonChecked) {
                case R.id.rb_per_unit:
                    if (mCurrentQuantity >= 1) {
                        mPositiveButton.setEnabled(true);
                        priceWithoutTax =
                                basePrice.multiply(new BigDecimal(mCurrentQuantity));
                        tax = ShoppingListItem.getTax(priceWithoutTax);
                        totalPrice = priceWithoutTax.add(tax);
                    } else {
                        mPositiveButton.setEnabled(false);
                    }
                    break;
                case R.id.rb_per_kg:
                    if (!TextUtils.isEmpty(mKilogramsEditText.getText())) {
                        mPositiveButton.setEnabled(true);
                        BigDecimal kilograms = new BigDecimal(
                                addLeadingZero(mKilogramsEditText.getText().toString()));
                        priceWithoutTax = basePrice.multiply(kilograms);
                        tax = ShoppingListItem.getTax(priceWithoutTax);
                        totalPrice = priceWithoutTax.add(tax);
                    } else {
                        mPositiveButton.setEnabled(false);
                    }
                    break;
                case R.id.rb_per_pound:
                    if (!TextUtils.isEmpty(mPoundsEditText.getText())) {
                        mPositiveButton.setEnabled(true);
                        BigDecimal pounds = new BigDecimal(
                                addLeadingZero(mPoundsEditText.getText().toString()));
                        BigDecimal ounces = TextUtils.isEmpty(mOuncesEditText.getText()) ?
                                BigDecimal.ZERO :
                                new BigDecimal(mOuncesEditText.getText().toString());
                        // Dividing an integer by 16 will never cause non-terminating expansion
                        @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
                        BigDecimal totalPounds = pounds.add(ounces.divide(OUNCES_PER_POUND));
                        priceWithoutTax = basePrice.multiply(totalPounds);
                        tax = ShoppingListItem.getTax(priceWithoutTax);
                        totalPrice = priceWithoutTax.add(tax);
                    } else {
                        mPositiveButton.setEnabled(false);
                    }
                    break;
            }
            if (priceWithoutTax != null) {
                String priceWithoutTaxString =
                        getString(R.string.item_dialog_placeholder_total_price_no_tax,
                                priceWithoutTax);
                String taxString = getString(R.string.item_dialog_placeholder_tax, tax);
                String totalPriceString =
                        getString(R.string.item_dialog_placeholder_total_price, totalPrice);
                mPriceNoTaxTextView.setText(priceWithoutTaxString);
                mTaxTextView.setText(taxString);
                mTotalPriceTextView.setText(totalPriceString);
            }
        } else {
            mPositiveButton.setEnabled(false);
        }
    }

    private void perUnitSetup() {
        String prefill =
                getString(R.string.item_dialog_placeholder_price_edit_text, mItem.getBasePrice());
        mPriceEditText.setText(prefill);

        mDecreaseQuantityButton.setOnClickListener(v -> {
            mCurrentQuantity--;
            mDecreaseQuantityButton.setEnabled(mCurrentQuantity > 0);
            updateDialog();
        });
        mIncreaseQuantityButton.setOnClickListener(v -> {
            mCurrentQuantity++;
            mDecreaseQuantityButton.setEnabled(mCurrentQuantity > 0);
            updateDialog();
        });
    }

    private void perKgSetup() {
        String prefill =
                getString(R.string.item_dialog_placeholder_price_edit_text, mItem.getBasePrice());
        mPriceEditText.setText(prefill);

        // Need to use DecimalFormat to strip trailing zeros from kilograms
        String format = getString(R.string.item_dialog_decimal_format_weight_edit_text);
        DecimalFormat df = new DecimalFormat(format);
        mKilogramsEditText.setText(df.format(mItem.getWeightInKilograms()));
        mKilogramsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void perLbSetup() {
        // Need to use DecimalFormat to strip trailing zeros from pounds
        String format = getString(R.string.item_dialog_decimal_format_weight_edit_text);
        DecimalFormat df = new DecimalFormat(format);
        mPoundsEditText.setText(df.format(mItem.getWeightInPounds()));

        mPoundsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mOuncesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private BigDecimal poundsToKilograms(BigDecimal pounds, BigDecimal ounces) {
        // Dividing an integer by 16 will never cause non-terminating expansion
        @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
        BigDecimal fractionalPart = ounces.divide(OUNCES_PER_POUND);
        BigDecimal totalPounds = pounds.add(fractionalPart);
        return totalPounds.multiply(KILOGRAMS_PER_POUND);
    }

    private BigDecimal poundsToKilograms(BigDecimal pounds) {
        return poundsToKilograms(pounds, BigDecimal.ZERO);
    }

    /**
     * The positive button updates the item to have the values specified by the user, and warns the
     * user if this made the total price go over budget.
     */
    private void onPositive() {
        switch (mWhichRadioButtonChecked) {
            case R.id.rb_per_unit:
                mItem.setBasePrice(
                        new BigDecimal(addLeadingZero(mPriceEditText.getText().toString())));
                mItem.setQuantity(mCurrentQuantity);
                break;
            case R.id.rb_per_kg:
                mItem.setBasePrice(
                        new BigDecimal(addLeadingZero(mPriceEditText.getText().toString())));
                mItem.setWeightInKilograms(
                        new BigDecimal(addLeadingZero(mKilogramsEditText.getText().toString())));
                break;
            case R.id.rb_per_pound:
                BigDecimal pounds =
                        new BigDecimal(addLeadingZero(mPoundsEditText.getText().toString()));
                mItem.setPricePerPound(
                        new BigDecimal(addLeadingZero(mPriceEditText.getText().toString())));
                if (!TextUtils.isEmpty(mOuncesEditText.getText())) {
                    int ounces = Integer.parseInt(mOuncesEditText.getText().toString());
                    mItem.setWeightInPounds(pounds, ounces);
                } else {
                    mItem.setWeightInPounds(pounds);
                }
        }
        new UpdateItemTask(this, true).execute(mItem);
    }

    /**
     * The neutral button moves the item back to the unchecked position.
     */
    private void onNeutral() {
        mItem.reset();
        new UpdateItemTask(this, false).execute(mItem);
    }

    /**
     * Adds a leading zero to a numeric string if it is needed, that is, if it starts with ".".
     * @param numericString A numeric string with at most one "."
     */
    private String addLeadingZero(String numericString) {
        return Character.isDigit(numericString.charAt(0)) ? numericString : ("0" + numericString);
    }

    private static void showToastMessage(Context context, int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static class UpdateItemTask
            extends AsyncTask<SingleShoppingListItem, Void, SingleShoppingListItem> {
        private final WeakReference<OnCheckedSingleItemClickDialogFragment> ref;
        private final boolean positive;

        UpdateItemTask(OnCheckedSingleItemClickDialogFragment fragment, boolean positive) {
            ref = new WeakReference<>(fragment);
            this.positive = positive;
        }

        @Override
        protected SingleShoppingListItem doInBackground(SingleShoppingListItem... items) {
            ShoppingListDao dao =
                    AppDatabase.getDatabaseInstance(ref.get().getContext()).shoppingListDao();
            dao.update(items[0]);
            return items[0];
        }

        @Override
        protected void onPostExecute(SingleShoppingListItem singleShoppingListItem) {
            super.onPostExecute(singleShoppingListItem);
            ref.get().mAdapter.onDataChanged();

            if (positive && ref.get().mBudgetIsSet
                    && ref.get().mAdapter.getTotalPrice().compareTo(ref.get().mBudget) > 0) {
                warnOverBudget();
            }
        }
    }
}
