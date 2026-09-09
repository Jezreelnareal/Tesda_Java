"""Preserve backend stdout byte-for-byte in portable gzip evidence; keep originals locally."""
import gzip
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'docs/performance/web-results'
records = []
for path in sorted(root.glob('*/c*-r*/backend-stdout.txt')):
    data = path.read_bytes()
    compressed = gzip.compress(data, compresslevel=9, mtime=0)
    output = path.with_suffix('.txt.gz')
    output.write_bytes(compressed)
    assert gzip.decompress(output.read_bytes()) == data
    records.append(dict(path=str(output.relative_to(root)).replace('\\', '/'),
                        originalBytes=len(data), compressedBytes=len(compressed),
                        originalSha256=hashlib.sha256(data).hexdigest()))
(root / 'backend-log-archive.json').write_text(json.dumps(records, indent=2) + '\n', encoding='utf-8')
print(f'Archived {len(records)} backend logs with verified byte-for-byte restoration.')
print(f'Original: {sum(row["originalBytes"] for row in records)/1024**2:.2f} MiB; gzip: {sum(row["compressedBytes"] for row in records)/1024**2:.2f} MiB')
