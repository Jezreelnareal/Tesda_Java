package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Creates and verifies salted PBKDF2 hashes for four-digit JCash PINs.
 */
public final class PinHasher {

    private static final String FORMAT_NAME = "pbkdf2_sha256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PinHasher() {
        // Utility class
    }

    public static String hash(String pin) {
        requirePin(pin);
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] derivedKey = derive(pin, salt, ITERATIONS, HASH_BYTES);
        return FORMAT_NAME + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivedKey);
    }

    public static boolean verify(String pin, String encodedHash) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return false;
        }

        HashParts parts = parse(encodedHash);
        if (parts == null) {
            return false;
        }

        byte[] actual = derive(
                pin,
                parts.salt(),
                parts.iterations(),
                parts.hash().length
        );
        return MessageDigest.isEqual(parts.hash(), actual);
    }

    public static boolean isEncodedHash(String value) {
        return parse(value) != null;
    }

    public static boolean isLegacyPlaintextPin(String value) {
        return value != null && value.matches("\\d{4}");
    }

    private static HashParts parse(String encodedHash) {
        if (encodedHash == null) {
            return null;
        }

        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !FORMAT_NAME.equals(parts[0])) {
            return null;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            if (iterations <= 0 || salt.length < SALT_BYTES
                    || hash.length < HASH_BYTES) {
                return null;
            }
            return new HashParts(iterations, salt, hash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static byte[] derive(
            String pin,
            byte[] salt,
            int iterations,
            int outputBytes
    ) {
        PBEKeySpec specification = new PBEKeySpec(
                pin.toCharArray(),
                salt,
                iterations,
                outputBytes * Byte.SIZE
        );
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(specification)
                    .getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException(
                    "PBKDF2 PIN hashing is unavailable",
                    exception
            );
        } finally {
            specification.clearPassword();
        }
    }

    private static void requirePin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "PIN must contain exactly 4 digits"
            );
        }
    }

    private record HashParts(int iterations, byte[] salt, byte[] hash) {
    }
}
