/*
 * Copyright (c) 2018 Oleksiy Bolgarov.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.bolgarov.alexjr.shoppinglist.Classes;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

class Converters {
    @TypeConverter
    public static BigDecimal stringToBigDecimal(String stringValue) {
        return new BigDecimal(stringValue);
    }

    @TypeConverter
    public static String bigDecimalToString(BigDecimal bd) {
        return bd.toString();
    }

    @TypeConverter
    public static String listOfSingleItemsToString(List<SingleShoppingListItem> l) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<SingleShoppingListItem>>() {
        }.getType();
        return gson.toJson(l, type);
    }

    @TypeConverter
    public static List<SingleShoppingListItem> stringToListOfSingleItems(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<SingleShoppingListItem>>() {
        }.getType();
        return gson.fromJson(json, type);
    }
}
