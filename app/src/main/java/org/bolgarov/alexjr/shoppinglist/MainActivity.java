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
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.dialogs.AddItemDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.DeleteAllItemsDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.ItemClickDialogListener;
import org.bolgarov.alexjr.shoppinglist.dialogs.OnCheckedItemClickDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.OnConditionedItemClickDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.OnNotBuyingItemClickDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.OnUncheckedItemClickDialogFragment;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.List;

public class MainActivity
        extends
        AppCompatActivity
        implements
        ShoppingListAdapter.ShoppingListAdapterOnClickHandler,
        AddItemDialogFragment.AddItemDialogListener,
        OnConditionedItemClickDialogFragment.OnConditionedItemClickDialogListener,
        ItemClickDialogListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Debug tag for logging
     */
    @SuppressWarnings("unused")
    public static final String TAG = MainActivity.class.getSimpleName();

    private ShoppingListAdapter mShoppingListAdapter;

    // Main view
    private RecyclerView mRecyclerView;
    private ConstraintLayout mErrorConstraintLayout;
    private ConstraintLayout mLoadingIndicatorConstraintLayout;

    // Footer
    private LinearLayout mFootnotes;
    private TextView mTotalPriceTextView;
    private TextView mOverBudgetWarningTextView;
    private TextView mOptionalTextView;
    private TextView mConditionTextView;

    // Settings
    private boolean mBudgetIsSet;
    private BigDecimal mBudget;
    private boolean mWarningIsSet;
    private String mWarningType;
    private BigDecimal mWarningFixedValue;
    private int mWarningPercentage;
    private BigDecimal mWarningValue;
    private boolean mIncludeTax;
    private BigDecimal mTaxRate;

    // Autocomplete
    private boolean mAutocompleteEnabled;
    private String[] mAutocompleteDictionary;

    /*
     * TODO: Next steps:
     * Add a "delete multiple" option
     * Add a calculator to conveniently calculate price per kg given price per lb or vice versa, or price per weight/unit given total price and weight/unit
     * Add promotions (e.g. 3 for $6.00, etc.)
     * Add categories
     * OPTIONAL: Use CardViews as list items
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recycler_view_shopping_list);
        mErrorConstraintLayout = findViewById(R.id.constraint_layout_no_items_display);
        mLoadingIndicatorConstraintLayout = findViewById(
                R.id.constraint_layout_loading_display_main);

        mFootnotes = findViewById(R.id.linear_layout_footnotes);
        mTotalPriceTextView = findViewById(R.id.tv_total_price);
        mOverBudgetWarningTextView = findViewById(R.id.tv_over_budget_footer);
        mOptionalTextView = findViewById(R.id.tv_optional_footnote);
        mConditionTextView = findViewById(R.id.tv_condition_footnote);

        FloatingActionButton addItemButton = findViewById(R.id.fab_add_item);
        addItemButton.setOnClickListener(v -> onAddItemButtonClick());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mShoppingListAdapter = new ShoppingListAdapter(this, this);
        mRecyclerView.setAdapter(mShoppingListAdapter);

        new RetrieveItemsTask(this).execute();

        setupSharedPreferences();
        updateWarning();
    }

    @Override
    protected void onStart() {
        super.onStart();
        new SetupAutocompleteDictionaryTask(this).execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new RetrieveItemsTask(this).execute();
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
                DialogFragment dialog = new DeleteAllItemsDialogFragment();
                dialog.show(getSupportFragmentManager(), "DeleteAllItemsDialogFragment");
                return true;
            case R.id.option_rearrange_items:
                Intent startRearrangeActivity = new Intent(this, RearrangeItemsActivity.class);
                startActivity(startRearrangeActivity);
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

    public void switchViews(boolean listIsEmpty) {
        if (listIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mErrorConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mErrorConstraintLayout.setVisibility(View.GONE);
        }
    }

    public void updateTotalPrice(BigDecimal price) {
        String totalString;
        int colorId;
        if (mBudgetIsSet) {
            totalString = getString(
                    R.string.main_activity_placeholder_footer_total_with_budget,
                    price,
                    mBudget
            );

            colorId = price.compareTo(mBudget) > 0 ?
                    R.color.text_color_price_over_budget :
                    mWarningIsSet && price.compareTo(mWarningValue) > 0 ?
                            R.color.text_color_price_warning :
                            R.color.text_color_price_normal;
            mOverBudgetWarningTextView.setVisibility(
                    price.compareTo(mBudget) > 0 ?
                            View.VISIBLE :
                            View.GONE
            );
        } else {
            totalString = getString(
                    R.string.main_activity_placeholder_footer_total_no_budget,
                    price
            );
            colorId = R.color.text_color_price_normal;
            mOverBudgetWarningTextView.setVisibility(View.GONE);
        }
        mTotalPriceTextView.setTextColor(getResources().getColor(colorId));
        mTotalPriceTextView.setText(totalString);
    }

    public void showFootnotes(boolean optionalItemsExist, boolean conditionedItemsExist) {
        if (optionalItemsExist || conditionedItemsExist) {
            mFootnotes.setVisibility(View.VISIBLE);
            mOptionalTextView.setVisibility(optionalItemsExist ? View.VISIBLE : View.GONE);
            mConditionTextView.setVisibility(conditionedItemsExist ? View.VISIBLE : View.GONE);
        } else {
            mFootnotes.setVisibility(View.GONE);
        }
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

    /**
     * Action to be executed when a shopping list item is clicked.
     *
     * @param position The index of the item that was clicked
     */
    @Override
    public void onItemClick(int position) {
        List<ShoppingListItem> items = mShoppingListAdapter.getAllItemsOrderedByStatus();
        ShoppingListItem item = items.get(position);
        switch (item.getStatus()) {
            case ShoppingListItem.UNCHECKED:
                if (item.hasCondition()) onConditionedItemClick(item);
                else onUncheckedItemClick(item);
                break;
            case ShoppingListItem.CHECKED:
                onCheckedItemClick(item);
                break;
            default:    // NOT_BUYING
                onNotBuyingItemClick(item);
        }
    }

    /**
     * Action to be executed when a shopping list item with a condition is clicked. The item must
     * have a non-null, nonempty condition.
     *
     * @param item The item that is clicked
     */
    private void onConditionedItemClick(ShoppingListItem item) {
        if (!item.hasCondition()) {
            throw new IllegalArgumentException(
                    "Item must have a non-null, nonempty condition specified.");
        }

        OnConditionedItemClickDialogFragment dialog = new OnConditionedItemClickDialogFragment();
        dialog.setItem(item);
        dialog.show(getSupportFragmentManager(), "OnConditionedItemClickDialogFragment");
    }

    public void onUncheckedItemClick(ShoppingListItem item) {
        OnUncheckedItemClickDialogFragment dialog = new OnUncheckedItemClickDialogFragment();
        dialog.setItem(item);
        dialog.show(getSupportFragmentManager(), "OnUncheckedItemClickDialogFragment");
    }

    private void onCheckedItemClick(ShoppingListItem item) {
        OnCheckedItemClickDialogFragment dialog = new OnCheckedItemClickDialogFragment();
        dialog.setItem(item);
        dialog.show(getSupportFragmentManager(), "OnCheckedItemClickDialogFragment");
    }

    private void onNotBuyingItemClick(ShoppingListItem item) {
        OnNotBuyingItemClickDialogFragment dialog = new OnNotBuyingItemClickDialogFragment();
        dialog.setItem(item);
        dialog.show(getSupportFragmentManager(), "OnNotBuyingItemClickDialogFragment");
    }

    public void onAddItemButtonClick() {
        AddItemDialogFragment dialog = new AddItemDialogFragment();
        dialog.setAutocompleteDictionary(
                mAutocompleteEnabled ?
                        mAutocompleteDictionary :
                        new String[0]
        );
        dialog.show(getSupportFragmentManager(), "AddItemDialogFragment");
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateAutocompleteDictionary() {
        new SetupAutocompleteDictionaryTask(this).execute();
    }

    @Override
    public ShoppingListAdapter getAdapter() {
        return mShoppingListAdapter;
    }

    @Override
    public boolean isBudgetSet() {
        return mBudgetIsSet;
    }

    @Override
    public BigDecimal getBudget() {
        return mBudget;
    }

    @Override
    public boolean isTaxIncluded() {
        return mIncludeTax;
    }

    private static class RetrieveItemsTask extends AsyncTask<Void, Void, List<ShoppingListItem>> {
        private final WeakReference<MainActivity> ref;

        RetrieveItemsTask(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity mainActivity = ref.get();
            mainActivity.mLoadingIndicatorConstraintLayout.setVisibility(View.VISIBLE);
            mainActivity.mRecyclerView.setVisibility(View.GONE);
            mainActivity.mErrorConstraintLayout.setVisibility(View.GONE);
        }

        @Override
        protected List<ShoppingListItem> doInBackground(Void... nothing) {
            return AppDatabase.getDatabaseInstance(ref.get())
                    .shoppingListItemDao()
                    .getAllItems();
        }

        @Override
        protected void onPostExecute(List<ShoppingListItem> shoppingListItems) {
            super.onPostExecute(shoppingListItems);

            MainActivity mainActivity = ref.get();
            mainActivity.mShoppingListAdapter.setItemList(shoppingListItems);

            // Hide the loading indicator and show the list of items or a message saying there are
            // no items
            mainActivity.mLoadingIndicatorConstraintLayout.setVisibility(View.GONE);
            mainActivity.switchViews(shoppingListItems.isEmpty());
        }
    }

    private static class SetupAutocompleteDictionaryTask extends AsyncTask<Void, Void, String[]> {
        private final WeakReference<MainActivity> ref;

        SetupAutocompleteDictionaryTask(MainActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected String[] doInBackground(Void... nothing) {
            return AppDatabase.getDatabaseInstance(ref.get())
                    .autocompleteEntryDao()
                    .getAllEntries()
                    .toArray(new String[0]);
        }

        @Override
        protected void onPostExecute(String[] dictionary) {
            super.onPostExecute(dictionary);
            MainActivity mainActivity = ref.get();
            mainActivity.mAutocompleteDictionary = dictionary;
        }
    }
}