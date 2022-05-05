package org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.bolgarov.alexjr.shoppinglist.AutocompleteDictionaryAdapter;
import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class AddEntryDialogFragment extends DialogFragment {

    private static final String TAG = AddEntryDialogFragment.class.getSimpleName();

    private Button mPositiveButton;

    private AutocompleteDictionaryAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            mPositiveButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
            mPositiveButton.setEnabled(false);  // Don't want to allow empty entries
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_edit_text, null);

        EditText input = view.findViewById(R.id.the_only_edit_text_in_this_dialog);
        input.setHint(R.string.autocomplete_add_entry_dialog_hint);

        builder.setTitle(R.string.autocomplete_add_entry_dialog_title)
                .setMessage(R.string.autocomplete_add_entry_dialog_prompt)
                .setView(view)
                .setPositiveButton(
                        R.string.autocomplete_add_entry_dialog_positive,
                        (dialog, which) -> {
                            String entry = input.getText().toString();
                            new AddEntryTask(getContext(), mAdapter).execute(entry);
                        }
                )
                .setNegativeButton(
                        R.string.autocomplete_add_entry_dialog_negative,
                        (dialog, which) -> dialog.dismiss()
                );
        Dialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPositiveButton.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return dialog;
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

    private static class AddEntryTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Context> ref;
        private final AutocompleteDictionaryAdapter adapter;

        AddEntryTask(Context context, AutocompleteDictionaryAdapter adapter) {
            ref = new WeakReference<>(context);
            this.adapter = adapter;
        }

        @Override
        protected String doInBackground(String... strings) {
            AutocompleteEntryDao dao =
                    AppDatabase.getDatabaseInstance(ref.get()).autocompleteEntryDao();
            AutocompleteEntry entry = new AutocompleteEntry(strings[0]);
            dao.insertAll(entry);

            return strings[0];
        }

        @Override
        protected void onPostExecute(String insertedEntryName) {
            super.onPostExecute(insertedEntryName);
            adapter.addEntry(insertedEntryName);
        }
    }
}
