package rw.delasoft.thacianoapp;

public class OtpResult {
    private int id;
    private String phoneNumber;
    private String successfulOtp;
    private String timestamp;
    private int attemptCount; // How many OTPs tried before success

    public OtpResult() {
    }

    public OtpResult(String phoneNumber, String successfulOtp, int attemptCount) {
        this.phoneNumber = phoneNumber;
        this.successfulOtp = successfulOtp;
        this.attemptCount = attemptCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSuccessfulOtp() {
        return successfulOtp;
    }

    public void setSuccessfulOtp(String successfulOtp) {
        this.successfulOtp = successfulOtp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    @Override
    public String toString() {
        return "Phone: " + phoneNumber + " | OTP: " + successfulOtp + " | Attempts: " + attemptCount;
    }
}
