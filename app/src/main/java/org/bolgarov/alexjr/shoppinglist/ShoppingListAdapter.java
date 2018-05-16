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
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.SLAViewHolder> {

    private final Context mContext;

    private static final int CHECKED_ITEM_BACKGROUND_COLOR = 0x40000000;
    private static final int NOT_BUYING_ITEM_BACKGROUND_COLOR = 0x40F44336;
    private static final int NOT_BUYING_ITEM_TEXT_COLOR = 0xFFF44336;
    private final ShoppingListAdapterOnClickHandler mClickHandler;
    private final List<ShoppingListItem> mUncheckedItems = new ArrayList<>();
    private final List<ShoppingListItem> mCheckedItems = new ArrayList<>();
    private final List<ShoppingListItem> mNotBuyingItems = new ArrayList<>();
    // Used to keep the order correct if any of the items are moved around
    private List<ShoppingListItem> mAllItems = new ArrayList<>();

    ShoppingListAdapter(ShoppingListAdapterOnClickHandler handler, Context context) {
        mClickHandler = handler;
        mContext = context;
    }

    @NonNull
    @Override
    public SLAViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int id = R.layout.item_shopping_list;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(id, parent, false);
        return new SLAViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SLAViewHolder holder, int position) {
        List<ShoppingListItem> allItems = getAllItemsOrderedByStatus();
        ShoppingListItem currentShoppingListItem = allItems.get(position);
        String itemName = currentShoppingListItem.getItemName();
        if (currentShoppingListItem.isOptional()) {
            itemName += "*";
        }
        if (currentShoppingListItem.hasCondition()) {
            itemName += mContext.getString(R.string.dagger);
        }
        holder.mItemNameTextView.setText(itemName);

        switch (currentShoppingListItem.getStatus()) {
            case ShoppingListItem.CHECKED:
                holder.mContainingLinearLayout.setBackgroundColor(CHECKED_ITEM_BACKGROUND_COLOR);
                holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                setStrikeThrough(holder.mItemNameTextView, false);

                String calculation;
                if (currentShoppingListItem.isPerUnitOrPerWeight() == ShoppingListItem.PER_UNIT
                        && mClickHandler.isTaxIncluded()) {
                    calculation = mContext.getString(
                            R.string.item_placeholder_calc_per_unit,
                            currentShoppingListItem.getPricePerUnit(),
                            currentShoppingListItem.getQuantity(),
                            ShoppingListItem.getTax(currentShoppingListItem.getTotalPriceWithoutTax())
                    );
                } else if (currentShoppingListItem.isPerUnitOrPerWeight() == ShoppingListItem.PER_UNIT
                        && !mClickHandler.isTaxIncluded()) {
                    calculation = mContext.getString(
                            R.string.item_placeholder_calc_per_unit_no_tax,
                            currentShoppingListItem.getPricePerUnit(),
                            currentShoppingListItem.getQuantity()
                    );
                } else if (currentShoppingListItem.isPerUnitOrPerWeight() == ShoppingListItem.PER_WEIGHT
                        && mClickHandler.isTaxIncluded()) {
                    String format =
                            mContext.getString(R.string.item_decimal_format_weight);
                    DecimalFormat df = new DecimalFormat(format);
                    calculation = mContext.getString(
                            R.string.item_placeholder_calc_per_weight,
                            currentShoppingListItem.getPricePerUnit(),
                            df.format(currentShoppingListItem.getWeightInKilograms()),
                            ShoppingListItem.getTax(currentShoppingListItem.getTotalPriceWithoutTax())
                    );
                } else {
                    String format =
                            mContext.getString(R.string.item_decimal_format_weight);
                    DecimalFormat df = new DecimalFormat(format);
                    calculation = mContext.getString(
                            R.string.item_placeholder_calc_per_weight_no_tax,
                            currentShoppingListItem.getPricePerUnit(),
                            df.format(currentShoppingListItem.getWeightInKilograms())
                    );
                }
                holder.mPriceCalculationTextView.setText(calculation);

                String priceText = mContext.getString(
                        R.string.item_placeholder_price, currentShoppingListItem.getTotalPrice());
                holder.mPriceTextView.setText(priceText);
                break;
            case ShoppingListItem.NOT_BUYING:
                holder.mContainingLinearLayout.setBackgroundColor(NOT_BUYING_ITEM_BACKGROUND_COLOR);
                holder.mItemNameTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
                setStrikeThrough(holder.mItemNameTextView, true);
                holder.mPriceCalculationTextView.setText("");
                holder.mPriceTextView.setText("");
                break;
            default:
                holder.mContainingLinearLayout.setBackground(holder.DEFAULT_BACKGROUND);
                holder.mItemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                holder.mPriceTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                setStrikeThrough(holder.mItemNameTextView, false);
                holder.mPriceCalculationTextView.setText("");
                holder.mPriceTextView.setText("");
                break;
        }

        holder.mOptionsButton.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(view.getContext(), holder.mOptionsButton);
            popup.getMenuInflater().inflate(R.menu.menu_item_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.delete_item) {
                    delete(view.getContext(), currentShoppingListItem);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    public void onDataChanged() {
        mUncheckedItems.clear();
        mCheckedItems.clear();
        mNotBuyingItems.clear();
        int optionalItems = 0;
        int conditionedItems = 0;
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
            if (item.isOptional()) {
                optionalItems++;
            }
            if (item.hasCondition()) {
                conditionedItems++;
            }
        }
        mClickHandler.updateTotalPrice(getTotalPrice());
        mClickHandler.switchViews(mAllItems.isEmpty());
        mClickHandler.showFootnotes(optionalItems > 0, conditionedItems > 0);
        notifyDataSetChanged();
    }

    public void deleteAll() {
        mAllItems.clear();
        onDataChanged();
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
                .content(R.string.delete_item_dialog_body)
                .positiveText(R.string.delete_item_dialog_positive)
                .negativeText(R.string.delete_item_dialog_negative)
                .onPositive((dialog, which) -> {
                    mAllItems.remove(item);
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

    public interface ShoppingListAdapterOnClickHandler {
        void onItemClick(int position);

        void updateTotalPrice(BigDecimal price);

        void switchViews(boolean listIsEmpty);

        boolean isTaxIncluded();

        void showFootnotes(boolean optionalItemsExist, boolean conditionedItemsExist);
    }

    public class SLAViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final Drawable DEFAULT_BACKGROUND;
        final int DEFAULT_TEXT_COLOR;

        final LinearLayout mContainingLinearLayout;
        final TextView mItemNameTextView;
        final TextView mPriceCalculationTextView;
        final TextView mPriceTextView;
        final ImageButton mOptionsButton;

        SLAViewHolder(View view) {
            super(view);
            mContainingLinearLayout = view.findViewById(R.id.ll_shopping_list_item);
            mItemNameTextView = view.findViewById(R.id.tv_shopping_list_item);
            mPriceCalculationTextView = view.findViewById(R.id.tv_item_price_calculations);
            mPriceTextView = view.findViewById(R.id.tv_item_price);
            mOptionsButton = view.findViewById(R.id.ib_shopping_list_item_options);

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
