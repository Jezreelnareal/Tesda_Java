# JCash Banking System

JCash uses a Next.js + TypeScript browser interface, a Java HTTP API, and
MySQL through JDBC. The Swing/JFrame interface has been replaced.

## Security and deployment readiness

JCash currently supports a local banking simulation. The initial source review
identified blockers for real-money deployment, including unverified customer
cash-in, session-only login limits, and missing duplicate-request protection.
A successful production build does not establish security readiness.

See the [security readiness checklist](docs/SECURITY_READINESS.md) for the
findings, recommended fixes, scan commands, security test scenarios, and
deployment acceptance criteria. These recommendations are pending work;
the review did not include vulnerability scans or penetration testing.

## Features

- Personal and administrator login with three failed attempts per role per browser session
- Customer registration with a zero starting balance
- Balance display, cash-in, withdrawal, transfers, and transaction receipts
- Searchable transaction history, including sent and received transfers
- Admin account search/creation, credit/debit adjustments, and system reports
- Responsive layouts, light/dark themes, and PIN visibility toggles
- The standalone JDBC cleanup utility and existing performance benchmark

## Structure

```text
Tesda_Java/
|-- frontend/                       Next.js + TypeScript
|   |-- src/app/
|   |   |-- page.tsx                 Login and registration
|   |   |-- user/page.tsx            Customer portal
|   |   |-- admin/page.tsx           Administrator portal
|   |   `-- api/[...path]/route.ts   Same-origin proxy to Java
|   |-- src/components/
|   |   |-- user/                   Customer dashboard
|   |   |-- admin/                  Administrator dashboard
|   |   `-- shared/                 Forms, navigation, sessions, and tables
|   |-- src/lib/api.ts              Typed API client and response contracts
|   `-- tests/                      Playwright browser tests
|-- src/main/java/
|   |-- Main.java                   Starts the Java API
|   |-- user/                       Customer API, models, repository, services
|   |-- admin/                      Admin API, models, repository, service, cleanup
|   |-- shared/                     HTTP/session code, shared models, JDBC, auth
|   `-- performance/                JDBC benchmark
|-- src/test/java/                  Java API and session tests
|-- database/                       MySQL schema and seed records
|-- scripts/                        API, browser test, and benchmark runners
|-- compose.yaml                    Local MySQL service
`-- pom.xml                         Java dependencies and build
```

The browser calls Next.js at `/api/*`. Next.js forwards those requests to
Java on loopback. Java retains the business rules, PIN verification,
authorization, money calculations, and JDBC transactions. Database passwords
stay in the Java process. Both roles use the same customer records and
transaction repository.

## Requirements

- JDK 17 or newer
- Maven, or the Maven bundled with IntelliJ IDEA
- Node.js 20.9 or newer and npm; verified here with Node.js 24
- Docker Desktop with Compose for MySQL
- Microsoft Edge for the supplied Playwright configuration

The frontend follows the [Next.js App Router](https://nextjs.org/docs/app/getting-started/installation)
and uses a [route handler](https://nextjs.org/docs/app/api-reference/file-conventions/route)
to forward API requests.

## Run locally

Create a root `.env` if one does not already exist:

```env
MYSQL_ROOT_PASSWORD=replace-with-a-root-password
JCASH_DB_PASSWORD=replace-with-an-app-password
```

Use the existing passwords if your MySQL volume already contains data.
Local environment files are ignored by Git.

Start the database from the repository root:

```powershell
docker compose up -d
```

In a terminal at the repository root, start the Java API:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-api.ps1
```

The script reads `JCASH_DB_PASSWORD` from the root `.env`, defaults to the
Compose `jcash` database user, and finds Maven on PATH or IntelliJ's bundled
Maven. Existing process environment settings take precedence.

In a second terminal:

```powershell
cd frontend
npm run dev
```

For the first setup, or after `package-lock.json` changes, run `npm ci` in
`frontend/` before starting the development server. Stop any running Next.js
server first (Ctrl+C in its terminal). Windows locks the native SWC dependency
while Next.js is running, so reinstalling at that time can fail with `EPERM`.
If installation fails, fix it and rerun `npm ci` successfully before using
`npm run dev`. You do not need to reinstall dependencies on each startup.

Open **http://localhost:3000**. Java listens on **127.0.0.1:8080**.
For a production frontend build, use `npm run build` followed by `npm run start`
in `frontend/`, with the Java API running separately.

### IntelliJ or manual Java startup

Run `Main.main()` or `mvn compile exec:java` with these environment variables:

| Variable | Local setting |
|---|---|
| `JCASH_DB_URL` | `jdbc:mysql://localhost:3306/jcash_db` |
| `JCASH_DB_USER` | `jcash` for the Compose database |
| `JCASH_DB_PASSWORD` | The app password from `.env` |
| `JCASH_API_PORT` | `8080` by default |

`Main` starts an HTTP server; it no longer opens a desktop window. When
bypassing the PowerShell runner, configure database variables yourself.
`DatabaseConnection` retains its original `root` fallback for standalone tools.

To change the frontend's backend address, copy `frontend/.env.example` to
`frontend/.env.local` and set `JCASH_API_URL`. This is a server-only setting.
Restart Next.js after changing it.

## Seeded development logins

| Role | Identifier | PIN | Initial balance |
|---|---|---|---:|
| Customer | `09171234567` | `1234` | PHP 1,000.00 |
| Customer | `09181234567` | `5678` | PHP 500.00 |
| Administrator | `admin` | `1234` | — |

Seeds run only when a new database is initialized. An existing database may
have different accounts and balances.

## Browser sessions and access

- Java checks the role on every protected API request and derives the acting
  account from the session. Clients cannot choose their sender or admin identity.
- The opaque session cookie is HttpOnly and SameSite=Strict. Next.js adds
  Secure when accessed through HTTPS.
- Session IDs rotate on login/logout. Idle sessions expire after 30 minutes.
- User and admin each have three failed login attempts per browser session.
  Reloading or signing out does not reset the counters. A new browser session
  or idle-session expiry starts a new allowance. This is session-level
  assignment behavior, not an account-wide lockout.
- Mutations require a same-origin check at Next.js and a custom request header.
  No cross-origin API access is enabled.
- Responses omit PINs and PIN hashes. Money is sent as decimal strings and
  calculated with Java BigDecimal.
- Money operations retain JDBC transactions and row locks.
- Sessions are in memory and cleared when Java restarts. Both supplied launch
  commands bind to loopback for local use.

## Verification

Java tests, without a database:

```powershell
mvn test
```

Frontend checks, inside `frontend/`:

```powershell
npm run typecheck
npm run build
```

End-to-end tests, from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-web.ps1
```

The browser runner uses Compose MySQL and a uniquely named `jcash_web_test_*`
database. It seeds test records, starts Java on port 8181 and Next.js on port
3100, runs Playwright in Edge, and removes the test database and grant in a
`finally` block. It does not reset `jcash_db`. Screenshots and failure traces
are written to `frontend/test-results/`.

See [docs/USAGE.md](docs/USAGE.md) for the manual demonstration flow.

## Database initialization

Compose runs `database/schema.sql` and `database/seed.sql` only when the volume
is empty. **Running `schema.sql` manually drops and recreates `jcash_db`,
deleting its data.** It is not needed for the UI migration.

Foreign-key updates use `RESTRICT` for compatibility with the transaction
participant check constraints on MySQL 8.4. The application does not change
account identifiers.

## JDBC cleanup and performance

`admin.TransactionCleanupTool` remains a standalone console entry point for
the optional JDBC delete demonstration. Removing a test transaction does not
reverse balances.

Run the JDBC benchmark with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-performance.ps1
```

The [performance report](docs/performance/REPORT.md) contains previously
captured JDBC evidence. Those measurements cover repository access, not the
new browser/API request path.

## Troubleshooting

- **Cannot reach Java:** start the API runner and check port 8080 and `JCASH_API_URL`.
- **Database unavailable:** run `docker compose ps`, confirm MySQL is healthy,
  and check the Java process's database settings. The API retries initialization.
- **Login locked:** wait for idle-session expiry or start a new browser session.
  Refreshing the page does not reset counters.
- **Maven not found:** use the PowerShell runner or IntelliJ's Maven window.
- **Port in use:** stop the old process or configure a different port.
- **Connection lost during a transaction:** refresh the balance and history
  before submitting the transaction again.
