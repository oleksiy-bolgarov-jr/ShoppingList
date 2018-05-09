package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(entities = {ShoppingListItemDatabaseEntity.class, AutocompleteEntry.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "shopping_list_database";

    private static AppDatabase instance;

    public static AppDatabase getDatabaseInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class,
                    DATABASE_NAME).build();
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    public abstract ShoppingListItemDao shoppingListItemDao();

    public abstract AutocompleteEntryDao autocompleteEntryDao();
}
