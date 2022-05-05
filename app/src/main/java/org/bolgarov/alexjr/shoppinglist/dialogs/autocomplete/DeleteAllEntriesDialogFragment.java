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

package org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import org.bolgarov.alexjr.shoppinglist.AutocompleteDictionaryAdapter;
import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class DeleteAllEntriesDialogFragment extends DialogFragment {

    private static final String TAG = DeleteAllEntriesDialogFragment.class.getSimpleName();

    private AutocompleteDictionaryAdapter mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(R.string.delete_all_dialog_title)
                .setMessage(R.string.autocomplete_delete_all_dialog_body)
                .setPositiveButton(
                        R.string.autocomplete_delete_all_dialog_positive,
                        (dialog, which) -> {
                            new DeleteAllEntriesTask(getContext(), mAdapter).execute();
                            mAdapter.deleteAllEntries();
                        }
                )
                .setNegativeButton(
                        R.string.delete_all_dialog_negative,
                        (dialog, which) -> dialog.dismiss()
                );
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mAdapter = ((AutocompleteDictionaryAdapterHolder) context).getAdapter();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement AutocompleteDictionaryAdapterHolder");
        }
    }

    private static class DeleteAllEntriesTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> ref;
        private final AutocompleteDictionaryAdapter adapter;

        DeleteAllEntriesTask(Context context, AutocompleteDictionaryAdapter adapter) {
            this.adapter = adapter;
            ref = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            Context context = ref.get();
            AutocompleteEntryDao dao =
                    AppDatabase.getDatabaseInstance(context).autocompleteEntryDao();
            dao.deleteAllEntries();
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
            adapter.deleteAllEntries();
        }
    }
}
