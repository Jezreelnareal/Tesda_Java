"""Validate and summarize the repeatable HTTP comparison; standard library only."""
import argparse
import csv
import gzip
import hashlib
import json
import math
from pathlib import Path
import statistics

ROOT = Path(__file__).resolve().parents[1]
def read_json(path):
    return json.loads(path.read_bytes())

def summarize(campaign):
    base=ROOT/'docs/performance/web-results'
    rows=[]
    versions={}
    for stage in ('baseline','pooled'):
        for repeat in range(1,4):
            folder=base/f'{stage}-round{repeat}-{campaign}'
            if not (folder/'completed.txt').exists():
                raise ValueError(f'Incomplete round: {folder.name}')
            assert (folder/'cleanup.txt').exists(), 'Missing isolated database cleanup evidence'
            before=read_json(folder/'dataset-before.json')
            assert before==read_json(folder/'dataset-after.json'), 'Dataset changed'
            assert before['customers']==100 and before['transactions']==10000
            metadata=read_json(folder/'environment.json')
            assert metadata['warmupSeconds']==30 and metadata['measuredSeconds']==60
            assert metadata['jvmFlags']==['-Xms256m','-Xmx512m','-XX:+UseG1GC']
            key=hashlib.sha256((folder/'source-sha256.txt').read_bytes()).hexdigest()
            assert versions.setdefault(stage,key)==key, 'Source changed between repeats'
            for clients in (1,5,10):
                trial=folder/f'c{clients}-r1'
                result=read_json(trial/'result.json')
                profile=read_json(trial/'jfr-analysis.json')
                with gzip.open(trial/'requests.csv.gz','rt',encoding='utf-8') as f:
                    samples=list(csv.DictReader(f))
                assert len(samples)==result['requests']==result['successes']
                assert result['errors']==0
                assert {int(s['customer']) for s in samples}==set(range(1,clients+1))
                assert all(s['result']=='ok' and s['status']=='200' for s in samples)
                times=sorted(int(s['durationNs'])/1e6 for s in samples)
                p95=times[math.ceil(.95*len(times))-1]
                assert abs(p95-result['p95Ms'])<1e-6
                throughput=len(samples)/result['elapsedSecondsIncludingDrain']
                assert abs(throughput-result['successfulRequestsPerSecond'])<1e-6
                rows.append(dict(stage=stage,repeat=repeat,clients=clients,
                    successes=result['successes'],errors=result['errors'],requests_per_second=throughput,
                    p50_ms=result['p50Ms'],p95_ms=p95,p99_ms=result['p99Ms'],max_ms=result['maxMs'],
                    database_monitor_wait_ms=profile['databaseConnectionMonitorWaitMs'],
                    monitor_wait_ms_per_request=profile['databaseConnectionMonitorWaitMs']/len(samples),
                    jvm_cpu_machine_percent=profile['jvmCpuMeanFractionOfMachine']*100,
                    gc_pause_ms=profile['gcPauseMs'],max_gc_pause_ms=profile['maxGcPauseMs'],
                    sampled_heap_mib=profile['maxSampledHeapUsedBytes']/1024**2,
                    evidence=str(trial.relative_to(ROOT)).replace('\\','/')))
    out=base/f'comparison-{campaign}'
    out.mkdir(exist_ok=True)
    with (out/'trials.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
    comparisons=[]
    lines=['# HTTP dashboard comparison','',
        'Median of three separately warmed, 60-second trials per configuration. p95 values are medians of trial p95s, not a pooled percentile. Raw samples were checked against the reported counts and p95 values.','',
        '| Customers | Baseline req/s | Pooled req/s | Throughput change | Baseline p95 | Pooled p95 | p95 reduction |',
        '|---:|---:|---:|---:|---:|---:|---:|']
    for clients in (1,5,10):
        b=[r for r in rows if r['stage']=='baseline' and r['clients']==clients]
        a=[r for r in rows if r['stage']=='pooled' and r['clients']==clients]
        rate_b=statistics.median(r['requests_per_second'] for r in b);rate_a=statistics.median(r['requests_per_second'] for r in a)
        p95_b=statistics.median(r['p95_ms'] for r in b);p95_a=statistics.median(r['p95_ms'] for r in a)
        comparisons.append(dict(clients=clients,baseline_rps=rate_b,pooled_rps=rate_a,throughput_change_percent=(rate_a/rate_b-1)*100,
            baseline_p95_ms=p95_b,pooled_p95_ms=p95_a,p95_reduction_percent=(1-p95_a/p95_b)*100))
        lines.append(f'| {clients} | {rate_b:.2f} | {rate_a:.2f} | {(rate_a/rate_b-1)*100:+.1f}% | {p95_b:.2f} ms | {p95_a:.2f} ms | {(1-p95_a/p95_b)*100:.1f}% |')
    total=sum(r['successes'] for r in rows)
    lines.extend(['',f'All {total:,} measured requests passed payload validation; zero errors. Dataset counts, balances, and checksums were unchanged after each round.','',
        'The load model is closed loop: each simulated customer waits for its response and validates it before sending another request. This is a local Java API comparison, not a browser, internet, or open-loop capacity test.'])
    (out/'summary.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (out/'comparison.json').write_text(json.dumps(comparisons,indent=2)+'\n',encoding='utf-8')
    print('\n'.join(lines))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--campaign',required=True)
    summarize(parser.parse_args().campaign)
