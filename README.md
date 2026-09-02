# JCash Banking System

JCash is a Java Swing banking application backed by MySQL and JDBC. It
supports PIN-based user and administrator login, transactional balance
updates, transaction history, administrative account management, and JDBC
performance benchmarking.

## Features

### User features

- Create a new zero-balance account from the welcome screen
- Sign in with a registered mobile number and four-digit PIN
- View the current balance and account details
- Cash in and withdraw funds
- Transfer funds to another registered mobile number
- Review cash-in, withdrawal, transfer, and admin-adjustment history
- Receive clear validation messages and detailed transaction receipts
- Search and filter transaction history and view recent activity
- Switch between light and dark mode for the current session
- Use a responsive dashboard with a compact sidebar on smaller windows
- Log out to the role-selection screen

### Administrator features

- Sign in with an admin username and four-digit PIN
- View all accounts or find one by mobile number
- Create zero-balance user accounts
- Credit or debit accounts with recorded admin audit transactions
- Generate aggregate totals and view the 100 most recent transactions
- Review live customer, combined-balance, and transaction metrics
- Search the customer table and view the 10 most recent system activities
- Log out to the role-selection screen

Both login types allow three failed attempts per application session.

## Technology

- Java Swing
- JDBC
- MySQL
- Maven dependency and build management
- FlatLaf light and dark themes
- Java Flight Recorder for performance evidence
- IntelliJ IDEA project configuration

## Requirements

