"""Export a plain Issue 11 PDF from validated local evidence. Requires reportlab."""
import argparse
import csv
from html import escape
import json
from pathlib import Path
import statistics

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

ROOT = Path(__file__).resolve().parents[1]


def main(campaign):
    evidence = ROOT / 'docs/performance/web-results'
    comparison = evidence / f'comparison-{campaign}'
    values = json.loads((comparison / 'comparison.json').read_bytes())
    with (comparison / 'trials.csv').open(encoding='utf-8') as stream:
        trials = list(csv.DictReader(stream))
    assert len(trials) == 18 and all(int(row['errors']) == 0 for row in trials)
    total = sum(int(row['successes']) for row in trials)
    ten = next(row for row in values if row['clients'] == 10)
    baseline = json.loads((evidence / f'baseline-round1-{campaign}/c10-r1/jfr-analysis.json').read_bytes())
    verification = (evidence / 'verification/summary.md').read_text(encoding='utf-8')
    assert '21 tests passed' in verification
    cleanup_file = evidence / 'verification/cleanup-check.json'
    cleanup = json.loads(cleanup_file.read_bytes()) if cleanup_file.exists() else None
    if cleanup:
        assert cleanup['failures'] == cleanup['errors'] == 0
        assert cleanup['passed'] + cleanup['skipped'] == cleanup['tests']
        assert cleanup['newBenchmarkCompiled'] and cleanup['obsoleteBenchmarkRemoved']
    fonts = Path('C:/Windows/Fonts')
    normal, bold = 'Helvetica', 'Helvetica-Bold'
    if (fonts / 'arial.ttf').exists() and (fonts / 'arialbd.ttf').exists():
        pdfmetrics.registerFont(TTFont('ReportArial', str(fonts / 'arial.ttf')))
        pdfmetrics.registerFont(TTFont('ReportArialBold', str(fonts / 'arialbd.ttf')))
        pdfmetrics.registerFontFamily('ReportArial', normal='ReportArial', bold='ReportArialBold')
        normal, bold = 'ReportArial', 'ReportArialBold'
    body = ParagraphStyle('Body', fontName=normal, fontSize=10.5, leading=15, spaceAfter=8)
    small = ParagraphStyle('Small', parent=body, fontSize=9, leading=12, spaceAfter=6)
    heading = ParagraphStyle('Heading', parent=body, fontName=bold, fontSize=12.5, leading=17, spaceBefore=10, spaceAfter=7, keepWithNext=True)
    title = ParagraphStyle('Title', parent=body, fontName=bold, fontSize=18, leading=23, spaceAfter=10)
    code = ParagraphStyle('Code', fontName='Courier', fontSize=8, leading=11, spaceAfter=7)
    story = []
    width = A4[0] - 104

    def p(text, style=body):
        story.append(Paragraph(text, style))

    def h(text):
        p(text, heading)

    def table(headers, rows, widths):
        cells = [[Paragraph(escape(str(cell)), small) for cell in row] for row in [headers] + rows]
        grid = Table(cells, colWidths=[width * fraction for fraction in widths], repeatRows=1, hAlign='LEFT')
        grid.setStyle(TableStyle([
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#eeeeee')),
            ('LINEBELOW', (0, 0), (-1, 0), .5, colors.black),
            ('LINEBELOW', (0, 1), (-1, -1), .25, colors.HexColor('#cccccc')),
            ('LEFTPADDING', (0, 0), (-1, -1), 7),
            ('RIGHTPADDING', (0, 0), (-1, -1), 7),
            ('TOPPADDING', (0, 0), (-1, -1), 5),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ]))
        story.extend([grid, Spacer(1, 8)])

    p('Issue 11: Java application<br/>performance optimization', title)
    p('JCash Wallet | Test date: 9 September 2026', small)
    p('<link href="https://github.com/Jezreelnareal/Tesda_Java">Repository: github.com/Jezreelnareal/Tesda_Java</link>', small)
    h('Objective and outcome')
    p('Monitor the running Java application, identify a real bottleneck, apply a specific fix, and compare both versions under the same workload.')
    p(f'At 10 concurrent customers, median throughput increased by <b>{ten["throughput_change_percent"]:.1f}%</b> and p95 response time decreased by <b>{ten["p95_reduction_percent"]:.1f}%</b>. All <b>{total:,} measured responses</b> passed validation.')
    h('Test environment')
    table(['Item', 'Configuration'], [
        ['Application', 'JCash Spring Boot 4.1.1; Java 26.0.2.1+1'],
        ['Host', 'Windows 11; Ryzen 5 PRO 4650G; 12 logical processors; 23.4 GiB RAM'],
        ['Database', 'MySQL 8.4.11 in a separate Docker container; port 3307; 2 CPUs; 1 GiB RAM'],
        ['JVM', '-Xms256m -Xmx512m -XX:+UseG1GC'],
        ['Data', '100 synthetic customers; 100 cash-in records each; PHP 10,000 per wallet'],
        ['Workload', 'Authenticated GET /api/user/dashboard; 1, 5 and 10 concurrent sessions'],
        ['Monitoring', 'Java Flight Recorder (JFR), jcmd, process samples and Docker stats'],
    ], [.23, .77])
    h('Named issue and implemented fix')
    p('The original DatabaseConnection.withReusableConnection() held a global Java monitor while executing repository callbacks on one shared JDBC connection. Independent customer requests queued behind that lock.')
    p('The implementation now uses <b>HikariCP 7.0.2</b> with a default pool of five connections and a two-second acquisition timeout. Each callback borrows and releases its own lease. Repository work executes outside the initialization monitor.')
    p('Money operations retain dedicated connections, row locks, explicit commits and rollbacks. Pool size and timeout are configurable using JCASH_DB_POOL_SIZE and JCASH_DB_POOL_TIMEOUT_MS.', small)

    story.append(PageBreak())
    p('Measured results and profiling evidence', title)
    p('Each version ran three trials at each load: 30 seconds of warm-up followed by 60 seconds of measurement. The backend restarted for every trial. Version order alternated, and the second round reversed load order.')
    table(['Customers', 'Before req/s', 'After req/s', 'Before p95', 'After p95'], [
        [row['clients'], f'{row["baseline_rps"]:.2f}', f'{row["pooled_rps"]:.2f}', f'{row["baseline_p95_ms"]:.2f} ms', f'{row["pooled_p95_ms"]:.2f} ms'] for row in values
    ], [.16, .21, .21, .21, .21])
    p('Values are medians of three trials. The p95 values are medians of trial p95s. Higher requests/second and lower response time indicate improvement.', small)
    h('Actual JFR output')
    p('Baseline round 1, 10 customers: the saved recording and its analysis captured waiting at the shared connection method. Extract from the saved analysis, rounded to two decimals:')
    p(f'databaseConnectionMonitorWaitMs: {baseline["databaseConnectionMonitorWaitMs"]:.2f}<br/>jdk.JavaMonitorEnter events: {baseline["eventCounts"]["jdk.JavaMonitorEnter"]}<br/>DatabaseConnection.withReusableConnection<br/>&nbsp;&nbsp;&lt;- UserRepository.findByMobileNumber<br/>&nbsp;&nbsp;&lt;- UserApi.find &lt;- UserApi.dashboard', code)
    p('Source: docs/performance/web-results/baseline-round1-' + campaign + '/c10-r1/jfr-analysis.json. The associated backend.jfr, jfr-start.txt and jfr-stop.txt retain the recording and tool output.', small)
    p('Wait time is summed across threads; it can exceed the 60-second wall-clock measurement. No waits on this old repository monitor were recorded in any pooled measurement window. Events shorter than 1 ms are excluded.')
    resources = []
    for label, key, unit in [
        ('Connection-monitor wait per request', 'monitor_wait_ms_per_request', ' ms'),
        ('JVM CPU / all logical processors', 'jvm_cpu_machine_percent', '%'),
        ('Total GC pause time per trial', 'gc_pause_ms', ' ms'),
    ]:
        medians = [statistics.median(float(row[key]) for row in trials if row['stage'] == stage and int(row['clients']) == 10) for stage in ('baseline', 'pooled')]
        resources.append([label, f'{medians[0]:.2f}{unit}', f'{medians[1]:.2f}{unit}'])
    table(['Median at 10 customers', 'Before', 'After'], resources, [.58, .21, .21])
    h('Impact and limits')
    p('Concurrent throughput improved consistently. Single-customer throughput ranges overlapped, so its small difference is inconclusive. More work completed per second also used more CPU and caused more total GC activity.')
    maximum = max(float(row['max_ms']) for row in trials)
    p(f'This is a local, closed-loop Java API comparison on a shared desktop host. It excludes browser rendering and the Next.js proxy. Slow samples were retained; maximum observed latency was {maximum:.2f} ms. These results do not establish production capacity or an SLA.', small)
    p('Both versions retained Spring DEBUG request logging despite the root WARN setting. Its overhead is included; performance with quieter logging was not measured.', small)

    story.append(PageBreak())
    p('Verification and implementation record', title)
    h('Correctness checks')
    p('<b>Performance verification: 21 tests passed.</b> This includes 11 existing API/session/cleanup tests, four benchmark-response validation tests, and six real-MySQL pool/transaction tests.')
    p('Database checks cover independent concurrent leases, bounded pool exhaustion, failure recovery, connection-state reset, shutdown/reopen, invalid configuration, concurrent withdrawals without overspending, and transfer rollback after an injected insert failure.')
    p('Every measured response was checked for the correct customer, balance and 100 ordered transactions. Counts, totals and row checksums matched before and after each round. Test databases were removed and the temporary container was stopped.')
    if cleanup:
        p(f'<b>Cleanup verification:</b> The previous standalone JDBC benchmark, runner, report, results and compiled classes were removed. The cleaned source compiled successfully: <b>{cleanup["passed"]} tests passed</b> and {cleanup["skipped"]} optional database tests were skipped. Those database tests passed in the earlier verification. All 18 measured trial records were retained.', small)
    h('The four tuning layers')
    table(['Layer', 'Action and interpretation'], [
        ['Application', 'Removed the global lock around independent repository callbacks.'],
        ['Data access', 'Applied a bounded connection pool. EXPLAIN showed existing sender/receiver indexes via index_merge; no query/index change was applied.'],
        ['Container', 'Held the separate MySQL container at 2 CPUs and 1 GiB; this is a controlled setting, not an independently measured speedup.'],
        ['JVM', 'Held heap and G1 settings constant and captured CPU/GC evidence. The measured fix addresses connection concurrency.'],
    ], [.20, .80])
    h('Acceptance evidence and reproduction')
    p('All four acceptance criteria are covered: actual profiling logs/recordings, a named shared-connection bottleneck, an applied pool fix, and validated before/after results.')
    p('The repository contains the detailed report in <b>docs/performance/WEB_REPORT.md</b>, instructions in <b>docs/performance/WEB_TEST_GUIDE.md</b>, and raw request samples, compressed backend logs and profiling evidence in <b>docs/performance/web-results/</b>. Binary JFR files are retained locally and excluded from Git.', small)
    p('To repeat: start compose.performance.yaml; create baseline and improved snapshots with New-PerformanceSnapshot.ps1; run compare-web-performance.ps1; run test-database-pool.ps1 separately; then summarize the campaign. The guide provides exact commands and the baseline revision.', small)
    p('Verification compiled the root source tree through disposable standalone POMs. Existing pending root Maven aggregator/module edits were preserved; their layout was not repaired. The diagnostic pilot and a controller-aborted trial were excluded from the final comparison.', small)
    h('Tool references')
    p('<link href="https://docs.oracle.com/en/java/javase/26/docs/specs/man/jfr.html">Oracle JDK 26: JFR recording analysis</link><br/><link href="https://docs.oracle.com/en/java/javase/26/docs/specs/man/jcmd.html">Oracle JDK 26: jcmd recording controls</link><br/><link href="https://github.com/brettwooldridge/HikariCP">HikariCP configuration and pool sizing</link>', small)

    def footer(canvas, doc):
        canvas.setFont(normal, 9)
        canvas.setFillColor(colors.HexColor('#555555'))
        canvas.drawString(52, 30, 'JCash Wallet | Issue 11')
        canvas.drawRightString(A4[0] - 52, 30, str(doc.page))

    output = ROOT / 'docs/performance/Issue11-Performance-Report.pdf'
    doc = SimpleDocTemplate(str(output), pagesize=A4, rightMargin=52, leftMargin=52,
                           topMargin=43, bottomMargin=47, title='JCash - Issue 11 Performance Report', author='JCash Project')
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print(output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--campaign', required=True)
    main(parser.parse_args().campaign)
