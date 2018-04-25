package org.bolgarov.alexjr.shoppinglist.Classes;

import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ShoppingListItem {
    public static final boolean PER_UNIT = true;
    public static final boolean PER_WEIGHT = !PER_UNIT;
    public static final int UNCHECKED = 0;
    public static final int CHECKED = 1;
    public static final int NOT_BUYING = 2;
    private static final String TAG = ShoppingListItem.class.getSimpleName();
    private static final int OUNCES_PER_POUND = 16;
    private static final double POUNDS_PER_KILOGRAM = 2.20462262185;
    // The following is defined explicitly to prevent floating point errors.
    private static final double KILOGRAMS_PER_POUND = 0.45359237;
    private static BigDecimal taxRate = new BigDecimal("0.13");

    private String itemName;
    private int status = UNCHECKED;
    private BigDecimal pricePerUnit = new BigDecimal("0");
    private int quantity;
    private BigDecimal weightInKilograms;
    // The weight in pounds can be obtained directly from the weight in kilograms. Therefore, there
    // is no need to store it as a field.
    private boolean perUnitOrPerWeight = PER_UNIT;
    private boolean optional;
    private String condition;

    /**
     * Creates a new ShoppingListItem with the specified name, whether or not it is optional, and
     * a condition under which the user is allowed to buy the item (if specified).
     *
     * @param name      The name of the item
     * @param optional  Is the item optional?
     * @param condition The condition under which the user is allowed to buy the item, or null if
     *                  the user does not wish to specify a condition
     */
    public ShoppingListItem(String name, boolean optional, @Nullable String condition) {
        this.itemName = name;
        this.quantity = 0;
        this.weightInKilograms = new BigDecimal(0);
        this.optional = optional;
        this.condition = condition;
    }

    /**
     * This constructor should only be used when using the database.
     *
     * @param name
     * @param status
     * @param pricePerUnit
     * @param quantity
     * @param weightInKilograms
     * @param perUnitOrPerWeight
     * @param optional
     * @param condition
     */
    private ShoppingListItem(String name, int status, BigDecimal pricePerUnit, int quantity,
                             BigDecimal weightInKilograms, boolean perUnitOrPerWeight,
                             boolean optional, String condition) {
        this.itemName = name;
        this.status = status;
        this.pricePerUnit = pricePerUnit;
        this.quantity = quantity;
        this.weightInKilograms = weightInKilograms;
        this.perUnitOrPerWeight = perUnitOrPerWeight;
        this.optional = optional;
        this.condition = condition;
    }

    /**
     * Returns a ShoppingListItem created from the corresponding database entity.
     *
     * @param entity The entity used
     * @return A ShoppingListItem instance based on the entity
     */
    public static ShoppingListItem getItemFromDatabaseEntity(ShoppingListItemDatabaseEntity entity) {
        return new ShoppingListItem(
                entity.getItemName(),
                entity.getStatus(),
                entity.getPricePerUnit(),
                entity.getQuantity(),
                entity.getWeightInKilograms(),
                entity.isPerUnitOrPerWeight(),
                entity.isOptional(),
                entity.getCondition()
        );
    }

    public static ShoppingListItemDatabaseEntity itemAsDatabaseEntity(ShoppingListItem item) {
        ShoppingListItemDatabaseEntity entity = new ShoppingListItemDatabaseEntity();
        entity.setItemName(item.getItemName());
        entity.setStatus(item.getStatus());
        entity.setPricePerUnit(item.getPricePerUnit());
        entity.setQuantity(item.getQuantity());
        entity.setWeightInKilograms(item.getWeightInKilograms());
        entity.setPerUnitOrPerWeight(item.isPerUnitOrPerWeight());
        entity.setOptional(item.isOptional());
        entity.setCondition(item.getCondition());

        return entity;
    }

    public static BigDecimal getTax(BigDecimal price) {
        return price.multiply(taxRate);
    }

    /**
     * Returns the current tax rate in decimal format, i.e. if the tax rate is 15%, then this will
     * return 0.15.
     *
     * @return The current tax rate in decimal format
     */
    public static BigDecimal getTaxRate() {
        return taxRate;
    }

    /**
     * Changes the current tax rate to the specified new tax rate.
     *
     * @param newTaxRate The new tax rate
     */
    public static void setTaxRate(BigDecimal newTaxRate) {
        taxRate = newTaxRate;
    }

    /**
     * Returns the tax-adjusted price.
     *
     * @param price The price before tax
     * @return The price after tax
     */
    public static BigDecimal getTaxAdjustedPrice(BigDecimal price) {
        BigDecimal tax = price.multiply(taxRate);
        return price.add(tax);
    }

    public static BigDecimal getPricePerKilogram(BigDecimal pricePerPound) {
        return pricePerPound.multiply(new BigDecimal(POUNDS_PER_KILOGRAM));
    }

    public static BigDecimal getPricePerPound(BigDecimal pricePerKilogram) {
        return pricePerKilogram.multiply(new BigDecimal(KILOGRAMS_PER_POUND));
    }

    /**
     * Given a weight in pounds and ounces, returns the weight in kilograms. The weight may be
     * specified either in whole pounds and ounces (e.g. 6lb8oz) , or in decimal fractions of
     * pounds (e.g. 6.5lb), in which case ounces is to be 0.
     *
     * @param pounds The weight in pounds, or the pound part of the weight in pounds and ounces.
     * @param ounces If the weight is specified in pounds and ounces, the ounce part of this weight.
     *               Otherwise, 0.
     * @return The weight in kilograms
     */
    public static BigDecimal poundsToKilograms(BigDecimal pounds, int ounces) {
        BigDecimal ouncesAsDecimal = new BigDecimal((double) ounces / (double) OUNCES_PER_POUND);
        return pounds.add(ouncesAsDecimal).multiply(new BigDecimal(KILOGRAMS_PER_POUND));
    }

    public static BigDecimal kilogramsToPounds(BigDecimal kilograms) {
        return kilograms.multiply(new BigDecimal(POUNDS_PER_KILOGRAM));
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getItemName() {
        return itemName;
    }

    /**
     * Returns the quantity of this item if it is priced based on quantity, or 0 if it is priced
     * based on weight.
     *
     * @return The number of units of this item, or 0 if the item is priced per weight
     */
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the weight of this item in kilograms if it is priced based on weight, or 0 if it is
     * priced based on quantity.
     *
     * @return The weight of this item in kilograms, or 0 if the item is priced per quantity
     */
    public BigDecimal getWeightInKilograms() {
        return weightInKilograms;
    }

    /**
     * Sets the weight of this item to the specified weight.
     *
     * @param weight The weight to be set
     */
    public void setWeightInKilograms(BigDecimal weight) {
        this.weightInKilograms = weight;
    }

    /**
     * Returns the weight of this item in pounds if it is priced based on weight, or 0 if it is
     * priced based on quantity.
     *
     * @return the weight of this item in pounds
     */
    public BigDecimal getWeightInPounds() {
        return weightInKilograms.multiply(new BigDecimal(POUNDS_PER_KILOGRAM));
    }

    public boolean isPerUnitOrPerWeight() {
        return perUnitOrPerWeight;
    }

    /**
     * Sets whether the item is priced per unit or per weight.
     *
     * @param perUnitOrPerWeight PER_UNIT or PER_WEIGHT, depending on whether the item is priced per
     *                           unit or per weight
     */
    public void setPerUnitOrPerWeight(boolean perUnitOrPerWeight) {
        this.perUnitOrPerWeight = perUnitOrPerWeight;
    }

    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the optionality of this item.
     *
     * @param optional The optionality of this item
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Returns the condition under which the user is allowed to buy the item, or null if no
     * condition is specified.
     *
     * @return the condition under which the user is allowed to buy the item, or null if no
     * condition is specified
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the condition under which the user is allowed to buy this item.
     *
     * @param condition The condition under which the user is allowed to buy this item
     */
    public void setCondition(@Nullable String condition) {
        this.condition = condition;
    }

    /**
     * If priced per unit, returns the price per unit. Otherwise, returns the price per kilogram.
     *
     * @return price per unit or price per kilogram
     */
    public BigDecimal getPricePerUnit() {
        return pricePerUnit.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sets the price per unit or the price per kilogram of this item (which one of the two depends
     * on whether the item is priced per unit or per weight).
     *
     * @param pricePerUnit The price per unit or price per kilogram
     */
    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    /**
     * Returns the total price of all units or weight of this item.
     *
     * @return The total price of this item
     */
    public BigDecimal getTotalPrice() {
        return getTaxAdjustedPrice(getTotalPriceWithoutTax());
    }

    /**
     * Returns the total price of this item before taxes.
     *
     * @return The total price of this item before taxes
     */
    public BigDecimal getTotalPriceWithoutTax() {

        return perUnitOrPerWeight == PER_UNIT ?
                pricePerUnit.multiply(new BigDecimal(quantity)) :
                pricePerUnit.multiply(weightInKilograms);
    }

    /**
     * Sets the price per kilogram of this item given the price per pound.
     *
     * @param pricePerPound The price per pound
     */
    public void setPricePerPound(BigDecimal pricePerPound) {
        this.pricePerUnit = pricePerPound.multiply(
                new BigDecimal(POUNDS_PER_KILOGRAM));
    }

    public void reset() {
        pricePerUnit = new BigDecimal("0");
        quantity = 0;
        weightInKilograms = new BigDecimal(0);
        perUnitOrPerWeight = PER_UNIT;
    }

}
