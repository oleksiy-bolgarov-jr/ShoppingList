package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.TypeConverter;

import java.math.BigDecimal;

// FIXME Uncomment all queries
public class Converters {
    @TypeConverter
    public static BigDecimal fromString(String stringValue) {
        return new BigDecimal(stringValue);
    }

    @TypeConverter
    public static String bigDecimalToString(BigDecimal bd) {
        return bd.toString();
    }
}
