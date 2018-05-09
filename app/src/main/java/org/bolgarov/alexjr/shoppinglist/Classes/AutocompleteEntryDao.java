package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface AutocompleteEntryDao {
    @Query("SELECT name FROM autocomplete_dictionary_entries")
    List<String> getAllEntries();

    @Query("SELECT * FROM autocomplete_dictionary_entries WHERE name = :name")
    AutocompleteEntry getEntry(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(AutocompleteEntry... entries);

    @Delete
    void delete(AutocompleteEntry entry);

    @Query("DELETE FROM autocomplete_dictionary_entries")
    void deleteAllEntries();
}
