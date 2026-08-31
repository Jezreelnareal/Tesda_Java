package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JCash customer and controls changes to their balance and
 * transaction history.
 */
public class User {

    private String fullName;
    private String mobileNumber;
    private String pin;
    private BigDecimal balance;
    private final List<Transaction> transactions;

    public User() {
        balance = BigDecimal.ZERO;
        transactions = new ArrayList<>();
    }

    public User(String fullName, String mobileNumber, String pin) {
        this();
        setFullName(fullName);
        setMobileNumber(mobileNumber);
        setPin(pin);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name cannot be null or empty"
            );
        }
        this.fullName = fullName.trim();
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain 11 digits and start with 09"
            );
        }
        this.mobileNumber = mobileNumber;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "PIN must contain exactly 4 digits"
            );
        }
        this.pin = pin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        requirePositiveAmount(amount, "Deposit");
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositiveAmount(amount, "Withdrawal");

        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        balance = balance.subtract(amount);
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        transactions.add(transaction);
    }

    private static void requirePositiveAmount(BigDecimal amount, String label) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " amount must be positive");
        }
    }
}
