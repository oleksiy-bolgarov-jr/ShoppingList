package org.bolgarov.alexjr.shoppinglist.dialogs.extendedItems;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.bolgarov.alexjr.shoppinglist.Classes.ExtendedShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.Classes.SingleShoppingListItem;
import org.bolgarov.alexjr.shoppinglist.R;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class OnUncheckedExtendedItemClickDialogFragment extends DialogFragment {
    private static final String TAG =
            OnUncheckedExtendedItemClickDialogFragment.class.getSimpleName();

    private static WeakReference<FragmentActivity> activityRef;

    private ExtendedShoppingListItem mItem;
    private List<SingleShoppingListItem> mSubItems;

    /**
     * Creates a dialog warning the user that he is over budget.
     */
    private static void warnOverBudget() {
        AlertDialog.Builder warningDialogBuilder =
                new AlertDialog.Builder(Objects.requireNonNull(activityRef.get()));
        warningDialogBuilder.setMessage(R.string.over_budget_dialog_body)
                .setPositiveButton(
                        R.string.over_budget_dialog_dismiss_button,
                        (dialog, which) -> dialog.dismiss()
                );
        warningDialogBuilder.create().show();
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRef = new WeakReference<>(getActivity());
        updateDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_extended_item, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(mItem.getName());

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private void updateDialog() {
        // TODO: Implement
    }
}
