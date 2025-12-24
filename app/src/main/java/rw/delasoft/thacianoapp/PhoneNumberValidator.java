package rw.delasoft.thacianoapp;

import java.util.regex.Pattern;

public class PhoneNumberValidator {

    // Rwandan phone numbers - Local format only:
    // Format: 07XXXXXXXX (10 digits - local format)
    // Starts with 07 followed by 8 more digits

    private static final Pattern RWANDAN_PATTERN_LOCAL = Pattern.compile("^0[7][0-9]{8}$");

    /**
     * Validates if a phone number is a valid Rwandan number (07XXXXXXXX format)
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidRwandanNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Remove all spaces, dashes, and parentheses
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-()]", "");

        // Check if it matches local format (07XXXXXXXX)
        return RWANDAN_PATTERN_LOCAL.matcher(cleanNumber).matches();
    }

    /**
     * Normalizes a Rwandan phone number by removing spaces and special characters
     * @param phoneNumber The phone number to normalize
     * @return Normalized phone number or null if invalid
     */
    public String normalizeNumber(String phoneNumber) {
        if (!isValidRwandanNumber(phoneNumber)) {
            return null;
        }

        // Remove all spaces, dashes, and parentheses
        return phoneNumber.replaceAll("[\\s\\-()]", "");
    }
}