- JDK 17 or newer; the current project has been verified with JDK 25
- MySQL 8 or newer
- Maven 3.9+ (IntelliJ IDEA's bundled Maven also works)
- IntelliJ IDEA is recommended; Connector/J and FlatLaf are downloaded by Maven

## Database setup

> [!WARNING]
> `database/schema.sql` runs `DROP DATABASE IF EXISTS jcash_db`. Running it
> permanently removes the current contents of `jcash_db` before recreating and
> seeding the assignment database.

1. Start MySQL.
2. Open [`database/schema.sql`](database/schema.sql) in MySQL Workbench or an
   IntelliJ database console and execute the whole file.
3. Open [`database/seed.sql`](database/seed.sql) and execute it afterward.
4. Confirm that the `users`, `admins`, and `transactions` tables exist in
   `jcash_db`.

The application uses these defaults:

| Setting | Default |
|---|---|
| JDBC URL | `jdbc:mysql://localhost:3306/jcash_db` |
| Database user | `root` |
| Database password | Empty |

Override the defaults in PowerShell when necessary:

```powershell
$env:JCASH_DB_URL = "jdbc:mysql://localhost:3306/jcash_db"
$env:JCASH_DB_USER = "root"
$env:JCASH_DB_PASSWORD = "your-password"
```

## IntelliJ setup and run

1. Open the repository directory (the folder containing `pom.xml`) in IntelliJ.
2. Select a JDK 17+ under **File > Project Structure > Project**.
3. Open the **Maven** tool window and select **Reload All Maven Projects**. If
   prompted, choose **Load Maven Project**. IntelliJ downloads FlatLaf and
   Connector/J automatically.
4. Open [`Main.java`](src/main/java/Main.java), click the green run arrow beside
   `main`, then choose **Run 'Main.main()'**.
5. Wait for the login form to show **Database connected**.

You can also run the `exec:java` goal from the Maven tool window under
**Plugins > exec**, or use a terminal:

```powershell
mvn clean compile
mvn exec:java
```

## Seeded logins

| Role | Account | PIN | Starting balance |
|---|---|---|---:|
| User | `09171234567` | `1234` | PHP 1,000.00 |
| User | `09181234567` | `5678` | PHP 500.00 |
| Admin | `admin` | `1234` | Not applicable |

These credentials are for development and assessment only. PINs remain in the
assignment-compatible plain four-digit format and are not suitable for a
production banking system.

## Manual acceptance test

Run the schema and seed files immediately before this test so the expected
balances are deterministic.

### 1. Navigation and login

1. Launch JCash and confirm that **User login**, **Admin login**, and
   **Create account** are available in that order.
2. Select **Dark mode**, confirm that the screen remains readable, then switch
   back to light mode. Resize the window below 1,050 pixels and confirm that
   the signed-in sidebar changes to its compact icon layout.
3. On either login form, confirm that the eye icon inside the PIN field reveals
   and hides the PIN.
4. Open the dedicated account-creation screen, confirm that mismatched PINs
   are rejected inline, and verify that successful registration opens the user
   login with the mobile number filled in.
5. Enter an incorrect user PIN and confirm that the remaining-attempt count
   decreases.
6. Restart the application if needed, then sign in as `09171234567` / `1234`.
7. Confirm that Juan Dela Cruz, a PHP 1,000.00 balance, and recent-activity
   panel are displayed.

To test the lockout separately, enter invalid credentials three times. The
selected login form must remain locked until JCash is closed and reopened.
User and admin attempt counts are independent.

### 2. User money operations

Perform these operations in order while signed in as Juan:

| Operation | Amount | Expected Juan balance |
|---|---:|---:|
| Starting balance | N/A | PHP 1,000.00 |
| Cash in | PHP 100.00 | PHP 1,100.00 |
| Withdraw | PHP 25.00 | PHP 1,075.00 |
| Transfer to `09181234567` | PHP 50.00 | PHP 1,025.00 |

Then verify:

- Maria Santos has PHP 550.00.
- Juan's account-details dialog shows his name, mobile/account number, and PHP
  1,025.00 balance.
- Juan's transaction history contains one cash-in, one withdrawal, and one
  sent transfer.
- Maria's history contains the received transfer.
- Each successful operation shows the amount and appropriate old/new balance.

### 3. User validation

Confirm that JCash rejects each case without changing balances or inserting a
transaction:

- Zero or negative amount
- More than two decimal places
- Withdrawal or transfer greater than the available balance
- Transfer to the sender's own mobile number
- Transfer to an unregistered mobile number
- Invalid mobile-number format

### 4. Administrator flow

1. Log out and sign in as `admin` / `1234`.
2. View all accounts and search for `09171234567`.
3. Create an account with:
   - Full name: `Test Account`
   - Mobile number: `09191234567`
   - PIN: `2468`
   - Confirm PIN: `2468`
4. Confirm that the account starts at PHP 0.00.
5. Credit the account PHP 200.00, then debit PHP 50.00.
6. Confirm that its final balance is PHP 150.00.
7. Generate the system report and confirm that it shows three users and the
   recent user/admin transactions.
8. Confirm that the test account's history contains `ADMIN_CREDIT` and
   `ADMIN_DEBIT` entries identifying the admin actor.
9. Confirm that creating the same mobile number again and deducting more than
   PHP 150.00 are rejected.

### 5. Logout and close

1. Log out and confirm that JCash returns to the role-selection screen.
2. Close the application window and confirm that it exits cleanly.

## PowerShell compile and run

Maven resolves every dependency, so no local Connector/J path is needed:

```powershell
mvn clean compile
mvn exec:java
```

## Performance benchmark

The performance runner compares a new physical JDBC connection per lookup
with the reusable single-threaded connection used by JCash:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File .\scripts\run-performance.ps1 `
    -Iterations 200
```

The script saves benchmark logs and Java Flight Recorder summaries under
`docs/performance/results`. See the
[`performance report`](docs/performance/REPORT.md) for the recorded analysis.

## Troubleshooting

### Database unavailable

- Confirm that MySQL is running.
- Confirm that `jcash_db` was created by the current schema.
- Check `JCASH_DB_URL`, `JCASH_DB_USER`, and `JCASH_DB_PASSWORD`.
- Use **Retry connection** on the login screen after fixing the database.

### Maven dependencies are red in IntelliJ

- Right-click `pom.xml` and choose **Add as Maven Project** if available.
- In the Maven tool window, select **Reload All Maven Projects**.
- Confirm IntelliJ is not in Maven offline mode and has internet access for the
  first dependency download.
- Use **File > Invalidate Caches** only if reloading Maven does not fix imports.

### `mvn` is not recognized in PowerShell

Use the Maven tool window in IntelliJ, or install Maven and add its `bin`
directory to `PATH`. The performance script automatically looks for IntelliJ's
bundled Maven when `mvn` is unavailable.

### Login is locked

Close and reopen JCash. The three-attempt limit applies to the current
application session.

## Additional documentation

- [`JCash usage reference`](docs/USAGE.md)
- [`Performance tuning report`](docs/performance/REPORT.md)
- [`Performance benchmark`](src/main/java/performance/PerformanceBenchmark.java)
