package org.bolgarov.alexjr.shoppinglist;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class RearrangeItemsActivity extends AppCompatActivity
        implements RearrangeItemsAdapter.OnStartDragListener {

    public static final String TAG = RearrangeItemsActivity.class.getSimpleName();

    private ConstraintLayout mLoadingDisplay;
    private FrameLayout mListContainer;

    private RearrangeItemsAdapter mAdapter;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rearrange_items);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLoadingDisplay = findViewById(R.id.constraint_layout_loading_display_rearrange);
        mListContainer = findViewById(R.id.frame_layout_item_container);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_rearrange);

        FloatingActionButton confirmButton = findViewById(R.id.fab_confirm);
        confirmButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //noinspection unchecked
            builder.setTitle(R.string.rearrange_confirm_dialog_title)
                    .setMessage(R.string.rearrange_confirm_dialog_body)
                    .setPositiveButton(
                            R.string.rearrange_confirm_dialog_positive,
                            (dialog, which) ->
                                    new UpdateOrderTask(this).execute(mAdapter.getItemList())
                    )
                    .setNegativeButton(
                            R.string.rearrange_confirm_dialog_negative,
                            (dialog, which) -> dialog.dismiss())
                    .show();
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        mAdapter = new RearrangeItemsAdapter(this, this);

        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new RearrangeItemsAdapter.DragHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        new RetrieveItemsTask(this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rearrange, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_help:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.rearrange_help_dialog_body)
                        .setPositiveButton(
                                R.string.rearrange_help_dialog_dismiss_button,
                                (dialog, id) -> dialog.dismiss())
                        .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder holder) {
        mItemTouchHelper.startDrag(holder);
    }

    private static class RetrieveItemsTask extends AsyncTask<Void, Void, List<ShoppingListItem>> {
        private final WeakReference<RearrangeItemsActivity> ref;

        RetrieveItemsTask(RearrangeItemsActivity a) {
            ref = new WeakReference<>(a);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ref.get().mLoadingDisplay.setVisibility(View.VISIBLE);
            ref.get().mListContainer.setVisibility(View.GONE);
        }

        @Override
        protected List<ShoppingListItem> doInBackground(Void... nothing) {
            return AppDatabase.getDatabaseInstance(ref.get()).shoppingListDao().getAllItems();
        }

        @Override
        protected void onPostExecute(List<ShoppingListItem> items) {
            super.onPostExecute(items);
            ref.get().mAdapter.setItemList(items);
            ref.get().mLoadingDisplay.setVisibility(View.GONE);
            ref.get().mListContainer.setVisibility(View.VISIBLE);
        }
    }

    private static class UpdateOrderTask extends AsyncTask<List<ShoppingListItem>, Void, Void> {
        private final WeakReference<RearrangeItemsActivity> ref;

        UpdateOrderTask(RearrangeItemsActivity a) {
            ref = new WeakReference<>(a);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ref.get().mLoadingDisplay.setVisibility(View.VISIBLE);
            ref.get().mListContainer.setVisibility(View.GONE);
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<ShoppingListItem>... lists) {
            List<ShoppingListItem> items = lists[0];
            ShoppingListDao dao = AppDatabase.getDatabaseInstance(ref.get()).shoppingListDao();
            for (ShoppingListItem item : items) {
                if (item instanceof SingleShoppingListItem) {
                    dao.update((SingleShoppingListItem) item);
                } else {
                    dao.update((ExtendedShoppingListItem) item);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
            ref.get().finish();
        }
    }

}
