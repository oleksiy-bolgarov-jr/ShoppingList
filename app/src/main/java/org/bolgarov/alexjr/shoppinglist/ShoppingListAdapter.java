package org.bolgarov.alexjr.shoppinglist;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.SLAViewHolder> {

    private static final int CHECKED_ITEM_BACKGROUND_COLOR = 0x40000000;
    private static final int NOT_BUYING_ITEM_BACKGROUND_COLOR = 0x40F44336;
    private static final int NOT_BUYING_ITEM_TEXT_COLOR = 0xFFF44336;
    private final ShoppingListAdapterOnClickHandler mClickHandler;
    // Used to keep the order correct if any of the items are moved around
    private List<ShoppingListItem> mAllItems = new ArrayList<ShoppingListItem>();
    private List<ShoppingListItem> mUncheckedItems = new ArrayList<ShoppingListItem>();
    private List<ShoppingListItem> mCheckedItems = new ArrayList<ShoppingListItem>();
    private List<ShoppingListItem> mNotBuyingItems = new ArrayList<ShoppingListItem>();

    public ShoppingListAdapter(ShoppingListAdapterOnClickHandler handler) {
        mClickHandler = handler;
    }

    @Override
    public SLAViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int id = R.layout.shopping_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(id, parent, false);
        return new SLAViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SLAViewHolder holder, int position) {
        List<ShoppingListItem> allItems = getAllItemsOrderedByStatus();
        ShoppingListItem currentShoppingListItem = allItems.get(position);
        holder.mItemNameTextView.setText(currentShoppingListItem.getItemName());
        holder.mPriceTextView.setText(
                "$" + currentShoppingListItem.getTotalPrice().setScale(2, RoundingMode.HALF_UP));

        if (currentShoppingListItem.getStatus() == ShoppingListItem.CHECKED) {
            holder.mContainingLinearLayout.setBackgroundColor(CHECKED_ITEM_BACKGROUND_COLOR);
            holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
            holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
            setStrikeThrough(holder.mItemNameTextView, false);
            setStrikeThrough(holder.mPriceTextView, false);
        } else if (currentShoppingListItem.getStatus() == ShoppingListItem.NOT_BUYING) {
            holder.mContainingLinearLayout.setBackgroundColor(NOT_BUYING_ITEM_BACKGROUND_COLOR);
            holder.mItemNameTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
            holder.mPriceTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
            setStrikeThrough(holder.mItemNameTextView, true);
            setStrikeThrough(holder.mPriceTextView, true);
        } else {
            holder.mContainingLinearLayout.setBackground(holder.DEFAULT_BACKGROUND);
            holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
            holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
            setStrikeThrough(holder.mItemNameTextView, false);
            setStrikeThrough(holder.mItemNameTextView, false);
        }

        holder.mOptionsButton.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(view.getContext(), holder.mOptionsButton);
            popup.getMenuInflater().inflate(R.menu.item_popup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.delete_item) {
                        delete(view.getContext(), currentShoppingListItem);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        });
    }

    public void onDataChanged() {
        mUncheckedItems.clear();
        mCheckedItems.clear();
        mNotBuyingItems.clear();
        for (ShoppingListItem item : mAllItems) {
            if (item.getStatus() == ShoppingListItem.UNCHECKED) {
                mUncheckedItems.add(item);
            } else if (item.getStatus() == ShoppingListItem.CHECKED) {
                mCheckedItems.add(item);
            } else {    // item.getStatus() == ShoppingListItem.NOT_BUYING
                mNotBuyingItems.add(item);
            }
        }
        mClickHandler.updateTotalPrice(getTotalPrice());
        mClickHandler.switchViews(mAllItems.isEmpty());
        notifyDataSetChanged();
    }

    public void deleteAll() {
        mAllItems.clear();
        onDataChanged();
        mClickHandler.updateTotalPrice(getTotalPrice());
    }

    @Override
    public int getItemCount() {
        return mAllItems.size();
    }

    public void addItemToEndOfShoppingList(ShoppingListItem item) {
        mAllItems.add(item);
        onDataChanged();
    }

    public List<ShoppingListItem> getAllItems() {
        return mAllItems;
    }

    public void setAllItems(List<ShoppingListItem> items) {
        mAllItems = items;
        onDataChanged();
    }

    public List<ShoppingListItem> getAllItemsOrderedByStatus() {
        List<ShoppingListItem> result = new ArrayList<>();
        result.addAll(mUncheckedItems);
        result.addAll(mCheckedItems);
        result.addAll(mNotBuyingItems);
        return result;
    }

    public BigDecimal getTotalPrice() {
        BigDecimal price = new BigDecimal("0");
        for (ShoppingListItem item : mCheckedItems) {
            price = price.add(item.getTotalPrice());
        }
        return price;
    }

    private void delete(Context context, ShoppingListItem item) {
        new MaterialDialog.Builder(context)
                .title(item.getItemName())
                .content(R.string.prompt_delete)
                .positiveText(R.string.confirm_delete)
                .negativeText(R.string.cancel_delete)
                .onPositive((dialog, which) -> {
                    mAllItems.remove(item);
                    Toast.makeText(
                            context,
                            "The entry \"" + item.getItemName() +
                                    "\" has been deleted from the shopping list.",
                            Toast.LENGTH_SHORT
                    ).show();
                    mClickHandler.updateTotalPrice(getTotalPrice());
                    onDataChanged();
                })
                .show();
    }

    private void setStrikeThrough(TextView tv, boolean strikeThrough) {
        if (strikeThrough) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private void showToastMessage(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public interface ShoppingListAdapterOnClickHandler {
        void onItemClick(int position);

        void updateTotalPrice(BigDecimal price);

        void switchViews(boolean listIsEmpty);
    }

    public class SLAViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final Drawable DEFAULT_BACKGROUND;
        public final int DEFAULT_TEXT_COLOR;

        public final LinearLayout mContainingLinearLayout;
        public final TextView mItemNameTextView;
        public final TextView mPriceTextView;
        public final ImageButton mOptionsButton;

        public SLAViewHolder(View view) {
            super(view);
            mContainingLinearLayout = (LinearLayout) view.findViewById(R.id.ll_shopping_list_item);
            mItemNameTextView = (TextView) view.findViewById(R.id.tv_shopping_list_item);
            mPriceTextView = (TextView) view.findViewById(R.id.tv_item_price);
            mOptionsButton = (ImageButton) view.findViewById(R.id.ib_shopping_list_item_options);

            DEFAULT_BACKGROUND = mContainingLinearLayout.getBackground();
            DEFAULT_TEXT_COLOR = mItemNameTextView.getCurrentTextColor();

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mClickHandler.onItemClick(adapterPosition);
        }
    }

}
