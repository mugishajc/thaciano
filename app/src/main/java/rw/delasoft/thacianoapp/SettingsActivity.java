package rw.delasoft.thacianoapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTotalCounts, etStartFrom, etEndAt, etPhoneNumbers, etPin, etShortCode;
    private Button btnImportFile, btnSaveGeneral, btnSaveSecurity, btnBackHome;
    private DatabaseHelper databaseHelper;
    private static final int PICK_FILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        etTotalCounts = findViewById(R.id.etTotalCounts);
        etStartFrom = findViewById(R.id.etStartFrom);
        etEndAt = findViewById(R.id.etEndAt);
        etPhoneNumbers = findViewById(R.id.etPhoneNumbers);
        etPin = findViewById(R.id.etPin);
        etShortCode = findViewById(R.id.etShortCode);
        btnImportFile = findViewById(R.id.btnImportFile);
        btnSaveGeneral = findViewById(R.id.btnSaveGeneral);
        btnSaveSecurity = findViewById(R.id.btnSaveSecurity);
        btnBackHome = findViewById(R.id.btnBackHome);

        // Set input types
        etTotalCounts.setInputType(InputType.TYPE_CLASS_NUMBER);
        etStartFrom.setInputType(InputType.TYPE_CLASS_NUMBER);
        etEndAt.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        // Set up auto-calculation for End At
        setupAutoCalculation();

        // Load existing settings
        loadSettings();

        // Button listeners
        btnImportFile.setOnClickListener(v -> openFilePicker());
        btnSaveGeneral.setOnClickListener(v -> saveGeneralSettings());
        btnSaveSecurity.setOnClickListener(v -> saveSecuritySettings());
        btnBackHome.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupAutoCalculation() {
        android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                calculateEndAt();
            }
        };

        etTotalCounts.addTextChangedListener(textWatcher);
        etStartFrom.addTextChangedListener(textWatcher);
    }

    private void calculateEndAt() {
        String totalCountsStr = etTotalCounts.getText().toString().trim();
        String startFromStr = etStartFrom.getText().toString().trim();

        if (!totalCountsStr.isEmpty() && !startFromStr.isEmpty()) {
            try {
                int totalCounts = Integer.parseInt(totalCountsStr);
                int startFrom = Integer.parseInt(startFromStr);
                int endAt = startFrom + totalCounts;

                // Format the end at to match the start from format (preserve leading zeros)
                String formattedEndAt = String.format("%0" + startFromStr.length() + "d", endAt);
                etEndAt.setText(formattedEndAt);
            } catch (NumberFormatException e) {
                etEndAt.setText("");
            }
        } else {
            etEndAt.setText("");
        }
    }

    private void loadSettings() {
        GeneralSettings settings = databaseHelper.getGeneralSettings();
        if (settings != null) {
            etTotalCounts.setText(String.valueOf(settings.getTotalCounts()));
            etStartFrom.setText(String.valueOf(settings.getStartFrom()));
            etEndAt.setText(String.valueOf(settings.getEndAt()));
            etPhoneNumbers.setText(settings.getPhoneNumbers());
        }

        SecurityConfig config = databaseHelper.getSecurityConfig();
        if (config != null) {
            etPin.setText(config.getPassword());
            etShortCode.setText(config.getShortCode());
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
                "text/csv",
                "text/comma-separated-values",
                "text/plain",  // Notepad/text files
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple file selection
        startActivityForResult(Intent.createChooser(intent, "Select Files (Can select multiple)"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Check if multiple files were selected
                if (data.getClipData() != null) {
                    // Multiple files selected
                    int count = data.getClipData().getItemCount();
                    StringBuilder allPhoneNumbers = new StringBuilder();

                    for (int i = 0; i < count; i++) {
                        Uri fileUri = data.getClipData().getItemAt(i).getUri();
                        String numbers = readPhoneNumbersFromFile(fileUri);
                        if (numbers != null && !numbers.isEmpty()) {
                            if (allPhoneNumbers.length() > 0 && !allPhoneNumbers.toString().endsWith(", ")) {
                                allPhoneNumbers.append(", ");
                            }
                            allPhoneNumbers.append(numbers);
                        }
                    }

                    etPhoneNumbers.setText(allPhoneNumbers.toString());
                    Toast.makeText(this, "Imported " + count + " file(s) successfully", Toast.LENGTH_SHORT).show();

                } else if (data.getData() != null) {
                    // Single file selected
                    Uri fileUri = data.getData();
                    String numbers = readPhoneNumbersFromFile(fileUri);
                    if (numbers != null && !numbers.isEmpty()) {
                        // Just show imported numbers, replacing existing content
                        etPhoneNumbers.setText(numbers);
                        Toast.makeText(this, "Phone numbers imported successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String readPhoneNumbersFromFile(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder phoneNumbers = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
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
            return phoneNumbers.toString();

        } catch (Exception e) {
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void saveGeneralSettings() {
        String totalCounts = etTotalCounts.getText().toString().trim();
        String startFrom = etStartFrom.getText().toString().trim();
        String endAt = etEndAt.getText().toString().trim();
        String phoneNumbers = etPhoneNumbers.getText().toString().trim();

        // Validation
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
            Toast.makeText(this, "End at is auto-calculated. Please enter Total Counts and Start From.", Toast.LENGTH_LONG).show();
            return;
        }

        if (phoneNumbers.isEmpty()) {
            etPhoneNumbers.setError("Phone numbers are required");
            etPhoneNumbers.requestFocus();
            return;
        }

        // Validate and shuffle phone numbers
        String[] numbers = phoneNumbers.split(",");
        PhoneNumberValidator validator = new PhoneNumberValidator();
        java.util.List<String> numbersList = new java.util.ArrayList<>();

        for (int i = 0; i < numbers.length; i++) {
            String number = numbers[i].trim();
            if (!validator.isValidRwandanNumber(number)) {
                Toast.makeText(this, "Invalid Rwandan phone number at position " + (i + 1) + ": " + number,
                              Toast.LENGTH_LONG).show();
                return;
            }
            numbersList.add(number);
        }

        // Shuffle the phone numbers randomly before saving
        java.util.List<String> shuffledNumbers = PhoneNumberProcessor.shufflePhoneNumbers(numbersList);

        // Convert shuffled list back to comma-separated string
        StringBuilder shuffledPhoneNumbers = new StringBuilder();
        for (int i = 0; i < shuffledNumbers.size(); i++) {
            shuffledPhoneNumbers.append(shuffledNumbers.get(i));
            if (i < shuffledNumbers.size() - 1) {
                shuffledPhoneNumbers.append(", ");
            }
        }

        // Update the text field to show shuffled order
        etPhoneNumbers.setText(shuffledPhoneNumbers.toString());

        // Save to database with shuffled phone numbers
        boolean success = databaseHelper.saveGeneralSettings(
                "",  // No title
                Integer.parseInt(totalCounts),
                Integer.parseInt(startFrom),
                Integer.parseInt(endAt),
                shuffledPhoneNumbers.toString()
        );

        if (success) {
            Toast.makeText(this, "General settings saved successfully! (Phone numbers shuffled)", Toast.LENGTH_SHORT).show();

            // Clear all general fields after successful save
            etTotalCounts.setText("");
            etStartFrom.setText("");
            etEndAt.setText("");
            etPhoneNumbers.setText("");
        } else {
            Toast.makeText(this, "Failed to save general settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSecuritySettings() {
        String pin = etPin.getText().toString().trim();
        String shortCode = etShortCode.getText().toString().trim();

        // Validation
        if (pin.isEmpty()) {
            etPin.setError("PIN is required");
            etPin.requestFocus();
            return;
        }

        if (pin.length() < 4) {
            etPin.setError("PIN must be at least 4 digits");
            etPin.requestFocus();
            return;
        }

        if (shortCode.isEmpty()) {
            etShortCode.setError("Short code is required");
            etShortCode.requestFocus();
            return;
        }

        if (!shortCode.matches("^\\*[0-9]+#$")) {
            etShortCode.setError("Invalid short code format. Use format like *348# or *123#");
            etShortCode.requestFocus();
            return;
        }

        // Save to database
        boolean success = databaseHelper.saveSecurityConfig(pin, shortCode);

        if (success) {
            Toast.makeText(this, "Security settings saved successfully!", Toast.LENGTH_SHORT).show();

            // Clear security fields after successful save
            etPin.setText("");
            etShortCode.setText("");
        } else {
            Toast.makeText(this, "Failed to save security settings", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
