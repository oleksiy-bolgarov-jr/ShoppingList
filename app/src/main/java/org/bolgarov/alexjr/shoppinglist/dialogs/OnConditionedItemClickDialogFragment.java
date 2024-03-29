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
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class OnConditionedItemClickDialogFragment extends DialogFragment {

    private static final String TAG = OnConditionedItemClickDialogFragment.class.getSimpleName();

    private ShoppingListItem mItem;
    private OnConditionedItemClickDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_conditioned_item, null);
        TextView conditionTextView = view.findViewById(R.id.tv_item_condition);
        conditionTextView.setText(mItem.getCondition());

        builder.setTitle(mItem.getName())
                .setView(view)
                .setPositiveButton(
                        R.string.item_dialog_condition_positive,
                        (dialog, which) -> mListener.onUncheckedItemClick(mItem)
                )
                .setNegativeButton(
                        R.string.item_dialog_condition_negative,
                        (dialog, which) -> dialog.dismiss()
                )
                .setNeutralButton(
                        R.string.item_dialog_unchecked_neutral,
                        (dialog, which) -> {
                            mItem.setStatus(ShoppingListItem.NOT_BUYING);
                            new UpdateItemTask(this).execute(mItem);
                        }
                );
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnConditionedItemClickDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnConditionedItemClickDialogListener");
        }
    }

    public void setItem(ShoppingListItem item) {
        mItem = item;
    }

    public interface OnConditionedItemClickDialogListener extends ShoppingListAdapterHolder {
        void onUncheckedItemClick(ShoppingListItem item);
    }

    private static class UpdateItemTask
            extends AsyncTask<ShoppingListItem, Void, ShoppingListItem> {
        private final WeakReference<OnConditionedItemClickDialogFragment> ref;

        UpdateItemTask(OnConditionedItemClickDialogFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected ShoppingListItem doInBackground(ShoppingListItem... items) {
            ShoppingListDao dao =
                    AppDatabase.getDatabaseInstance(ref.get().getContext()).shoppingListDao();
            if (items[0] instanceof ExtendedShoppingListItem) {
                dao.update((ExtendedShoppingListItem) items[0]);
            } else {
                dao.update((SingleShoppingListItem) items[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ShoppingListItem item) {
            super.onPostExecute(item);
            ref.get().mListener.getAdapter().onDataChanged();
        }
    }
}
