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

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.dialogs.DeleteItemDialogFragment;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.SLAViewHolder> {

    private static final String TAG = ShoppingListAdapter.class.getSimpleName();

    private static final int CHECKED_ITEM_BACKGROUND_COLOR = 0x40000000;
    private static final int NOT_BUYING_ITEM_BACKGROUND_COLOR = 0x40F44336;
    private static final int NOT_BUYING_ITEM_TEXT_COLOR = 0xFFF44336;

    private final Context mContext;

    private final ShoppingListAdapterOnClickHandler mClickHandler;

    private List<ShoppingListItem> mAllItems = new ArrayList<>();
    private List<ShoppingListItem> mUncheckedItems = new ArrayList<>(),
            mCheckedItems = new ArrayList<>(),
            mNotBuyingItems = new ArrayList<>();

    ShoppingListAdapter(ShoppingListAdapterOnClickHandler handler, Context context) {
        mClickHandler = handler;
        mContext = context;
    }

    /**
     * Enables or disables strikethrough on the given TextView.
     *
     * @param tv            The TextView to be affected
     * @param strikeThrough True iff the given TextView should have strikethrough enabled
     */
    private static void setStrikeThrough(TextView tv, boolean strikeThrough) {
        if (strikeThrough) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    @NonNull
    @Override
    public SLAViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_shopping_list, parent, false);
        return new SLAViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SLAViewHolder holder, int position) {
        List<ShoppingListItem> allItems = getAllItemsOrderedByStatus();
        ShoppingListItem currentItem = allItems.get(position);
        String itemName = currentItem.getName();
        if (currentItem.isOptional()) {
            itemName += "*";
        }
        if (currentItem.hasCondition()) {
            itemName += mContext.getString(R.string.dagger);
        }
        holder.mItemNameTextView.setText(itemName);

        switch (currentItem.getStatus()) {
            case ShoppingListItem.CHECKED:
                holder.mContainingLinearLayout.setBackgroundColor(CHECKED_ITEM_BACKGROUND_COLOR);
                holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mDeleteButton.setColorFilter(null);
                setStrikeThrough(holder.mItemNameTextView, false);

                setCalculationView(holder, currentItem);

                String priceText =
                        mContext.getString(R.string.item_placeholder_price,
                                currentItem.getTotalPrice());
                holder.mPriceTextView.setText(priceText);
                break;
            case ShoppingListItem.NOT_BUYING:
                holder.mContainingLinearLayout.setBackgroundColor(NOT_BUYING_ITEM_BACKGROUND_COLOR);
                holder.mItemNameTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
                holder.mDeleteButton.setColorFilter(NOT_BUYING_ITEM_TEXT_COLOR);
                setStrikeThrough(holder.mItemNameTextView, true);
                holder.mPriceCalculationTextView.setText("");
                holder.mPriceTextView.setText("");
                break;
            default:    // ShoppingListItem.UNCHECKED
                holder.mContainingLinearLayout.setBackground(holder.DEFAULT_BACKGROUND);
                holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mDeleteButton.setColorFilter(null);
                setStrikeThrough(holder.mItemNameTextView, false);
                holder.mPriceCalculationTextView.setText("");
                holder.mPriceTextView.setText("");
                break;
        }

        holder.mDeleteButton.setOnClickListener(view -> onDeleteItemClick(currentItem));
    }

    /**
     * Helper method to set the calculation TextView for a checked item. The item must have status
     * CHECKED.
     *
     * @param holder The SLAViewHolder passed to onBindViewHolder
     * @param item   The ShoppingListItem that was selected in onBindViewHolder
     */
    private void setCalculationView(SLAViewHolder holder, ShoppingListItem item) {
        if (item.getStatus() != ShoppingListItem.CHECKED) {
            throw new IllegalArgumentException("The item must be CHECKED.");
        }

        String calculation;
        if (item.isPerUnitOrPerWeight() == ShoppingListItem.PER_UNIT) {
            if (mClickHandler.isTaxIncluded()) {
                calculation = mContext.getString(
                        R.string.item_placeholder_calc_per_unit,
                        item.getBasePrice(),
                        item.getQuantity(),
                        item.getTax()
                );
            } else {
                calculation = mContext.getString(
                        R.string.item_placeholder_calc_per_unit_no_tax,
                        item.getBasePrice(),
                        item.getQuantity()
                );
            }
        } else {    // item.isPerUnitOrPerWeight() == ShoppingListItem.PER_WEIGHT
            String format = mContext.getString(R.string.item_decimal_format_weight);
            DecimalFormat df = new DecimalFormat(format);
            if (mClickHandler.isTaxIncluded()) {
                calculation = mContext.getString(
                        R.string.item_placeholder_calc_per_weight,
                        item.getBasePrice(),
                        df.format(item.getWeightInKilograms()),
                        item.getTax()
                );
            } else {
                calculation = mContext.getString(
                        R.string.item_placeholder_calc_per_weight_no_tax,
                        item.getBasePrice(),
                        df.format(item.getWeightInKilograms())
                );
            }
        }
        holder.mPriceCalculationTextView.setText(calculation);
    }

    @Override
    public int getItemCount() {
        return mAllItems.size();
    }

    public void addItem(ShoppingListItem item) {
        mAllItems.add(item);
        onDataChanged();
    }

    public void setItemList(List<ShoppingListItem> items) {
        mAllItems = items;
        onDataChanged();
    }

    /**
     * When data is changed (e.g. when an item is added or removed), please use this method. Please
     * do not use notifyDataSetChanged(), because it does not handle the change properly for this
     * purpose.
     */
    public void onDataChanged() {
        boolean optionalItemsExist = false, conditionedItemsExist = false;
        mUncheckedItems.clear();
        mCheckedItems.clear();
        mNotBuyingItems.clear();

        // Sort the list according to the order of the items in it
        Collections.sort(
                mAllItems,
                (item1, item2) -> Integer.compare(item1.getOrderInList(), item2.getOrderInList())
        );

        for (ShoppingListItem item : mAllItems) {
            switch (item.getStatus()) {
                case ShoppingListItem.UNCHECKED:
                    mUncheckedItems.add(item);
                    break;
                case ShoppingListItem.CHECKED:
                    mCheckedItems.add(item);
                    break;
                default:    // ShoppingListItem.NOT_BUYING
                    mNotBuyingItems.add(item);
                    break;
            }
            if (item.isOptional()) optionalItemsExist = true;
            if (item.hasCondition()) conditionedItemsExist = true;
        }

        mClickHandler.updateTotalPrice(getTotalPrice());

        mClickHandler.switchViews(mAllItems.isEmpty());

        mClickHandler.showFootnotes(optionalItemsExist, conditionedItemsExist);

        notifyDataSetChanged();
    }

    /**
     * Returns the list of all shopping list items ordered by status, that is, all unchecked items
     * followed by all checked items followed by all items marked "not buying".
     *
     * @return The list of all shopping list items ordered by status
     */
    public List<ShoppingListItem> getAllItemsOrderedByStatus() {
        List<ShoppingListItem> result = new ArrayList<>();
        result.addAll(mUncheckedItems);
        result.addAll(mCheckedItems);
        result.addAll(mNotBuyingItems);
        return result;
    }

    private void onDeleteItemClick(ShoppingListItem item) {
        DeleteItemDialogFragment dialog = new DeleteItemDialogFragment();
        dialog.setItem(item);
        dialog.show(((MainActivity) mContext).getSupportFragmentManager(), "AddItemDialogFragment");
    }

    public void deleteItem(ShoppingListItem item) {
        mAllItems.remove(item);
        onDataChanged();
    }

    public void deleteAll() {
        mAllItems.clear();
        onDataChanged();
    }

    public BigDecimal getTotalPrice() {
        BigDecimal result = BigDecimal.ZERO;
        for (ShoppingListItem item : mCheckedItems) {
            result = result.add(item.getTotalPrice());
        }
        return result;
    }

    public interface ShoppingListAdapterOnClickHandler {
        void onItemClick(int position);

        void switchViews(boolean listIsEmpty);

        void updateTotalPrice(BigDecimal price);

        void showFootnotes(boolean optionalItemExist, boolean conditionedItemExist);
        boolean isTaxIncluded();
    }

    class SLAViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final Drawable DEFAULT_BACKGROUND;
        final int DEFAULT_TEXT_COLOR;

        final LinearLayout mContainingLinearLayout;
        final TextView mItemNameTextView;
        final TextView mPriceCalculationTextView;
        final TextView mPriceTextView;
        final ImageButton mDeleteButton;

        SLAViewHolder(View view) {
            super(view);

            mContainingLinearLayout = view.findViewById(R.id.ll_shopping_list_item);
            mItemNameTextView = view.findViewById(R.id.tv_shopping_list_item);
            mPriceCalculationTextView = view.findViewById(R.id.tv_item_price_calculations);
            mPriceTextView = view.findViewById(R.id.tv_item_price);
            mDeleteButton = view.findViewById(R.id.ib_shopping_list_item_delete);

            DEFAULT_BACKGROUND = mContainingLinearLayout.getBackground();
            DEFAULT_TEXT_COLOR = mItemNameTextView.getCurrentTextColor();

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int clickPosition = getAdapterPosition();
            mClickHandler.onItemClick(clickPosition);
        }
    }
}