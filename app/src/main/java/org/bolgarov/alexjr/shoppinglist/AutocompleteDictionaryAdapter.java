package org.bolgarov.alexjr.shoppinglist;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bolgarov.alexjr.shoppinglist.Classes.AppDatabase;

import java.util.List;

public class AutocompleteDictionaryAdapter
        extends RecyclerView.Adapter<AutocompleteDictionaryAdapter.AutocompleteViewHolder> {
    private final AutocompleteDictionaryOnClickHandler mClickHandler;

    private List<String> mAutocompleteEntries;

    public AutocompleteDictionaryAdapter(AutocompleteDictionaryOnClickHandler handler) {
        mClickHandler = handler;
    }

    @NonNull
    @Override
    public AutocompleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int id = R.layout.autocomplete_dictionary_entry;
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
        notifyDataSetChanged();
    }

    /**
     * Does nothing if the entry already exists in the list
     *
     * @param entry
     */
    public void addEntry(String entry) {
        if (!mAutocompleteEntries.contains(entry)) {
            mAutocompleteEntries.add(entry);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Sort the list alphabetically if the API level is correct
                mAutocompleteEntries.sort(String::compareToIgnoreCase);
            }
            notifyDataSetChanged();
        }
    }

    public void deleteEntry(String entry) {
        mAutocompleteEntries.remove(entry);
        notifyDataSetChanged();
    }

    public void deleteAllEntries() {
        mAutocompleteEntries.clear();
        notifyDataSetChanged();
    }

    public boolean entryListIsEmpty() {
        return mAutocompleteEntries.isEmpty();
    }

    public interface AutocompleteDictionaryOnClickHandler {
        void onItemClick(int position);

        AppDatabase getDatabase();
    }

    public class AutocompleteViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public final TextView mEntryTextView;

        public AutocompleteViewHolder(View itemView) {
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
