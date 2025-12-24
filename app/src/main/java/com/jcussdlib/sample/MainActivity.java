package com.jcussdlib.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jcussdlib.USSDApi;
import com.jcussdlib.USSDController;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Sample activity demonstrating JCUSSDLib usage
 */
public class MainActivity extends AppCompatActivity implements USSDApi {

    private static final int REQUEST_CALL_PERMISSION = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;

    private USSDController ussdController;
    private EditText editUssdCode;
    private RadioGroup radioGroupSim;
    private TextView textResponse;
    private Button btnDial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editUssdCode = findViewById(R.id.edit_ussd_code);
        radioGroupSim = findViewById(R.id.radio_group_sim);
        textResponse = findViewById(R.id.text_response);
        btnDial = findViewById(R.id.btn_dial);
        Button btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        Button btnEnableOverlay = findViewById(R.id.btn_enable_overlay);

        // Initialize USSD controller
        ussdController = USSDController.getInstance(this);
        ussdController.setUSSDApi(this);

        // Set up response map (customize based on your needs)
        HashMap<String, HashSet<String>> map = new HashMap<>();
        HashSet<String> loginKeys = new HashSet<>();
        loginKeys.add("successful");
        loginKeys.add("balance");
        map.put("KEY_LOGIN", loginKeys);

        HashSet<String> errorKeys = new HashSet<>();
        errorKeys.add("error");
        errorKeys.add("failed");
        errorKeys.add("invalid");
        map.put("KEY_ERROR", errorKeys);

        ussdController.setMap(map);

        // Set up button listeners
        btnEnableAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnEnableOverlay.setOnClickListener(v -> requestOverlayPermission());
        btnDial.setOnClickListener(v -> dialUSSD());
    }

    /**
     * Open accessibility settings
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Please enable JCUSSDLib accessibility service", Toast.LENGTH_LONG).show();
    }

    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Dial USSD code
     */
    private void dialUSSD() {
        String ussdCode = editUssdCode.getText().toString().trim();

        if (ussdCode.isEmpty()) {
            Toast.makeText(this, "Please enter a USSD code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    REQUEST_CALL_PERMISSION);
            return;
        }

        // Get selected SIM slot
        int simSlot = -1; // Default
        int selectedId = radioGroupSim.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_sim_1) {
            simSlot = 0;
        } else if (selectedId == R.id.radio_sim_2) {
            simSlot = 1;
        }

        // Clear previous response
        textResponse.setText("Dialing " + ussdCode + "...");

        // Make USSD call
        ussdController.callUSSDInvoke(ussdCode, simSlot);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dialUSSD();
            } else {
                Toast.makeText(this, "Call permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // USSDApi callbacks
    @Override
    public void responseInvoke(String message) {
        runOnUiThread(() -> {
            textResponse.setText(textResponse.getText() + "\n\n" + message);

            // Example: Send response back to USSD menu
            // ussdController.send("1"); // Uncomment to send option "1"
        });
    }

    @Override
    public void over(String message) {
        runOnUiThread(() -> {
            textResponse.setText(textResponse.getText() + "\n\n[Session Ended]\n" + message);
            Toast.makeText(MainActivity.this, "USSD session completed", Toast.LENGTH_SHORT).show();
        });
    }
}
