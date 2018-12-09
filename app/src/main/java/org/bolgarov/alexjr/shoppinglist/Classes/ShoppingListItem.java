package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigDecimal;

/**
 * Represents any entry in the shopping list.
 */
public abstract class ShoppingListItem implements Comparable<ShoppingListItem> {
    @Ignore
    public static final int UNCHECKED = 0;
    @Ignore
    public static final int CHECKED = 1;
    @Ignore
    public static final int NOT_BUYING = 2;

    @Ignore
    private static BigDecimal taxRate = BigDecimal.ZERO;    // Set by shared preferences
    int status = UNCHECKED;

    private final String name;
    @Nullable
    private final String condition;
    private final boolean optional;
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "order_in_list")
    private int orderInList;

    /**
     * Creates a new ShoppingListItem with the given name, optionality, and condition. All
     * other associated values (price, weight, etc.) are set to 0.
     *
     * @param name        The name of the item (e.g. "Milk", "Eggs", etc.)
     * @param optional    True iff the item is optional.
     * @param condition   A condition under which the user is allowed to buy the item if the user
     *                    wishes to specify such a condition, null otherwise.
     * @param orderInList The order that this item appears in the shopping list, where the first
     *                    item has order 0. In some cases, a SingleShoppingListItem may have order
     *                    -1; see the javadoc for the constructor of SingleShoppingListItem for
     *                    details. This is never -1 for ExtendedShoppingListItems.
     */
    ShoppingListItem(String name, boolean optional, @Nullable String condition,
                     int orderInList) {
        this.name = name;
        this.optional = optional;
        this.condition = condition;
        this.orderInList = orderInList;
    }

    public static void setTaxRate(BigDecimal taxRate) {
        ShoppingListItem.taxRate = taxRate;
    }

    /**
     * Returns the tax of the given price.
     *
     * @param price A price
     * @return The tax on that price, based on the tax rate
     */
    public static BigDecimal getTax(BigDecimal price) {
        return price.multiply(taxRate);
    }

    /**
     * Resets this item, setting all values to what they were when this item was created. When
     * implementing this method, please remember to set the status to UNCHECKED.
     */
    public abstract void reset();

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Returns the ID of this item in the SQL database.
     *
     * @return The ID of this item
     */
    public int getId() {
        return id;
    }

    /**
     * This method is only to be used by the Room database. Do not call this method, or else errors
     * will occur.
     *
     * @param id This method is only to be used by the Room database. Do not call this method, or
     *           else errors will occur.
     */
    public void setId(int id) {
        this.id = id;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    @Nullable
    public String getCondition() {
        return condition;
    }

    public int getOrderInList() {
        return orderInList;
    }

    public void setOrderInList(int orderInList) {
        this.orderInList = orderInList;
    }

    /**
     * Calculates and returns the total price of this item without tax.
     *
     * @return The total price of this item without tax
     */
    abstract BigDecimal getTotalPriceWithoutTax();

    /**
     * Returns the tax of this item. This is not the same as the static method getTax(price).
     *
     * @return The tax of this item
     */
    public BigDecimal getTax() {
        return getTax(this.getTotalPriceWithoutTax());
    }

    /**
     * Returns the total price of this item including tax.
     *
     * @return The total price of this item
     */
    public BigDecimal getTotalPrice() {
        return getTotalPriceWithoutTax().add(this.getTax());
    }

    @Override
    public int compareTo(@NonNull ShoppingListItem other) {
        return this.name.compareToIgnoreCase(other.getName());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShoppingListItem &&
                this.name.equalsIgnoreCase(((ShoppingListItem) other).getName());
    }
}
