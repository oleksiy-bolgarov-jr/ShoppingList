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

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItemDao;
import org.bolgarov.alexjr.shoppinglist.R;
import org.bolgarov.alexjr.shoppinglist.ShoppingListAdapter;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class DeleteItemDialogFragment extends DialogFragment {

    private static final String TAG = DeleteItemDialogFragment.class.getSimpleName();

    private ShoppingListItem mItem;
    private ShoppingListAdapter mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(mItem.getName())
                .setMessage(R.string.delete_item_dialog_body)
                .setPositiveButton(
                        R.string.delete_item_dialog_positive,
                        (dialog, id) -> new DeleteItemTask(getContext(), mAdapter).execute(mItem)
                )
                .setNegativeButton(
                        R.string.delete_item_dialog_negative,
                        (dialog, id) -> dialog.dismiss()
                );

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mAdapter = ((ShoppingListAdapterHolder) context).getAdapter();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement ShoppingListAdapterHolder");
        }
    }

    public void setItem(ShoppingListItem item) {
        mItem = item;
    }

    private static class DeleteItemTask
            extends AsyncTask<ShoppingListItem, Void, ShoppingListItem> {
        private final WeakReference<Context> ref;
        private final ShoppingListAdapter adapter;

        DeleteItemTask(Context context, ShoppingListAdapter adapter) {
            ref = new WeakReference<>(context);
            this.adapter = adapter;
        }

        @Override
        protected ShoppingListItem doInBackground(ShoppingListItem... items) {
            SingleShoppingListItemDao singleItemDao =
                    AppDatabase.getDatabaseInstance(ref.get()).singleShoppingListItemDao();
            ExtendedShoppingListItemDao extendedItemDao =
                    AppDatabase.getDatabaseInstance(ref.get()).extendedShoppingListItemDao();
            ShoppingListItem item = items[0];

            if (item instanceof SingleShoppingListItem) {
                singleItemDao.delete((SingleShoppingListItem) item);
            } else {
                extendedItemDao.delete((ExtendedShoppingListItem) item);
            }

            return item;
        }

        @Override
        protected void onPostExecute(ShoppingListItem deletedItem) {
            super.onPostExecute(deletedItem);
            adapter.deleteItem(deletedItem);
        }
    }
}
