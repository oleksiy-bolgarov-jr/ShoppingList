package org.bolgarov.alexjr.shoppinglist.dialogs.extendedItems;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;
import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListDao;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

public class ExtendedItemAdapter extends RecyclerView.Adapter<ExtendedItemAdapter.ViewHolder> {

    private static final String TAG = ExtendedItemAdapter.class.getSimpleName();

    private Context mContext;
    private ExtendedItemAdapterDialog mDialog;

    private ExtendedShoppingListItem mExtendedItem;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SingleShoppingListItem currentItem = mExtendedItem.get(position);
        holder.mItemNameTextView.setText(currentItem.getName());
        String priceText =
                mContext.getString(R.string.item_placeholder_price, currentItem.getTotalPrice());
        holder.mPriceTextView.setText(priceText);
        setCalculationView(holder, currentItem);

        holder.mDeleteButton.setOnClickListener(view -> onDeleteItemButtonClick(currentItem));
    }

    @Override
    public int getItemCount() {
        return mExtendedItem.getItemCount();
    }

    /**
     * When data is changed (i.e. when an item is added or removed), please use this method instead
     * of notifyDataSetChanged().
     */
    public void onDataChanged(boolean itemBeingDeleted) {
        mDialog.updateDialog(itemBeingDeleted);
        notifyDataSetChanged();
    }

    public void onDataChanged() {
        onDataChanged(false);
    }

    public void setExtendedItem(ExtendedShoppingListItem item) {
        mExtendedItem = item;
    }

    public void setDialog(ExtendedItemAdapterDialog dialog) {
        mDialog = dialog;
    }

    /**
     * When the delete button for the item is clicked, delete the item from the extended item.
     *
     * @param itemToDelete The item that is to be deleted
     */
    private void onDeleteItemButtonClick(SingleShoppingListItem itemToDelete) {
        new DeleteSubItemTask(mContext, this).execute(itemToDelete);
    }

    /**
     * Helper method to set the calculation TextView for an item.
     *
     * @param holder The SLAViewHolder passed to onBindViewHolder
     * @param item   The item that was selected in onBindViewHolder
     */
    private void setCalculationView(ViewHolder holder, SingleShoppingListItem item) {
        String calculation;
        if (item.isPerUnitOrPerWeight() == SingleShoppingListItem.PER_UNIT) {
            calculation = mContext.getString(
                    R.string.item_placeholder_calc_per_unit_no_tax,
                    item.getBasePrice(),
                    item.getQuantity()
            );
        } else {    // item.isPerUnitOrPerWeight() == SingleShoppingListItem.PER_WEIGHT
            String format = mContext.getString(R.string.item_decimal_format_weight);
            DecimalFormat df = new DecimalFormat(format);
            calculation = mContext.getString(
                    R.string.item_placeholder_calc_per_weight_no_tax,
                    item.getBasePrice(),
                    df.format(item.getWeightInKilograms())
            );
        }
        holder.mPriceCalculationTextView.setText(calculation);
    }

    public interface ExtendedItemAdapterDialog {
        void updateDialog();

        void updateDialog(boolean itemBeingDeleted);
    }

    private static class DeleteSubItemTask
            extends AsyncTask<SingleShoppingListItem, Void, SingleShoppingListItem> {
        private final WeakReference<Context> ref;
        private final ExtendedItemAdapter adapter;

        DeleteSubItemTask(Context context, ExtendedItemAdapter adapter) {
            ref = new WeakReference<>(context);
            this.adapter = adapter;
        }

        @Override
        protected SingleShoppingListItem doInBackground(SingleShoppingListItem... items) {
            ShoppingListDao dao = AppDatabase.getDatabaseInstance(ref.get()).shoppingListDao();
            dao.delete(items[0]);
            return items[0];
        }

        @Override
        protected void onPostExecute(SingleShoppingListItem deletedItem) {
            super.onPostExecute(deletedItem);
            adapter.mExtendedItem.removeItem(deletedItem);
            adapter.onDataChanged(true);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mItemNameTextView;
        final TextView mPriceCalculationTextView;
        final TextView mPriceTextView;
        final ImageButton mDeleteButton;

        ViewHolder(View view) {
            super(view);

            mItemNameTextView = view.findViewById(R.id.tv_shopping_list_item);
            mPriceCalculationTextView = view.findViewById(R.id.tv_item_price_calculations);
            mPriceTextView = view.findViewById(R.id.tv_item_price);
            mDeleteButton = view.findViewById(R.id.ib_shopping_list_item_delete);
        }
    }
}
