package org.bolgarov.alexjr.shoppinglist.dialogs.extendedItems;

import org.bolgarov.alexjr.shoppinglist.Classes.ShoppingListItem;

public interface MainActivityDataChangeListener {
    void refreshItem(ShoppingListItem item);
    void onDataChanged();
}
