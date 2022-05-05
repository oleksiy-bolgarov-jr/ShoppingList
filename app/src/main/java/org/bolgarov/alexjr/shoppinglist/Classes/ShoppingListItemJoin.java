package org.bolgarov.alexjr.shoppinglist.Classes;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "shopping_list_item_join",
        primaryKeys = {
                "group_id",
                "item_id"
        },
        foreignKeys = {
                @ForeignKey(
                        onDelete = CASCADE,
                        entity = ExtendedShoppingListItem.class,
                        parentColumns = "id",
                        childColumns = "group_id"
                ),
                @ForeignKey(
                        onDelete = CASCADE,
                        entity = SingleShoppingListItem.class,
                        parentColumns = "id",
                        childColumns = "item_id"
                )
        }
)
public class ShoppingListItemJoin {
    @ColumnInfo(name = "group_id")
    private int groupId;
    @ColumnInfo(name = "item_id")
    private int itemId;

    public ShoppingListItemJoin(int groupId, int itemId) {
        this.groupId = groupId;
        this.itemId = itemId;
    }

    public ShoppingListItemJoin(ExtendedShoppingListItem group, SingleShoppingListItem item) {
        this(group.getId(), item.getId());
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}
