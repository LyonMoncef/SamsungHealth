---
type: code-source
language: python
file_path: scripts/explore_samsung_export.py
git_blob: afe815b1a285cc799fa5d6c62d7a35b7ea538137
last_synced: '2026-04-23T10:49:30Z'
loc: 108
annotations: []
imports:
- csv
- sys
- datetime
- pathlib
exports:
- infer_type
- mask_row
- explore_csv
- print_file_report
- main
tags:
- code
- python
coverage_pct: 0.0
---

# scripts/explore_samsung_export.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`scripts/explore_samsung_export.py`](../../../scripts/explore_samsung_export.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Explore Samsung Health CSV export — outputs schema only (no personal values).
Safe to paste in chat: all cell values are replaced by their Python type token.

Usage:
    python3 scripts/explore_samsung_export.py [export_dir]

Default export_dir: /mnt/c/Users/idsmf/Desktop/SamsungHealth
"""

import csv
import sys
from datetime import datetime
from pathlib import Path

EXPORT_DIR = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/mnt/c/Users/idsmf/Desktop/SamsungHealth")

SAMPLE_ROWS = 2


def infer_type(value) -> str:
    if not isinstance(value, str):
        return f"<{type(value).__name__}>"
    if value == "":
        return "<empty>"
    try:
        int(value)
        return "<int>"
    except ValueError:
        pass
    try:
        float(value)
        return "<float>"
    except ValueError:
        pass
    for fmt in ("%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            datetime.strptime(value, fmt)
            return "<datetime>"
        except ValueError:
            pass
    return "<str>"


def mask_row(row: dict) -> dict:
    # Skip the restkey=None bucket (trailing-comma overflow values)
    return {k: infer_type(v) for k, v in row.items() if k is not None}


def explore_csv(path: Path) -> dict:
    try:
        with open(path, encoding="utf-8-sig", newline="") as f:
            # Samsung Health format: line 1 is metadata (table_name, id, count),
            # line 2 is the actual column headers, line 3+ is data.
            first_line = f.readline()
            meta = first_line.strip().split(",")
            table_meta = meta[0] if meta else ""

            reader = csv.DictReader(f)
            headers = reader.fieldnames or []
            rows = []
            for i, row in enumerate(reader):
                if i >= SAMPLE_ROWS:
                    break
                rows.append(mask_row(row))
            return {"headers": headers, "sample_rows": rows, "meta": table_meta, "error": None}
    except Exception as e:
        return {"headers": [], "sample_rows": [], "meta": "", "error": str(e)}


def print_file_report(path: Path, info: dict) -> None:
    rel = path.relative_to(EXPORT_DIR)
    print(f"\n### {rel}")
    if info["error"]:
        print(f"  ERROR: {info['error']}")
        return
    if not info["headers"]:
        print("  (empty or no headers)")
        return
    if info.get("meta"):
        print(f"  meta: {info['meta']}")
    print(f"  columns ({len(info['headers'])}):")
    for h in info["headers"]:
        print(f"    - {h}")
    for i, row in enumerate(info["sample_rows"]):
        print(f"  row[{i}]: {dict(row)}")


def main() -> None:
    if not EXPORT_DIR.exists():
        print(f"ERROR: export dir not found: {EXPORT_DIR}", file=sys.stderr)
        sys.exit(1)

    csv_files = sorted(EXPORT_DIR.rglob("*.csv"))
    print(f"Samsung Health export: {EXPORT_DIR}")
    print(f"Found {len(csv_files)} CSV files\n")
    print("=" * 72)

    for path in csv_files:
        info = explore_csv(path)
        print_file_report(path, info)

    print("\n" + "=" * 72)
    print(f"Done — {len(csv_files)} files explored, no personal values included.")


if __name__ == "__main__":
    main()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `infer_type` (function) — lines 21-42 · ⚠️ no test
- `mask_row` (function) — lines 45-47 · ⚠️ no test
- `explore_csv` (function) — lines 50-68 · ⚠️ no test
- `print_file_report` (function) — lines 71-86 · ⚠️ no test
- `main` (function) — lines 89-104 · ⚠️ no test

### Imports
- `csv`
- `sys`
- `datetime`
- `pathlib`

### Exports
- `infer_type`
- `mask_row`
- `explore_csv`
- `print_file_report`
- `main`
