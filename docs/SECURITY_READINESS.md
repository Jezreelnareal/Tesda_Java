# Security and deployment readiness

JCash is currently a local banking simulation and is not ready to handle real
money. This document records the initial source review dated September 7, 2026
and the recommended checks before deployment.

The review covered the Java API, Next.js proxy, authentication, transaction
services, and database configuration. It did not include vulnerability scans,
penetration tests, or verification of a deployed environment. Findings below
are based on source inspection, not demonstrated attacks against a running
system. All checklist items remain pending until supporting evidence is recorded.

## Existing safeguards

The reviewed code includes prepared SQL statements, salted PBKDF2 PIN hashing,
server-side role checks, opaque session tokens, session rotation on login and
logout, HttpOnly and SameSite cookies, same-origin checks for mutations,
request body limits, and BigDecimal money calculations. Money operations use
JDBC transactions and row locks.

These controls are useful foundations. Their presence does not establish that
the whole application or its deployment is secure.

## Known blockers for real-money deployment

| ID | Finding and source | Impact | Recommended fix |
|---|---|---|---|
| SEC-01 | The customer cash-in endpoint calls [CashIn.java](../src/main/java/user/service/CashIn.java) to credit a client-supplied amount without verifying external payment. | A signed-in customer can create a balance without providing funds. | Credit funds only through a verified payment or authorized deposit workflow. Validate payment authenticity, amount, currency, recipient, and settlement status; prevent reuse of the same payment reference. |
| SEC-02 | [SessionStore.java](../src/main/java/shared/api/SessionStore.java) stores login failures per browser session. [Auth.java](../src/main/java/shared/service/Auth.java) accepts four-digit PINs for customers and administrators. | A new session resets the guessing allowance; a four-digit PIN has only 10,000 possibilities. | Add persistent account-based throttling with supplementary IP limits and abuse monitoring. Use stronger authentication and administrator MFA. Review customer MFA and reauthentication for sensitive transactions. |
| SEC-03 | [ApiServer.java](../src/main/java/shared/api/ApiServer.java) and [Transfer.java](../src/main/java/user/service/Transfer.java) have no duplicate-request protection in the reviewed money-operation flow. | Retrying after a lost response can execute a transfer or other money operation twice. | Add persistent idempotency keys scoped to the authenticated account and operation. Atomically store the key, request details, and result with the money change; reject key reuse with different details. |
| SEC-04 | [compose.yaml](../compose.yaml) publishes MySQL on all host interfaces. [DatabaseConnection.java](../src/main/java/shared/util/DatabaseConnection.java) falls back to root with an empty password. | Database exposure depends on the firewall and actual credentials, but these defaults are unsuitable for production. | Keep MySQL on a private network, use a least-privilege application account, and fail startup when required configuration or secrets are missing. |
| SEC-05 | [Development credentials](../README.md#seeded-development-logins) are documented and [seed.sql](../database/seed.sql) provisions development accounts. | Known accounts provide an immediate access risk if carried into production. | Separate development seeds from production provisioning. Ensure development accounts are absent and provision production administrators with unique credentials. |

- [ ] SEC-01 fixed and independently verified.
- [ ] SEC-02 fixed and independently verified.
- [ ] SEC-03 fixed and independently verified.
- [ ] SEC-04 fixed and verified in the deployment environment.
- [ ] SEC-05 fixed and verified in the production provisioning process.

Authentication and transaction controls should be checked against the
[OWASP authentication guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
and [transaction authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Transaction_Authorization_Cheat_Sheet.html).

## Security verification workflow

### 1. Define requirements and retain evidence

Use [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/)
as the security requirements framework. Select and document the applicable
requirements for the deployment and record each as passed, failed, untested,
or not applicable with a justification.

For each finding or test, record the commit, environment, date, tool version
where applicable, expected and actual results, owner, and evidence location.
Keep reports containing secrets, customer data, or exploit details in restricted
storage rather than committing them to a public repository.

### 2. Scan dependencies, source code, and secrets

Run the Java dependency scan from the repository root, with Maven and a JDK
available:

```powershell
mvn org.owasp:dependency-check-maven:check
```

[OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) checks
dependencies for known vulnerabilities. It needs access to vulnerability data;
initial downloads can take time and may require feed/API configuration. Inspect
the generated report under `target/`. A failed or incomplete scan is not a pass.
Pin the scanner version and configure its failure threshold when integrating it
into CI so the release checks are reproducible.

Run the frontend dependency scan from the repository root:

```powershell
Push-Location frontend
try {
    npm audit --audit-level=high
} finally {
    Pop-Location
}
```

[npm audit](https://docs.npmjs.com/cli/audit/) reports known dependency
vulnerabilities. The `high` threshold controls its exit status; review lower
severity findings too. This command does not automatically upgrade packages.
Record and investigate nonzero exit codes, including connectivity failures.

- [ ] Scan Java and frontend dependencies, including transitive dependencies.
- [ ] Review findings for applicability, apply fixes, and rescan.
- [ ] Scan source code for security defects with a Java/TypeScript-capable static analyzer.
- [ ] Scan tracked files and Git history for exposed credentials and other secrets.
- [ ] Revoke and rotate any exposed credentials; deleting the text alone is insufficient.
- [ ] Scan deployment images and check the actual JDK, Node.js, database, and operating system for known vulnerabilities.
- [ ] Add security scans to CI and retain reports for the release commit.

Dependency scans cannot establish authorization correctness or money integrity.
Continue with the application-specific checks below even when scans are clean.

### 3. Test authentication and authorization

Use an isolated staging environment with fake money and dedicated customer and
administrator accounts. Exercise the API directly as well as through the UI.

- [ ] Unauthenticated requests cannot access customer or administrator data or operations.
- [ ] Customer A cannot read or modify customer B's account by changing request fields or paths.
- [ ] Customers cannot invoke administrator endpoints or choose the acting account identity.
- [ ] Failed-login limits survive cookie deletion, new sessions, and application restarts.
- [ ] Logout and expiry invalidate old session tokens; login rotates the token.
- [ ] Cross-origin mutation requests and requests missing the verification header are rejected.
- [ ] PINs, credential hashes, and session tokens are absent from application responses and logs where they do not belong.
- [ ] Administrator MFA and sensitive-operation reauthentication work after implementation.

### 4. Test money integrity and failure handling

- [ ] An unverified cash-in cannot increase any balance.
- [ ] Reusing a payment reference cannot credit funds twice.
- [ ] Repeating the same idempotency key produces one money change and the original result, including concurrent requests and retries after restart.
- [ ] Reusing an idempotency key with a different amount or recipient is rejected.
- [ ] Concurrent withdrawals or transfers from separate sessions cannot overspend an account.
- [ ] Transfers preserve the combined sender and recipient balance and produce the expected transaction record.
- [ ] A database failure before commit rolls back both balances and the transaction record.
- [ ] A lost response after commit can be retried without executing the operation again.
- [ ] Zero, negative, oversized, and excessive-decimal amounts are rejected without changing balances.
- [ ] Self-transfers and nonexistent recipients are rejected without changing balances.
- [ ] Administrator adjustments record the actor, target, amount, reason, and time in a protected audit trail.

Existing Java and browser tests are useful regression checks, but must not be
assumed to cover every scenario above. See the
[README verification commands](../README.md#verification) for the existing suite.

### 5. Assess the running application

Use [OWASP ZAP](https://www.zaproxy.org/getting-started/) against the isolated
staging deployment. Include authenticated customer and administrator flows;
an unauthenticated scan does not cover protected functionality. Start with
passive assessment, then perform authorized active testing with disposable
test data because requests may alter state.

- [ ] Check for injection, cross-site scripting, broken access control, CSRF, and sensitive-data exposure.
- [ ] Manually verify important findings and retest fixes.
- [ ] Test request limits, timeouts, and abuse controls under expected and excessive traffic.
- [ ] Arrange an independent penetration test before handling real money.

## Deployment checks

- [ ] HTTPS is enforced at the public entry point; verify HSTS and appropriate security headers, including a tested Content Security Policy.
- [ ] Session cookies actually carry Secure, HttpOnly, and SameSite attributes through the production reverse proxy.
- [ ] Origin validation works with the public hostname and HTTPS configuration; forwarded headers are trusted only from the intended proxy.
- [ ] The Java API and MySQL are accessible only through the intended private paths.
- [ ] Database connections use verified TLS when crossing an untrusted network.
- [ ] Secrets are supplied through protected deployment configuration, with least-privilege access and a rotation process.
- [ ] Production uses separate accounts and data; development seeds and destructive initialization scripts cannot run accidentally.
- [ ] Runtime database permissions are restricted; schema migrations use separate privileges where needed.
- [ ] Security events and money operations have protected audit records without logging credentials or session tokens.
- [ ] Alerts cover suspicious login activity, unusual adjustments, transaction failures, and reconciliation discrepancies.
- [ ] Backups are protected and a restore has been tested successfully; recovery objectives are documented.
- [ ] Balances and transaction records can be reconciled against external payment records where applicable.
- [ ] Incident response, credential revocation, and rollback procedures have assigned owners and have been exercised.

## Release acceptance criteria

Real-money deployment should remain blocked until:

- [ ] All known blockers above are fixed and retested.
- [ ] Applicable security requirements have evidence of passing, with no unexplained untested items.
- [ ] No unresolved high or critical security findings remain; lower severity findings have documented owners and treatment decisions.
- [ ] Dependency, source, secret, and deployment scans completed successfully for the release candidate.
- [ ] Authentication, authorization, concurrency, retry, and rollback tests pass.
- [ ] An independent penetration test is complete and required remediation is verified.
- [ ] Deployment protections, monitoring, reconciliation, and backup restoration have been verified.

A clean scan or successful build alone does not establish readiness. Reassess
after code, dependency, infrastructure, or authentication changes and monitor
new vulnerability disclosures after deployment. This checklist addresses
technical security; it does not establish regulatory approval to operate a
financial service.
