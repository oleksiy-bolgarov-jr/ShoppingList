package org.bolgarov.alexjr.shoppinglist.dialogs.extendedItems;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Objects;

public class OnExtendedItemClickDialogFragment
        extends
        DialogFragment
        implements
        ExtendedItemAdapter.ExtendedItemAdapterDialog {
    private static final String TAG =
            OnExtendedItemClickDialogFragment.class.getSimpleName();

    private static WeakReference<FragmentActivity> activityRef;

    private String[] mAutocompleteDictionary;

    private ExtendedShoppingListItem mItem;
    private MainActivityDataChangeListener mDataChangeListener;

    private RecyclerView mRecyclerView;
    private ExtendedItemAdapter mAdapter;

    private FloatingActionButton mAddSubitemButton;
    private TextView mPriceTextView;
    private TextView mOverBudgetWarning;
    private TextView mTotalPriceTextView;

    private boolean mBudgetIsSet;
    private BigDecimal mBudget, mPreviousPrice;

    private FloatingActionButton mConfirmButton, mNotBuyingButton, mResetButton;

    /**
     * Creates a dialog warning the user that he is over budget.
     */
    private static void warnOverBudget() {
        AlertDialog.Builder warningDialogBuilder =
                new AlertDialog.Builder(Objects.requireNonNull(activityRef.get()));
        warningDialogBuilder.setMessage(R.string.over_budget_dialog_body)
                .setPositiveButton(
                        R.string.over_budget_dialog_dismiss_button,
                        (dialog, which) -> dialog.dismiss()
                );
        warningDialogBuilder.create().show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRef = new WeakReference<>(getActivity());
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);
        }
        updateDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.dialog_extended_item, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(mItem.getName());
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.setNavigationOnClickListener(v -> onDoneButtonClick());

        mRecyclerView = view.findViewById(R.id.rv_item_list);
        mConfirmButton = view.findViewById(R.id.fab_extended_item_confirm);
        mNotBuyingButton = view.findViewById(R.id.fab_extended_item_not_buying);
        mResetButton = view.findViewById(R.id.fab_extended_item_reset);
        mAddSubitemButton = view.findViewById(R.id.fab_extended_item_add);
        mPriceTextView = view.findViewById(R.id.tv_this_item_price);
        mOverBudgetWarning = view.findViewById(R.id.over_budget_warning);
        mTotalPriceTextView = view.findViewById(R.id.tv_total_price);

        LinearLayoutManager manager = new LinearLayoutManager(view.getContext(),
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new ExtendedItemAdapter();
        mAdapter.setExtendedItem(mItem);
        mAdapter.setDialog(this);
        mRecyclerView.setAdapter(mAdapter);

        mAddSubitemButton.setOnClickListener(v -> onAddItemButtonClick());
        mConfirmButton.setOnClickListener(v -> onDoneButtonClick());
        mNotBuyingButton.setOnClickListener(v -> onNotBuyingButtonClick());
        mResetButton.setOnClickListener(v -> onResetButtonClick());

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        if (mItem == null) {
            throw new IllegalStateException("You must set an ExtendedShoppingListItem before " +
                    "calling show.");
        }
        if (mDataChangeListener == null) {
            throw new IllegalStateException("You must set a MainActivityDataChangeListener before" +
                    " calling show.");
        }
        return super.show(transaction, tag);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (mItem == null) {
            throw new IllegalStateException("You must set an ExtendedShoppingListItem before " +
                    "calling show.");
        }
        if (mDataChangeListener == null) {
            throw new IllegalStateException("You must set a MainActivityDataChangeListener before" +
                    " calling show.");
        }
        super.show(manager, tag);
    }

    public void setItem(ExtendedShoppingListItem item) {
        mItem = item;
    }

    public void setBudgetIsSet(boolean budgetIsSet) {
        mBudgetIsSet = budgetIsSet;
    }

    public void setBudget(BigDecimal budget) {
        mBudget = budget;
    }

    public void setPreviousPrice(BigDecimal price) {
        mPreviousPrice = price.subtract(mItem.getTotalPrice());
    }

    public void setDataChangeListener(MainActivityDataChangeListener listener) {
        mDataChangeListener = listener;
    }

    public void setAutocompleteDictionary(String[] autocompleteDictionary) {
        mAutocompleteDictionary = autocompleteDictionary;
    }

    public void updateDialog(boolean itemBeingDeleted) {
        String priceString = getString(R.string.footer_total_no_budget, getThisItemPrice());
        mPriceTextView.setText(priceString);

        String totalString;
        if (mBudgetIsSet) {
            totalString =
                    getString(R.string.footer_grand_total_with_budget, getTotalPrice(), mBudget);
        } else {
            totalString = getString(R.string.footer_total_no_budget, getTotalPrice());
        }
        mTotalPriceTextView.setText(totalString);

        int colourId;
        if (mBudgetIsSet && getTotalPrice().compareTo(mBudget) > 0) {
            colourId = R.color.text_color_price_over_budget;
            mOverBudgetWarning.setVisibility(View.VISIBLE);
            if (!itemBeingDeleted) warnOverBudget();
        } else {
            colourId = R.color.text_color_price_normal;
            mOverBudgetWarning.setVisibility(View.GONE);
        }
        mPriceTextView.setTextColor(getResources().getColor(colourId));
        mTotalPriceTextView.setTextColor(getResources().getColor(colourId));
    }

    public void updateDialog() {
        updateDialog(false);
    }

    private void onAddItemButtonClick() {
        AddSubitemDialogFragment dialog = new AddSubitemDialogFragment();
        dialog.setAdapter(mAdapter);
        dialog.setExtendedItem(mItem);
        dialog.setAutocompleteDictionary(mAutocompleteDictionary);
        dialog.show(getFragmentManager(), AddSubitemDialogFragment.class.getSimpleName());
    }

    private void onDoneButtonClick() {
        if (mItem.getItemCount() > 0) {
            mItem.setStatus(ShoppingListItem.CHECKED);
        } else {
            mItem.setStatus(ShoppingListItem.UNCHECKED);
        }
        new UpdateTask(getContext(), this).execute(mItem);
    }

    private void onNotBuyingButtonClick() {
        mItem.reset();
        mItem.setStatus(ShoppingListItem.NOT_BUYING);
        new UpdateTask(getContext(), this, true).execute(mItem);
    }

    private void onResetButtonClick() {
        mItem.reset();
        new UpdateTask(getContext(), this, true).execute(mItem);
    }

    private BigDecimal getThisItemPrice() {
        return mItem.getTotalPrice();
    }

    private BigDecimal getTotalPrice() {
        return getThisItemPrice().add(mPreviousPrice);
    }

    private static class UpdateTask
            extends AsyncTask<ExtendedShoppingListItem, Void, ExtendedShoppingListItem> {
        private final WeakReference<Context> ref;
        private final OnExtendedItemClickDialogFragment dialog;
        private final boolean resetting;

        UpdateTask(Context context, OnExtendedItemClickDialogFragment dialog, boolean resetting) {
            ref = new WeakReference<>(context);
            this.dialog = dialog;
            this.resetting = resetting;
        }

        UpdateTask(Context context, OnExtendedItemClickDialogFragment dialog) {
            this(context, dialog, false);
        }

        @Override
        protected ExtendedShoppingListItem doInBackground(ExtendedShoppingListItem... items) {
            ShoppingListDao dao = AppDatabase.getDatabaseInstance(ref.get()).shoppingListDao();
            if (resetting) dao.deleteAllSubitems(items[0].getId());
            dao.update(items[0]);
            return items[0];
        }

        @Override
        protected void onPostExecute(ExtendedShoppingListItem item) {
            super.onPostExecute(item);
            dialog.mDataChangeListener.refreshItem(item);
            dialog.mDataChangeListener.onDataChanged();
            dialog.dismiss();
        }
    }
}
