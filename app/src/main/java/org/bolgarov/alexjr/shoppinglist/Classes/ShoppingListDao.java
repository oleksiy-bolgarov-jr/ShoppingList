package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class ShoppingListDao {
    @Query("SELECT * FROM single_shopping_list_items WHERE order_in_list >= 0")
    abstract List<SingleShoppingListItem> getSingleItemsNotInCategory();

    @Query("SELECT * FROM single_shopping_list_items WHERE order_in_list = -1")
    abstract List<SingleShoppingListItem> getSingleItemsInCategories();

    @Query("SELECT * FROM extended_shopping_list_items")
    abstract List<ExtendedShoppingListItem> getExtendedItems();

    @Transaction
    public List<ShoppingListItem> getAllItems() {
        List<ShoppingListItem> result = new ArrayList<>();
        List<SingleShoppingListItem> singleItems = getSingleItemsNotInCategory();
        List<ExtendedShoppingListItem> extendedItems = getExtendedItems();

        for (ExtendedShoppingListItem e : extendedItems) {
            List<SingleShoppingListItem> itemsInE = getAllItemsInCategory(e.getId());
            for (SingleShoppingListItem item : itemsInE) {
                e.addItem(item);
            }
        }

        result.addAll(singleItems);
        result.addAll(extendedItems);
        return result;
    }

    @Query("SELECT * " +
            "FROM shopping_list_item_join " +
            "INNER JOIN single_shopping_list_items " +
            "ON shopping_list_item_join.item_id = single_shopping_list_items.id " +
            "WHERE shopping_list_item_join.group_id = :categoryId")
    abstract List<SingleShoppingListItem> getAllItemsInCategory(int categoryId);

    @Query("SELECT * FROM single_shopping_list_items WHERE id = :id")
    abstract SingleShoppingListItem get(int id);

    @Insert
    public abstract long insert(SingleShoppingListItem item);

    @Insert
    public abstract void insert(ExtendedShoppingListItem item);

    @Update
    public abstract void update(SingleShoppingListItem item);

    @Update
    public abstract void update(ExtendedShoppingListItem item);

    @Delete
    public abstract void delete(SingleShoppingListItem item);

    @Delete
    public abstract void delete(ExtendedShoppingListItem item);

    @Insert
    public abstract void insertJoin(ShoppingListItemJoin join);

    @Query("DELETE FROM single_shopping_list_items " +
            "WHERE id IN (SELECT item_id " +
            "             FROM shopping_list_item_join " +
            "             WHERE group_id = :categoryId)")
    public abstract void deleteAllSubitems(int categoryId);

    @Query("DELETE FROM single_shopping_list_items")
    abstract void deleteAllSingleItems();

    @Query("DELETE FROM extended_shopping_list_items")
    abstract void deleteAllExtendedItems();

    @Transaction
    public void deleteAllItems() {
        deleteAllSingleItems();
        deleteAllExtendedItems();
    }
}
