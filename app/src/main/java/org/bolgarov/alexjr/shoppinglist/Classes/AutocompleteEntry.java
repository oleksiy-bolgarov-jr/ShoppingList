package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "autocomplete_dictionary_entries")
public class AutocompleteEntry {
    @PrimaryKey
    private @NonNull
    String name;

    public AutocompleteEntry() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
