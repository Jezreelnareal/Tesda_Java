package service;

import java.sql.SQLException;
import model.Admin;
import model.User;
import repository.AdminRepository;
import repository.UserRepository;
import util.PinHasher;

public final class Auth {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public Auth() {
        this(new UserRepository(), new AdminRepository());
    }

    public Auth(UserRepository userRepository) {
        this(userRepository, new AdminRepository());
    }

    public Auth(
            UserRepository userRepository,
            AdminRepository adminRepository
    ) {
        if (userRepository == null || adminRepository == null) {
            throw new IllegalArgumentException(
                    "Authentication repositories cannot be null"
            );
        }
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    public User authenticate(String mobileNumber, String pin)
            throws SQLException {
        if (!hasValidCredentialFormat(mobileNumber, pin)) {
            return null;
        }

        String normalizedMobileNumber = mobileNumber.trim();
        String normalizedPin = pin.trim();
        User user = userRepository.findByMobileNumber(normalizedMobileNumber);

        if (user == null
                || !PinHasher.verify(normalizedPin, user.getPinHash())) {
            return null;
        }

        return user;
    }

    public Admin authenticateAdmin(String username, String pin)
            throws SQLException {
        if (username == null || pin == null
                || !username.trim().matches("[A-Za-z0-9_.-]{3,30}")
                || !pin.trim().matches("\\d{4}")) {
            return null;
        }

        Admin admin = adminRepository.findByUsername(username.trim());
        return admin != null
                && PinHasher.verify(pin.trim(), admin.pinHash())
                ? admin : null;
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
