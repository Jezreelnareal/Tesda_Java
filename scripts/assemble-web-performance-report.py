"""Assemble Issue 11's human-readable report from validated completed evidence."""
import argparse
import csv
import json
from pathlib import Path
import statistics
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
def read_json(path): return json.loads(path.read_bytes())
def read_text(path):
    data=path.read_bytes()
    value=data.decode('utf-16') if data.startswith((b'\xff\xfe',b'\xfe\xff')) else data.decode('utf-8-sig')
    return value.replace('\r\n','\n')

def main(campaign,verification_snapshot):
    subprocess.run([sys.executable,str(ROOT/'scripts/summarize-web-performance.py'),'--campaign',campaign],check=True)
    evidence=ROOT/'docs/performance/web-results'
    comparison=evidence/f'comparison-{campaign}'
    values=read_json(comparison/'comparison.json')
    with (comparison/'trials.csv').open(encoding='utf-8') as f: trials=list(csv.DictReader(f))
    for trial in trials:
        for key in ('repeat','clients','successes','errors'): trial[key]=int(trial[key])
        for key in ('requests_per_second','p50_ms','p95_ms','p99_ms','max_ms','database_monitor_wait_ms','monitor_wait_ms_per_request','jvm_cpu_machine_percent','gc_pause_ms','max_gc_pause_ms','sampled_heap_mib'): trial[key]=float(trial[key])
    test_dir=ROOT/f'target/web-performance/snapshots/{verification_snapshot}'
    suites=[ET.parse(p).getroot() for p in (test_dir/'target/surefire-reports').glob('TEST-*.xml')]
    assert suites,'Missing verification evidence'
    for suite in suites:
        assert all(int(suite.get(key,'0'))==0 for key in ('failures','errors','skipped')),suite.attrib
    test_count=sum(int(s.get('tests','0')) for s in suites)
    assert test_count>=21,'Expected ordinary, benchmark-validator, and real-MySQL tests'
    checks=evidence/'verification'
    checks.mkdir(exist_ok=True)
    import shutil
    for filename in ('build.txt','pool-integration-tests.txt','source-sha256.txt'):
        shutil.copy2(test_dir/filename,checks/filename)
    test_lines=['# Verification results','',f'{test_count} tests passed, zero failures/errors/skips across the retained final suite reports.','',
                '| Suite | Passed |','|---|---:|']
    test_lines += [f"| {s.get('name')} | {s.get('tests')} |" for s in suites]
    (checks/'summary.md').write_text('\n'.join(test_lines)+'\n',encoding='utf-8')
    total=sum(t['successes'] for t in trials)
    ten=next(v for v in values if v['clients']==10)
    summary=read_text(comparison/'summary.md')
    table=summary[summary.index('| Customers'):summary.index('\n\nAll ')]
    env=read_json(evidence/f'baseline-round1-{campaign}'/'environment.json')
    pooled_env=read_json(evidence/f'pooled-round1-{campaign}'/'environment.json')
    range_lines=['| Customers | Baseline req/s range | Pooled req/s range |','|---:|---:|---:|']
    for clients in (1,5,10):
        b=[t['requests_per_second'] for t in trials if t['stage']=='baseline' and t['clients']==clients]
        a=[t['requests_per_second'] for t in trials if t['stage']=='pooled' and t['clients']==clients]
        range_lines.append(f'| {clients} | {min(b):.2f}–{max(b):.2f} | {min(a):.2f}–{max(a):.2f} |')
    resource_lines=['| Metric at 10 customers | Baseline | Pooled |','|---|---:|---:|']
    for label,key,unit in [('Recorded connection-monitor wait per request','monitor_wait_ms_per_request',' ms'),('JVM CPU / all logical processors','jvm_cpu_machine_percent','%'),('GC pause time per trial','gc_pause_ms',' ms'),('Largest sampled used heap per trial','sampled_heap_mib',' MiB')]:
        b=statistics.median(t[key] for t in trials if t['stage']=='baseline' and t['clients']==10)
        a=statistics.median(t[key] for t in trials if t['stage']=='pooled' and t['clients']==10)
        resource_lines.append(f'| {label} | {b:.2f}{unit} | {a:.2f}{unit} |')
    docker_cpu={};driver_cpu={}
    for stage in ('baseline','pooled'):
        cpu=[];driver=[]
        for repeat in range(1,4):
            folder=evidence/f'{stage}-round{repeat}-{campaign}'/'c10-r1'
            cpu.extend(float(json.loads(line)['CPUPerc'].strip('%')) for line in read_text(folder/'docker-stats.jsonl').splitlines() if line.strip())
            rows=list(csv.DictReader(read_text(folder/'process-samples.csv').splitlines()))
            # Cumulative process CPU includes all threads; divide by sampled elapsed wall time.
            from datetime import datetime
            if len(rows)>1:
                elapsed=(datetime.fromisoformat(rows[-1]['utc'].replace('Z','+00:00'))-datetime.fromisoformat(rows[0]['utc'].replace('Z','+00:00'))).total_seconds()
                driver.append((float(rows[-1]['driverCpuSeconds'])-float(rows[0]['driverCpuSeconds']))/elapsed)
        docker_cpu[stage]=statistics.mean(cpu);driver_cpu[stage]=statistics.median(driver)
    max_latency=max(t['max_ms'] for t in trials)
    lines=f'''# Issue 11: Java application performance optimization

[Download the plain PDF report](Issue11-Performance-Report.pdf).

The bounded JDBC repository pool improved the measured JCash dashboard workload.
At 10 concurrent customers, median throughput increased from
**{ten['baseline_rps']:.2f} to {ten['pooled_rps']:.2f} successful requests/second**
({ten['throughput_change_percent']:.1f}% higher), and the median trial p95 decreased
from **{ten['baseline_p95_ms']:.2f} to {ten['pooled_p95_ms']:.2f} ms**
({ten['p95_reduction_percent']:.1f}% lower).
All **{total:,} measured requests** passed response validation with zero errors.

These are local Java HTTP API results from three repeated trials at each load.
They do not measure browser rendering, the Next.js proxy, internet latency, or
production capacity.

## Specific issue and applied fix

The original `DatabaseConnection.withReusableConnection()` synchronized the whole
repository callback on the class monitor and shared a single physical connection.
Independent HTTP sessions therefore waited behind one another for account/history
reads. The diagnostic pilot recorded roughly 113 seconds of cumulative waiting
across 10 threads during 15 seconds of workload. The full baseline recordings
confirmed the same stack in the measured windows:

```text
shared.util.DatabaseConnection.withReusableConnection
  <- user.repository.UserRepository.findByMobileNumber
  <- user.api.UserApi.find
  <- user.api.UserApi.dashboard
  <- shared.api.ApiController.userDashboard
```

The [implementation](../../src/main/java/shared/util/DatabaseConnection.java)
now borrows a connection from **HikariCP 7.0.2**, managed by Spring Boot's
dependency BOM. The pool defaults to five connections and a two-second borrowing
timeout. Each callback releases its lease through try-with-resources, including
after failure. Synchronization is limited to pool initialization, maintenance,
and shutdown; independent repository callbacks run concurrently. The existing
money services retain dedicated JDBC connections, row locks, commits, and rollbacks.

The connection change and Hikari dependency are the application differences in
the measured snapshots. SHA-256 source manifests and derived POMs are saved per round.

## Measured environment and protocol

| Item | Recorded setting |
|---|---|
| Machine | AMD Ryzen 5 PRO 4650G; 12 logical processors; {env['computer']['TotalPhysicalMemory']/1024**3:.1f} GiB reported RAM |
| OS / Java | Windows 11 Home; Temurin 26.0.2.1+1 |
| Application | Spring Boot 4.1.1, direct HTTP/1.1 requests to loopback port 8281 |
| Database | MySQL 8.4.11, separate Docker container on loopback port 3307 |
| Container resources | 2 CPU quota; 1 GiB memory limit; unchanged between versions |
| JVM settings | `-Xms256m -Xmx512m -XX:+UseG1GC`; unchanged between versions |
| Fixture | 100 synthetic customers; 100 cash-in records each; PHP 10,000 per wallet |
| Load | 1, 5, and 10 separate authenticated sessions; one request in flight per session |
| Timing | 30-second warm-up + 60-second measurement; three trials per version/load |
| Profiling | Actual JFR recording before warm-up; event analysis limited to measured timestamps |

There are 18 measured trials. Version order alternates across rounds, and the
second round reverses concurrency order. Every trial restarts the Java backend.
Both versions use the same query, schema, dataset shape, JVM flags, profiling
settings, and container limits. Builds and integration tests run outside the
comparison windows. Logins are outside timing.

Every response must have the expected identity, name, balance, and exactly 100
correctly ordered transaction IDs with matching type, amount, recipient, and
direction. Error responses, invalid payloads, and timeouts cannot count as successes.
The load generator saves raw compressed request samples; the summary independently
checks sample counts, p95 values, and throughput. Dataset counts, totals, and row
checksums match before and after every completed round.

## Before and after

{table}

![Baseline and pooled dashboard performance](web-results/comparison-{campaign}/comparison.svg)

Each displayed value is the median of three trial measurements. The p95 column
is the median of trial p95 values, not a percentile calculated over pooled trials.

{chr(10).join(range_lines)}

Single-customer throughput ranges overlap, so the small unloaded difference is
inconclusive with three trials. The consistent gains occur at concurrent loads.

The complete per-trial values, including p99 and maximum latency, are in
[trials.csv](web-results/comparison-{campaign}/trials.csv). No slow samples were
removed. The largest observed request latency across the comparison was
{max_latency:.2f} ms; three repetitions do not eliminate local scheduling and
environmental variability.

Saved backend output shows Spring DEBUG request logging in both versions, despite
the runner's root WARN setting. This logging overhead is part of the measured
environment and limits extrapolation to deployments with quieter logging. It was
not changed between versions; disabling it would require a separate comparison.

## Profiling and the four tuning layers

{chr(10).join(resource_lines)}

JFR no longer recorded waits on the old repository class monitor in the pooled
measurement windows. This does not mean all waiting disappeared: borrowers can
still wait for a pool lease, and server threads also legitimately park while idle.
Wait time is summed across threads and can exceed elapsed wall time. Events below
the 1 ms monitor/socket threshold are omitted. Heap values are sampled, and CPU
fractions use all 12 logical processors.

- **Application code:** removed the global lock around independent repository
  callbacks; retained synchronization for initialization and credential maintenance.
- **Data access:** bounded connection reuse replaces serialized access. The
  recorded `EXPLAIN` uses the sender/receiver indexes through `index_merge`, with
  about 101 estimated rows and a sort. No extra index or SQL change was needed
  to demonstrate the identified improvement on this 100-record-per-customer fixture.
- **Container configuration:** a separate, resource-limited MySQL container keeps
  the test away from normal development data. Mean sampled Docker CPU at 10 users
  was {docker_cpu['baseline']:.1f}% baseline and {docker_cpu['pooled']:.1f}% pooled
  (Docker's scale permits approximately 200% with the two-CPU quota). The resource
  limit is an experimental control, not an independently measured speedup.
- **JVM runtime:** the heap and G1 settings are explicit and held constant. GC
  pause evidence is retained; the primary improvement is attributed to connection
  concurrency. No separate heap-size or garbage-collector speedup is claimed.

The load generator used a median of approximately {driver_cpu['baseline']:.2f}
CPU cores in baseline 10-user trials and {driver_cpu['pooled']:.2f} with pooling,
based on cumulative process CPU samples. The backend, load generator, Docker,
and normal desktop processes share the host; this is not a dedicated benchmark machine.

## Correctness and acceptance evidence

The final retained verification reports contain **{test_count} passing tests**:
the existing API/session/cleanup tests, benchmark-payload validation tests, and
six real-MySQL pool/transaction tests. The database suite checks independent
concurrent leases, bounded exhaustion, callback failure recovery, rollback and
connection-state reset, shutdown/reopen, invalid settings/connection failures,
concurrent withdrawals without overspending, and transfer rollback after an
injected transaction-insert failure. See [verification](web-results/verification/summary.md).

| Acceptance criterion | Evidence |
|---|---|
| A tool was actually used and output captured | JFR start/stop logs, recordings, event summaries, stack/duration analysis, process samples, Docker samples |
| A specific issue was identified | Shared synchronized JDBC connection; recorded monitor waits in baseline |
| A specific fix was applied | Bounded HikariCP pool; callback execution outside the class monitor |
| Before/after impact is documented | 18 trials, raw samples, response/data validation, median/range tables and profiling explanation |

Readable evidence is stored in the six `baseline-roundN-{campaign}` and
`pooled-roundN-{campaign}` folders. Binary `.jfr` recordings remain locally
available but are excluded from Git; JSON/text summaries and compressed request
samples provide portable repository evidence. Initial environment and system
property events are disabled in the full recordings.
Backend stdout is also retained as byte-verified `backend-stdout.txt.gz` files;
the uncompressed originals remain local and are excluded from Git.

The preliminary `pilot-baseline-{campaign}` run is diagnostic only. A controller-
aborted pooled trial is explicitly marked excluded and was rerun: PowerShell had
not retained the native process handle needed to read ExitCode after completion.
The final runner retains that handle. No partial trial is included in the tables.

## Reproduction and project build note

Follow [WEB_TEST_GUIDE.md](WEB_TEST_GUIDE.md). The measured baseline snapshot is
`{env['snapshot']}`; the pooled snapshot is `{pooled_env['snapshot']}`.
The starting Git revision is `571e60bf43c2658d5ffa9768f023c2eb58d56384`; per-round
source hashes also identify the uncommitted implementation and harness.

The workspace's pending root Maven aggregator/`untitled` module edits were
preserved. Verification uses disposable standalone POMs that compile the actual
root `src/` tree. A successful snapshot build is not a claim that the pending
root-module layout has been repaired. The normal database and UI were not modified
by the performance workload. Test databases are removed after each round.

## References

- [JDK 26 JFR command and recording analysis](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jfr.html)
- [JDK 26 jcmd recording controls](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jcmd.html)
- [HikariCP configuration and pool sizing](https://github.com/brettwooldridge/HikariCP)
'''
    (ROOT/'docs/performance/WEB_REPORT.md').write_text(lines,encoding='utf-8')
    print('Wrote docs/performance/WEB_REPORT.md and verification summary')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--campaign',required=True)
    parser.add_argument('--verification-snapshot',required=True)
    args=parser.parse_args();main(args.campaign,args.verification_snapshot)
