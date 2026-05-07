---
type: code-source
language: python
file_path: tests/server/test_sleep_api.py
git_blob: 6ac2e53698293088b0a420773ffa91ba7bd26d92
last_synced: '2026-05-06T08:02:35Z'
loc: 43
annotations: []
imports: []
exports:
- test_post_sleep_session
- test_post_sleep_dedup
- test_get_sleep_sessions
- test_post_sleep_with_stages
- test_get_sleep_not_found
tags:
- code
- python
---

# tests/server/test_sleep_api.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_sleep_api.py`](../../../tests/server/test_sleep_api.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
def test_post_sleep_session(client_pg_ready):
    client = client_pg_ready
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    r = client.post("/api/sleep", json=payload)
    assert r.status_code == 201
    assert r.json()["inserted"] == 1


def test_post_sleep_dedup(client_pg_ready):
    client = client_pg_ready
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    client.post("/api/sleep", json=payload)
    r = client.post("/api/sleep", json=payload)
    assert r.json()["skipped"] == 1


def test_get_sleep_sessions(client_pg_ready):
    client = client_pg_ready
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    client.post("/api/sleep", json=payload)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22")
    assert r.status_code == 200
    assert len(r.json()) == 1


def test_post_sleep_with_stages(client_pg_ready):
    client = client_pg_ready
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": [
        {"stage_type": "deep", "stage_start": "2026-04-20T23:00:00", "stage_end": "2026-04-21T00:00:00"},
        {"stage_type": "rem",  "stage_start": "2026-04-21T00:00:00", "stage_end": "2026-04-21T01:00:00"},
    ]}]}
    r = client.post("/api/sleep", json=payload)
    assert r.status_code == 201
    r2 = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    sessions = r2.json()
    assert len(sessions[0]["stages"]) == 2


def test_get_sleep_not_found(client_pg_ready):
    client = client_pg_ready
    r = client.get("/api/sleep?from=2000-01-01&to=2000-01-02")
    assert r.status_code == 200
    assert r.json() == []
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `test_post_sleep_session` (function) — lines 1-6
- `test_post_sleep_dedup` (function) — lines 9-14
- `test_get_sleep_sessions` (function) — lines 17-23
- `test_post_sleep_with_stages` (function) — lines 26-36
- `test_get_sleep_not_found` (function) — lines 39-43

### Exports
- `test_post_sleep_session`
- `test_post_sleep_dedup`
- `test_get_sleep_sessions`
- `test_post_sleep_with_stages`
- `test_get_sleep_not_found`


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
