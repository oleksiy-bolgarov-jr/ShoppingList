package org.bolgarov.alexjr.shoppinglist.dialogs;

import java.math.BigDecimal;

public interface ItemClickDialogListener extends ShoppingListAdapterHolder {
    boolean isBudgetSet();

    BigDecimal getBudget();
}
