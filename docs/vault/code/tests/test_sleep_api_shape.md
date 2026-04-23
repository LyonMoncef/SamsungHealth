---
type: code-source
language: python
file_path: tests/test_sleep_api_shape.py
git_blob: 1582c0dca1a6b21a8e5f205ce76f492f6877451a
last_synced: '2026-04-23T09:43:48Z'
loc: 94
annotations: []
imports: []
exports:
- test_sleep_response_fields
- test_sleep_stages_fields
- test_sleep_stages_empty_without_flag
- test_sleep_iso_strings_parseable
- test_sleep_ordered_by_start
- test_sleep_date_range_filter
- test_sleep_empty_db_returns_list
tags:
- code
- python
---

# tests/test_sleep_api_shape.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/test_sleep_api_shape.py`](../../../tests/test_sleep_api_shape.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests du shape de GET /api/sleep — contrat attendu par le frontend Nightfall (api.js).
"""

SLEEP_PAYLOAD = {"sessions": [
    {
        "sleep_start": "2026-04-20T23:15:00",
        "sleep_end":   "2026-04-21T07:30:00",
        "stages": [
            {"stage_type": "light", "stage_start": "2026-04-20T23:15:00", "stage_end": "2026-04-20T23:45:00"},
            {"stage_type": "deep",  "stage_start": "2026-04-20T23:45:00", "stage_end": "2026-04-21T01:15:00"},
            {"stage_type": "rem",   "stage_start": "2026-04-21T01:15:00", "stage_end": "2026-04-21T02:00:00"},
            {"stage_type": "light", "stage_start": "2026-04-21T02:00:00", "stage_end": "2026-04-21T02:30:00"},
            {"stage_type": "deep",  "stage_start": "2026-04-21T02:30:00", "stage_end": "2026-04-21T03:30:00"},
            {"stage_type": "rem",   "stage_start": "2026-04-21T03:30:00", "stage_end": "2026-04-21T04:30:00"},
            {"stage_type": "light", "stage_start": "2026-04-21T04:30:00", "stage_end": "2026-04-21T05:30:00"},
            {"stage_type": "rem",   "stage_start": "2026-04-21T05:30:00", "stage_end": "2026-04-21T07:30:00"},
        ],
    },
    {
        "sleep_start": "2026-04-21T22:45:00",
        "sleep_end":   "2026-04-22T06:45:00",
        "stages": [
            {"stage_type": "light", "stage_start": "2026-04-21T22:45:00", "stage_end": "2026-04-22T00:00:00"},
            {"stage_type": "deep",  "stage_start": "2026-04-22T00:00:00", "stage_end": "2026-04-22T02:00:00"},
            {"stage_type": "rem",   "stage_start": "2026-04-22T02:00:00", "stage_end": "2026-04-22T06:45:00"},
        ],
    },
]}


def test_sleep_response_fields(client):
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    assert r.status_code == 200
    sessions = r.json()
    assert len(sessions) == 2

    s = sessions[0]
    assert "id" in s
    assert "sleep_start" in s
    assert "sleep_end" in s
    assert "stages" in s


def test_sleep_stages_fields(client):
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    stage = r.json()[0]["stages"][0]
    assert "stage_type" in stage
    assert "stage_start" in stage
    assert "stage_end" in stage
    assert stage["stage_type"] in ("light", "deep", "rem", "awake")


def test_sleep_stages_empty_without_flag(client):
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22")
    s = r.json()[0]
    assert s.get("stages") is None or s.get("stages") == []


def test_sleep_iso_strings_parseable(client):
    """sleep_start / sleep_end doivent être des ISO strings parsables par JS new Date()."""
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    s = r.json()[0]
    from datetime import datetime
    datetime.fromisoformat(s["sleep_start"])
    datetime.fromisoformat(s["sleep_end"])
    for st in s["stages"]:
        datetime.fromisoformat(st["stage_start"])
        datetime.fromisoformat(st["stage_end"])


def test_sleep_ordered_by_start(client):
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    sessions = r.json()
    starts = [s["sleep_start"] for s in sessions]
    assert starts == sorted(starts)


def test_sleep_date_range_filter(client):
    client.post("/api/sleep", json=SLEEP_PAYLOAD)
    # to=2026-04-20 → seule la session du 20 (sleep_start < 2026-04-21)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-20&include_stages=true")
    assert len(r.json()) == 1


def test_sleep_empty_db_returns_list(client):
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    assert r.status_code == 200
    assert r.json() == []
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `test_sleep_response_fields` (function) — lines 32-43
- `test_sleep_stages_fields` (function) — lines 46-53
- `test_sleep_stages_empty_without_flag` (function) — lines 56-60
- `test_sleep_iso_strings_parseable` (function) — lines 63-73
- `test_sleep_ordered_by_start` (function) — lines 76-81
- `test_sleep_date_range_filter` (function) — lines 84-88
- `test_sleep_empty_db_returns_list` (function) — lines 91-94

### Exports
- `test_sleep_response_fields`
- `test_sleep_stages_fields`
- `test_sleep_stages_empty_without_flag`
- `test_sleep_iso_strings_parseable`
- `test_sleep_ordered_by_start`
- `test_sleep_date_range_filter`
- `test_sleep_empty_db_returns_list`


## Exercises *(auto — this test file touches)*

### `test_sleep_api_shape.test_sleep_date_range_filter`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_empty_db_returns_list`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_iso_strings_parseable`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_ordered_by_start`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_response_fields`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_stages_empty_without_flag`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep_api_shape.test_sleep_stages_fields`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`
