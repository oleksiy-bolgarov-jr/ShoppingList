package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Entity;
import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "extended_shopping_list_items")
public class ExtendedShoppingListItem extends ShoppingListItem {
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
    BigDecimal getTotalPriceWithoutTax() {
        BigDecimal result = BigDecimal.ZERO;
        for (SingleShoppingListItem item : subItems) {
            result = result.add(item.getTotalPriceWithoutTax());
        }
        return result;
    }
}
