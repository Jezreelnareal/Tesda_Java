# JCash Performance Tuning Report

## Environment

- Application: standalone, single-threaded Java console application
- Runtime: Eclipse Temurin JDK 25
- Database: local MySQL through Connector/J
- Monitoring tool: Java Flight Recorder (JFR)
- JVM benchmark settings: `-Xms64m -Xmx64m -XX:+UseG1GC`
- Workload: repeated read-only user lookups using the same prepared SQL query

JCash does not run inside an application server or container. Consequently,
there is no servlet-thread or container pool to tune. The equivalent runtime
configuration for this project is its JDBC connection lifecycle and JVM launch
configuration.

## Identified Issue

Login and transaction-log repository operations previously called
`DriverManager.getConnection` for every request. Each request therefore paid
for a new physical MySQL connection handshake even though JCash processes menu
operations sequentially on one thread.

The `unpooled` benchmark preserves that original behavior. Its unique MySQL
connection ID count demonstrates that it creates a new physical connection for
each measured lookup.

## Applied Improvement

`DatabaseConnection.withReusableConnection` now keeps one synchronized
connection for short, single-operation repository calls. The application and
cleanup entry points release it in a `finally` block. A failed or invalid
connection is discarded so a later operation can reconnect.

Cash-in and transfer retain dedicated connections because their row locks,
balance updates, transaction inserts, commits, and rollbacks must stay inside
explicit JDBC transactions.

## Before and After Results

Run `scripts/run-performance.ps1` from PowerShell to regenerate the evidence.
The measured values from the implementation run are recorded below after both
JFR benchmark modes complete.

| Metric | Before: new connection | After: reused connection | Impact |
|---|---:|---:|---:|
| Physical connection IDs | 200 | 1 | 99.5% fewer |
| Average lookup latency | 3.674 ms | 0.857 ms | 76.7% lower |
| Median lookup latency | 3.508 ms | 0.795 ms | 77.3% lower |
| P95 lookup latency | 5.241 ms | 1.382 ms | 73.6% lower |
| Throughput | 272.15 lookups/s | 1167.37 lookups/s | 4.29x higher |
| JFR socket-read events | 421 | 240 | 43.0% fewer |
| JFR socket-write events | 420 | 210 | 50.0% fewer |
| JFR allocation samples | 99 | 40 | 59.6% fewer |

The measured workload completed all 200 lookups successfully in both modes.
The lower latency and network-event counts confirm that connection handshake
work, rather than the indexed user query, was the main avoidable overhead.

The benchmark logs contain latency and throughput measurements. The JFR summary
logs capture JVM monitoring output from the two runs. Binary `.jfr` recordings
are intentionally excluded from Git because the text summaries provide the
portable assessment evidence.

- [Before benchmark log](results/unpooled-benchmark.txt)
- [After benchmark log](results/reused-benchmark.txt)
- [Before JFR summary](results/unpooled-jfr-summary.txt)
- [After JFR summary](results/reused-jfr-summary.txt)

## Reproduction

```powershell
cd C:\Users\jezre\Tesda_Java
powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File .\scripts\run-performance.ps1
```

Use a different database user or iteration count when required:

```powershell
$env:JCASH_BENCHMARK_MOBILE = "09181234567"
powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File .\scripts\run-performance.ps1 -Iterations 500
```

The benchmark only executes `SELECT` statements and does not alter balances,
users, or transaction records.
