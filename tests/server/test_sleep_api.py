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
