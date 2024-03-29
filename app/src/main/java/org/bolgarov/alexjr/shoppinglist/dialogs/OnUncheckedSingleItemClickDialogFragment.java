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
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.Objects;

public class OnUncheckedSingleItemClickDialogFragment extends DialogFragment {

    private static final String TAG = OnUncheckedSingleItemClickDialogFragment.class.getSimpleName();

    private static final BigDecimal OUNCES_PER_POUND = new BigDecimal(16);

    private static WeakReference<FragmentActivity> activityRef;

    private SingleShoppingListItem mItem;
    private int mCurrentQuantity;

    private ShoppingListAdapter mAdapter;
    private boolean mBudgetIsSet;
    private BigDecimal mBudget;

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
            mPositiveButton.setEnabled(false);
        }
        activityRef = new WeakReference<>(getActivity());
        updateDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCurrentQuantity = 1;

        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_buying_item, null);

        RadioGroup pricingRadioGroup = view.findViewById(R.id.radio_group_pricing);
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
                        R.string.item_dialog_unchecked_positive,
                        (dialog, which) -> onPositive()
                )
                .setNegativeButton(
                        R.string.item_dialog_unchecked_negative,
                        (dialog, which) -> dialog.dismiss()
                )
                .setNeutralButton(
                        R.string.item_dialog_unchecked_neutral,
                        (dialog, which) -> onNeutral()
                );
        Dialog dialog = builder.create();

        mWhichRadioButtonChecked = R.id.rb_per_unit;    // This is the default one

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
                    break;
                case R.id.rb_per_pound:
                    mIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    mIfPerKgSelectedLayout.setVisibility(View.GONE);
                    mIfPerLbSelectedLayout.setVisibility(View.VISIBLE);
                    mPriceTitleTextView.setText(R.string.price_per_lb);
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

        perUnitSetup();
        perKgSetup();
        perLbSetup();

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
            BigDecimal basePrice =
                    new BigDecimal(padWithZeros(mPriceEditText.getText().toString()));
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
                                padWithZeros(mKilogramsEditText.getText().toString()));
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
                                padWithZeros(mPoundsEditText.getText().toString()));
                        BigDecimal ounces = TextUtils.isEmpty(mOuncesEditText.getText()) ?
                                BigDecimal.ZERO :
                                new BigDecimal(mOuncesEditText.getText().toString());
                        // Suppressed because this should never produce a non-terminating expansion.
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
        mKilogramsEditText.addTextChangedListener(new TextWatcher() {
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
    }

    private void perLbSetup() {
        mPoundsEditText.addTextChangedListener(new TextWatcher() {
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
        mOuncesEditText.addTextChangedListener(new TextWatcher() {
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
    }

    /**
     * The positive button updates the item to have the values specified by the user, moves the
     * item to the checked position, and warns the user if the item caused the total price to go
     * over budget.
     */
    private void onPositive() {
        mItem.setStatus(ShoppingListItem.CHECKED);

        switch (mWhichRadioButtonChecked) {
            case R.id.rb_per_unit:
                mItem.setPerUnitOrPerWeight(SingleShoppingListItem.PER_UNIT);
                mItem.setBasePrice(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString())));
                mItem.setQuantity(mCurrentQuantity);
                break;
            case R.id.rb_per_kg:
                mItem.setPerUnitOrPerWeight(SingleShoppingListItem.PER_WEIGHT);
                mItem.setBasePrice(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString())));
                mItem.setWeightInKilograms(
                        new BigDecimal(padWithZeros(mKilogramsEditText.getText().toString())));
                break;
            case R.id.rb_per_pound:
                BigDecimal pounds =
                        new BigDecimal(padWithZeros(mPoundsEditText.getText().toString()));
                mItem.setPerUnitOrPerWeight(SingleShoppingListItem.PER_WEIGHT);
                mItem.setPricePerPound(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString())));
                if (!TextUtils.isEmpty(mOuncesEditText.getText())) {
                    int ounces = Integer.parseInt(mOuncesEditText.getText().toString());
                    mItem.setWeightInPounds(pounds, ounces);
                } else {
                    mItem.setWeightInPounds(pounds);
                }
                break;
        }
        new UpdateItemTask(this, true).execute(mItem);
    }

    /**
     * The neutral button moves the item to the "not buying" position.
     */
    private void onNeutral() {
        mItem.setStatus(ShoppingListItem.NOT_BUYING);
        new UpdateItemTask(this, false).execute(mItem);
    }

    /**
     * To prevent number parsing errors, adds a 0 at the beginning or end of the numeric string if
     * it has a decimal point at either the beginning or the end.
     *
     * @param numericString A numeric string with at most one "."
     */
    private String padWithZeros(String numericString) {
        return !Character.isDigit(numericString.charAt(0)) ?
                ("0" + numericString) :
                !Character.isDigit(numericString.charAt(numericString.length() - 1)) ?
                        (numericString + "0") :
                        numericString;
    }

    private static void showToastMessage(Context context, int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static class UpdateItemTask
            extends AsyncTask<SingleShoppingListItem, Void, SingleShoppingListItem> {
        private final WeakReference<OnUncheckedSingleItemClickDialogFragment> ref;
        private final boolean positive;

        UpdateItemTask(OnUncheckedSingleItemClickDialogFragment fragment, boolean positive) {
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
