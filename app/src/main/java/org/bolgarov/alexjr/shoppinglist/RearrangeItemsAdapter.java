package org.bolgarov.alexjr.shoppinglist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;

import java.util.Collections;
import java.util.List;

import static androidx.recyclerview.widget.ItemTouchHelper.DOWN;
import static androidx.recyclerview.widget.ItemTouchHelper.UP;
import static org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem.CHECKED;
import static org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem.NOT_BUYING;

/*
 * Suppressed warnings:
 * ClickableViewAccessibility: Not needed for drag handles
 */
public class RearrangeItemsAdapter extends RecyclerView.Adapter<RearrangeItemsAdapter.ViewHolder> {

    private static final int CHECKED_ITEM_BACKGROUND_COLOR = 0x40000000;
    private static final int NOT_BUYING_ITEM_BACKGROUND_COLOR = 0x40F44336;
    private static final int NOT_BUYING_ITEM_TEXT_COLOR = 0xFFF44336;

    private final Context mContext;
    private final OnStartDragListener mListener;

    private List<ShoppingListItem> mItems;

    RearrangeItemsAdapter(Context context, OnStartDragListener listener) {
        mContext = context;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_rearrange, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingListItem item = mItems.get(position);
        String itemName = item.getName();
        if (item.isOptional()) {
            itemName += "*";
        }
        if (item.hasCondition()) {
            itemName += mContext.getString(R.string.dagger);
        }
        holder.itemNameTextView.setText(itemName);

        switch (item.getStatus()) {
            case CHECKED:
                holder.containingLinearLayout.setBackgroundColor(CHECKED_ITEM_BACKGROUND_COLOR);
                holder.itemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
                break;
            case NOT_BUYING:
                holder.containingLinearLayout.setBackgroundColor(NOT_BUYING_ITEM_BACKGROUND_COLOR);
                holder.itemNameTextView.setTextColor(NOT_BUYING_ITEM_TEXT_COLOR);
                break;
            default:
                holder.containingLinearLayout.setBackground(holder.DEFAULT_BACKGROUND);
                holder.itemNameTextView.setTextColor(holder.DEFAULT_TEXT_COLOR);
        }

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mListener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public List<ShoppingListItem> getItemList() {
        return mItems;
    }

    public void setItemList(List<ShoppingListItem> items) {
        mItems = items;
        Collections.sort(
                mItems,
                (item1, item2) -> Integer.compare(item1.getOrderInList(), item2.getOrderInList())
        );
        notifyDataSetChanged();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        mItems.get(fromPosition).setOrderInList(toPosition);
        mItems.get(toPosition).setOrderInList(fromPosition);
        Collections.swap(mItems, fromPosition, toPosition);

        notifyItemMoved(fromPosition, toPosition);
    }

    interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final Drawable DEFAULT_BACKGROUND;
        final int DEFAULT_TEXT_COLOR;

        final LinearLayout containingLinearLayout;
        final ImageView dragHandle;
        final TextView itemNameTextView;

        ViewHolder(View view) {
            super(view);
            containingLinearLayout = view.findViewById(R.id.ll_rearrangeable_item);
            dragHandle = view.findViewById(R.id.iv_rearrange_drag_handle);
            itemNameTextView = view.findViewById(R.id.tv_rearrange_item_name);

            DEFAULT_BACKGROUND = containingLinearLayout.getBackground();
            DEFAULT_TEXT_COLOR = itemNameTextView.getCurrentTextColor();
        }
    }

    public static class DragHelperCallback extends ItemTouchHelper.Callback {
        private final RearrangeItemsAdapter mAdapter;

        DragHelperCallback(RearrangeItemsAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(UP | DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source,
                              RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Empty; no need to support swipes
        }
    }
}
