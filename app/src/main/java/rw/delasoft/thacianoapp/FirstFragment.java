package rw.delasoft.thacianoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirstFragment extends Fragment implements PhoneNumberAdapter.OnPhoneNumberClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyMessage;
    private PhoneNumberAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(getContext());

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewPhoneNumbers);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PhoneNumberAdapter(this);
        recyclerView.setAdapter(adapter);

        // Set up swipe to delete
        setupSwipeToDelete();

        return view;
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String phoneNumber = adapter.getPhoneNumberAt(position);

                // Show confirmation dialog
                showDeleteConfirmationDialog(phoneNumber, position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmationDialog(String phoneNumber, int position) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Delete Phone Number");
        builder.setMessage("Are you sure you want to delete this phone number?\n\n" + phoneNumber);
        builder.setPositiveButton("Yes, Delete", (dialog, which) -> {
            // Delete from database
            boolean success = databaseHelper.deletePhoneNumber(phoneNumber);
            if (success) {
                adapter.removePhoneNumber(position);
                Toast.makeText(getContext(), "Phone number deleted", Toast.LENGTH_SHORT).show();

                // Check if list is empty and show empty message
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                }
            } else {
                adapter.notifyItemChanged(position); // Restore item in view
                Toast.makeText(getContext(), "Failed to delete phone number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Restore the item in the RecyclerView
            adapter.notifyItemChanged(position);
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setOnCancelListener(dialog -> {
            // Restore the item if dialog is dismissed
            adapter.notifyItemChanged(position);
        });
        builder.show();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPhoneNumbers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload phone numbers when returning to this fragment
        loadPhoneNumbers();
    }

    private void loadPhoneNumbers() {
        GeneralSettings settings = databaseHelper.getGeneralSettings();
        if (settings != null && settings.getPhoneNumbers() != null && !settings.getPhoneNumbers().isEmpty()) {
            String phoneNumbersString = settings.getPhoneNumbers();
            String[] numbersArray = phoneNumbersString.split(",");
            List<String> phoneNumbersList = new ArrayList<>();

            for (String number : numbersArray) {
                phoneNumbersList.add(number.trim());
            }

            adapter.setPhoneNumbers(phoneNumbersList);
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
        } else {
            // No phone numbers saved
            recyclerView.setVisibility(View.GONE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPhoneNumberClick(String phoneNumber) {
        Toast.makeText(getContext(), "Phone Number: " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPhoneNumberDelete(String phoneNumber, int position) {
        showDeleteConfirmationDialog(phoneNumber, position);
    }
}