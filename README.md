# JCash Banking System

JCash is a banking simulation where customers can manage their balance and
transactions, while administrators can manage accounts and view reports.

| Part | Technology |
|---|---|
| Frontend | Next.js 16, React 19, and TypeScript |
| Backend | Java 26, Spring Boot 4.1.1, Spring MVC, and Spring Security |
| Database | MySQL 8.4 with JDBC, running in Docker |

## Features

- Customer registration and login.
- Balance checking, cash-in, withdrawal, and money transfers.
- Transaction receipts and searchable transaction history.
- Administrator account creation, search, balance adjustments, and reports.
- Three failed login attempts per role per browser session.
- Responsive design, light/dark themes, and PIN visibility controls.
- Automated tests and optional JDBC performance tools.

## Setup: step by step

These instructions use **Windows PowerShell**. Skip the installation steps
if this project is already set up on your computer.

### 1. Install the required tools

- **JDK 26** for the Java backend.
- **Node.js 24** with npm for the frontend.
- **Docker Desktop** with Linux containers for MySQL.

You do not need to install Maven separately. The included `mvnw` helper
downloads it automatically and selects Java 26 on Windows.

If you use IntelliJ, select **JDK 26** as the Project SDK and Maven runner JDK,
then reload `pom.xml`.

### 2. Open the project folder

Open PowerShell and run:

```powershell
cd C:\Users\jezre\Tesda_Java
```

Use your own folder path if you saved the project somewhere else.

### 3. Set up the database settings once

Create `.env` from the example only if it does not already exist:

```powershell
if (!(Test-Path .env)) {
    Copy-Item .env.example .env
}
```

Open `.env` in your editor. For a new installation, replace the placeholder
values for `MYSQL_ROOT_PASSWORD` and `JCASH_DB_PASSWORD` with your chosen
local passwords. Leave the other settings as provided.

**If your database already exists, keep its existing passwords.** You do not
need to repeat this step every time you start the project.

The backend and frontend both default to port **8081** for API communication.
You do not need to configure an API address in the terminal.

### 4. Install the frontend dependencies once

```powershell
cd frontend
npm ci
cd ..
```

Repeat `npm ci` when `package-lock.json` changes. Stop the frontend before
reinstalling dependencies.

### 5. Start the database and backend

Open **Docker Desktop** and wait until it is running. From the project folder:

```powershell
docker compose up -d --wait
.\mvnw spring-boot:run
```

Wait for the backend to start, and **leave this terminal open**.

