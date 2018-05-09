package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_items WHERE status = " + ShoppingListItem.UNCHECKED)
    List<ShoppingListItemDatabaseEntity> getUncheckedItems();

    @Query("SELECT * FROM shopping_list_items WHERE status = " + ShoppingListItem.CHECKED)
    List<ShoppingListItemDatabaseEntity> getCheckedItems();

    @Query("SELECT * FROM shopping_list_items WHERE status = " + ShoppingListItem.NOT_BUYING)
    List<ShoppingListItemDatabaseEntity> getNotBuyingItems();

    @Query("SELECT * FROM shopping_list_items")
    List<ShoppingListItemDatabaseEntity> getAllItems();

    @Insert
    void insertAll(ShoppingListItemDatabaseEntity... items);

    @Delete
    void delete(ShoppingListItemDatabaseEntity item);

    @Query("DELETE FROM shopping_list_items")
    void deleteAllItems();
}
