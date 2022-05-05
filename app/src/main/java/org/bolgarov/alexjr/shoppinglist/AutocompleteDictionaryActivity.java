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
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.AutocompleteEntryDao;
import org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete.AddEntryDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete.AutocompleteDictionaryAdapterHolder;
import org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete.DeleteAllEntriesDialogFragment;
import org.bolgarov.alexjr.shoppinglist.dialogs.autocomplete.DeleteEntryDialogFragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class AutocompleteDictionaryActivity
        extends AppCompatActivity
        implements AutocompleteDictionaryAdapter.AutocompleteDictionaryOnClickHandler,
        AutocompleteDictionaryAdapterHolder {
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

        new RetrieveEntriesTask(this).execute();
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
                new DeleteAllEntriesDialogFragment()
                        .show(getSupportFragmentManager(), "DeleteAllEntriesDialogFragment");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(int position) {
        List<String> entries = mAdapter.getEntryList();
        String entry = entries.get(position);
        DeleteEntryDialogFragment dialog = new DeleteEntryDialogFragment();
        dialog.setEntry(entry);
        dialog.show(getSupportFragmentManager(), "DeleteEntryDialogFragment");
    }

    @Override
    public AutocompleteDictionaryAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Shows either the RecyclerView showing the list of entries, or an error view if the dictionary
     * is empty.
     *
     * @param dictionaryIsEmpty true iff the autocomplete dictionary is empty
     */
    public void switchViews(boolean dictionaryIsEmpty) {
        if (dictionaryIsEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mNoEntriesDisplay.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mNoEntriesDisplay.setVisibility(View.GONE);
        }
    }

    private void onAddEntryButtonClick() {
        new AddEntryDialogFragment().show(getSupportFragmentManager(), "AddEntryDialogFragment");
    }

    private static class RetrieveEntriesTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<AutocompleteDictionaryActivity> ref;

        RetrieveEntriesTask(AutocompleteDictionaryActivity context) {
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
            // mRecyclerView and mNoEntriesDisplay will have their visibilities set appropriately
            // by mAdapter.setEntryList(results)
        }
    }
}
