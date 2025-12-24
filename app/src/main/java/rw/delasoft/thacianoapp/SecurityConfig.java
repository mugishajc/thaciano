package rw.delasoft.thacianoapp;

public class SecurityConfig {
    private String password;
    private String shortCode;

    public SecurityConfig() {
    }

    public SecurityConfig(String password, String shortCode) {
        this.password = password;
        this.shortCode = shortCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
}
