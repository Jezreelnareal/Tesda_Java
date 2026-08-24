/**
 * User class represents a user in the system with their personal information, balance, and transaction history.
 */
package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class User {

    private String fullName;
    private String mobileNumber;
    private String pin;
    private BigDecimal balance;
    private List<Transaction> transactions;

    /**
     * Default constructor initializes the user's balance to zero and creates an
     * empty transaction list.
     */
    public User() {
        this.balance = BigDecimal.ZERO;
        this.transactions = new ArrayList<>();
    }

    /**
     * Constructor to initialize the user with personal information.
     *
     * @param fullName The full name of the user.
     * @param mobileNumber The mobile number of the user.
     * @param pin The PIN of the user.
     */
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
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        this.fullName = fullName.trim();
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) { // Validates that the mobile number starts with "09" and is followed by 9 digits
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
        if (pin == null || !pin.matches("\\d{4}")) { // Validates that the PIN contains exactly 4 digits
            throw new IllegalArgumentException("PIN must contain exactly 4 digits");
        }
        this.pin = pin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Deposits a specified amount into the user's balance.
     *
     * @param amount The amount to deposit.
     * @throws IllegalArgumentException if the amount is null or not positive.
     */
    public void deposit(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        balance = balance.add(amount);

    }

    /**
     * Withdraws a specified amount from the user's balance.
     *
     * @param amount The amount to withdraw.
     * @throws IllegalArgumentException if the amount is null, not positive, or
     * exceeds the current balance.
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        balance = balance.subtract(amount);
    }

    /**
     * Returns a copy of the user's transaction history.
     *
     * @return A list of transactions.
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Adds a transaction to the user's transaction history.
     *
     * @param transaction The transaction to add.
     * @throws IllegalArgumentException if the transaction is null.
     */
    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        transactions.add(transaction);
    }
}
