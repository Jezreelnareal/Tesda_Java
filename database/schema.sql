DROP DATABASE IF EXISTS jcash_db;

CREATE DATABASE jcash_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jcash_db;

CREATE TABLE users (
    mobile_number VARCHAR(11) NOT NULL,
    pin CHAR(4) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (mobile_number)
);

CREATE TABLE admins (
    username VARCHAR(30) NOT NULL,
    pin CHAR(4) NOT NULL,
    PRIMARY KEY (username)
);

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    details VARCHAR(255) NOT NULL,
    transaction_date_time DATETIME NOT NULL,
    sender_mobile_number VARCHAR(11) NULL,
    receiver_mobile_number VARCHAR(11) NULL,
    admin_username VARCHAR(30) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_sender
        FOREIGN KEY (sender_mobile_number)
        REFERENCES users (mobile_number)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_receiver
        FOREIGN KEY (receiver_mobile_number)
        REFERENCES users (mobile_number)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_admin
        FOREIGN KEY (admin_username)
        REFERENCES admins (username)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN (
            'CASH_IN', 'WITHDRAWAL', 'TRANSFER',
            'ADMIN_CREDIT', 'ADMIN_DEBIT'
        )),
    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0.00),
    CONSTRAINT chk_transactions_participants
        CHECK (
            (transaction_type = 'CASH_IN'
                AND sender_mobile_number IS NULL
                AND receiver_mobile_number IS NOT NULL
                AND admin_username IS NULL)
            OR
            (transaction_type = 'WITHDRAWAL'
                AND sender_mobile_number IS NOT NULL
                AND receiver_mobile_number IS NULL
                AND admin_username IS NULL)
            OR
            (transaction_type = 'TRANSFER'
                AND sender_mobile_number IS NOT NULL
                AND receiver_mobile_number IS NOT NULL
                AND admin_username IS NULL)
            OR
            (transaction_type = 'ADMIN_CREDIT'
                AND sender_mobile_number IS NULL
                AND receiver_mobile_number IS NOT NULL
                AND admin_username IS NOT NULL)
            OR
            (transaction_type = 'ADMIN_DEBIT'
                AND sender_mobile_number IS NOT NULL
                AND receiver_mobile_number IS NULL
                AND admin_username IS NOT NULL)
        ),
    INDEX idx_transactions_sender (sender_mobile_number),
    INDEX idx_transactions_receiver (receiver_mobile_number),
    INDEX idx_transactions_admin (admin_username),
    INDEX idx_transactions_date_time (transaction_date_time)
);
