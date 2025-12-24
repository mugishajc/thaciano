package rw.delasoft.thacianoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jcussdlib.controller.USSDController;
import com.jcussdlib.matcher.OTPBruteForceMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ProcessingActivity extends AppCompatActivity {

    private static final String TAG = "ThacianoProcessing";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvStatus, tvCurrentNumber, tvProgress;
    private ProgressBar progressBar;
    private Button btnStart, btnStop, btnClose;

    private OTPBruteForceMatcher otpMatcher;
    private USSDController ussdController;
    private DatabaseHelper databaseHelper;

    // Success tracking
    private int successCount = 0;
    private int failureCount = 0;
    private int totalAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "========================================");
        Log.d(TAG, "onCreate: Activity started");
        Log.d(TAG, "========================================");
        setContentView(R.layout.activity_processing);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Processing Phone Numbers");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        tvStatus = findViewById(R.id.tvStatus);
        tvCurrentNumber = findViewById(R.id.tvCurrentNumber);
        tvProgress = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.progressBar);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnClose = findViewById(R.id.btnClose);

        Log.d(TAG, "onCreate: Views initialized - checking for nulls:");
        Log.d(TAG, "  tvStatus: " + (tvStatus != null ? "OK" : "NULL"));
        Log.d(TAG, "  tvCurrentNumber: " + (tvCurrentNumber != null ? "OK" : "NULL"));
        Log.d(TAG, "  tvProgress: " + (tvProgress != null ? "OK" : "NULL"));
        Log.d(TAG, "  progressBar: " + (progressBar != null ? "OK" : "NULL"));
        Log.d(TAG, "  btnStart: " + (btnStart != null ? "OK" : "NULL"));
        Log.d(TAG, "  btnStop: " + (btnStop != null ? "OK" : "NULL"));
        Log.d(TAG, "  btnClose: " + (btnClose != null ? "OK" : "NULL"));

        if (btnStart == null) {
            Log.e(TAG, "onCreate: CRITICAL ERROR - btnStart is NULL!");
            Toast.makeText(this, "ERROR: Start button not found in layout!", Toast.LENGTH_LONG).show();
            return;
        }

        // Get data from intent
        Log.d(TAG, "onCreate: Loading data from database...");
        loadData();

        // Initialize USSD API
        Log.d(TAG, "onCreate: Initializing USSD API...");
        initializeUSSD();

        // Set up buttons
        btnStart.setOnClickListener(v -> {
            // IMMEDIATE FEEDBACK - User should see this instantly
            Toast.makeText(this, "🔘 START BUTTON CLICKED!", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Button clicked, checking requirements...");

            Log.d(TAG, "========================================");
            Log.d(TAG, "btnStart CLICKED - IMMEDIATE RESPONSE");
            Log.d(TAG, "  Button enabled: " + btnStart.isEnabled());
            Log.d(TAG, "  Button clickable: " + btnStart.isClickable());
            Log.d(TAG, "  Activity: " + this.getClass().getSimpleName());
            Log.d(TAG, "========================================");

            // Run on UI thread to ensure visibility
            runOnUiThread(() -> {
                try {
                    checkPermissionsAndStart();
                } catch (Exception e) {
                    Log.e(TAG, "btnStart: EXCEPTION in checkPermissionsAndStart!", e);
                    e.printStackTrace();
                    Toast.makeText(ProcessingActivity.this, "❌ ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    tvStatus.setText("ERROR: " + e.getMessage());
                }
            });
        });
        btnStop.setOnClickListener(v -> {
            Log.d(TAG, "========================================");
            Log.d(TAG, "btnStop CLICKED");
            Log.d(TAG, "========================================");
            stopProcessing();
        });
        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "btnClose CLICKED - finishing activity");
            finish();
        });

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        updateUI();
        Log.d(TAG, "onCreate: UI updated");
        Log.d(TAG, "onCreate: Button state - Enabled: " + btnStart.isEnabled() + ", Clickable: " + btnStart.isClickable());
        Log.d(TAG, "onCreate: Complete - Activity ready");
        Log.d(TAG, "========================================");
    }

    private void loadData() {
        Log.d(TAG, "loadData: Starting to load data from database");
        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        // Load phone numbers and OTP range
        GeneralSettings settings = databaseHelper.getGeneralSettings();
        Log.d(TAG, "loadData: General settings = " + (settings != null ? "LOADED" : "NULL"));

        if (settings != null && settings.getPhoneNumbers() != null) {
            String[] numbersArray = settings.getPhoneNumbers().split(",");
            phoneNumbers = new ArrayList<>();
            for (String number : numbersArray) {
                phoneNumbers.add(number.trim());
            }
            Log.d(TAG, "loadData: Loaded " + phoneNumbers.size() + " phone numbers");

            // Shuffle phone numbers randomly to avoid consecutive numbers
            shuffledNumbers = PhoneNumberProcessor.shufflePhoneNumbers(phoneNumbers);
            Log.d(TAG, "loadData: Phone numbers shuffled, total: " + shuffledNumbers.size());

            // Load OTP range for retry attempts
            otpStartFrom = settings.getStartFrom();
            otpEndAt = settings.getEndAt();
            Log.d(TAG, "loadData: OTP range: " + otpStartFrom + " to " + otpEndAt);
        } else {
            Log.e(TAG, "loadData: No phone numbers found in settings!");
        }

        // Load security config
        SecurityConfig config = databaseHelper.getSecurityConfig();
        Log.d(TAG, "loadData: Security config = " + (config != null ? "LOADED" : "NULL"));

        if (config != null) {
            pin = config.getPassword();
            shortCode = config.getShortCode();
            Log.d(TAG, "loadData: PIN = " + (pin != null ? "'" + pin + "' (length: " + pin.length() + ")" : "NULL"));
            Log.d(TAG, "loadData: ShortCode = " + (shortCode != null ? "'" + shortCode + "'" : "NULL"));
        } else {
            Log.e(TAG, "loadData: Security config not found!");
        }

        if (shuffledNumbers == null || shuffledNumbers.isEmpty()) {
            Log.w(TAG, "loadData: No phone numbers to process - disabling start button");
            tvStatus.setText("❌ NO PHONE NUMBERS! Go to Settings → General Settings and add phone numbers");
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f); // Visual indicator that button is disabled
            Toast.makeText(this, "⚠️ Please add phone numbers in Settings → General Settings", Toast.LENGTH_LONG).show();
        } else if (pin == null || pin.isEmpty()) {
            Log.w(TAG, "loadData: PIN not configured - disabling start button");
            tvStatus.setText("❌ NO PIN! Go to Settings → Security Config and add PIN");
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f); // Visual indicator that button is disabled
            Toast.makeText(this, "⚠️ Please add PIN in Settings → Security Config", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "loadData: All data loaded successfully, start button enabled");
            btnStart.setEnabled(true);
            btnStart.setAlpha(1.0f); // Button fully visible and enabled
            tvStatus.setText("✅ Ready to process " + shuffledNumbers.size() + " phone numbers. Click 'Start Processing' button.");
            Toast.makeText(this, "✅ Ready! All settings configured correctly.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUSSD() {
        Log.d(TAG, "initializeUSSD: Initializing USSD API");
        // Initialize USSD API instance
        ussdApi = USSDController.getInstance(this);
        Log.d(TAG, "initializeUSSD: USSD API initialized = " + (ussdApi != null ? "SUCCESS" : "FAILED"));
    }

    private void checkPermissionsAndStart() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "checkPermissionsAndStart: CALLED");
        Log.d(TAG, "========================================");

        // Debug: Show what we have loaded
        String debugInfo = "Phone numbers: " + (shuffledNumbers != null ? shuffledNumbers.size() : 0) +
                       ", PIN: " + (pin != null ? "SET" : "NOT SET") +
                       ", ShortCode: " + (shortCode != null ? shortCode : "NOT SET") +
                       ", USSD API: " + (ussdApi != null ? "INITIALIZED" : "NULL");
        Log.d(TAG, "checkPermissionsAndStart: " + debugInfo);
        Toast.makeText(this, debugInfo, Toast.LENGTH_LONG).show();

        // CRITICAL: Check if we have data
        if (shuffledNumbers == null || shuffledNumbers.isEmpty()) {
            Log.e(TAG, "checkPermissionsAndStart: BLOCKED - No phone numbers!");
            Toast.makeText(this, "ERROR: No phone numbers configured in settings!", Toast.LENGTH_LONG).show();
            tvStatus.setText("ERROR: No phone numbers. Go to Settings → General Settings");
            return;
        }

        if (pin == null || pin.isEmpty()) {
            Log.e(TAG, "checkPermissionsAndStart: BLOCKED - No PIN!");
            Toast.makeText(this, "ERROR: No PIN configured in settings!", Toast.LENGTH_LONG).show();
            tvStatus.setText("ERROR: No PIN. Go to Settings → Security Config");
            return;
        }

        if (ussdApi == null) {
            Log.e(TAG, "checkPermissionsAndStart: BLOCKED - USSD API is NULL! Trying to reinitialize...");
            initializeUSSD();
            if (ussdApi == null) {
                Toast.makeText(this, "ERROR: USSD API failed to initialize!", Toast.LENGTH_LONG).show();
                tvStatus.setText("ERROR: USSD API initialization failed");
                return;
            }
        }

        // Check phone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "checkPermissionsAndStart: Phone permission NOT granted, requesting...");
            Toast.makeText(this, "Phone permission required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_REQUEST_CODE);
            return;
        }
        Log.d(TAG, "checkPermissionsAndStart: Phone permission OK");

        // Check overlay permission (Display over other apps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.w(TAG, "checkPermissionsAndStart: Overlay permission NOT granted");
                Toast.makeText(this, "Overlay permission required for USSD", Toast.LENGTH_LONG).show();
                showOverlayPermissionDialog();
                return;
            }
            Log.d(TAG, "checkPermissionsAndStart: Overlay permission OK");
        }

        // Check accessibility service
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "checkPermissionsAndStart: Accessibility service NOT enabled");
            Toast.makeText(this, "Accessibility service not enabled", Toast.LENGTH_SHORT).show();
            showAccessibilityDialog();
            return;
        }
        Log.d(TAG, "checkPermissionsAndStart: Accessibility service OK");

        Toast.makeText(this, "Starting processing...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "checkPermissionsAndStart: All checks passed, calling startProcessing()");
        startProcessing();
    }

    private boolean isAccessibilityServiceEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String services = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                // Check for the library's USSDService
                return services != null && (services.contains(getPackageName()) ||
                       services.contains("com.jcussdlib.service.USSDService"));
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Display Over Other Apps Required")
                .setMessage("This app needs permission to display over other apps to show USSD dialogs.\n\nPlease enable 'Display over other apps' or 'Appear on top' permission for this app.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("Please enable the Thaciano App accessibility service to process USSD codes automatically.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startProcessing() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "startProcessing: CALLED");
        Log.d(TAG, "========================================");

        // Load configuration from database
        GeneralSettings settings = databaseHelper.getGeneralSettings();
        SecurityConfig security = databaseHelper.getSecurityConfig();

        if (settings == null || security == null) {
            Toast.makeText(this, "ERROR: Please configure settings first", Toast.LENGTH_LONG).show();
            tvStatus.setText("ERROR: Settings not configured!");
            return;
        }

        // Parse phone numbers
        String[] numbersArray = settings.getPhoneNumbers().split(",");
        List<String> phoneNumbers = new ArrayList<>();
        for (String number : numbersArray) {
            String trimmed = number.trim();
            if (!trimmed.isEmpty()) {
                phoneNumbers.add(trimmed);
            }
        }

        if (phoneNumbers.isEmpty()) {
            Toast.makeText(this, "ERROR: No phone numbers to process", Toast.LENGTH_LONG).show();
            tvStatus.setText("ERROR: No phone numbers loaded!");
            return;
        }

        // Shuffle for randomness
        Collections.shuffle(phoneNumbers);
        Log.d(TAG, "startProcessing: Processing " + phoneNumbers.size() + " phone numbers");

        // Initialize controller
        ussdController = USSDController.getInstance(this);

        // Create OTP brute force matcher
        String pin = security.getPassword();  // Your PIN
        String shortCodeTemplate = security.getShortCode();  // e.g., *348#

        // Build USSD template (replace # with *{pin}#)
        String ussdTemplate;
        if (shortCodeTemplate.endsWith("#")) {
            ussdTemplate = shortCodeTemplate.replace("#", "*{pin}#");
        } else {
            ussdTemplate = shortCodeTemplate + "*{pin}#";
        }

        Log.d(TAG, "startProcessing: USSD template: " + ussdTemplate);
        Log.d(TAG, "startProcessing: PIN: " + pin);

        otpMatcher = new OTPBruteForceMatcher(
            this,
            pin,
            ussdTemplate,
            0  // SIM slot (0 for SIM1, -1 for default)
        );

        // Calculate OTP digits from settings
        int otpDigits = String.valueOf(settings.getEndAt()).length();  // 6-digit if endAt=999999
        Log.d(TAG, "startProcessing: OTP digits: " + otpDigits + " (from " + settings.getStartFrom() + " to " + settings.getEndAt() + ")");

        // Reset counters
        successCount = 0;
        failureCount = 0;
        totalAttempts = 0;

        // Update UI
        runOnUiThread(() -> {
            tvStatus.setText("Starting OTP matching for " + phoneNumbers.size() + " phones...");
            progressBar.setMax(phoneNumbers.size());
            progressBar.setProgress(0);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnClose.setEnabled(false);
        });

        // Start matching!
        otpMatcher.startMatching(phoneNumbers, otpDigits, new OTPBruteForceMatcher.MatchingCallback() {

            @Override
            public void onMatchingStarted(int totalPhones, int totalOTPs) {
                runOnUiThread(() -> {
                    tvStatus.setText("Processing " + totalPhones + " phones with " + totalOTPs + " OTP combinations");
                    Toast.makeText(ProcessingActivity.this,
                        "Started: " + totalPhones + " phones, " + totalOTPs + " OTPs",
                        Toast.LENGTH_SHORT).show();
                });
                Log.d(TAG, "onMatchingStarted: " + totalPhones + " phones, " + totalOTPs + " OTPs");
            }

            @Override
            public void onPhoneStarted(String phone, int phoneIndex, int totalPhones) {
                runOnUiThread(() -> {
                    tvCurrentNumber.setText("Phone: " + phone + " (" + phoneIndex + "/" + totalPhones + ")");
                    tvProgress.setText("Progress: " + (phoneIndex - 1) + "/" + totalPhones);
                    progressBar.setProgress(phoneIndex - 1);
                });
                Log.d(TAG, "onPhoneStarted: [" + phoneIndex + "/" + totalPhones + "] " + phone);
            }

            @Override
            public void onOTPAttempt(String phone, String otp, int attemptNumber) {
                runOnUiThread(() -> {
                    tvStatus.setText("Trying OTP: " + otp + " (Attempt #" + attemptNumber + ")");
                });
                Log.d(TAG, "onOTPAttempt: " + phone + " → OTP: " + otp + " (attempt " + attemptNumber + ")");
            }

            @Override
            public void onOTPSuccess(String phone, String otp, int attemptNumber) {
                runOnUiThread(() -> {
                    Toast.makeText(ProcessingActivity.this,
                        "✓ SUCCESS: " + phone + " → " + otp,
                        Toast.LENGTH_SHORT).show();
                });
                Log.d(TAG, "✓ OTP SUCCESS: " + phone + " → OTP: " + otp + " (took " + attemptNumber + " attempts)");
            }

            @Override
            public void onOTPFailure(String phone, String otp, String errorMessage) {
                Log.d(TAG, "✗ OTP FAILED: " + phone + " → OTP: " + otp + " - " + errorMessage);
            }

            @Override
            public void onPhoneCompleted(OTPBruteForceMatcher.MatchResult result) {
                // Save to database
                databaseHelper.saveOtpResult(
                    result.phone,
                    result.matchedOTP,
                    result.attemptsCount
                );

                runOnUiThread(() -> {
                    successCount++;
                    totalAttempts += result.attemptsCount;

                    int avgAttempts = totalAttempts / successCount;
                    tvProgress.setText("Success: " + successCount + " | Failed: " + failureCount + " | Avg: " + avgAttempts);

                    Toast.makeText(ProcessingActivity.this,
                        "✓ " + result.phone + " → " + result.matchedOTP + " (" + result.attemptsCount + " attempts)",
                        Toast.LENGTH_LONG).show();
                });

                Log.d(TAG, "✓ PHONE COMPLETED: " + result.toString());
            }

            @Override
            public void onPhoneFailed(String phone, String reason) {
                runOnUiThread(() -> {
                    failureCount++;
                    tvProgress.setText("Success: " + successCount + " | Failed: " + failureCount);
                    Toast.makeText(ProcessingActivity.this,
                        "✗ FAILED: " + phone + " - " + reason,
                        Toast.LENGTH_SHORT).show();
                });
                Log.w(TAG, "✗ PHONE FAILED: " + phone + " - " + reason);
            }

            @Override
            public void onAllPhonesCompleted(Map<String, OTPBruteForceMatcher.MatchResult> matches,
                                            List<String> failed,
                                            long totalDurationMs) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progressBar.getMax());
                    tvStatus.setText("COMPLETED!");

                    double successRate = (double) matches.size() / (matches.size() + failed.size()) * 100;
                    double avgTimePerPhone = totalDurationMs / (matches.size() + failed.size()) / 1000.0;
                    int avgAttemptsPerSuccess = successCount > 0 ? totalAttempts / successCount : 0;

                    String summary = String.format(
                        "Processing Complete!\n\n" +
                        "Successful: %d\n" +
                        "Failed: %d\n" +
                        "Success Rate: %.1f%%\n\n" +
                        "Total Time: %.1f minutes\n" +
                        "Avg Time/Phone: %.1f seconds\n" +
                        "Avg Attempts/Success: %d",
                        matches.size(),
                        failed.size(),
                        successRate,
                        totalDurationMs / 60000.0,
                        avgTimePerPhone,
                        avgAttemptsPerSuccess
                    );

                    new AlertDialog.Builder(ProcessingActivity.this)
                        .setTitle("Processing Complete")
                        .setMessage(summary)
                        .setPositiveButton("OK", (dialog, which) -> {
                            btnStart.setEnabled(true);
                            btnStop.setEnabled(false);
                            btnClose.setEnabled(true);
                        })
                        .setCancelable(false)
                        .show();
                });

                Log.d(TAG, "========================================");
                Log.d(TAG, "ALL PHONES COMPLETED!");
                Log.d(TAG, "  Successful: " + matches.size());
                Log.d(TAG, "  Failed: " + failed.size());
                Log.d(TAG, "  Total duration: " + (totalDurationMs / 1000) + " seconds");
                Log.d(TAG, "========================================");
            }
        });

        Log.d(TAG, "startProcessing: OTP matching initiated");
        Log.d(TAG, "========================================");
    }

    private void stopProcessing() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "stopProcessing: CALLED");
        Log.d(TAG, "========================================");

        isProcessing = false;
        isUSSDActive = false;
        sessionCompleted = true; // Prevent any callbacks from continuing

        // Cancel any pending timeout
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
            Log.d(TAG, "stopProcessing: Cancelled pending timeout");
        }

        // Remove any pending handler callbacks
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "stopProcessing: Cleared all pending handler tasks");

        tvStatus.setText("Processing stopped by user");
        Toast.makeText(this, "Processing stopped", Toast.LENGTH_SHORT).show();

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnClose.setEnabled(true);

        Log.d(TAG, "stopProcessing: Complete");
    }

    private void finishProcessing() {
        isProcessing = false;

        // Calculate session statistics
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        long durationSeconds = sessionDuration / 1000;
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;

        // Calculate success rate
        int totalProcessed = successCount + failureCount;
        double successRate = totalProcessed > 0 ? (successCount * 100.0 / totalProcessed) : 0;

        String stats = String.format("✅ Success: %d | ❌ Failed: %d | ⏱️ Time: %dm %ds | 📊 Rate: %.1f%%",
                                    successCount, failureCount, minutes, seconds, successRate);

        tvStatus.setText(stats);
        tvCurrentNumber.setText("Session completed");

        Log.d(TAG, "========================================");
        Log.d(TAG, "SESSION COMPLETE - STATISTICS:");
        Log.d(TAG, "  Total phones processed: " + totalProcessed);
        Log.d(TAG, "  Successful: " + successCount);
        Log.d(TAG, "  Failed: " + failureCount);
        Log.d(TAG, "  Total OTP attempts: " + totalAttempts);
        Log.d(TAG, "  Session duration: " + minutes + "m " + seconds + "s");
        Log.d(TAG, "  Success rate: " + String.format("%.2f%%", successRate));
        Log.d(TAG, "  Average time per phone: " + (totalProcessed > 0 ? (durationSeconds / totalProcessed) + "s" : "N/A"));
        Log.d(TAG, "========================================");

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnClose.setEnabled(true);

        Toast.makeText(this, stats, Toast.LENGTH_LONG).show();
    }

    private void updateProgress() {
        int total = shuffledNumbers.size();
        int current = currentIndex + 1;
        tvProgress.setText(current + " / " + total);
        progressBar.setMax(total);
        progressBar.setProgress(current);
    }

    private void updateUI() {
        if (shuffledNumbers != null && !shuffledNumbers.isEmpty()) {
            tvProgress.setText("0 / " + shuffledNumbers.size());
            progressBar.setMax(shuffledNumbers.size());
            progressBar.setProgress(0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndStart();
            } else {
                Toast.makeText(this, "Phone permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up resources");

        // Stop and cleanup OTP matcher
        if (otpMatcher != null) {
            otpMatcher.stopMatching();
            otpMatcher.cleanup();
            Log.d(TAG, "onDestroy: OTP matcher stopped and cleaned up");
        }

        // Cleanup USSD controller
        if (ussdController != null) {
            ussdController.cleanup();
            Log.d(TAG, "onDestroy: USSD controller cleaned up");
        }

        // Close database
        if (databaseHelper != null) {
            databaseHelper.close();
            Log.d(TAG, "onDestroy: Database helper closed");
        }

        Log.d(TAG, "onDestroy: Cleanup complete");
    }
}
