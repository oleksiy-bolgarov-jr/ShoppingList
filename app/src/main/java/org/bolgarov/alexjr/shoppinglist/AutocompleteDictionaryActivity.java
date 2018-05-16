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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntry;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class AutocompleteDictionaryActivity
        extends AppCompatActivity
        implements AutocompleteDictionaryAdapter.AutocompleteDictionaryOnClickHandler {
    private AppDatabase db;

    private AutocompleteDictionaryAdapter mAdapter;

    private ConstraintLayout mNoEntriesDisplay;
    private RecyclerView mRecyclerView;
    private ConstraintLayout mLoadingDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autocomplete_dictionary);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab_add_autocomplete_entry);
        fab.setOnClickListener(view -> onAddEntryButtonClick());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mNoEntriesDisplay = findViewById(R.id.constraint_layout_no_autocomplete_entries_display);
        mRecyclerView = findViewById(R.id.recycler_view_autocomplete_dictionary);
        mLoadingDisplay =
                findViewById(R.id.constraint_layout_loading_display_autocomplete_dictionary);

        mAdapter = new AutocompleteDictionaryAdapter(this);
        db = AppDatabase.getDatabaseInstance(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        switchViews(mAdapter.getItemCount() == 0);

        executeRetrieveEntriesAction();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_autocomplete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_from_autocomplete:
                new MaterialDialog.Builder(this)
                        .title(R.string.delete_all_dialog_title)
                        .content(R.string.autocomplete_delete_all_dialog_body)
                        .positiveText(R.string.autocomplete_delete_all_dialog_positive)
                        .negativeText(R.string.delete_all_dialog_negative)
                        .onPositive((dialog, which) -> executeDeleteAllEntriesAction())
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(int position) {
        List<String> entries = mAdapter.getEntryList();
        String entry = entries.get(position);
        new MaterialDialog.Builder(this)
                .title(entry)
                .content(R.string.autocomplete_delete_entry_dialog_body)
                .positiveText(R.string.autocomplete_delete_entry_dialog_positive)
                .negativeText(R.string.autocomplete_delete_entry_dialog_negative)
                .onPositive((dialog, which) -> executeDeleteEntryAction(entry))
                .show();
    }

    /**
     * Shows either the RecyclerView showing the list of entries, or an error view if the dictionary
     * is empty.
     *
     * @param dictionaryIsEmpty true iff the autocomplete dictionary is empty
     */
    private void switchViews(boolean dictionaryIsEmpty) {
        if (dictionaryIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mNoEntriesDisplay.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mNoEntriesDisplay.setVisibility(View.GONE);
        }
    }

    private void onAddEntryButtonClick() {
        new MaterialDialog.Builder(this)
                .title(R.string.autocomplete_add_entry_dialog_title)
                .content(R.string.autocomplete_add_entry_dialog_prompt)
                .input(R.string.autocomplete_add_entry_dialog_hint, R.string.empty, false,
                        (dialog, input) -> executeAddEntryAction(input.toString()))
                .positiveText(R.string.autocomplete_add_entry_dialog_positive)
                .negativeText(R.string.autocomplete_add_entry_dialog_negative)
                .show();
    }

    private void executeRetrieveEntriesAction() {
        new RetrieveItemsTask(this).execute();
    }

    private void executeAddEntryAction(String entry) {
        new AddEntryTask(this).execute(entry);
    }

    private void executeDeleteEntryAction(String entry) {
        new DeleteEntryTask(this).execute(entry);
    }

    private void executeDeleteAllEntriesAction() {
        new DeleteAllEntriesTask(this).execute();
    }

    private static class RetrieveItemsTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<AutocompleteDictionaryActivity> ref;

        RetrieveItemsTask(AutocompleteDictionaryActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AutocompleteDictionaryActivity activity = ref.get();

            activity.mLoadingDisplay.setVisibility(View.VISIBLE);
            activity.mRecyclerView.setVisibility(View.GONE);
            activity.mNoEntriesDisplay.setVisibility(View.GONE);
        }

        @Override
        protected List<String> doInBackground(Void... nothing) {
            AutocompleteDictionaryActivity activity = ref.get();

            AutocompleteEntryDao dao = activity.db.autocompleteEntryDao();
            return dao.getAllEntries();
        }

        @Override
        protected void onPostExecute(List<String> results) {
            super.onPostExecute(results);
            AutocompleteDictionaryActivity activity = ref.get();

            activity.mAdapter.setEntryList(results);
            activity.mLoadingDisplay.setVisibility(View.GONE);
            activity.switchViews(results.isEmpty());
        }
    }

    private static class AddEntryTask extends AsyncTask<String, Void, String> {
        private final WeakReference<AutocompleteDictionaryActivity> ref;

        AddEntryTask(AutocompleteDictionaryActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            AutocompleteDictionaryActivity activity = ref.get();

            AutocompleteEntryDao dao = activity.db.autocompleteEntryDao();
            String entryName = strings[0];
            AutocompleteEntry entry = new AutocompleteEntry(entryName);
            dao.insertAll(entry);

            return entryName;
        }

        @Override
        protected void onPostExecute(String insertedEntryName) {
            super.onPostExecute(insertedEntryName);
            AutocompleteDictionaryActivity activity = ref.get();

            activity.mAdapter.addEntry(insertedEntryName);
            activity.switchViews(activity.mAdapter.entryListIsEmpty());
        }
    }

    private static class DeleteEntryTask extends AsyncTask<String, Void, String> {
        private final WeakReference<AutocompleteDictionaryActivity> ref;

        DeleteEntryTask(AutocompleteDictionaryActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            AutocompleteDictionaryActivity activity = ref.get();
            AutocompleteEntryDao dao = activity.db.autocompleteEntryDao();
            String entryName = strings[0];
            AutocompleteEntry entry = dao.getEntry(entryName);
            dao.delete(entry);

            return entryName;
        }

        @Override
        protected void onPostExecute(String deletedEntryName) {
            super.onPostExecute(deletedEntryName);
            AutocompleteDictionaryActivity activity = ref.get();

            activity.mAdapter.deleteEntry(deletedEntryName);
            activity.switchViews(activity.mAdapter.entryListIsEmpty());
        }
    }

    private static class DeleteAllEntriesTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<AutocompleteDictionaryActivity> ref;

        DeleteAllEntriesTask(AutocompleteDictionaryActivity context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            AutocompleteDictionaryActivity activity = ref.get();
            AutocompleteEntryDao dao = activity.db.autocompleteEntryDao();
            dao.deleteAllEntries();
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
            AutocompleteDictionaryActivity activity = ref.get();

            activity.mAdapter.deleteAllEntries();
            activity.switchViews(activity.mAdapter.entryListIsEmpty());
        }
    }
}
