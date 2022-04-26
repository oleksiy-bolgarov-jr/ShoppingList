package org.bolgarov.alexjr.shoppinglist.dialogs.extendedItems;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItemJoin;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Objects;

public class AddSubitemDialogFragment extends DialogFragment {
    private static final String TAG = AddSubitemDialogFragment.class.getSimpleName();

    private static final BigDecimal OUNCES_PER_POUND = new BigDecimal(16);

    private ExtendedShoppingListItem mExtendedItem;
    private int mCurrentQuantity;
    private ExtendedItemAdapter mAdapter;
    private String[] mAutocompleteDictionary;

    private int mWhichRadioButtonChecked;
    private AutoCompleteTextView mItemNameEditText;
    private CheckBox mAddToAutocompleteCheckBox;
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

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            mPositiveButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
            mPositiveButton.setEnabled(false);
        }
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

        mItemNameEditText = view.findViewById(R.id.et_item_name);
        mItemNameEditText.setVisibility(View.VISIBLE);
        mAddToAutocompleteCheckBox = view.findViewById(R.id.save_item_check_box);
        mAddToAutocompleteCheckBox.setVisibility(View.VISIBLE);
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

        builder.setTitle(R.string.item_dialog_subitem_title)
                .setView(view)
                .setPositiveButton(
                        R.string.item_dialog_subitem_positive,
                        (dialog, which) -> onPositive()
                )
                .setNegativeButton(
                        R.string.item_dialog_unchecked_negative,
                        (dialog, which) -> dialog.dismiss()
                );
        Dialog dialog = builder.create();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                Objects.requireNonNull(getContext()),
                android.R.layout.simple_dropdown_item_1line,
                mAutocompleteDictionary
        );
        mItemNameEditText.setAdapter(adapter);

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

        mItemNameEditText.addTextChangedListener(new UpdateDialogTextChangedListener());
        mPriceEditText.addTextChangedListener(new UpdateDialogTextChangedListener());

        perUnitSetup();
        perKgSetup();
        perLbSetup();

        return dialog;
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        checkValidState();
        return super.show(transaction, tag);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        checkValidState();
        super.show(manager, tag);
    }

    public void setAdapter(ExtendedItemAdapter adapter) {
        mAdapter = adapter;
    }

    public void setExtendedItem(ExtendedShoppingListItem item) {
        mExtendedItem = item;
    }

    public void setAutocompleteDictionary(String[] autocompleteDictionary) {
        mAutocompleteDictionary = autocompleteDictionary;
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
        mKilogramsEditText.addTextChangedListener(new UpdateDialogTextChangedListener());
    }

    private void perLbSetup() {
        mPoundsEditText.addTextChangedListener(new UpdateDialogTextChangedListener());
        mOuncesEditText.addTextChangedListener(new UpdateDialogTextChangedListener());
    }

    private void onPositive() {
        String itemName = mItemNameEditText.getText().toString();
        SingleShoppingListItem item = new SingleShoppingListItem(itemName, false, null, -1);

        switch (mWhichRadioButtonChecked) {
            case R.id.rb_per_unit:
                item.setPerUnitOrPerWeight(SingleShoppingListItem.PER_UNIT);
                item.setBasePrice(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString()))
                );
                item.setQuantity(mCurrentQuantity);
                break;
            case R.id.rb_per_kg:
                item.setPerUnitOrPerWeight(SingleShoppingListItem.PER_WEIGHT);
                item.setBasePrice(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString()))
                );
                item.setWeightInKilograms(
                        new BigDecimal(padWithZeros(mKilogramsEditText.getText().toString()))
                );
                break;
            case R.id.rb_per_pound:
                BigDecimal pounds =
                        new BigDecimal(padWithZeros(mPoundsEditText.getText().toString()));
                item.setPerUnitOrPerWeight(SingleShoppingListItem.PER_WEIGHT);
                item.setPricePerPound(
                        new BigDecimal(padWithZeros(mPriceEditText.getText().toString()))
                );
                if (!TextUtils.isEmpty(mOuncesEditText.getText())) {
                    int ounces = Integer.parseInt(mOuncesEditText.getText().toString());
                    item.setWeightInPounds(pounds, ounces);
                } else {
                    item.setWeightInPounds(pounds);
                }
                break;
        }

        new AddSubItemTask(
                getContext(),
                mAdapter,
                mAddToAutocompleteCheckBox.isChecked()
        ).execute(mExtendedItem, item);
    }

    private void updateDialog() {
        String quantityString = Integer.toString(mCurrentQuantity);
        mQuantityTextView.setText(quantityString);
        if (!TextUtils.isEmpty(mItemNameEditText.getText()) &&
                !TextUtils.isEmpty(mPriceEditText.getText())) {
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

    /**
     * Throws an IllegalStateException if an ExtendedShoppingListItem or an ExtendedItemAdapter
     * have not been set.
     */
    private void checkValidState() {
        if (mExtendedItem == null || mAdapter == null) {
            throw new IllegalStateException("You must set an ExtendedShoppingListItem and an " +
                    "ExtendedItemAdapter before calling this method.");
        }
    }

    private static class AddSubItemTask
            extends AsyncTask<ShoppingListItem, Void, ShoppingListItem[]> {
        private final WeakReference<Context> ref;
        private final ExtendedItemAdapter adapter;
        private final boolean addToAutocomplete;

        AddSubItemTask(Context context, ExtendedItemAdapter adapter, boolean addToAutocomplete) {
            ref = new WeakReference<>(context);
            this.adapter = adapter;
            this.addToAutocomplete = addToAutocomplete;
        }

        @Override
        protected ShoppingListItem[] doInBackground(ShoppingListItem... items) {
            ExtendedShoppingListItem extendedItem = (ExtendedShoppingListItem) items[0];
            SingleShoppingListItem singleItem = (SingleShoppingListItem) items[1];
            ShoppingListDao dao = AppDatabase.getDatabaseInstance(ref.get()).shoppingListDao();

            int insertedId = (int) dao.insert(singleItem);

            ShoppingListItemJoin join = new ShoppingListItemJoin(extendedItem.getId(), insertedId);
            dao.insertJoin(join);

            if (addToAutocomplete) {
                AutocompleteEntryDao autocompleteDao =
                        AppDatabase.getDatabaseInstance(ref.get()).autocompleteEntryDao();
                autocompleteDao.insertAll(new AutocompleteEntry(singleItem.getName()));
            }

            // To ensure that the ID of the single item object matches the ID in the database
            singleItem.setId(insertedId);

            return new ShoppingListItem[]{extendedItem, singleItem};
        }

        @Override
        protected void onPostExecute(ShoppingListItem[] items) {
            super.onPostExecute(items);
            ExtendedShoppingListItem extendedItem = (ExtendedShoppingListItem) items[0];
            SingleShoppingListItem singleItem = (SingleShoppingListItem) items[1];

            extendedItem.addItem(singleItem);
            adapter.onDataChanged();
        }
    }

    private class UpdateDialogTextChangedListener implements TextWatcher {
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
    }
}
