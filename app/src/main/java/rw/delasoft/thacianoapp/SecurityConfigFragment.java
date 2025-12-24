package rw.delasoft.thacianoapp;

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

public class SecurityConfigFragment extends Fragment {

    private EditText etPassword, etShortCode;
    private Button btnSaveSecurity;
    private DatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_config, container, false);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(getContext());

        // Initialize views
        etPassword = view.findViewById(R.id.etPassword);
        etShortCode = view.findViewById(R.id.etShortCode);
        btnSaveSecurity = view.findViewById(R.id.btnSaveSecurity);

        // Set password field to password type
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Load existing settings from database
        loadSecurityConfig();

        // Save button click
        btnSaveSecurity.setOnClickListener(v -> saveSecurityConfig());

        return view;
    }

    private void loadSecurityConfig() {
        SecurityConfig config = databaseHelper.getSecurityConfig();
        if (config != null) {
            etPassword.setText(config.getPassword());
            etShortCode.setText(config.getShortCode());
        }
    }

    private void saveSecurityConfig() {
        String password = etPassword.getText().toString().trim();
        String shortCode = etShortCode.getText().toString().trim();

        // Validation
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (shortCode.isEmpty()) {
            etShortCode.setError("Short code is required");
            etShortCode.requestFocus();
            return;
        }

        // Validate short code format (USSD format like *348*1#)
        if (!shortCode.matches("^\\*[0-9]+\\*[0-9]+#$") && !shortCode.matches("^\\*[0-9]+#$")) {
            etShortCode.setError("Invalid short code format. Use format like *348*1# or *123#");
            etShortCode.requestFocus();
            return;
        }

        // All validations passed - save to SQLite database
        boolean success = databaseHelper.saveSecurityConfig(password, shortCode);

        if (success) {
            Toast.makeText(getContext(), "Security configuration saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to save security configuration", Toast.LENGTH_SHORT).show();
        }
    }
}
