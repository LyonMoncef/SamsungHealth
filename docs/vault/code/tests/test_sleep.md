---
type: code-source
language: python
file_path: tests/test_sleep.py
git_blob: 01b6423b7be698284bc474debdf5e9de3e6080b1
last_synced: '2026-04-23T10:21:39Z'
loc: 38
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

# tests/test_sleep.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/test_sleep.py`](../../../tests/test_sleep.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
def test_post_sleep_session(client):
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    r = client.post("/api/sleep", json=payload)
    assert r.status_code == 201
    assert r.json()["inserted"] == 1


def test_post_sleep_dedup(client):
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    client.post("/api/sleep", json=payload)
    r = client.post("/api/sleep", json=payload)
    assert r.json()["skipped"] == 1


def test_get_sleep_sessions(client):
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}]}
    client.post("/api/sleep", json=payload)
    r = client.get("/api/sleep?from=2026-04-20&to=2026-04-22")
    assert r.status_code == 200
    assert len(r.json()) == 1


def test_post_sleep_with_stages(client):
    payload = {"sessions": [{"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": [
        {"stage_type": "deep", "stage_start": "2026-04-20T23:00:00", "stage_end": "2026-04-21T00:00:00"},
        {"stage_type": "rem",  "stage_start": "2026-04-21T00:00:00", "stage_end": "2026-04-21T01:00:00"},
    ]}]}
    r = client.post("/api/sleep", json=payload)
    assert r.status_code == 201
    r2 = client.get("/api/sleep?from=2026-04-20&to=2026-04-22&include_stages=true")
    sessions = r2.json()
    assert len(sessions[0]["stages"]) == 2


def test_get_sleep_not_found(client):
    r = client.get("/api/sleep?from=2000-01-01&to=2000-01-02")
    assert r.status_code == 200
    assert r.json() == []
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `test_post_sleep_session` (function) — lines 1-5
- `test_post_sleep_dedup` (function) — lines 8-12
- `test_get_sleep_sessions` (function) — lines 15-20
- `test_post_sleep_with_stages` (function) — lines 23-32
- `test_get_sleep_not_found` (function) — lines 35-38

### Exports
- `test_post_sleep_session`
- `test_post_sleep_dedup`
- `test_get_sleep_sessions`
- `test_post_sleep_with_stages`
- `test_get_sleep_not_found`


## Exercises *(auto — this test file touches)*

### `test_sleep.test_get_sleep_not_found`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep.test_get_sleep_sessions`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

### `test_sleep.test_post_sleep_dedup`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`

### `test_sleep.test_post_sleep_session`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`

### `test_sleep.test_post_sleep_with_stages`
- [[../../code/server/database|server/database.py]] · `get_connection`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `create_sleep_sessions`
- [[../../code/server/routers/sleep|server/routers/sleep.py]] · `get_sleep_sessions`

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