To check the connection, open
[http://localhost:8081/api/health](http://localhost:8081/api/health).
You should see:

```json
{"status":"connected"}
```

### 6. Start the frontend

Open a **second PowerShell terminal** and run:

```powershell
cd C:\Users\jezre\Tesda_Java\frontend
npm run dev
```

Leave this terminal open, then visit **http://localhost:3000**.

The frontend connects to the backend automatically. Keep all three services
running: **Docker starts MySQL**, **Maven starts Java**, and **npm starts Next.js**.

## Starting the project next time

Open Docker Desktop, then run these commands in the first terminal:

```powershell
cd C:\Users\jezre\Tesda_Java
docker compose up -d --wait
.\mvnw spring-boot:run
```

In the second terminal:

```powershell
cd C:\Users\jezre\Tesda_Java\frontend
npm run dev
```

Open **http://localhost:3000**. No dependency reinstall or terminal configuration
is needed for normal startup.

## Seeded development logins

These accounts are created when MySQL initializes a new database:

| Role | Mobile number / username | PIN | Starting balance |
|---|---|---|---:|
| Customer: Juan | `09171234567` | `1234` | PHP 1,000.00 |
| Customer: Maria | `09181234567` | `5678` | PHP 500.00 |
| Administrator | `admin` | `1234` | N/A |

Existing accounts and balances may differ if you have used the app before.

## Using the application

**Customers:** Select **Personal account** and log in. Check your balance,
then open the **Cash in**, **Send money**, or **Withdraw** tab. Each tab has its own
form, balance preview, guidance, and recent records. Enter the details
and confirm. Open **Activity** to view or search transaction history.

**New customers:** Choose **Create an account**, enter your name, mobile number,
and matching four-digit PINs. Your account starts with a zero balance; log in
with the new credentials.

**Administrators:** Select **Administrator** and log in. Use **Accounts** to
search or create customers and apply credit/debit adjustments. Open **Reports**
to view transaction totals and recent activity.

**Issue 10 — test transaction cleanup:** In **Reports → Recent transactions**,
find a disposable test record (you can search by record ID) and select **Delete
log**. Review the record, type `DELETE <record ID>` exactly, then select **Delete
permanently**. The backend requires an administrator session and matching
confirmation before executing the JDBC delete. Missing records return a clear
error; successful deletion refreshes the dashboard.

This permanently removes the history row from customer logs and report totals.
It **does not reverse the transaction or change wallet balances**. There is no
automatic test-record classification, so use only disposable test data. For an
isolated demonstration, the browser tests create their own temporary database.
The existing `admin.TransactionCleanupTool` console utility remains available.

For a simple demo with fresh data, cash in PHP 100 to Juan, withdraw PHP 25,
then send PHP 50 to Maria. Juan should have PHP 1,025 and Maria PHP 550.

You can also try an invalid amount, insufficient funds, a nonexistent recipient,
or mismatched registration PINs to see validation messages. Customers cannot
access administrator features. Use the theme toggle to switch appearance and
**Sign out** when finished.

## Stopping the project

Press **Ctrl+C** in both server terminals. From the project folder, run:

```powershell
docker compose stop
```

Your database is preserved. Do not run `database/schema.sql` manually for
normal startup; it drops and recreates the database.

## Common problems

| Problem | What to check |
|---|---|
| Frontend cannot reach the backend | Keep the Java terminal running and check the health link in step 5. |
| Database connection fails | Check Docker Desktop, run `docker compose ps`, and verify the passwords in `.env`. |
| Port is already in use | Stop the previous instance before starting another. JCash uses port 8081 for Java and 3000 for Next.js. |
| Java version error | Use `.\mvnw` and make sure JDK 26 is installed and selected in IntelliJ. |
| PowerShell blocks `npm.ps1` | Use `npm.cmd run dev` as a fallback. |
| Login is locked | A role locks after three failed attempts in one browser session. Reloading or signing out does not reset it; use a new session or wait for 30 minutes of inactivity. |

To change the API port, update `JCASH_API_PORT` in the root `.env` and set the
matching `JCASH_API_URL` in `frontend/.env.local`, then restart both servers.
The frontend example file shows the format. Existing terminal environment
variables override saved file settings.

## Verification

These checks are optional when you just want to run the app:

| Check | Run from | Command |
|---|---|---|
| Backend tests | Project folder | `.\mvnw test` |
| Frontend types | `frontend` | `npm run typecheck` |
| Frontend build | `frontend` | `npm run build` |
| Browser tests | Project folder | `.\scripts\test-web.ps1` |
| Backend build | Project folder | `.\mvnw package` |

Browser tests need MySQL and Microsoft Edge. They use a temporary test database
and remove it afterward, preserving your normal database.

## Project folders

| Folder | Contains |
|---|---|
| `frontend/` | Next.js pages, React components, and browser tests |
| `src/main/java/` | Java backend, banking services, and JDBC repositories |
| `src/main/resources/` | Spring Boot configuration |
| `src/test/java/` | Backend tests |
| `database/` | Database schema and demo accounts |
| `.mvn/` | Maven and Java startup helpers |
| `scripts/` | Optional startup, test, and performance helpers |
| `docs/` | Security checklist and performance report |

JCash is a learning simulation. See the [security checklist](docs/SECURITY_READINESS.md)
for deployment limitations, the [Issue 11 web performance report](docs/performance/WEB_REPORT.md),
[PDF report](docs/performance/Issue11-Performance-Report.pdf), and the
[performance test guide](docs/performance/WEB_TEST_GUIDE.md) for the repeatable
HTTP benchmark.

Short repository operations use a bounded JDBC pool (default: five connections).
Optional `.env` settings `JCASH_DB_POOL_SIZE` and `JCASH_DB_POOL_TIMEOUT_MS` control
the pool size and wait timeout; restart the backend after changing them. Money
operations retain their dedicated JDBC transactions.
