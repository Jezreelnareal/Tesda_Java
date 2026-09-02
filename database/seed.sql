USE jcash_db;

INSERT INTO admins (username, pin)
VALUES ('admin', '1234');

INSERT INTO users (mobile_number, pin, full_name, balance)
VALUES
    ('09171234567', '1234', 'Juan Dela Cruz', 1000.00),
    ('09181234567', '5678', 'Maria Santos', 500.00);
