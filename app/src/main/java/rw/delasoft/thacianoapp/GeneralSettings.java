package rw.delasoft.thacianoapp;

public class GeneralSettings {
    private String title;
    private int totalCounts;
    private int startFrom;
    private int endAt;
    private String phoneNumbers;

    public GeneralSettings() {
    }

    public GeneralSettings(String title, int totalCounts, int startFrom, int endAt, String phoneNumbers) {
        this.title = title;
        this.totalCounts = totalCounts;
        this.startFrom = startFrom;
        this.endAt = endAt;
        this.phoneNumbers = phoneNumbers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTotalCounts() {
        return totalCounts;
    }

    public void setTotalCounts(int totalCounts) {
        this.totalCounts = totalCounts;
    }

    public int getStartFrom() {
        return startFrom;
    }

    public void setStartFrom(int startFrom) {
        this.startFrom = startFrom;
    }

    public int getEndAt() {
        return endAt;
    }

    public void setEndAt(int endAt) {
        this.endAt = endAt;
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
}
