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

package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.support.annotation.Nullable;

import java.math.BigDecimal;

/**
 * Represents a single item in the shopping list.
 */
@Entity(tableName = "single_shopping_list_items")
public class SingleShoppingListItem extends ShoppingListItem {
    @Ignore
    public static final boolean PER_UNIT = true;
    @Ignore
    public static final boolean PER_WEIGHT = !PER_UNIT;
    @Ignore
    @SuppressWarnings("unused")
    private static final String TAG = SingleShoppingListItem.class.getSimpleName();
    @Ignore
    private static final int OUNCES_PER_POUND = 16;
    @Ignore
    private static final BigDecimal POUNDS_PER_KILOGRAM = new BigDecimal("2.20462262185");
    @Ignore
    private static final BigDecimal KILOGRAMS_PER_POUND = new BigDecimal("0.45359237");

    @ColumnInfo(name = "base_price")
    private BigDecimal basePrice = BigDecimal.ZERO;
    private int quantity = 0;
    @ColumnInfo(name = "weight_in_kilograms")
    private BigDecimal weightInKilograms = BigDecimal.ZERO;
    @ColumnInfo(name = "per_unit_or_per_weight")
    private boolean perUnitOrPerWeight = PER_UNIT;

    // TODO: Update this Javadoc when you create the ExtendedShoppingListItem class

    /**
     * Creates a new SingleShoppingListItem with the given name, optionality, and condition. All other
     * associated values (price, weight, etc.) are set to 0.
     *
     * @param name        The name of the item (e.g. "Milk", "Eggs", etc.)
     * @param optional    True iff the item is optional.
     * @param condition   A condition under which the user is allowed to buy the item if the user
     *                    wishes to specify such a condition, null otherwise.
     * @param orderInList The order that this item appears in the list, where the first item has
     *                    order 0. A future implementation will require this to be set to -1 in
     *                    some cases.
     */
    public SingleShoppingListItem(String name, boolean optional, @Nullable String condition,
                                  int orderInList) {
        super(name, optional, condition, orderInList);
    }

    /**
     * Resets this item, setting all values to what they were when this item was created.
     */
    public void reset() {
        status = UNCHECKED;
        basePrice = BigDecimal.ZERO;
        quantity = 0;
        weightInKilograms = BigDecimal.ZERO;
        perUnitOrPerWeight = PER_UNIT;
    }

    /**
     * Returns either the price per unit if the item is priced by number of units, or the price per
     * kilogram if the item is priced by weight.
     *
     * @return The price per unit or the price per kilogram, whichever is applicable to this item
     */
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    /**
     * Sets the base price of this item, that is, the price per unit if the item is priced by
     * number of units, or the price per kilogram if the item is priced by weight..
     *
     * @param basePrice The price per unit or the price per kilogram, whichever is applicable to
     *                  this item
     */
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /**
     * Sets the base price of this item based on the price per pound. The item must be priced per
     * weight.
     *
     * @param pricePerPound The price per pound of the item
     */
    public void setPricePerPound(BigDecimal pricePerPound) {
        if (perUnitOrPerWeight == PER_UNIT) {
            throw new IllegalStateException("Item must be priced per weight, not per unit.");
        }
        basePrice = pricePerPound.multiply(POUNDS_PER_KILOGRAM);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getWeightInKilograms() {
        return weightInKilograms;
    }

    public void setWeightInKilograms(BigDecimal weightInKilograms) {
        this.weightInKilograms = weightInKilograms;
    }

    /**
     * Returns the weight of this item converted to pounds.
     *
     * @return The weight of this item converted to pounds
     */
    public BigDecimal getWeightInPounds() {
        return weightInKilograms.multiply(POUNDS_PER_KILOGRAM);
    }

    /**
     * Given the item's weight in pounds, sets the item's weight. Note that this is the same as
     * setWeightInPounds(pounds, 0).
     *
     * @param pounds The item's weight in pounds
     */
    public void setWeightInPounds(BigDecimal pounds) {
        weightInKilograms = pounds.multiply(KILOGRAMS_PER_POUND);
    }

    /**
     * Given the item's weight in pounds and ounces, sets the item's weight. Note that
     * setWeightInPounds(pounds, 0) is the same as setWeightInPounds(pounds).
     *
     * @param pounds The pound part of the item's weight
     * @param ounces The ounce part of the item's weight
     */
    public void setWeightInPounds(BigDecimal pounds, int ounces) {
        //noinspection BigDecimalMethodWithoutRoundingCalled
        setWeightInPounds(
                pounds.add(
                        new BigDecimal(ounces)
                                .divide(new BigDecimal(OUNCES_PER_POUND))
                )
        );
    }

    public boolean isPerUnitOrPerWeight() {
        return perUnitOrPerWeight;
    }

    public void setPerUnitOrPerWeight(boolean perUnitOrPerWeight) {
        this.perUnitOrPerWeight = perUnitOrPerWeight;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SingleShoppingListItem)) return false;

        SingleShoppingListItem otherItem = (SingleShoppingListItem) other;
        return this.getId() == otherItem.getId() &&
                this.status == otherItem.getStatus() &&
                this.basePrice.equals(otherItem.getBasePrice()) &&
                this.quantity == otherItem.getQuantity() &&
                this.weightInKilograms.equals(otherItem.getWeightInKilograms()) &&
                this.perUnitOrPerWeight == otherItem.isPerUnitOrPerWeight();
    }

    /**
     * Calculates and returns the total price of this item without tax.
     *
     * @return The total price of this item without tax
     */
    BigDecimal getTotalPriceWithoutTax() {
        if (perUnitOrPerWeight == PER_UNIT) {
            return basePrice.multiply(new BigDecimal(quantity));
        } else {    // I know I don't need the "else", but this makes it more clear
            return basePrice.multiply(weightInKilograms);
        }
    }
}
