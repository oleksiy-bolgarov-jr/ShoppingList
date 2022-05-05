package org.bolgarov.alexjr.shoppinglist.Classes;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity(
        tableName = "extended_shopping_list_items"
)
public class ExtendedShoppingListItem extends ShoppingListItem {
    @Ignore
    private List<SingleShoppingListItem> subItems = new ArrayList<>();

    /**
     * Creates a new ExtendedShoppingListItem with the given name, optionality, and condition. All
     * other associated values (price, weight, etc.) are set to 0.
     *
     * @param name        The name of the item (e.g. "fruits")
     * @param optional    True iff the item is optional.
     * @param condition   A condition under which the user is allowed to buy the item if the user
     *                    wishes to specify such a condition, null otherwise.
     * @param orderInList The order that this item appears in the shopping list, where the first
     *                    item has order 0. In this case, this should never be -1.
     */
    public ExtendedShoppingListItem(String name, boolean optional, @Nullable String condition,
                                    int orderInList) {
        super(name, optional, condition, orderInList);
    }

    /**
     * Returns the list of single items that this extended item contains.
     *
     * @return The list of single items that this extended item contains
     */
    public List<SingleShoppingListItem> getSubItems() {
        return subItems;
    }

    /**
     * Sets the list of single items for this extended item.
     *
     * @param subItems The list of single items
     */
    public void setSubItems(List<SingleShoppingListItem> subItems) {
        this.subItems = subItems;
    }

    /**
     * Adds a subitem to this extended item in such a manner that the list of subitems is sorted in
     * alphabetical order.
     *
     * @param item The item to add
     */
    public void addItem(SingleShoppingListItem item) {
        if (subItems.isEmpty()) {
            subItems.add(item);
            return;
        }
        if (subItems.get(0).compareTo(item) > 0) {
            subItems.add(0, item);
            return;
        }
        if (subItems.get(subItems.size() - 1).compareTo(item) <= 0) {
            subItems.add(item);
            return;
        }

        int min = 0, max = subItems.size() - 1;
        while (max - min > 1) {
            int index = min + (max - min) / 2;
            if (subItems.get(index).compareTo(item) > 0) {
                max = index;
            } else {
                min = index;
            }
        }
        subItems.add(max, item);
    }

    /**
     * Removes the given subitem from this extended item, if it exists. Returns true if successful,
     * false if item was not in the list.
     *
     * @param item The item to be removed
     * @return True iff successful
     */
    public boolean removeItem(SingleShoppingListItem item) {
        return subItems.remove(item);
    }

    /**
     * Returns the number of single items in this extended item.
     *
     * @return Read the javadoc.
     */
    public int getItemCount() {
        return subItems.size();
    }

    public SingleShoppingListItem get(int index) {
        return subItems.get(index);
    }

    /**
     * Resets this item, setting this item to unchecked and clearing the list of single items.
     */
    @Override
    public void reset() {
        status = UNCHECKED;
        subItems.clear();
    }

    /**
     * Calculates and returns the total price of this extended item without tax.
     *
     * @return The total price of this item without tax
     */
    @Override
    public BigDecimal getTotalPriceWithoutTax() {
        BigDecimal result = BigDecimal.ZERO;
        for (SingleShoppingListItem item : subItems) {
            result = result.add(item.getTotalPriceWithoutTax());
        }
        return result;
    }
}
