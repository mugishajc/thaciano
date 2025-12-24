package rw.delasoft.thacianoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ThacianoApp.db";
    private static final int DATABASE_VERSION = 2; // Incremented for new table

    // General Settings Table
    private static final String TABLE_GENERAL_SETTINGS = "general_settings";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_TOTAL_COUNTS = "total_counts";
    private static final String COL_START_FROM = "start_from";
    private static final String COL_END_AT = "end_at";
    private static final String COL_PHONE_NUMBERS = "phone_numbers";

    // Security Configuration Table
    private static final String TABLE_SECURITY_CONFIG = "security_config";
    private static final String COL_PASSWORD = "password";
    private static final String COL_SHORT_CODE = "short_code";

    // OTP Results Table
    private static final String TABLE_OTP_RESULTS = "otp_results";
    private static final String COL_PHONE_NUMBER = "phone_number";
    private static final String COL_SUCCESSFUL_OTP = "successful_otp";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_ATTEMPT_COUNT = "attempt_count";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create general settings table
        String createGeneralSettingsTable = "CREATE TABLE " + TABLE_GENERAL_SETTINGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_TOTAL_COUNTS + " INTEGER, " +
                COL_START_FROM + " INTEGER, " +
                COL_END_AT + " INTEGER, " +
                COL_PHONE_NUMBERS + " TEXT)";
        db.execSQL(createGeneralSettingsTable);

        // Create security config table
        String createSecurityConfigTable = "CREATE TABLE " + TABLE_SECURITY_CONFIG + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PASSWORD + " TEXT, " +
                COL_SHORT_CODE + " TEXT)";
        db.execSQL(createSecurityConfigTable);

        // Create OTP results table
        String createOtpResultsTable = "CREATE TABLE " + TABLE_OTP_RESULTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PHONE_NUMBER + " TEXT, " +
                COL_SUCCESSFUL_OTP + " TEXT, " +
                COL_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                COL_ATTEMPT_COUNT + " INTEGER)";
        db.execSQL(createOtpResultsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add OTP results table for version 2
            String createOtpResultsTable = "CREATE TABLE " + TABLE_OTP_RESULTS + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PHONE_NUMBER + " TEXT, " +
                    COL_SUCCESSFUL_OTP + " TEXT, " +
                    COL_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COL_ATTEMPT_COUNT + " INTEGER)";
            db.execSQL(createOtpResultsTable);
        }
    }

    // Save or update general settings
    public boolean saveGeneralSettings(String title, int totalCounts, int startFrom, int endAt, String phoneNumbers) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_TOTAL_COUNTS, totalCounts);
        values.put(COL_START_FROM, startFrom);
        values.put(COL_END_AT, endAt);
        values.put(COL_PHONE_NUMBERS, phoneNumbers);

        // Check if settings already exist
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GENERAL_SETTINGS, null);
        if (cursor.getCount() > 0) {
            // Update existing record
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
            cursor.close();
            int result = db.update(TABLE_GENERAL_SETTINGS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
            return result > 0;
        } else {
            // Insert new record
            cursor.close();
            long result = db.insert(TABLE_GENERAL_SETTINGS, null, values);
            return result != -1;
        }
    }

    // Get general settings
    public GeneralSettings getGeneralSettings() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GENERAL_SETTINGS + " ORDER BY " + COL_ID + " DESC LIMIT 1", null);

        if (cursor.moveToFirst()) {
            GeneralSettings settings = new GeneralSettings();
            settings.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
            settings.setTotalCounts(cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_COUNTS)));
            settings.setStartFrom(cursor.getInt(cursor.getColumnIndexOrThrow(COL_START_FROM)));
            settings.setEndAt(cursor.getInt(cursor.getColumnIndexOrThrow(COL_END_AT)));
            settings.setPhoneNumbers(cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE_NUMBERS)));
            cursor.close();
            return settings;
        }

        cursor.close();
        return null;
    }

    // Save or update security configuration
    public boolean saveSecurityConfig(String password, String shortCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PASSWORD, password);
        values.put(COL_SHORT_CODE, shortCode);

        // Check if config already exists
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SECURITY_CONFIG, null);
        if (cursor.getCount() > 0) {
            // Update existing record
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
            cursor.close();
            int result = db.update(TABLE_SECURITY_CONFIG, values, COL_ID + "=?", new String[]{String.valueOf(id)});
            return result > 0;
        } else {
            // Insert new record
            cursor.close();
            long result = db.insert(TABLE_SECURITY_CONFIG, null, values);
            return result != -1;
        }
    }

    // Get security configuration
    public SecurityConfig getSecurityConfig() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SECURITY_CONFIG + " ORDER BY " + COL_ID + " DESC LIMIT 1", null);

        if (cursor.moveToFirst()) {
            SecurityConfig config = new SecurityConfig();
            config.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)));
            config.setShortCode(cursor.getString(cursor.getColumnIndexOrThrow(COL_SHORT_CODE)));
            cursor.close();
            return config;
        }

        cursor.close();
        return null;
    }

    // Delete all phone numbers
    public boolean deleteAllPhoneNumbers() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHONE_NUMBERS, ""); // Clear phone numbers

        // Update all records in the table to clear phone numbers
        int result = db.update(TABLE_GENERAL_SETTINGS, values, null, null);

        // Return true if at least one row was updated, or if there are no rows (nothing to delete)
        return result >= 0;
    }

    // Delete a specific phone number
    public boolean deletePhoneNumber(String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        GeneralSettings settings = getGeneralSettings();
        if (settings != null && settings.getPhoneNumbers() != null) {
            String phoneNumbers = settings.getPhoneNumbers();
            String[] numbersArray = phoneNumbers.split(",");
            StringBuilder newPhoneNumbers = new StringBuilder();

            for (String number : numbersArray) {
                String trimmedNumber = number.trim();
                if (!trimmedNumber.equals(phoneNumber.trim())) {
                    if (newPhoneNumbers.length() > 0) {
                        newPhoneNumbers.append(", ");
                    }
                    newPhoneNumbers.append(trimmedNumber);
                }
            }

            ContentValues values = new ContentValues();
            values.put(COL_TITLE, settings.getTitle());
            values.put(COL_TOTAL_COUNTS, settings.getTotalCounts());
            values.put(COL_START_FROM, settings.getStartFrom());
            values.put(COL_END_AT, settings.getEndAt());
            values.put(COL_PHONE_NUMBERS, newPhoneNumbers.toString());

            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GENERAL_SETTINGS, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                cursor.close();
                int result = db.update(TABLE_GENERAL_SETTINGS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
                return result > 0;
            }
            cursor.close();
        }
        return false;
    }

    // Save successful OTP result
    public boolean saveOtpResult(String phoneNumber, String successfulOtp, int attemptCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHONE_NUMBER, phoneNumber);
        values.put(COL_SUCCESSFUL_OTP, successfulOtp);
        values.put(COL_ATTEMPT_COUNT, attemptCount);

        long result = db.insert(TABLE_OTP_RESULTS, null, values);
        return result != -1;
    }

    // Get all OTP results
    public java.util.List<OtpResult> getAllOtpResults() {
        java.util.List<OtpResult> results = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_OTP_RESULTS + " ORDER BY " + COL_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                OtpResult result = new OtpResult();
                result.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                result.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE_NUMBER)));
                result.setSuccessfulOtp(cursor.getString(cursor.getColumnIndexOrThrow(COL_SUCCESSFUL_OTP)));
                result.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)));
                result.setAttemptCount(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATTEMPT_COUNT)));
                results.add(result);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    // Get count of successful OTPs
    public int getSuccessfulOtpCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_OTP_RESULTS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Clear all OTP results
    public boolean clearOtpResults() {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_OTP_RESULTS, null, null);
        return result >= 0;
    }
}
