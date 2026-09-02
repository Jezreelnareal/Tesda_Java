USE jcash_db;

INSERT INTO admins (username, pin)
VALUES (
    'admin',
    'pbkdf2_sha256$210000$q4Q3b5dQzpK0hvYGJQfSHA==$R1gfJJTj2NgKb8RAh5gx4jUuAGYd6oSvZ4HWzxi/4cg='
);

INSERT INTO users (mobile_number, pin, full_name, balance)
VALUES
    (
        '09171234567',
        'pbkdf2_sha256$210000$AR2//FlhIVVRNw+8b1jdpw==$zRZwgm/NUcCZ/7ZORCSTGdPlj91J52ZwbDMIy/EqRAg=',
        'Juan Dela Cruz',
        1000.00
    ),
    (
        '09181234567',
        'pbkdf2_sha256$210000$QmWQKZTW2udMb6iNwdUDRw==$DGsMdowJmCC8FuVs7AIfQ514kzeEJgURhwLNCBFA9AQ=',
        'Maria Santos',
        500.00
    );
