package rw.delasoft.thacianoapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhoneNumberProcessor {

    /**
     * Shuffles phone numbers randomly
     * This helps mix them to prevent OTP issues
     */
    public static List<String> shufflePhoneNumbers(List<String> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> shuffled = new ArrayList<>(phoneNumbers);
        Collections.shuffle(shuffled); // Random shuffling

        return shuffled;
    }

    /**
     * Get formatted USSD code with short code and PIN
     * For example: if shortCode is "*348#" and pin is "1234"
     * It will insert the PIN to get "*348*1234#"
     */
    public static String getUSSDCode(String shortCode, String pin) {
        if (shortCode == null || shortCode.isEmpty()) {
            // Fallback to default format
            return "*348*" + pin + "#";
        }

        // Insert PIN into the short code
        // Pattern: *XXX# -> *XXX*PIN#
        // Example: *348# becomes *348*1234#
        String ussdCode = shortCode.replace("#", "*" + pin + "#");
        return ussdCode;
    }

    /**
     * Gets the sequence: enter 1, then phone number
     */
    public static List<String> getUSSDSequence(String phoneNumber) {
        List<String> sequence = new ArrayList<>();
        sequence.add("1"); // First prompt response
        sequence.add(phoneNumber); // Second prompt response
        return sequence;
    }
}
