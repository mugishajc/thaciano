package rw.delasoft.thacianoapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GeneralSettingsFragment extends Fragment {

    private EditText etTitle, etTotalCounts, etStartFrom, etEndAt, etPhoneNumbers;
    private Button btnImportFile, btnSave;
    private static final int PICK_FILE_REQUEST = 1;
    private DatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_settings, container, false);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(getContext());

        // Initialize views
        etTitle = view.findViewById(R.id.etTitle);
        etTotalCounts = view.findViewById(R.id.etTotalCounts);
        etStartFrom = view.findViewById(R.id.etStartFrom);
        etEndAt = view.findViewById(R.id.etEndAt);
        etPhoneNumbers = view.findViewById(R.id.etPhoneNumbers);
        btnImportFile = view.findViewById(R.id.btnImportFile);
        btnSave = view.findViewById(R.id.btnSave);

        // Set input types for number fields
        etTotalCounts.setInputType(InputType.TYPE_CLASS_NUMBER);
        etStartFrom.setInputType(InputType.TYPE_CLASS_NUMBER);
        etEndAt.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Load existing settings from database
        loadSettings();

        // Import file button click
        btnImportFile.setOnClickListener(v -> openFilePicker());

        // Save button click
        btnSave.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void loadSettings() {
        GeneralSettings settings = databaseHelper.getGeneralSettings();
        if (settings != null) {
            etTitle.setText(settings.getTitle());
            etTotalCounts.setText(String.valueOf(settings.getTotalCounts()));
            etStartFrom.setText(String.valueOf(settings.getStartFrom()));
            etEndAt.setText(String.valueOf(settings.getEndAt()));
            etPhoneNumbers.setText(settings.getPhoneNumbers());
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values",
                              "application/vnd.ms-excel",
                              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                readPhoneNumbersFromFile(fileUri);
            }
        }
    }

    private void readPhoneNumbersFromFile(Uri fileUri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder phoneNumbers = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Extract phone numbers from CSV (assuming they're in first column or comma-separated)
                String[] parts = line.split(",");
                for (String part : parts) {
                    String cleaned = part.trim().replaceAll("[^0-9+]", "");
                    if (!cleaned.isEmpty()) {
                        if (phoneNumbers.length() > 0) {
                            phoneNumbers.append(", ");
                        }
                        phoneNumbers.append(cleaned);
                    }
                }
            }

            reader.close();
            etPhoneNumbers.setText(phoneNumbers.toString());
            Toast.makeText(getContext(), "Phone numbers imported successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveSettings() {
        String title = etTitle.getText().toString().trim();
        String totalCounts = etTotalCounts.getText().toString().trim();
        String startFrom = etStartFrom.getText().toString().trim();
        String endAt = etEndAt.getText().toString().trim();
        String phoneNumbers = etPhoneNumbers.getText().toString().trim();

        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (totalCounts.isEmpty()) {
            etTotalCounts.setError("Total counts is required");
            etTotalCounts.requestFocus();
            return;
        }

        if (startFrom.isEmpty()) {
            etStartFrom.setError("Start from is required");
            etStartFrom.requestFocus();
            return;
        }

        if (endAt.isEmpty()) {
            etEndAt.setError("End at is required");
            etEndAt.requestFocus();
            return;
        }

        if (phoneNumbers.isEmpty()) {
            etPhoneNumbers.setError("Phone numbers are required");
            etPhoneNumbers.requestFocus();
            return;
        }

        // Validate phone numbers
        String[] numbers = phoneNumbers.split(",");
        if (numbers.length != 10) {
            Toast.makeText(getContext(), "You must enter exactly 10 phone numbers", Toast.LENGTH_LONG).show();
            return;
        }

        PhoneNumberValidator validator = new PhoneNumberValidator();
        for (int i = 0; i < numbers.length; i++) {
            String number = numbers[i].trim();
            if (!validator.isValidRwandanNumber(number)) {
                Toast.makeText(getContext(), "Invalid Rwandan phone number at position " + (i + 1) + ": " + number,
                              Toast.LENGTH_LONG).show();
                return;
            }
        }

        // All validations passed - save to SQLite database
        boolean success = databaseHelper.saveGeneralSettings(
                title,
                Integer.parseInt(totalCounts),
                Integer.parseInt(startFrom),
                Integer.parseInt(endAt),
                phoneNumbers
        );

        if (success) {
            Toast.makeText(getContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to save settings", Toast.LENGTH_SHORT).show();
        }
    }
}
