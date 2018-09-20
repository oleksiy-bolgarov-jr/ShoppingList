package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ExtendedShoppingListItemDao {
    @Query("SELECT * FROM extended_shopping_list_items")
    List<ExtendedShoppingListItem> getAllItems();

    @Query("SELECT * FROM extended_shopping_list_items WHERE id = :id")
    ExtendedShoppingListItem get(int id);

    @Insert
    void insertAll(ExtendedShoppingListItem... items);

    @Update
    void update(ExtendedShoppingListItem... items);

    @Delete
    void delete(ExtendedShoppingListItem... items);

    @Query("DELETE FROM extended_shopping_list_items")
    void deleteAllItems();
}
