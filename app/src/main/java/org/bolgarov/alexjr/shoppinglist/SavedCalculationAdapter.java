package org.bolgarov.alexjr.shoppinglist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SavedCalculationAdapter
        extends RecyclerView.Adapter<SavedCalculationAdapter.ViewHolder> {
    private List<String> calculations;

    SavedCalculationAdapter() {
        calculations = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_saved_calculation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.calculationTextView.setText(calculations.get(position));
        holder.removeButton.setOnClickListener(view -> {
            calculations.remove(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return calculations.size();
    }

    public void addCalculation(String calculation) {
        calculations.add(calculation);
        notifyDataSetChanged();
    }

    public void clearCalculations() {
        calculations.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView calculationTextView;
        final ImageButton removeButton;

        ViewHolder(View view) {
            super(view);
            calculationTextView = view.findViewById(R.id.tv_saved_calculation);
            removeButton = view.findViewById(R.id.btn_remove);
        }
    }
}
