# JCash Browser Usage

Start MySQL, the Java API, and Next.js as described in
[README.md](../README.md). Open http://localhost:3000.

## Customer walkthrough

1. Select **Personal account** and sign in with a mobile number and PIN.
2. Check the available balance and account details on the wallet card.
3. Choose **Cash in**, enter an amount, and confirm. Check the balance and receipt.
4. Choose **Withdraw**, enter an amount, and confirm.
5. Choose **Send money**, enter another registered mobile number and an amount,
   and confirm.
6. Open **Activity** to search history or filter by transaction type.
7. Use the theme button for light/dark mode and **Sign out** to end the login.

For fresh seed data, Juan (`09171234567` / `1234`) starts with PHP 1,000.
A PHP 100 cash-in, PHP 25 withdrawal, and PHP 50 transfer to Maria
(`09181234567`) should leave Juan with PHP 1,025 and Maria with PHP 550.
Existing database balances may differ.

## Registration

Choose **Create an account** on the welcome page. Enter a full name, mobile
number, four-digit PIN, and matching confirmation. The new account starts
with a zero balance. Sign in with the new credentials.

## Administrator walkthrough

1. Select **Administrator** and sign in as `admin` / `1234` on a seeded database.
2. Review customer count, combined balances, and transaction count.
3. Open **Accounts**, search by name or mobile number, and view balances.
4. Use **New account** to create a customer with a zero starting balance.
5. Use **Credit** or **Debit** on a customer row to make an audited adjustment.
6. Open **Reports** for transaction totals and the latest 100 transactions.
7. Sign out when finished.

## Validation demonstrations

- Use incorrect credentials three times; sign-in locks for that role in the
  browser session, including after a page refresh.
- Try a transfer to an unregistered mobile number or to your own account.
- Try an amount larger than the available balance.
- Try zero, a negative amount, or more than two decimal places.
- Try registering an already-used mobile number or mismatched PINs.
- Visit the admin portal as a customer; API access remains forbidden.
- Resize to a phone width and verify navigation, forms, and table scrolling.

Sessions expire after 30 minutes without API activity. A Java restart ends all
sessions. After a network failure during a money operation, refresh and check
history before attempting it again.

The desktop Swing UI has been replaced. The JDBC cleanup utility remains a
separate console tool for the optional delete checklist item.
