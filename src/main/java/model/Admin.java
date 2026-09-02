package model;

import util.PinHasher;

public record Admin(String username, String pinHash) {

    public Admin {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Admin username must contain 3 to 30 valid characters"
            );
        }
        if (!PinHasher.isEncodedHash(pinHash)) {
            throw new IllegalArgumentException("Stored admin PIN hash is invalid");
        }
    }
}
