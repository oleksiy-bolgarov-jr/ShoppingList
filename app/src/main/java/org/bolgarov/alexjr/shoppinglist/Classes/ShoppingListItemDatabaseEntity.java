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
import android.arch.persistence.room.PrimaryKey;

import java.math.BigDecimal;

/**
 * A representation of a shopping list item in the database. A ShoppingListItem must be converted to
 * this in order to store it in the database.
 */
@Entity(tableName = "shopping_list_items")
public class ShoppingListItemDatabaseEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "item_name")
    private String itemName;
    private int status;
    @ColumnInfo(name = "price_per_unit")
    private BigDecimal pricePerUnit;
    private int quantity;
    @ColumnInfo(name = "weight_in_kilograms")
    private BigDecimal weightInKilograms;
    @ColumnInfo(name = "per_unit_or_per_weight")
    private boolean perUnitOrPerWeight;
    private boolean optional;
    private String condition;

    ShoppingListItemDatabaseEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
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

    public boolean isPerUnitOrPerWeight() {
        return perUnitOrPerWeight;
    }

    public void setPerUnitOrPerWeight(boolean perUnitOrPerWeight) {
        this.perUnitOrPerWeight = perUnitOrPerWeight;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
