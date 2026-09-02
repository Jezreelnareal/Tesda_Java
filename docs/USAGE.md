# JCash Usage

## Database setup

Run `database/schema.sql` and then `database/seed.sql` in MySQL. The schema
script intentionally drops and recreates only `jcash_db`, so existing data in
that database is removed.

The seed creates these development logins:

| Role | Account | PIN |
|---|---|---|
| User | `09171234567` | `1234` |
| User | `09181234567` | `5678` |
| Admin | `admin` | `1234` |

PINs contain exactly four digits. User accounts sign in with their mobile
number and PIN; administrators sign in with their admin username and PIN.

## User features

- Create a new zero-balance account from the welcome screen
- View balance and account details
- Cash in and withdraw funds
- Transfer funds to another registered mobile number
- Review cash-in, withdrawal, transfer, and admin-adjustment history
- Search/filter history and review the five most recent activities
- Switch between light and dark mode during the session
- Log out to the role-selection screen

## Administrator features

- View all accounts or find one by mobile number
- Create a zero-balance user account
- Credit or debit an account with a recorded admin audit transaction
- Generate aggregate totals and a table of the 100 most recent transactions
- View dashboard metrics, search accounts, and review recent system activity
- Log out to the role-selection screen

Both login types allow three failed attempts per application session. Closing
and reopening JCash starts a new session.
