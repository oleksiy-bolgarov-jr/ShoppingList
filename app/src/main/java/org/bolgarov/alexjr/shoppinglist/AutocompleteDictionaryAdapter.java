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

package org.bolgarov.alexjr.shoppinglist;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class AutocompleteDictionaryAdapter
        extends RecyclerView.Adapter<AutocompleteDictionaryAdapter.AutocompleteViewHolder> {
    private final AutocompleteDictionaryOnClickHandler mClickHandler;

    private List<String> mAutocompleteEntries;

    AutocompleteDictionaryAdapter(AutocompleteDictionaryOnClickHandler handler) {
        mClickHandler = handler;
    }

    @NonNull
    @Override
    public AutocompleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int id = R.layout.item_autocomplete_dictionary_entry;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(id, parent, false);
        return new AutocompleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AutocompleteViewHolder holder, int position) {
        String entry = mAutocompleteEntries.get(position);
        holder.mEntryTextView.setText(entry);
    }

    @Override
    public int getItemCount() {
        return mAutocompleteEntries == null ? 0 : mAutocompleteEntries.size();
    }

    public List<String> getEntryList() {
        return mAutocompleteEntries;
    }

    public void setEntryList(List<String> entries) {
        mAutocompleteEntries = entries;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Sort the list alphabetically if the API level is correct
            mAutocompleteEntries.sort(String::compareToIgnoreCase);
        }
        onDataChanged();
    }

    /**
     * Adds an entry to the dictionary. Does nothing if the entry already exists in the dictionary.
     *
     * @param entry The entry to be added
     */
    public void addEntry(String entry) {
        if (!mAutocompleteEntries.contains(entry)) {
            mAutocompleteEntries.add(entry);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Sort the list alphabetically if the API level is correct
                mAutocompleteEntries.sort(String::compareToIgnoreCase);
            }
            onDataChanged();
        }
    }

    public void deleteEntry(String entry) {
        mAutocompleteEntries.remove(entry);
        onDataChanged();
    }

    public void deleteAllEntries() {
        mAutocompleteEntries.clear();
        onDataChanged();
    }

    private boolean entryListIsEmpty() {
        return mAutocompleteEntries.isEmpty();
    }

    private void onDataChanged() {
        mClickHandler.switchViews(entryListIsEmpty());
        notifyDataSetChanged();
    }

    public interface AutocompleteDictionaryOnClickHandler {
        void onItemClick(int position);

        void switchViews(boolean dictionaryIsEmpty);
    }

    public class AutocompleteViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        final TextView mEntryTextView;

        AutocompleteViewHolder(View itemView) {
            super(itemView);
            mEntryTextView = itemView.findViewById(R.id.tv_autocomplete_entry);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mClickHandler.onItemClick(adapterPosition);
        }
    }
}
