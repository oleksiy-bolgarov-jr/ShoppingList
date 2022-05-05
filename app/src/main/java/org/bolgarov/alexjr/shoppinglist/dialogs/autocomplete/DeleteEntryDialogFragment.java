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
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class DeleteEntryDialogFragment extends DialogFragment {

    private static final String TAG = DeleteEntryDialogFragment.class.getSimpleName();

    private String mEntry;
    private AutocompleteDictionaryAdapter mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(mEntry)
                .setMessage(R.string.autocomplete_delete_entry_dialog_body)
                .setPositiveButton(
                        R.string.autocomplete_delete_entry_dialog_positive,
                        (dialog, id) -> new DeleteEntryTask(getContext(), mAdapter).execute(mEntry)
                )
                .setNegativeButton(
                        R.string.autocomplete_delete_entry_dialog_negative,
                        (dialog, id) -> dialog.dismiss()
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

    public void setEntry(String entry) {
        mEntry = entry;
    }

    private static class DeleteEntryTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Context> ref;
        private final AutocompleteDictionaryAdapter adapter;

        DeleteEntryTask(Context context, AutocompleteDictionaryAdapter adapter) {
            ref = new WeakReference<>(context);
            this.adapter = adapter;
        }

        @Override
        protected String doInBackground(String... strings) {
            AutocompleteEntryDao dao =
                    AppDatabase.getDatabaseInstance(ref.get()).autocompleteEntryDao();
            AutocompleteEntry entry = dao.getEntry(strings[0]);
            dao.delete(entry);

            return strings[0];
        }

        @Override
        protected void onPostExecute(String deletedEntryName) {
            super.onPostExecute(deletedEntryName);
            adapter.deleteEntry(deletedEntryName);
        }
    }
}
