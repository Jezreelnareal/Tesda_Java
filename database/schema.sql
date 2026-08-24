CREATE DATABASE IF NOT EXISTS jcash_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jcash_db;

CREATE TABLE IF NOT EXISTS users (
    mobile_number VARCHAR(11) NOT NULL,
    pin CHAR(4) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (mobile_number)
);
