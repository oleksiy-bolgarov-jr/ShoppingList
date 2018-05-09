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

import java.util.List;

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> onAddEntryButtonClick());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNoEntriesDisplay = findViewById(R.id.no_entries_display);
        mRecyclerView = findViewById(R.id.recycler_view_autocomplete_dictionary);
        mLoadingDisplay = findViewById(R.id.loading_display_autocomplete_dictionary);

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
        getMenuInflater().inflate(R.menu.autocomplete_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_from_autocomplete:
                new MaterialDialog.Builder(this)
                        .title(R.string.delete_all)
                        .content(R.string.prompt_delete_all_entries)
                        .positiveText(R.string.confirm_delete_all)
                        .negativeText(R.string.cancel_delete_all)
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
                .content(R.string.prompt_delete_autocomplete_entry)
                .positiveText(R.string.confirm_delete_entry)
                .negativeText(R.string.cancel_delete_entry)
                .onPositive((dialog, which) -> executeDeleteEntryAction(entry))
                .show();
    }

    private void switchViews(boolean dictionaryIsEmpty) {
        if (dictionaryIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mNoEntriesDisplay.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mNoEntriesDisplay.setVisibility(View.GONE);
        }
    }

    @Override
    public AppDatabase getDatabase() {
        return db;
    }

    private void onAddEntryButtonClick() {
        new MaterialDialog.Builder(this)
                .title(R.string.add_entry_title)
                .content(R.string.add_entry_title_prompt)
                .input(R.string.add_entry_hint, R.string.empty, false,
                        (dialog, input) -> executeAddEntryAction(input.toString()))
                .positiveText(R.string.confirm_add_entry)
                .negativeText(R.string.cancel)
                .show();
    }

    private void executeRetrieveEntriesAction() {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mLoadingDisplay.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                mNoEntriesDisplay.setVisibility(View.GONE);
            }

            @Override
            protected List<String> doInBackground(Void... nothing) {
                AutocompleteEntryDao dao = db.autocompleteEntryDao();
                return dao.getAllEntries();
            }

            @Override
            protected void onPostExecute(List<String> results) {
                super.onPostExecute(results);

                mAdapter.setEntryList(results);

                mLoadingDisplay.setVisibility(View.GONE);
                switchViews(results.isEmpty());
            }
        }.execute();
    }

    private void executeAddEntryAction(String entry) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                AutocompleteEntryDao dao = db.autocompleteEntryDao();
                String entryName = strings[0];
                AutocompleteEntry entry = new AutocompleteEntry();
                entry.setName(entryName);
                dao.insertAll(entry);

                return entryName;
            }

            @Override
            protected void onPostExecute(String insertedEntryName) {
                super.onPostExecute(insertedEntryName);

                mAdapter.addEntry(insertedEntryName);
                switchViews(mAdapter.entryListIsEmpty());
            }
        }.execute(entry);
    }

    private void executeDeleteEntryAction(String entry) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                AutocompleteEntryDao dao = db.autocompleteEntryDao();
                String entryName = strings[0];
                AutocompleteEntry entry = dao.getEntry(entryName);
                dao.delete(entry);

                return entryName;
            }

            @Override
            protected void onPostExecute(String deletedEntryName) {
                super.onPostExecute(deletedEntryName);

                mAdapter.deleteEntry(deletedEntryName);
                switchViews(mAdapter.entryListIsEmpty());
            }
        }.execute(entry);
    }

    private void executeDeleteAllEntriesAction() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... nothing) {
                AutocompleteEntryDao dao = db.autocompleteEntryDao();
                dao.deleteAllEntries();
                return null;
            }

            @Override
            protected void onPostExecute(Void nothing) {
                super.onPostExecute(nothing);

                mAdapter.deleteAllEntries();
                switchViews(mAdapter.entryListIsEmpty());
            }
        }.execute();
    }
}
