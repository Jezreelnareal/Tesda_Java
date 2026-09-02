package model;

public record Admin(String username, String pin) {

    public Admin {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Admin username must contain 3 to 30 valid characters"
            );
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "Admin PIN must contain exactly 4 digits"
            );
        }
    }
}
