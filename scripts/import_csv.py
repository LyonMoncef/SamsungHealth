#!/usr/bin/env python3
"""Import Samsung Health sleep CSV export and POST to the API."""

import csv
import json
import sys
import urllib.request
from datetime import datetime

API_URL = "http://localhost:8000/api/sleep"

# Samsung Health CSV sleep export typically has these columns:
# "com.samsung.health.sleep"
# Relevant columns vary by export version; common ones:
#   - "start_time" or "bed_time"
#   - "end_time" or "wake_up_time"

POSSIBLE_START_COLS = ["start_time", "bed_time", "Start time", "Bed time"]
POSSIBLE_END_COLS = ["end_time", "wake_up_time", "End time", "Wake-up time"]

# Samsung Health datetime formats
DT_FORMATS = [
    "%Y-%m-%d %H:%M:%S.%f",
    "%Y-%m-%d %H:%M:%S",
    "%Y-%m-%d %H:%M",
]


def parse_dt(value: str) -> str:
    for fmt in DT_FORMATS:
        try:
            return datetime.strptime(value.strip(), fmt).isoformat()
        except ValueError:
            continue
    raise ValueError(f"Cannot parse datetime: {value!r}")


def find_col(headers: list[str], candidates: list[str]) -> str:
    for c in candidates:
        if c in headers:
            return c
    raise KeyError(f"None of {candidates} found in CSV headers: {headers}")


def import_csv(path: str):
    with open(path, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        headers = reader.fieldnames or []
        start_col = find_col(headers, POSSIBLE_START_COLS)
        end_col = find_col(headers, POSSIBLE_END_COLS)

        sessions = []
        for row in reader:
            try:
                sessions.append({
                    "sleep_start": parse_dt(row[start_col]),
                    "sleep_end": parse_dt(row[end_col]),
                })
            except (ValueError, KeyError) as e:
                print(f"Skipping row: {e}", file=sys.stderr)

    if not sessions:
        print("No valid sessions found in CSV.")
        return

    payload = json.dumps({"sessions": sessions}).encode()
    req = urllib.request.Request(
        API_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
        print(f"Imported {result['inserted']} sleep sessions.")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <path-to-sleep.csv>")
        sys.exit(1)
    import_csv(sys.argv[1])
