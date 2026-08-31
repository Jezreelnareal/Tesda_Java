package service;

import java.sql.SQLException;
import model.User;
import repository.UserRepository;

public final class Auth {

    private final UserRepository userRepository;

    public Auth() {
        this(new UserRepository());
    }

    public Auth(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException(
                    "User repository cannot be null"
            );
        }
        this.userRepository = userRepository;
    }

    public User authenticate(String mobileNumber, String pin)
            throws SQLException {
        if (!hasValidCredentialFormat(mobileNumber, pin)) {
            return null;
        }

        String normalizedMobileNumber = mobileNumber.trim();
        String normalizedPin = pin.trim();
        User user = userRepository.findByMobileNumber(normalizedMobileNumber);

        if (user == null || !user.getPin().equals(normalizedPin)) {
            return null;
        }

        return user;
    }

    private static boolean hasValidCredentialFormat(
            String mobileNumber,
            String pin
    ) {
        return mobileNumber != null
                && pin != null
                && mobileNumber.trim().matches("09\\d{9}")
                && pin.trim().matches("\\d{4}");
    }
}
