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
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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
import android.widget.RadioButton;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class AddItemDialogFragment extends DialogFragment {

    private static final String TAG = AddItemDialogFragment.class.getSimpleName();

    private AddItemDialogListener mListener;

    private RadioButton mExtendedItemRadioButton;

    private Button mPositiveButton, mNeutralButton;

    private String[] mAutocompleteDictionary;

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            mPositiveButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
            mNeutralButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);

            mPositiveButton.setEnabled(false);
            mNeutralButton.setEnabled(false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_add_new_item, null);
        AutoCompleteTextView itemNameEditText = view.findViewById(R.id.item_name_edit_text);
        CheckBox saveItemCheckBox = view.findViewById(R.id.save_item_check_box);
        CheckBox optionalCheckBox = view.findViewById(R.id.make_item_optional_check_box);
        EditText conditionEditText = view.findViewById(R.id.condition_edit_text);

        mExtendedItemRadioButton = view.findViewById(R.id.rb_extended_item);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                Objects.requireNonNull(getContext()),
                android.R.layout.simple_dropdown_item_1line,
                mAutocompleteDictionary
        );
        itemNameEditText.setAdapter(adapter);

        builder.setTitle(R.string.add_new_item_dialog_title)
                .setView(view)
                .setPositiveButton(
                        R.string.add_new_item_dialog_positive,
                        (dialog, id) -> onPositive(
                                itemNameEditText.getText().toString(),
                                optionalCheckBox.isChecked(),
                                conditionEditText.getText().toString(),
                                saveItemCheckBox.isChecked()
                        )
                )
                .setNeutralButton(
                        R.string.add_new_item_dialog_neutral,
                        (dialog, id) -> onNeutral(
                                itemNameEditText.getText().toString(),
                                optionalCheckBox.isChecked(),
                                conditionEditText.getText().toString(),
                                saveItemCheckBox.isChecked()
                        )
                )
                .setNegativeButton(
                        R.string.add_new_item_dialog_negative,
                        (dialog, id) -> dialog.dismiss()
                );
        Dialog dialog = builder.create();

        itemNameEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mPositiveButton.setEnabled(s.length() > 0);
                        mNeutralButton.setEnabled(s.length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (AddItemDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement AddItemDialogListener");
        }
    }

    /**
     * The positive button adds the item to the shopping list and closes the dialog.
     *
     * @param itemName           The name of the item to add
     * @param optional           Whether or not the item is optional, specified by the user
     * @param condition          The condition specified by the user, or null if no condition is
     *                           specified.
     * @param saveToAutocomplete Whether or not to save this item to the autocomplete dictionary
     */
    private void onPositive(String itemName, boolean optional, @Nullable String condition,
                            boolean saveToAutocomplete) {
        addItem(itemName, optional, condition, saveToAutocomplete, false);
    }

    /**
     * The neutral button adds the item to the shopping list and then spawns the same dialog again
     * so that the user can more quickly add multiple items.
     *
     * @param itemName           The name of the item to add
     * @param optional           Whether or not the item is optional, specified by the user
     * @param condition          The condition specified by the user, or null if no condition is
     *                           specified.
     * @param saveToAutocomplete Whether or not to save this item to the autocomplete dictionary
     */
    private void onNeutral(String itemName, boolean optional, @Nullable String condition,
                           boolean saveToAutocomplete) {
        addItem(itemName, optional, condition, saveToAutocomplete, true);
    }

    /**
     * The method called by either the positive or neutral button that adds the item to the shopping
     * list and saves it to the autocomplete dictionary if the user specified that this should be
     * done.
     *
     * @param itemName           The name of the item to add
     * @param optional           Whether or not the item is optional, specified by the user
     * @param condition          The condition specified by the user, or null if no condition is
     *                           specified.
     * @param saveToAutocomplete Whether or not to save this item to the autocomplete dictionary
     * @param neutral            True iff the neutral button was the one that was clicked to call
     *                           this method
     */
    private void addItem(String itemName, boolean optional, @Nullable String condition,
                         boolean saveToAutocomplete, boolean neutral) {
        ShoppingListItem item;
        if (mExtendedItemRadioButton.isChecked()) {
            item = new ExtendedShoppingListItem(
                    itemName,
                    optional,
                    // Empty condition is to be treated as null
                    TextUtils.isEmpty(condition) ? null : condition,
                    // To make the order the last number, we simply get the number of already existing
                    // items, since the last item will have order n-1, where n is the number of items
                    mListener.getAdapter().getItemCount()
            );
        } else {
            item = new SingleShoppingListItem(
                    itemName,
                    optional,
                    // Empty condition is to be treated as null
                    TextUtils.isEmpty(condition) ? null : condition,
                    // To make the order the last number, we simply get the number of already existing
                    // items, since the last item will have order n-1, where n is the number of items
                    mListener.getAdapter().getItemCount()
            );
        }
        new AddItemTask(getContext(), mListener, saveToAutocomplete, neutral).execute(item);
    }

    public void setAutocompleteDictionary(String[] autocompleteDictionary) {
        mAutocompleteDictionary = autocompleteDictionary;
    }

    public interface AddItemDialogListener extends ShoppingListAdapterHolder {
        // This method is meant for the neutral button, to bring up the same dialog again if that
        // button is pressed
        void onAddItemButtonClick();

        void updateAutocompleteDictionary();
    }

    private static class AddItemTask extends AsyncTask<ShoppingListItem, Void, ShoppingListItem> {
        private final WeakReference<Context> ref;
        private final AddItemDialogListener listener;
        private final boolean addToAutocomplete;
        private final boolean neutral;

        AddItemTask(Context context, AddItemDialogListener listener, boolean addToAutocomplete,
                    boolean neutral) {
            ref = new WeakReference<>(context);
            this.listener = listener;
            this.addToAutocomplete = addToAutocomplete;
            this.neutral = neutral;
        }

        @Override
        protected ShoppingListItem doInBackground(ShoppingListItem... items) {
            SingleShoppingListItemDao singleItemDao =
                    AppDatabase.getDatabaseInstance(ref.get()).singleShoppingListItemDao();
            ExtendedShoppingListItemDao extendedItemDao =
                    AppDatabase.getDatabaseInstance(ref.get()).extendedShoppingListItemDao();
            AutocompleteEntryDao autocompleteEntryDao =
                    AppDatabase.getDatabaseInstance(ref.get()).autocompleteEntryDao();

            ShoppingListItem item = items[0];

            if (item instanceof SingleShoppingListItem) {
                singleItemDao.insertAll((SingleShoppingListItem) item);
            } else {
                extendedItemDao.insertAll((ExtendedShoppingListItem) item);
            }

            if (addToAutocomplete) {
                AutocompleteEntry entry = new AutocompleteEntry(item.getName());
                autocompleteEntryDao.insertAll(entry);
            }

            return item;
        }

        @Override
        protected void onPostExecute(ShoppingListItem item) {
            super.onPostExecute(item);
            listener.getAdapter().addItem(item);

            if (addToAutocomplete) {
                listener.updateAutocompleteDictionary();
            }

            if (neutral) {
                // If the neutral button is pressed, show the same dialog again, to speed up the
                // process of adding multiple items
                listener.onAddItemButtonClick();
            }
        }
    }
}
