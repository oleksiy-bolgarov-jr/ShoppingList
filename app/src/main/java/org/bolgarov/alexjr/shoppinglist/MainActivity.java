package org.bolgarov.alexjr.shoppinglist;

import android.arch.persistence.room.Room;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItemDatabaseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends
        AppCompatActivity
        implements
        ShoppingListAdapter.ShoppingListAdapterOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    /*
     * TODO: Next steps:
     * Implement settings
     * Add an autocomplete dictionary
     * Display more information on the items
     * Implement optional and condition
     * Add the ability to rearrange items
     * Improve the "delete all" option so you can delete from separate categories
     * Try to prevent user from entering negative values
     * Add a calculator to conveniently calculate price per kg given price per lb or vice versa, or price per weight/uint given total price and weight/unit
     * Add promotions (e.g. 3 for $6.00, etc.)
     * Save states to settings
     */

    private AppDatabase db;

    private RecyclerView mRecyclerView;
    private ShoppingListAdapter mShoppingListAdapter;

    private ConstraintLayout mErrorDisplay;
    private ConstraintLayout mLoadingIndicator;

    private FloatingActionButton mAddItemButton;

    private LinearLayout mFooter;
    private TextView mTotalPriceTextView;

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

    // Add new item dialog
    private AutoCompleteTextView itemNameEditText;
    private CheckBox saveItemCheckBox;
    private CheckBox optionalCheckBox;
    private EditText conditionEditText;
    private View addNewItemPositiveAction;
    private View addNewItemNeutralAction;

    // On select checked item dialog
    private RadioGroup pricingRadioGroup;
    private RadioButton perUnitRadioButton, perKgRadioButton, perLbRadioButton;
    private int whichRadioButtonChecked;
    private TextView priceTitleTextView;
    private LinearLayout ifPerUnitSelectedLayout, ifPerKilogramSelectedLayout,
            ifPerPoundSelectedLayout;
    private EditText priceEditText;
    private ImageButton decreaseQuantityImageButton, increaseQuantityImageButton;
    private TextView quantityTextView;
    private EditText kilogramsEditText;
    private EditText poundsEditText, ouncesEditText;
    private TextView totalItemPriceNoTaxTextView;
    private TextView taxTextView;
    private TextView totalItemPriceTextView;
    private LinearLayout priceWithoutTaxLinearLayout, taxLinearLayout;
    private View itemPositiveAction;
    private ShoppingListItem currentItem;
    private int currentItemQuantity;
    private boolean weightWasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_shopping_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);

        mShoppingListAdapter = new ShoppingListAdapter(this);

        mRecyclerView.setAdapter(mShoppingListAdapter);

        mFooter = (LinearLayout) findViewById(R.id.footer);
        mTotalPriceTextView = (TextView) findViewById(R.id.total_price_text_view);

        mErrorDisplay = (ConstraintLayout) findViewById(R.id.no_items_display);
        mLoadingIndicator = (ConstraintLayout) findViewById(R.id.loading_display);

        mAddItemButton = findViewById(R.id.add_item_fab);
        mAddItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewItemDialog();
            }
        });

        setupSharedPreferences();
        updateWarning();

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,
                "shopping_list_items").build();
        switchViews(mShoppingListAdapter.getItemCount() == 0);

        new RetrieveItemsTask().execute();
    }

    private void setupSharedPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        mBudgetIsSet = sp.getBoolean(
                getString(R.string.key_set_maximum_budget_checkbox),
                getResources().getBoolean(R.bool.pref_set_budget_default)
        );

        String budgetString = sp.getString(
                getString(R.string.key_maximum_budget_edit_text),
                getResources().getString(R.string.pref_maximum_budget_default)
        );
        mBudget = new BigDecimal(budgetString);

        mWarningIsSet = sp.getBoolean(
                getString(R.string.key_set_warning_checkbox),
                getResources().getBoolean(R.bool.pref_set_warning_default)
        );

        mWarningType = sp.getString(
                getString(R.string.key_warning_type_list),
                getString(R.string.warn_fixed_price_list_value)
        );

        String warningValueString = sp.getString(
                getString(R.string.key_warning_fixed_edit_text),
                getString(R.string.pref_warning_fixed_default)
        );
        mWarningFixedValue = new BigDecimal(warningValueString);

        mWarningPercentage = sp.getInt(
                getString(R.string.key_warning_percentage_seek_bar),
                getResources().getInteger(R.integer.pref_warning_percentage_default)
        );

        mIncludeTax = sp.getBoolean(
                getString(R.string.key_include_tax_checkbox),
                getResources().getBoolean(R.bool.pref_include_tax_default)
        );

        String taxRateString = sp.getString(
                getString(R.string.key_tax_rate_edit_text),
                getResources().getString(R.string.pref_tax_rate_default)
        );
        mTaxRate = new BigDecimal(taxRateString).multiply(new BigDecimal("0.01"));
        if (mIncludeTax) {
            ShoppingListItem.setTaxRate(mTaxRate);
        } else {
            ShoppingListItem.setTaxRate(new BigDecimal("0"));
        }

        sp.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateWarning() {
        if (mWarningType.equals(getString(R.string.warn_fixed_price_list_value))) {
            mWarningValue = mWarningFixedValue;
        } else {
            mWarningValue = mBudget.multiply(
                    new BigDecimal(mWarningPercentage)
                            .multiply(new BigDecimal("0.01")));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShoppingListItem[] items =
                mShoppingListAdapter.getAllItems().toArray(new ShoppingListItem[0]);
        new StoreItemsTask().execute(items);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                new MaterialDialog.Builder(this)
                        .title(R.string.delete_all)
                        .content(R.string.prompt_delete_all)
                        .positiveText(R.string.confirm_delete_all)
                        .negativeText(R.string.cancel_delete_all)
                        .onPositive((dialog, which) -> {
                            mShoppingListAdapter.deleteAll();
                            showToastMessage(
                                    "All entries have been deleted from the shopping list.");
                        })
                        .show();
                return true;
            case R.id.settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
            case R.id.fuck_shit_piss:
                // TODO: Once everything works properly, remove this case
                populateListWithDummyData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateListWithDummyData() {
        String[] dummyItemNames = {
                "Milk",
                "Eggs",
                "Bread",
                "Dog shit",
                "Cunt waffles",
                "Twat waffles",
                "Time machine",
                "Donkey testicles",
                "Fuck you and your mother",
                "Fish assholes",
                "Alarm clock",
                "Tablet",
                "Giant douche",
                "Turd sandwich",
                "EGgs",
                "Spam",
                "Fish sticks",
                "SPAM",
                "Earphones",
                "Giant meteors",
                "Cat litter",
                "$pam",
                "SpAm"
        };
        int numItems = dummyItemNames.length;
        ShoppingListItem[] dummyItems = new ShoppingListItem[numItems];
        for (int i = 0; i < numItems; i++) {
            dummyItems[i] = new ShoppingListItem(
                    dummyItemNames[i],
                    false,
                    null
            );
        }
        for (ShoppingListItem item : dummyItems) {
            mShoppingListAdapter.addItemToEndOfShoppingList(item);
        }
    }

    @Override
    public void switchViews(boolean listIsEmpty) {
        if (listIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mErrorDisplay.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mErrorDisplay.setVisibility(View.GONE);
        }
    }

    public void showAddNewItemDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .title(R.string.add_item_title)
                .customView(R.layout.shopping_list_dialog, true)
                .positiveText(R.string.add_another_item)
                .negativeText(R.string.cancel)
                .neutralText(R.string.add_this_and_finish)
                .onPositive(
                        (dialog, which) -> onPositiveAddNewItem(
                                itemNameEditText.getText().toString(),
                                optionalCheckBox.isChecked(),
                                conditionEditText.getText().toString(),
                                saveItemCheckBox.isChecked()
                        )
                )
                .onNeutral(
                        (dialog, which) -> onNeutralAddNewItem(
                                itemNameEditText.getText().toString(),
                                optionalCheckBox.isChecked(),
                                conditionEditText.getText().toString(),
                                saveItemCheckBox.isChecked()
                        )
                )
                .build();

        itemNameEditText = (AutoCompleteTextView) materialDialog.getCustomView()
                .findViewById(R.id.item_name_edit_text);
        saveItemCheckBox = (CheckBox) materialDialog.getCustomView()
                .findViewById(R.id.save_item_check_box);
        optionalCheckBox = (CheckBox) materialDialog.getCustomView()
                .findViewById(R.id.make_item_optional_check_box);
        conditionEditText = (EditText) materialDialog.getCustomView()
                .findViewById(R.id.condition_edit_text);

        // TODO: This just populates the autocomplete list with dummy data. Make it use correct data.
        String[] dummyAutocompleteData = {"Fuck", "Shit", "Piss", "Cunt", "Fart", "Rumpelstiltskin",
                "Fuck you and your mother", "Fuck you and your grandmother",
                "Fuck you and your sister", "Bitch"};
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                        dummyAutocompleteData);
        itemNameEditText.setAdapter(adapter);

        addNewItemPositiveAction = materialDialog.getActionButton(DialogAction.POSITIVE);
        addNewItemNeutralAction = materialDialog.getActionButton(DialogAction.NEUTRAL);
        addNewItemPositiveAction.setEnabled(false);
        addNewItemNeutralAction.setEnabled(false);
        itemNameEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // do nothing
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        addNewItemPositiveAction.setEnabled(s.toString().trim().length() > 0);
                        addNewItemNeutralAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // do nothing
                    }
                }
        );

        materialDialog.show();
    }

    private void addItem(String itemName, boolean optional, String condition, boolean save) {
        ShoppingListItem itemToAdd = new ShoppingListItem(
                itemName,
                optional,
                TextUtils.isEmpty(condition) ? null : condition
        );
        mShoppingListAdapter.addItemToEndOfShoppingList(itemToAdd);
        if (save) {
            showToastMessage("Saving has not been implemented yet.");
            // TODO Save this to the predictive text list
        }
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

    @Override
    public void onItemClick(int index) {
        List<ShoppingListItem> allItems = mShoppingListAdapter.getAllItemsOrderedByStatus();
        ShoppingListItem item = allItems.get(index);
        if (item.getStatus() == ShoppingListItem.UNCHECKED) {
            onUncheckedItemClick(item);
        } else if (item.getStatus() == ShoppingListItem.CHECKED) {
            onCheckedItemClick(item);
        } else {
            onNotBuyingItemClick(item);
        }
    }

    private void onUncheckedItemClick(ShoppingListItem item) {
        currentItem = item;
        currentItemQuantity = 0;
        MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .customView(R.layout.buying_item_dialog, true)
                .positiveText(R.string.confirm)
                .neutralText(R.string.not_buying)
                .negativeText(R.string.cancel)
                .onPositive(new UncheckedItemClickPositiveActionCallback())
                .onNeutral(new UncheckedItemClickNeutralActionCallback())
                .build();
        View view = materialDialog.getCustomView();
        itemPositiveAction = materialDialog.getActionButton(DialogAction.POSITIVE);

        pricingRadioGroup = view.findViewById(R.id.pricing_radio_group);
        perUnitRadioButton = view.findViewById(R.id.per_unit);
        perKgRadioButton = view.findViewById(R.id.per_kg);
        perLbRadioButton = view.findViewById(R.id.per_pound);
        priceTitleTextView = view.findViewById(R.id.price_title);
        ifPerUnitSelectedLayout = view.findViewById(R.id.if_per_unit_selected);
        ifPerKilogramSelectedLayout = view.findViewById(R.id.if_per_kg_selected);
        ifPerPoundSelectedLayout = view.findViewById(R.id.if_per_lb_selected);
        priceEditText = view.findViewById(R.id.price);
        decreaseQuantityImageButton = view.findViewById(R.id.decrease_quantity_button);
        increaseQuantityImageButton = view.findViewById(R.id.increase_quantity_button);
        quantityTextView = view.findViewById(R.id.quantity);
        kilogramsEditText = view.findViewById(R.id.kg);
        poundsEditText = view.findViewById(R.id.lb);
        ouncesEditText = view.findViewById(R.id.oz);
        totalItemPriceNoTaxTextView = view.findViewById(R.id.total_item_price_no_tax_text_view);
        taxTextView = view.findViewById(R.id.tax_text_view);
        totalItemPriceTextView = view.findViewById(R.id.total_item_price_text_view);
        priceWithoutTaxLinearLayout = view.findViewById(R.id.total_price_no_tax_linear_layout);
        taxLinearLayout = view.findViewById(R.id.tax_linear_layout);

        if (!mIncludeTax) {
            priceWithoutTaxLinearLayout.setVisibility(View.GONE);
            taxLinearLayout.setVisibility(View.GONE);
        }

        itemPositiveAction.setEnabled(false);  // User needs to add the proper values first

        whichRadioButtonChecked = R.id.per_unit;    // This is the default checked radio button

        pricingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            whichRadioButtonChecked = checkedId;
            switch (checkedId) {
                case R.id.per_unit:
                    ifPerUnitSelectedLayout.setVisibility(View.VISIBLE);
                    ifPerKilogramSelectedLayout.setVisibility(View.GONE);
                    ifPerPoundSelectedLayout.setVisibility(View.GONE);
                    priceTitleTextView.setText(R.string.price_per_unit);
                    updateDialog();
                    break;
                case R.id.per_kg:
                    ifPerUnitSelectedLayout.setVisibility(View.GONE);
                    ifPerKilogramSelectedLayout.setVisibility(View.VISIBLE);
                    ifPerPoundSelectedLayout.setVisibility(View.GONE);
                    priceTitleTextView.setText(R.string.price_per_kg);
                    updateDialog();
                    break;
                case R.id.per_pound:
                    ifPerUnitSelectedLayout.setVisibility(View.GONE);
                    ifPerKilogramSelectedLayout.setVisibility(View.GONE);
                    ifPerPoundSelectedLayout.setVisibility(View.VISIBLE);
                    priceTitleTextView.setText(R.string.price_per_lb);
                    updateDialog();
                    break;
            }
        });

        priceEditText.addTextChangedListener(
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

        materialDialog.show();
    }

    private void updateDialog() {
        quantityTextView.setText("" + currentItemQuantity);
        if (!TextUtils.isEmpty(priceEditText.getText())) {
            BigDecimal pricePerUnit = new BigDecimal(priceEditText.getText().toString());
            BigDecimal totalPriceWithoutTax, tax, totalPrice;
            switch (whichRadioButtonChecked) {
                case R.id.per_unit:
                    if (currentItemQuantity >= 1) {
                        itemPositiveAction.setEnabled(true);
                        totalPriceWithoutTax =
                                pricePerUnit.multiply(new BigDecimal(currentItemQuantity));
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                        totalItemPriceNoTaxTextView.setText("$" +
                                totalPriceWithoutTax.setScale(2, RoundingMode.HALF_UP));
                        taxTextView.setText("+ $" + tax.setScale(2, RoundingMode.HALF_UP));
                        totalItemPriceTextView.setText("$" +
                                totalPrice.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        itemPositiveAction.setEnabled(false);
                    }
                    break;
                case R.id.per_kg:
                    if (!TextUtils.isEmpty(kilogramsEditText.getText())) {
                        itemPositiveAction.setEnabled(true);
                        BigDecimal kilograms =
                                new BigDecimal(kilogramsEditText.getText().toString());
                        totalPriceWithoutTax = pricePerUnit.multiply(kilograms);
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                        totalItemPriceNoTaxTextView.setText("$" +
                                totalPriceWithoutTax.setScale(2, RoundingMode.HALF_UP));
                        taxTextView.setText("+ $" + tax.setScale(2, RoundingMode.HALF_UP));
                        totalItemPriceTextView.setText("$" +
                                totalPrice.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        itemPositiveAction.setEnabled(false);
                    }
                    break;
                case R.id.per_pound:
                    if (!TextUtils.isEmpty(poundsEditText.getText())) {
                        itemPositiveAction.setEnabled(true);
                        BigDecimal pounds = new BigDecimal(poundsEditText.getText().toString());
                        int ounces;
                        if (!TextUtils.isEmpty(ouncesEditText.getText())) {
                            ounces = Integer.parseInt(ouncesEditText.getText().toString());
                        } else {
                            ounces = 0;
                        }
                        BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                        BigDecimal pricePerKilogram =
                                ShoppingListItem.getPricePerKilogram(pricePerUnit);
                        totalPriceWithoutTax = pricePerKilogram.multiply(kilograms);
                        tax = ShoppingListItem.getTax(totalPriceWithoutTax);
                        totalPrice = ShoppingListItem.getTaxAdjustedPrice(totalPriceWithoutTax);
                        totalItemPriceNoTaxTextView.setText("$" +
                                totalPriceWithoutTax.setScale(2, RoundingMode.HALF_UP));
                        taxTextView.setText("+ $" + tax.setScale(2, RoundingMode.HALF_UP));
                        totalItemPriceTextView.setText("$" +
                                totalPrice.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        itemPositiveAction.setEnabled(false);
                    }
                    break;
            }
        } else {
            itemPositiveAction.setEnabled(false);
        }
    }

    private void perUnitUncheckedSetup() {
        decreaseQuantityImageButton.setOnClickListener((v) -> {
            currentItemQuantity--;
            decreaseQuantityImageButton.setEnabled(currentItemQuantity > 0);
            updateDialog();
        });

        increaseQuantityImageButton.setOnClickListener((v) -> {
            currentItemQuantity++;
            decreaseQuantityImageButton.setEnabled(currentItemQuantity > 0);
            updateDialog();
        });
    }

    private void perKgUncheckedSetup() {
        kilogramsEditText.addTextChangedListener(new TextWatcher() {
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
        poundsEditText.addTextChangedListener(new TextWatcher() {
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

        ouncesEditText.addTextChangedListener(new TextWatcher() {
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
        currentItem = item;
        currentItemQuantity = item.getQuantity();
        MaterialDialog materialDialog = new MaterialDialog.Builder(this)
                .title(item.getItemName())
                .customView(R.layout.buying_item_dialog, true)
                .positiveText(R.string.save_changes)
                .neutralText(R.string.move_to_top)
                .negativeText(R.string.cancel)
                .onPositive(new CheckedItemClickPositiveActionCallback())
                .onNeutral(new CheckedItemClickNeutralActionCallback())
                .build();
        View view = materialDialog.getCustomView();
        itemPositiveAction = materialDialog.getActionButton(DialogAction.POSITIVE);

        pricingRadioGroup = view.findViewById(R.id.pricing_radio_group);
        perUnitRadioButton = view.findViewById(R.id.per_unit);
        perKgRadioButton = view.findViewById(R.id.per_kg);
        perLbRadioButton = view.findViewById(R.id.per_pound);
        priceTitleTextView = view.findViewById(R.id.price_title);
        ifPerUnitSelectedLayout = view.findViewById(R.id.if_per_unit_selected);
        ifPerKilogramSelectedLayout = view.findViewById(R.id.if_per_kg_selected);
        ifPerPoundSelectedLayout = view.findViewById(R.id.if_per_lb_selected);
        priceEditText = view.findViewById(R.id.price);
        decreaseQuantityImageButton = view.findViewById(R.id.decrease_quantity_button);
        increaseQuantityImageButton = view.findViewById(R.id.increase_quantity_button);
        quantityTextView = view.findViewById(R.id.quantity);
        kilogramsEditText = view.findViewById(R.id.kg);
        poundsEditText = view.findViewById(R.id.lb);
        ouncesEditText = view.findViewById(R.id.oz);
        totalItemPriceNoTaxTextView = view.findViewById(R.id.total_item_price_no_tax_text_view);
        taxTextView = view.findViewById(R.id.tax_text_view);
        totalItemPriceTextView = view.findViewById(R.id.total_item_price_text_view);
        priceWithoutTaxLinearLayout = view.findViewById(R.id.total_price_no_tax_linear_layout);
        taxLinearLayout = view.findViewById(R.id.tax_linear_layout);

        if (!mIncludeTax) {
            priceWithoutTaxLinearLayout.setVisibility(View.GONE);
            taxLinearLayout.setVisibility(View.GONE);
        }

        if (item.isPerUnitOrPerWeight() == ShoppingListItem.PER_UNIT) {
            perKgRadioButton.setEnabled(false);
            perLbRadioButton.setEnabled(false);
            whichRadioButtonChecked = R.id.per_unit;
            perUnitCheckedSetup(item);
        } else {
            ifPerUnitSelectedLayout.setVisibility(View.GONE);
            ifPerKilogramSelectedLayout.setVisibility(View.VISIBLE);

            perUnitRadioButton.setEnabled(false);
            perUnitRadioButton.setChecked(false);
            perKgRadioButton.setChecked(true);
            whichRadioButtonChecked = R.id.per_kg;
            perKgCheckedSetup(item);
            perLbCheckedSetup(item);
        }

        pricingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            whichRadioButtonChecked = checkedId;
            switch (checkedId) {
                case R.id.per_unit:
                    ifPerUnitSelectedLayout.setVisibility(View.VISIBLE);
                    ifPerKilogramSelectedLayout.setVisibility(View.GONE);
                    ifPerPoundSelectedLayout.setVisibility(View.GONE);
                    priceTitleTextView.setText(R.string.price_per_unit);
                    updateDialog();
                    break;
                case R.id.per_kg:
                    ifPerUnitSelectedLayout.setVisibility(View.GONE);
                    ifPerKilogramSelectedLayout.setVisibility(View.VISIBLE);
                    ifPerPoundSelectedLayout.setVisibility(View.GONE);
                    priceTitleTextView.setText(R.string.price_per_kg);
                    BigDecimal pricePerPound = new BigDecimal(priceEditText.getText().toString());
                    if (!TextUtils.isEmpty(priceEditText.getText())) {
                        priceEditText.setText(
                                ShoppingListItem.getPricePerKilogram(pricePerPound)
                                        .setScale(2, RoundingMode.HALF_UP)
                                        .toString()
                        );
                    }
                    if (!TextUtils.isEmpty(poundsEditText.getText()) && weightWasChanged) {
                        BigDecimal pounds = new BigDecimal(poundsEditText.getText().toString());
                        int ounces;
                        try {
                            ounces = Integer.parseInt(ouncesEditText.getText().toString());
                        } catch (NumberFormatException e) {
                            ounces = 0;
                        }
                        BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                        kilogramsEditText.setText(
                                kilograms.setScale(3, RoundingMode.HALF_UP).toString());
                        weightWasChanged = false;
                    }
                    updateDialog();
                    break;
                case R.id.per_pound:
                    ifPerUnitSelectedLayout.setVisibility(View.GONE);
                    ifPerKilogramSelectedLayout.setVisibility(View.GONE);
                    ifPerPoundSelectedLayout.setVisibility(View.VISIBLE);
                    priceTitleTextView.setText(R.string.price_per_lb);
                    BigDecimal pricePerKilogram = new BigDecimal(
                            priceEditText.getText().toString());
                    if (!TextUtils.isEmpty(priceEditText.getText())) {
                        priceEditText.setText(
                                ShoppingListItem.getPricePerPound(pricePerKilogram)
                                        .setScale(2, RoundingMode.HALF_UP)
                                        .toString()
                        );
                    }
                    if (!TextUtils.isEmpty(kilogramsEditText.getText()) && weightWasChanged) {
                        BigDecimal kilograms =
                                new BigDecimal(kilogramsEditText.getText().toString());
                        BigDecimal pounds = ShoppingListItem.kilogramsToPounds(kilograms);
                        poundsEditText.setText(pounds.setScale(3, RoundingMode.HALF_UP).toString());
                        ouncesEditText.setText("");
                        weightWasChanged = false;
                    }
                    updateDialog();
                    break;
            }
        });

        priceEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence pricePerUnit, int start, int before,
                                              int count) {
                        itemPositiveAction.setEnabled(
                                !TextUtils.isEmpty(pricePerUnit) && currentItemQuantity >= 1);
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
        priceEditText.setText(item.getPricePerUnit().toString());

        decreaseQuantityImageButton.setOnClickListener(v -> {
            currentItemQuantity--;
            decreaseQuantityImageButton.setEnabled(currentItemQuantity > 0);
            updateDialog();
        });

        increaseQuantityImageButton.setOnClickListener(v -> {
            currentItemQuantity++;
            decreaseQuantityImageButton.setEnabled(currentItemQuantity > 0);
            updateDialog();
        });
    }

    private void perKgCheckedSetup(ShoppingListItem item) {
        priceEditText.setText(item.getPricePerUnit().toString());
        kilogramsEditText.setText(
                item.getWeightInKilograms().setScale(3, RoundingMode.HALF_UP).toString());

        kilogramsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence kg, int start, int before, int count) {
                weightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void perLbCheckedSetup(ShoppingListItem item) {
        poundsEditText.setText(
                item.getWeightInPounds().setScale(3, RoundingMode.HALF_UP).toString());

        poundsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence pounds, int start, int before, int count) {
                weightWasChanged = true;
                updateDialog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ouncesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence ounces, int start, int before, int count) {
                weightWasChanged = true;
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
                .content(R.string.prompt_restore)
                .positiveText(R.string.confirm_restore)
                .negativeText(R.string.cancel_restore)
                .onPositive((dialog, which) -> {
                    item.setStatus(ShoppingListItem.UNCHECKED);
                    mShoppingListAdapter.onDataChanged();
                })
                .show();
    }

    /**
     * Sets the total price in the textview.
     * TODO: Add limit, colouring, and everything else
     *
     * @param price
     */
    public void updateTotalPrice(BigDecimal price) {
        String priceString = "$" + price.setScale(2, RoundingMode.HALF_UP);
        String budgetString = mBudgetIsSet ?
                " / $" + mBudget.setScale(2, RoundingMode.HALF_UP) :
                "";
        mTotalPriceTextView.setText(priceString + budgetString);

        if (mBudgetIsSet) {
            if (price.compareTo(mBudget) > 0) {
                // i.e. if price > mBudget
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_over_budget));
            } else if (mWarningIsSet && price.compareTo(mWarningValue) > 0) {
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_warning));
            } else {
                mTotalPriceTextView.setTextColor(
                        getResources().getColor(R.color.text_color_price_normal));
            }
        }
    }

    /**
     * This method is for testing purposes only. Use it to demonstrate that something works.
     */
    private void genericAction() {
        Toast.makeText(this, "This has not been implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void showToastMessage(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_set_maximum_budget_checkbox))) {
            mBudgetIsSet = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_set_budget_default));
        } else if (key.equals(getString(R.string.key_maximum_budget_edit_text))) {
            mBudget = new BigDecimal(sharedPreferences.getString(key,
                    getResources().getString(R.string.pref_maximum_budget_default)));
        } else if (key.equals(getString(R.string.key_set_warning_checkbox))) {
            mWarningIsSet = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_set_warning_default));
        } else if (key.equals(getString(R.string.key_warning_type_list))) {
            mWarningType = sharedPreferences.getString(key,
                    getResources().getString(R.string.warn_fixed_price_list_value));
        } else if (key.equals(getString(R.string.key_warning_fixed_edit_text))) {
            mWarningFixedValue =
                    new BigDecimal(sharedPreferences.getString(key,
                            getResources().getString(R.string.pref_warning_fixed_default)));
        } else if (key.equals(getString(R.string.key_warning_percentage_seek_bar))) {
            mWarningPercentage = sharedPreferences.getInt(key,
                    getResources().getInteger(R.integer.pref_warning_percentage_default));
        } else if (key.equals(getString(R.string.key_include_tax_checkbox))) {
            mIncludeTax = sharedPreferences.getBoolean(key,
                    getResources().getBoolean(R.bool.pref_include_tax_default));
            if (mIncludeTax) {
                ShoppingListItem.setTaxRate(mTaxRate);
            } else {
                ShoppingListItem.setTaxRate(new BigDecimal("0"));
            }
        } else if (key.equals(getString(R.string.key_tax_rate_edit_text))) {
            mTaxRate = new BigDecimal(sharedPreferences.getString(key,
                    getResources().getString(R.string.pref_tax_rate_default)))
                    .multiply(new BigDecimal("0.01"));
            if (mIncludeTax) {
                ShoppingListItem.setTaxRate(mTaxRate);
            } else {
                ShoppingListItem.setTaxRate(new BigDecimal("0"));
            }
        }

        updateWarning();
        updateTotalPrice(mShoppingListAdapter.getTotalPrice());
    }

    public class RetrieveItemsTask extends AsyncTask<Void, Void, List<ShoppingListItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mErrorDisplay.setVisibility(View.GONE);
        }

        @Override
        protected List<ShoppingListItem> doInBackground(Void... voids) {
            ShoppingListItemDao dao = db.dao();
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
            mShoppingListAdapter.setAllItems(shoppingListItems);
            mLoadingIndicator.setVisibility(View.GONE);
            switchViews(shoppingListItems.isEmpty());
        }
    }

    public class StoreItemsTask extends AsyncTask<ShoppingListItem, Void, Void> {
        @Override
        protected Void doInBackground(ShoppingListItem... shoppingListItems) {
            ShoppingListItemDao dao = db.dao();
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
            currentItem.setStatus(ShoppingListItem.CHECKED);

            switch (whichRadioButtonChecked) {
                case R.id.per_unit:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_UNIT);
                    currentItem.setPricePerUnit(
                            new BigDecimal(priceEditText.getText().toString()));
                    currentItem.setQuantity(currentItemQuantity);
                    break;
                case R.id.per_kg:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    currentItem.setPricePerUnit(
                            new BigDecimal(priceEditText.getText().toString()));
                    currentItem.setWeightInKilograms(
                            new BigDecimal(kilogramsEditText.getText().toString()));
                    break;
                case R.id.per_pound:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    BigDecimal pricePerPound = new BigDecimal(priceEditText.getText().toString());
                    BigDecimal pounds = new BigDecimal(poundsEditText.getText().toString());
                    int ounces = TextUtils.isEmpty(ouncesEditText.getText())
                            ? 0
                            : Integer.parseInt(ouncesEditText.getText().toString());
                    BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                    currentItem.setPricePerPound(pricePerPound);
                    currentItem.setWeightInKilograms(kilograms);
                    break;
            }

            mShoppingListAdapter.onDataChanged();

            // If over budget, warn the user
            if (mBudgetIsSet && mShoppingListAdapter.getTotalPrice().compareTo(mBudget) > 0) {
                new MaterialDialog.Builder(MainActivity.this)
                        .content(R.string.over_budget_warning_message)
                        .positiveText(R.string.ok)
                        .show();
            }
        }
    }

    private class UncheckedItemClickNeutralActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            currentItem.setStatus(ShoppingListItem.NOT_BUYING);
            mShoppingListAdapter.onDataChanged();
        }
    }

    private class CheckedItemClickPositiveActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            switch (whichRadioButtonChecked) {
                case R.id.per_unit:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_UNIT);
                    currentItem.setPricePerUnit(
                            new BigDecimal(priceEditText.getText().toString()));
                    currentItem.setQuantity(currentItemQuantity);
                    break;
                case R.id.per_kg:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    currentItem.setPricePerUnit(
                            new BigDecimal(priceEditText.getText().toString()));
                    currentItem.setWeightInKilograms(
                            new BigDecimal(kilogramsEditText.getText().toString()));
                    break;
                case R.id.per_pound:
                    currentItem.setPerUnitOrPerWeight(ShoppingListItem.PER_WEIGHT);
                    BigDecimal pricePerPound = new BigDecimal(priceEditText.getText().toString());
                    BigDecimal pounds = new BigDecimal(poundsEditText.getText().toString());
                    int ounces;
                    try {
                        ounces = Integer.parseInt(ouncesEditText.getText().toString());
                    } catch (NumberFormatException e) {
                        ounces = 0;
                    }
                    BigDecimal kilograms = ShoppingListItem.poundsToKilograms(pounds, ounces);
                    currentItem.setPricePerPound(pricePerPound);
                    currentItem.setWeightInKilograms(kilograms);
                    break;
            }

            mShoppingListAdapter.onDataChanged();
        }
    }

    private class CheckedItemClickNeutralActionCallback
            implements MaterialDialog.SingleButtonCallback {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            currentItem.reset();
            currentItem.setStatus(ShoppingListItem.UNCHECKED);
            mShoppingListAdapter.onDataChanged();
        }
    }
}
