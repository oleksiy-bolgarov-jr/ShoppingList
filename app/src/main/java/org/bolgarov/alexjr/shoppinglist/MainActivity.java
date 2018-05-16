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

package org.bolgarov.alexjr.shoppinglist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItemDatabaseEntity;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends
        AppCompatActivity
        implements
        ShoppingListAdapter.ShoppingListAdapterOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Debug tag for logging
     */
    @SuppressWarnings("unused")
    public static final String TAG = MainActivity.class.getSimpleName();

    /*
     * TODO: Next steps:
     * Improve the "delete all" option so you can delete from separate categories
     * Try to prevent user from entering negative values
     * Add a calculator to conveniently calculate price per kg given price per lb or vice versa, or price per weight/unit given total price and weight/unit
     * Add promotions (e.g. 3 for $6.00, etc.)
     * Add categories (after Github)
     * Save states to settings
     * OPTIONAL: Use CardViews as list items
     */

    private AppDatabase mDatabase;

    private ShoppingListAdapter mShoppingListAdapter;

    // Main view
    private RecyclerView mRecyclerView;
    private ConstraintLayout mErrorDisplayConstraintLayout;
    private ConstraintLayout mLoadingIndicatorConstraintLayout;

    // Footer
    private LinearLayout mFootnotes;
    private TextView mTotalPriceTextView;
    private TextView mOverBudgetTextView;
    private TextView mOptionalTextView;
    private TextView mConditionTextView;

    // Budget and tax values
    private boolean mBudgetIsSet;
    private BigDecimal mBudget;
    private boolean mWarningIsSet;
    private String mWarningType;
    private BigDecimal mWarningFixedValue;
    private int mWarningPercentage;
    private BigDecimal mWarningValue;
    private boolean mIncludeTax;
    private BigDecimal mTaxRate;

    // Autocomplete values
    private boolean mAutocompleteEnabled;
    private String[] mAutocompleteDictionary;

    // Add new item dialog
    // I have chosen not to prefix these with m to distinguish them from the main values.
    private AutoCompleteTextView addNewItemDialogItemNameEditText;
    private CheckBox addNewItemDialogSaveItemCheckBox;
    private CheckBox addNewItemDialogOptionalCheckBox;
    private EditText addNewItemDialogConditionEditText;
    private View addNewItemDialogPositiveAction;
    private View addNewItemDialogNeutralAction;

    // On select item dialog
    private RadioGroup itemDialogPricingRadioGroup;
    private RadioButton itemDialogPerUnitRadioButton, itemDialogPerKgRadioButton,
            itemDialogPerLbRadioButton;
    private int itemDialogWhichRadioButtonChecked;
    private TextView itemDialogPriceTitleTextView;
    private LinearLayout itemDialogIfPerUnitSelectedLayout, itemDialogIfPerKilogramSelectedLayout,
            itemDialogIfPerPoundSelectedLayout;
    private EditText itemDialogPriceEditText;
    private ImageButton itemDialogDecreaseQuantityImageButton,
            itemDialogIncreaseQuantityImageButton;
    private TextView itemDialogQuantityTextView;
    private EditText itemDialogKilogramsEditText;
    private EditText itemDialogPoundsEditText, itemDialogOuncesEditText;
    private TextView itemDialogTotalItemPriceNoTaxTextView;
    private TextView itemDialogTaxTextView;
    private TextView itemDialogTotalItemPriceTextView;
    private LinearLayout itemDialogPriceWithoutTaxLinearLayout, itemDialogTaxLinearLayout;
    private View itemDialogPositiveAction;
    private ShoppingListItem itemDialogCurrentItem;
    private int itemDialogCurrentItemQuantity;
    private boolean itemDialogWeightWasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recycler_view_shopping_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);

        mShoppingListAdapter = new ShoppingListAdapter(this, this);

        mRecyclerView.setAdapter(mShoppingListAdapter);

        mFootnotes = findViewById(R.id.linear_layout_footnotes);
        mTotalPriceTextView = findViewById(R.id.tv_total_price);
        mOverBudgetTextView = findViewById(R.id.tv_over_budget_footer);
        mOptionalTextView = findViewById(R.id.tv_optional_footnote);
        mConditionTextView = findViewById(R.id.tv_condition_footnote);

        mErrorDisplayConstraintLayout = findViewById(R.id.constraint_layout_no_items_display);
        mLoadingIndicatorConstraintLayout = findViewById(R.id.constraint_layout_loading_display_main);

        FloatingActionButton addItemButton = findViewById(R.id.fab_add_item);
        addItemButton.setOnClickListener(v -> showAddNewItemDialog());

        setupSharedPreferences();
        updateWarning();

        mDatabase = AppDatabase.getDatabaseInstance(this);
        switchViews(mShoppingListAdapter.getItemCount() == 0);

        executeRetrieveItemsAction();
        setupAutocompleteDictionary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShoppingListItem[] items =
                mShoppingListAdapter.getAllItems().toArray(new ShoppingListItem[0]);
        executeStoreItemsAction(items);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_delete_all:
                new MaterialDialog.Builder(this)
                        .title(R.string.delete_all_dialog_title)
                        .content(R.string.delete_all_dialog_body)
                        .positiveText(R.string.delete_all_dialog_positive)
                        .negativeText(R.string.delete_all_dialog_negative)
                        .onPositive((dialog, which) -> mShoppingListAdapter.deleteAll())
                        .show();
                return true;
            case R.id.option_settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_set_maximum_budget_checkbox))) {
            mBudgetIsSet = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_default_set_budget));
        } else if (key.equals(getString(R.string.key_maximum_budget_edit_text))) {
            mBudget = new BigDecimal(sharedPreferences.getString(key,
                    getResources().getString(R.string.pref_default_maximum_budget)));
        } else if (key.equals(getString(R.string.key_set_warning_checkbox))) {
            mWarningIsSet = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_default_set_warning));
        } else if (key.equals(getString(R.string.key_warning_type_list))) {
            mWarningType = sharedPreferences.getString(key,
                    getResources().getString(R.string.pref_value_warn_fixed_price));
        } else if (key.equals(getString(R.string.key_warning_fixed_edit_text))) {
            mWarningFixedValue =
                    new BigDecimal(sharedPreferences.getString(key,
                            getResources().getString(R.string.pref_default_warning_fixed)));
        } else if (key.equals(getString(R.string.key_warning_percentage_seek_bar))) {
            mWarningPercentage = sharedPreferences.getInt(key,
                    getResources().getInteger(R.integer.pref_default_warning_percentage));
        } else if (key.equals(getString(R.string.key_include_tax_checkbox))) {
            mIncludeTax = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_default_include_tax));
            if (mIncludeTax) {
                ShoppingListItem.setTaxRate(mTaxRate);
            } else {
                ShoppingListItem.setTaxRate(new BigDecimal("0"));
            }
        } else if (key.equals(getString(R.string.key_tax_rate_edit_text))) {
            mTaxRate = new BigDecimal(sharedPreferences.getString(key,
                    getResources().getString(R.string.pref_default_tax_rate)))
                    .multiply(new BigDecimal("0.01"));
            if (mIncludeTax) {
                ShoppingListItem.setTaxRate(mTaxRate);
            } else {
                ShoppingListItem.setTaxRate(new BigDecimal("0"));
            }
        } else if (key.equals(getString(R.string.key_enable_autocomplete_checkbox))) {
            mAutocompleteEnabled = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_default_enable_autocomplete));
        }

        updateWarning();
        mShoppingListAdapter.onDataChanged();
    }

    /**
     * Shows either the RecyclerView showing the list of items, or an error view if the list is
     * empty.
     *
     * @param listIsEmpty true iff the shopping list is empty
     */
    @Override
    public void switchViews(boolean listIsEmpty) {
        if (listIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mErrorDisplayConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mErrorDisplayConstraintLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Action to be executed when the user clicks on one of the items in the shopping list.
     *
     * @param index The position in the shopping list of the item that was clicked.
     */
    @Override
    public void onItemClick(int index) {
        List<ShoppingListItem> allItems = mShoppingListAdapter.getAllItemsOrderedByStatus();
        ShoppingListItem item = allItems.get(index);
        switch (item.getStatus()) {
            case ShoppingListItem.UNCHECKED:
                if (item.hasCondition()) {
                    onConditionedItemClick(item);
                } else {
                    onUncheckedItemClick(item);
                }
                break;
            case ShoppingListItem.CHECKED:
                onCheckedItemClick(item);
                break;
            default:
                onNotBuyingItemClick(item);
                break;
        }
    }

    /**
     * Sets the total price in the TextView in the footer, and colours it accordingly.
     *
     * @param price The price to be set
     */
    @Override
    public void updateTotalPrice(BigDecimal price) {
        String totalString =
                getString(R.string.main_activity_placeholder_footer_total, price, mBudget);
        mTotalPriceTextView.setText(totalString);
        mOverBudgetTextView.setVisibility(View.GONE);

        if (mBudgetIsSet) {
            if (price.compareTo(mBudget) > 0) {
                // i.e. if price > mBudget
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_over_budget));
                mOverBudgetTextView.setVisibility(View.VISIBLE);
            } else if (mWarningIsSet && price.compareTo(mWarningValue) > 0) {
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_warning));
            } else {
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_normal));
            }
        }
    }

    @Override
    public boolean isTaxIncluded() {
        return mIncludeTax;
    }

    @Override
    public void showFootnotes(boolean optionalItemsExist, boolean conditionedItemsExist) {
        if (optionalItemsExist || conditionedItemsExist) {
            mFootnotes.setVisibility(View.VISIBLE);
            mOptionalTextView.setVisibility(optionalItemsExist ? View.VISIBLE : View.GONE);
            mConditionTextView.setVisibility(conditionedItemsExist ? View.VISIBLE : View.GONE);
        } else {
            mFootnotes.setVisibility(View.GONE);
        }
    }

    private void showAddNewItemDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .title(R.string.add_new_item_dialog_title)
                .customView(R.layout.dialog_add_new_item, true)
                .positiveText(R.string.add_new_item_dialog_positive)
                .negativeText(R.string.add_new_item_dialog_negative)
                .neutralText(R.string.add_new_item_dialog_neutral)
                .onPositive(
                        (dialog, which) -> onPositiveAddNewItem(
                                addNewItemDialogItemNameEditText.getText().toString(),
                                addNewItemDialogOptionalCheckBox.isChecked(),
                                addNewItemDialogConditionEditText.getText().toString(),
                                addNewItemDialogSaveItemCheckBox.isChecked()
                        )
                )
                .onNeutral(
                        (dialog, which) -> onNeutralAddNewItem(
                                addNewItemDialogItemNameEditText.getText().toString(),
                                addNewItemDialogOptionalCheckBox.isChecked(),
                                addNewItemDialogConditionEditText.getText().toString(),
                                addNewItemDialogSaveItemCheckBox.isChecked()
                        )
                )
                .build();

        assert materialDialog.getCustomView() != null;
        addNewItemDialogItemNameEditText = materialDialog.getCustomView()
                .findViewById(R.id.item_name_edit_text);
        addNewItemDialogSaveItemCheckBox = materialDialog.getCustomView()
                .findViewById(R.id.save_item_check_box);
        addNewItemDialogOptionalCheckBox = materialDialog.getCustomView()
                .findViewById(R.id.make_item_optional_check_box);
        addNewItemDialogConditionEditText = materialDialog.getCustomView()
                .findViewById(R.id.condition_edit_text);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        mAutocompleteEnabled ? mAutocompleteDictionary : new String[0]
                );
        addNewItemDialogItemNameEditText.setAdapter(adapter);

        addNewItemDialogPositiveAction = materialDialog.getActionButton(DialogAction.POSITIVE);
        addNewItemDialogNeutralAction = materialDialog.getActionButton(DialogAction.NEUTRAL);
        addNewItemDialogPositiveAction.setEnabled(false);
        addNewItemDialogNeutralAction.setEnabled(false);
        addNewItemDialogItemNameEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        addNewItemDialogPositiveAction.setEnabled(s.toString().trim().length() > 0);
                        addNewItemDialogNeutralAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );

        materialDialog.show();
    }

    private void setupSharedPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        mBudgetIsSet = sp.getBoolean(
                getString(R.string.key_set_maximum_budget_checkbox),
                getResources().getBoolean(R.bool.pref_default_set_budget)
        );

        String budgetString = sp.getString(
                getString(R.string.key_maximum_budget_edit_text),
                getResources().getString(R.string.pref_default_maximum_budget)
        );
        mBudget = new BigDecimal(budgetString);

        mWarningIsSet = sp.getBoolean(
                getString(R.string.key_set_warning_checkbox),
                getResources().getBoolean(R.bool.pref_default_set_warning)
        );

        mWarningType = sp.getString(
                getString(R.string.key_warning_type_list),
                getString(R.string.pref_value_warn_fixed_price)
        );

        String warningValueString = sp.getString(
                getString(R.string.key_warning_fixed_edit_text),
                getString(R.string.pref_default_warning_fixed)
        );
        mWarningFixedValue = new BigDecimal(warningValueString);

        mWarningPercentage = sp.getInt(
                getString(R.string.key_warning_percentage_seek_bar),
                getResources().getInteger(R.integer.pref_default_warning_percentage)
        );

        mIncludeTax = sp.getBoolean(
                getString(R.string.key_include_tax_checkbox),
                getResources().getBoolean(R.bool.pref_default_include_tax)
        );

        String taxRateString = sp.getString(
                getString(R.string.key_tax_rate_edit_text),
                getResources().getString(R.string.pref_default_tax_rate)
        );
        mTaxRate = new BigDecimal(taxRateString).multiply(new BigDecimal("0.01"));
        if (mIncludeTax) {
            ShoppingListItem.setTaxRate(mTaxRate);
        } else {
            ShoppingListItem.setTaxRate(new BigDecimal("0"));
        }

        mAutocompleteEnabled = sp.getBoolean(
                getString(R.string.key_enable_autocomplete_checkbox),
                getResources().getBoolean(R.bool.pref_default_enable_autocomplete)
        );

        sp.registerOnSharedPreferenceChangeListener(this);
    }

    private void setupAutocompleteDictionary() {
        new SetupAutocompleteDictionaryTask(this).execute();
    }

    private void updateWarning() {
        if (mWarningType.equals(getString(R.string.pref_value_warn_fixed_price))) {
            mWarningValue = mWarningFixedValue;
        } else {
            mWarningValue = mBudget.multiply(
                    new BigDecimal(mWarningPercentage)
                            .multiply(new BigDecimal("0.01")));
        }
    }

    private void addItem(String itemName, boolean optional, String condition, boolean save) {
        ShoppingListItem itemToAdd = new ShoppingListItem(
                itemName,
                optional,
                TextUtils.isEmpty(condition) ? null : condition
        );
        mShoppingListAdapter.addItemToEndOfShoppingList(itemToAdd);
        if (save) {
            executeAddToAutocompleteAction(itemName);
        }
    }

    private void executeAddToAutocompleteAction(String itemName) {
        new AddToAutocompleteTask(this).execute(itemName);
    }

    private void onPositiveAddNewItem(String itemName, boolean optional, String condition,
                                      boolean save) {
        addItem(itemName, optional, condition, save);
        showAddNewItemDialog();
    }

    private void onNeutralAddNewItem(String itemName, boolean optional, String condition,
                                     boolean save) {
        addItem(itemName, optional, condition, save);
    }

    /**
     * Precondition: item.hasCondition() must be true
     *
     * @param item The shopping list item, with a non-null, nonempty condition
     */
    private void onConditionedItemClick(ShoppingListItem item) {
        if (!item.hasCondition()) {
            throw new IllegalArgumentException(
                    "Item must have a non-null, nonempty condition specified.");
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .customView(R.layout.dialog_conditioned_item, true)
                .positiveText(R.string.item_dialog_condition_positive)
                .negativeText(R.string.item_dialog_condition_negative)
                .neutralText(R.string.item_dialog_unchecked_neutral)
                .onPositive((d, which) -> onUncheckedItemClick(item))
                .onNeutral((d, which) -> {
                    item.setStatus(ShoppingListItem.NOT_BUYING);
                    mShoppingListAdapter.onDataChanged();
                })
                .build();
        View view = dialog.getCustomView();
        assert view != null;
        TextView conditionTextView = view.findViewById(R.id.tv_item_condition);
        conditionTextView.setText(item.getCondition());
        dialog.show();
    }

    private void onUncheckedItemClick(ShoppingListItem item) {
        itemDialogCurrentItem = item;
        itemDialogCurrentItemQuantity = 0;
        MaterialDialog itemBuyingDialog = new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .customView(R.layout.dialog_buying_item, true)
                .positiveText(R.string.item_dialog_unchecked_positive)
                .neutralText(R.string.item_dialog_unchecked_neutral)
                .negativeText(R.string.item_dialog_unchecked_negative)
                .onPositive(new UncheckedItemClickPositiveActionCallback())
                .onNeutral(new UncheckedItemClickNeutralActionCallback())
                .build();
        View view = itemBuyingDialog.getCustomView();
        itemDialogPositiveAction = itemBuyingDialog.getActionButton(DialogAction.POSITIVE);

        assert view != null;
        itemDialogPricingRadioGroup = view.findViewById(R.id.radio_group_pricing);
        itemDialogPerUnitRadioButton = view.findViewById(R.id.rb_per_unit);
        itemDialogPerKgRadioButton = view.findViewById(R.id.rb_per_kg);
        itemDialogPerLbRadioButton = view.findViewById(R.id.rb_per_pound);
        itemDialogPriceTitleTextView = view.findViewById(R.id.tv_price_title);
        itemDialogIfPerUnitSelectedLayout = view.findViewById(R.id.ll_if_per_unit_selected);
        itemDialogIfPerKilogramSelectedLayout = view.findViewById(R.id.ll_if_per_kg_selected);
        itemDialogIfPerPoundSelectedLayout = view.findViewById(R.id.ll_if_per_lb_selected);
        itemDialogPriceEditText = view.findViewById(R.id.et_price);
        itemDialogDecreaseQuantityImageButton = view.findViewById(R.id.btn_decrease_quantity);
        itemDialogIncreaseQuantityImageButton = view.findViewById(R.id.btn_increase_quantity);
        itemDialogQuantityTextView = view.findViewById(R.id.tv_quantity);
        itemDialogKilogramsEditText = view.findViewById(R.id.et_kg);
        itemDialogPoundsEditText = view.findViewById(R.id.et_lb);
        itemDialogOuncesEditText = view.findViewById(R.id.et_oz);
        itemDialogTotalItemPriceNoTaxTextView =
                view.findViewById(R.id.tv_total_item_price_no_tax);
        itemDialogTaxTextView = view.findViewById(R.id.tv_tax);
        itemDialogTotalItemPriceTextView = view.findViewById(R.id.tv_total_item_price);
        itemDialogPriceWithoutTaxLinearLayout =
                view.findViewById(R.id.ll_total_price_no_tax);
        itemDialogTaxLinearLayout = view.findViewById(R.id.ll_tax);

        if (!mIncludeTax) {
            itemDialogPriceWithoutTaxLinearLayout.setVisibility(View.GONE);
            itemDialogTaxLinearLayout.setVisibility(View.GONE);
        }

        itemDialogPositiveAction.setEnabled(false);  // User needs to add the proper values first

        itemDialogWhichRadioButtonChecked = R.id.rb_per_unit;  // This is the default one

        itemDialogPricingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            itemDialogWhichRadioButtonChecked = checkedId;
            switch (checkedId) {
                case R.id.rb_per_unit:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.GONE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_unit);
                    updateDialog();
                    break;
                case R.id.rb_per_kg:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.GONE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_kg);
                    updateDialog();
                    break;
                case R.id.rb_per_pound:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_lb);
                    updateDialog();
                    break;
            }
        });

        itemDialogPriceEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count,
                                                  int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence pricePerUnit, int start, int before,
                                              int count) {
                        updateDialog();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );

        perUnitUncheckedSetup();
        perKgUncheckedSetup();
        perLbUncheckedSetup();
        updateDialog();

        itemBuyingDialog.show();
    }

    private void updateDialog() {
        String quantityString = Integer.toString(itemDialogCurrentItemQuantity);
        itemDialogQuantityTextView.setText(quantityString);
        if (!TextUtils.isEmpty(itemDialogPriceEditText.getText())) {
            BigDecimal pricePerUnit = new BigDecimal(itemDialogPriceEditText.getText().toString());
            BigDecimal totalPriceWithoutTax, tax, totalPrice;
            totalPriceWithoutTax = tax = totalPrice = null;
            switch (itemDialogWhichRadioButtonChecked) {
                case R.id.rb_per_unit:
                    if (itemDialogCurrentItemQuantity >= 1) {
                        itemDialogPositiveAction.setEnabled(true);
                        totalPriceWithoutTax =
                                pricePerUnit.multiply(
                                        new BigDecimal(itemDialogCurrentItemQuantity));
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                    } else {
                        itemDialogPositiveAction.setEnabled(false);
                    }
                    break;
                case R.id.rb_per_kg:
                    if (!TextUtils.isEmpty(itemDialogKilogramsEditText.getText())) {
                        itemDialogPositiveAction.setEnabled(true);
                        BigDecimal kilograms =
                                new BigDecimal(itemDialogKilogramsEditText.getText().toString());
                        totalPriceWithoutTax = pricePerUnit.multiply(kilograms);
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                    } else {
                        itemDialogPositiveAction.setEnabled(false);
                    }
                    break;
                case R.id.rb_per_pound:
                    if (!TextUtils.isEmpty(itemDialogPoundsEditText.getText())) {
                        itemDialogPositiveAction.setEnabled(true);
                        BigDecimal pounds =
                                new BigDecimal(itemDialogPoundsEditText.getText().toString());
                        int ounces;
                        if (!TextUtils.isEmpty(itemDialogOuncesEditText.getText())) {
                            ounces =
                                    Integer.parseInt(itemDialogOuncesEditText.getText().toString());
                        } else {
                            ounces = 0;
                        }
                        BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                        BigDecimal pricePerKilogram =
                                ShoppingListItem.getPricePerKilogram(pricePerUnit);
                        totalPriceWithoutTax = pricePerKilogram.multiply(kilograms);
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                    } else {
                        itemDialogPositiveAction.setEnabled(false);
                    }
                    break;
            }
            if (totalPriceWithoutTax != null) {
                String totalPriceNoTaxString =
                        getString(R.string.item_dialog_placeholder_total_price_no_tax,
                                totalPriceWithoutTax);
                String taxString = getString(R.string.item_dialog_placeholder_tax, tax);
                String totalPriceString =
                        getString(R.string.item_dialog_placeholder_total_price, totalPrice);
                itemDialogTotalItemPriceNoTaxTextView.setText(totalPriceNoTaxString);
                itemDialogTaxTextView.setText(taxString);
                itemDialogTotalItemPriceTextView.setText(totalPriceString);
            }
        } else {
            itemDialogPositiveAction.setEnabled(false);
        }
    }

    private void perUnitUncheckedSetup() {
        itemDialogDecreaseQuantityImageButton.setOnClickListener((v) -> {
            itemDialogCurrentItemQuantity--;
            itemDialogDecreaseQuantityImageButton.setEnabled(itemDialogCurrentItemQuantity > 0);
            updateDialog();
        });

        itemDialogIncreaseQuantityImageButton.setOnClickListener((v) -> {
            itemDialogCurrentItemQuantity++;
            itemDialogDecreaseQuantityImageButton.setEnabled(itemDialogCurrentItemQuantity > 0);
            updateDialog();
        });
    }

    private void perKgUncheckedSetup() {
        itemDialogKilogramsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence kg, int start, int before, int count) {
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void perLbUncheckedSetup() {
        itemDialogPoundsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence pounds, int start, int before, int count) {
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        itemDialogOuncesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence ounces, int start, int before, int count) {
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void onCheckedItemClick(ShoppingListItem item) {
        itemDialogCurrentItem = item;
        itemDialogCurrentItemQuantity = item.getQuantity();
        MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .customView(R.layout.dialog_buying_item, true)
                .positiveText(R.string.item_dialog_checked_positive)
                .neutralText(R.string.item_dialog_checked_neutral)
                .negativeText(R.string.item_dialog_checked_negative)
                .onPositive(new CheckedItemClickPositiveActionCallback())
                .onNeutral(new CheckedItemClickNeutralActionCallback())
                .build();
        View view = materialDialog.getCustomView();
        itemDialogPositiveAction = materialDialog.getActionButton(DialogAction.POSITIVE);

        assert view != null;
        itemDialogPricingRadioGroup = view.findViewById(R.id.radio_group_pricing);
        itemDialogPerUnitRadioButton = view.findViewById(R.id.rb_per_unit);
        itemDialogPerKgRadioButton = view.findViewById(R.id.rb_per_kg);
        itemDialogPerLbRadioButton = view.findViewById(R.id.rb_per_pound);
        itemDialogPriceTitleTextView = view.findViewById(R.id.tv_price_title);
        itemDialogIfPerUnitSelectedLayout = view.findViewById(R.id.ll_if_per_unit_selected);
        itemDialogIfPerKilogramSelectedLayout = view.findViewById(R.id.ll_if_per_kg_selected);
        itemDialogIfPerPoundSelectedLayout = view.findViewById(R.id.ll_if_per_lb_selected);
        itemDialogPriceEditText = view.findViewById(R.id.et_price);
        itemDialogDecreaseQuantityImageButton = view.findViewById(R.id.btn_decrease_quantity);
        itemDialogIncreaseQuantityImageButton = view.findViewById(R.id.btn_increase_quantity);
        itemDialogQuantityTextView = view.findViewById(R.id.tv_quantity);
        itemDialogKilogramsEditText = view.findViewById(R.id.et_kg);
        itemDialogPoundsEditText = view.findViewById(R.id.et_lb);
        itemDialogOuncesEditText = view.findViewById(R.id.et_oz);
        itemDialogTotalItemPriceNoTaxTextView = view.findViewById(R.id.tv_total_item_price_no_tax);
        itemDialogTaxTextView = view.findViewById(R.id.tv_tax);
        itemDialogTotalItemPriceTextView = view.findViewById(R.id.tv_total_item_price);
        itemDialogPriceWithoutTaxLinearLayout = view.findViewById(R.id.ll_total_price_no_tax);
        itemDialogTaxLinearLayout = view.findViewById(R.id.ll_tax);

        if (!mIncludeTax) {
            itemDialogPriceWithoutTaxLinearLayout.setVisibility(View.GONE);
            itemDialogTaxLinearLayout.setVisibility(View.GONE);
        }

        if (item.isPerUnitOrPerWeight() == ShoppingListItem.PER_UNIT) {
            itemDialogPerKgRadioButton.setEnabled(false);
            itemDialogPerLbRadioButton.setEnabled(false);
            itemDialogWhichRadioButtonChecked = R.id.rb_per_unit;
            perUnitCheckedSetup(item);
        } else {
            itemDialogIfPerUnitSelectedLayout.setVisibility(View.GONE);
            itemDialogIfPerKilogramSelectedLayout.setVisibility(View.VISIBLE);

            itemDialogPerUnitRadioButton.setEnabled(false);
            itemDialogPerUnitRadioButton.setChecked(false);
            itemDialogPerKgRadioButton.setChecked(true);
            itemDialogWhichRadioButtonChecked = R.id.rb_per_kg;
            perKgCheckedSetup(item);
            perLbCheckedSetup(item);
        }

        itemDialogPricingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            itemDialogWhichRadioButtonChecked = checkedId;
            switch (checkedId) {
                case R.id.rb_per_unit:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.GONE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_unit);
                    updateDialog();
                    break;
                case R.id.rb_per_kg:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.GONE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_kg);
                    BigDecimal pricePerPound =
                            new BigDecimal(itemDialogPriceEditText.getText().toString());
                    if (!TextUtils.isEmpty(itemDialogPriceEditText.getText())) {
                        String prefill =
                                getString(R.string.item_dialog_placeholder_price_edit_text,
                                        ShoppingListItem.getPricePerKilogram(pricePerPound));
                        itemDialogPriceEditText.setText(prefill);
                    }
                    if (!TextUtils.isEmpty(itemDialogPoundsEditText.getText()) && itemDialogWeightWasChanged) {
                        BigDecimal pounds = new BigDecimal(itemDialogPoundsEditText.getText().toString());
                        int ounces;
                        try {
                            ounces = Integer.parseInt(itemDialogOuncesEditText.getText().toString());
                        } catch (NumberFormatException e) {
                            ounces = 0;
                        }
                        BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);

                        // Need to use DecimalFormat to strip trailing zeros
                        String format =
                                getString(R.string.item_dialog_decimal_format_weight_edit_text);
                        DecimalFormat df = new DecimalFormat(format);
                        itemDialogKilogramsEditText.setText(df.format(kilograms));
                        itemDialogWeightWasChanged = false;
                    }
                    updateDialog();
                    break;
                case R.id.rb_per_pound:
                    itemDialogIfPerUnitSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerKilogramSelectedLayout.setVisibility(View.GONE);
                    itemDialogIfPerPoundSelectedLayout.setVisibility(View.VISIBLE);
                    itemDialogPriceTitleTextView.setText(R.string.price_per_lb);
                    BigDecimal pricePerKilogram = new BigDecimal(
                            itemDialogPriceEditText.getText().toString());
                    if (!TextUtils.isEmpty(itemDialogPriceEditText.getText())) {
                        String format =
                                getString(R.string.item_dialog_decimal_format_weight_edit_text);
                        DecimalFormat df = new DecimalFormat(format);
                        itemDialogPriceEditText.setText(
                                df.format(ShoppingListItem.getPricePerPound(pricePerKilogram)));
                    }
                    if (!TextUtils.isEmpty(itemDialogKilogramsEditText.getText()) && itemDialogWeightWasChanged) {
                        BigDecimal kilograms =
                                new BigDecimal(itemDialogKilogramsEditText.getText().toString());
                        BigDecimal pounds = ShoppingListItem.kilogramsToPounds(kilograms);

                        String format =
                                getString(R.string.item_dialog_decimal_format_weight_edit_text);
                        DecimalFormat df = new DecimalFormat(format);
                        itemDialogPoundsEditText.setText(df.format(pounds));
                        itemDialogOuncesEditText.setText("");
                        itemDialogWeightWasChanged = false;
                    }
                    updateDialog();
                    break;
            }
        });

        itemDialogPriceEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence pricePerUnit, int start, int before,
                                              int count) {
                        itemDialogPositiveAction.setEnabled(
                                !TextUtils.isEmpty(pricePerUnit) && itemDialogCurrentItemQuantity >= 1);
                        updateDialog();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );

        updateDialog();

        materialDialog.show();
    }

    private void perUnitCheckedSetup(ShoppingListItem item) {
        String prefill =
                getString(R.string.item_dialog_placeholder_price_edit_text, item.getPricePerUnit());
        itemDialogPriceEditText.setText(prefill);

        itemDialogDecreaseQuantityImageButton.setOnClickListener(v -> {
            itemDialogCurrentItemQuantity--;
            itemDialogDecreaseQuantityImageButton.setEnabled(itemDialogCurrentItemQuantity > 0);
            updateDialog();
        });

        itemDialogIncreaseQuantityImageButton.setOnClickListener(v -> {
            itemDialogCurrentItemQuantity++;
            itemDialogDecreaseQuantityImageButton.setEnabled(itemDialogCurrentItemQuantity > 0);
            updateDialog();
        });
    }

    private void perKgCheckedSetup(ShoppingListItem item) {
        String prefill =
                getString(R.string.item_dialog_placeholder_price_edit_text, item.getPricePerUnit());
        itemDialogPriceEditText.setText(prefill);

        // Need to use DecimalFormat to strip trailing zeros
        String format =
                getString(R.string.item_dialog_decimal_format_weight_edit_text);
        DecimalFormat df = new DecimalFormat(format);
        itemDialogKilogramsEditText.setText(df.format(item.getWeightInKilograms()));

        itemDialogKilogramsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence kg, int start, int before, int count) {
                itemDialogWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void perLbCheckedSetup(ShoppingListItem item) {
        String format =
                getString(R.string.item_dialog_decimal_format_weight_edit_text);
        DecimalFormat df = new DecimalFormat(format);
        itemDialogPoundsEditText.setText(df.format(item.getWeightInPounds()));

        itemDialogPoundsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence pounds, int start, int before, int count) {
                itemDialogWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        itemDialogOuncesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence ounces, int start, int before, int count) {
                itemDialogWeightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void onNotBuyingItemClick(ShoppingListItem item) {
        new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .content(R.string.item_dialog_not_buying_body)
                .positiveText(R.string.item_dialog_not_buying_positive)
                .negativeText(R.string.item_dialog_not_buying_negative)
                .onPositive((dialog, which) -> {
                    item.setStatus(ShoppingListItem.UNCHECKED);
                    mShoppingListAdapter.onDataChanged();
                })
                .show();
    }

    private void executeRetrieveItemsAction() {
        new RetrieveItemsTask(this).execute();
    }

    private void executeStoreItemsAction(ShoppingListItem[] items) {
        new StoreItemsTask(this).execute(items);
    }

    private static class SetupAutocompleteDictionaryTask extends AsyncTask<Void, Void, String[]> {
        private final WeakReference<MainActivity> ref;

        SetupAutocompleteDictionaryTask(MainActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected String[] doInBackground(Void... nothing) {
            MainActivity activity = ref.get();
            AutocompleteEntryDao dao = activity.mDatabase.autocompleteEntryDao();
            return dao.getAllEntries().toArray(new String[0]);
        }

        @Override
        protected void onPostExecute(String[] autocompleteDictionary) {
            super.onPostExecute(autocompleteDictionary);
            MainActivity activity = ref.get();
            activity.mAutocompleteDictionary = autocompleteDictionary;
        }
    }

    private static class AddToAutocompleteTask extends AsyncTask<String, Void, Void> {
        private final WeakReference<MainActivity> ref;

        AddToAutocompleteTask(MainActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            MainActivity activity = ref.get();
            AutocompleteEntryDao dao = activity.mDatabase.autocompleteEntryDao();
            AutocompleteEntry entry = new AutocompleteEntry(params[0]);
            dao.insertAll(entry);
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
            MainActivity activity = ref.get();
            activity.setupAutocompleteDictionary();
        }
    }

    private static class RetrieveItemsTask extends AsyncTask<Void, Void, List<ShoppingListItem>> {
        private final WeakReference<MainActivity> ref;

        RetrieveItemsTask(MainActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = ref.get();
            activity.mLoadingIndicatorConstraintLayout.setVisibility(View.VISIBLE);
            activity.mRecyclerView.setVisibility(View.GONE);
            activity.mErrorDisplayConstraintLayout.setVisibility(View.GONE);
        }

        @Override
        protected List<ShoppingListItem> doInBackground(Void... nothing) {
            MainActivity activity = ref.get();
            ShoppingListItemDao dao = activity.mDatabase.shoppingListItemDao();
            List<ShoppingListItemDatabaseEntity> entities = dao.getAllItems();
            List<ShoppingListItem> items = new ArrayList<>();
            for (ShoppingListItemDatabaseEntity entity : entities) {
                items.add(ShoppingListItem.getItemFromDatabaseEntity(entity));
            }
            return items;
        }

        @Override
        protected void onPostExecute(List<ShoppingListItem> shoppingListItems) {
            super.onPostExecute(shoppingListItems);
            MainActivity activity = ref.get();

            activity.mShoppingListAdapter.setAllItems(shoppingListItems);
            activity.mLoadingIndicatorConstraintLayout.setVisibility(View.GONE);
            activity.switchViews(shoppingListItems.isEmpty());
        }
    }

    private static class StoreItemsTask extends AsyncTask<ShoppingListItem, Void, Void> {
        private final WeakReference<MainActivity> ref;

        StoreItemsTask(MainActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(ShoppingListItem... shoppingListItems) {
            MainActivity activity = ref.get();
            ShoppingListItemDao dao = activity.mDatabase.shoppingListItemDao();
            ShoppingListItemDatabaseEntity[] entities =
                    new ShoppingListItemDatabaseEntity[shoppingListItems.length];
            for (int i = 0; i < entities.length; i++) {
                entities[i] = ShoppingListItem.itemAsDatabaseEntity(shoppingListItems[i]);
            }
            dao.deleteAllItems();
            dao.insertAll(entities);
            return null;
        }
    }

    private class UncheckedItemClickPositiveActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            itemDialogCurrentItem.setStatus(ShoppingListItem.CHECKED);

            switch (itemDialogWhichRadioButtonChecked) {
                case R.id.rb_per_unit:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_UNIT);
                    itemDialogCurrentItem.setPricePerUnit(
                            new BigDecimal(itemDialogPriceEditText.getText().toString()));
                    itemDialogCurrentItem.setQuantity(itemDialogCurrentItemQuantity);
                    break;
                case R.id.rb_per_kg:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    itemDialogCurrentItem.setPricePerUnit(
                            new BigDecimal(itemDialogPriceEditText.getText().toString()));
                    itemDialogCurrentItem.setWeightInKilograms(
                            new BigDecimal(itemDialogKilogramsEditText.getText().toString()));
                    break;
                case R.id.rb_per_pound:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    BigDecimal pricePerPound =
                            new BigDecimal(itemDialogPriceEditText.getText().toString());
                    BigDecimal pounds =
                            new BigDecimal(itemDialogPoundsEditText.getText().toString());
                    int ounces = TextUtils.isEmpty(itemDialogOuncesEditText.getText())
                            ? 0
                            : Integer.parseInt(itemDialogOuncesEditText.getText().toString());
                    BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                    itemDialogCurrentItem.setPricePerPound(pricePerPound);
                    itemDialogCurrentItem.setWeightInKilograms(kilograms);
                    break;
            }

            mShoppingListAdapter.onDataChanged();

            // If over budget, warn the user
            if (mBudgetIsSet && mShoppingListAdapter.getTotalPrice().compareTo(mBudget) > 0) {
                new MaterialDialog.Builder(MainActivity.this)
                        .content(R.string.over_budget_dialog_body)
                        .positiveText(R.string.over_budget_dialog_dismiss_button)
                        .show();
            }
        }
    }

    private class UncheckedItemClickNeutralActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            itemDialogCurrentItem.setStatus(ShoppingListItem.NOT_BUYING);
            mShoppingListAdapter.onDataChanged();
        }
    }

    private class CheckedItemClickPositiveActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            switch (itemDialogWhichRadioButtonChecked) {
                case R.id.rb_per_unit:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_UNIT);
                    itemDialogCurrentItem.setPricePerUnit(
                            new BigDecimal(itemDialogPriceEditText.getText().toString()));
                    itemDialogCurrentItem.setQuantity(itemDialogCurrentItemQuantity);
                    break;
                case R.id.rb_per_kg:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    itemDialogCurrentItem.setPricePerUnit(
                            new BigDecimal(itemDialogPriceEditText.getText().toString()));
                    itemDialogCurrentItem.setWeightInKilograms(
                            new BigDecimal(itemDialogKilogramsEditText.getText().toString()));
                    break;
                case R.id.rb_per_pound:
                    itemDialogCurrentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    BigDecimal pricePerPound = new BigDecimal(itemDialogPriceEditText.getText().toString());
                    BigDecimal pounds = new BigDecimal(itemDialogPoundsEditText.getText().toString());
                    int ounces;
                    try {
                        ounces = Integer.parseInt(itemDialogOuncesEditText.getText().toString());
                    } catch (NumberFormatException e) {
                        ounces = 0;
                    }
                    BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                    itemDialogCurrentItem.setPricePerPound(pricePerPound);
                    itemDialogCurrentItem.setWeightInKilograms(kilograms);
                    break;
            }

            mShoppingListAdapter.onDataChanged();
        }
    }

    private class CheckedItemClickNeutralActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            itemDialogCurrentItem.reset();
            mShoppingListAdapter.onDataChanged();
        }
    }
}
