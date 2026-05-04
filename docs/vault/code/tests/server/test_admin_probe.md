---
type: code-source
language: python
file_path: tests/server/test_admin_probe.py
git_blob: 97d91d9bd524e3d5723f3e2bba90a427e712f5be
last_synced: '2026-04-28T14:04:54Z'
loc: 135
annotations: []
imports:
- pytest
exports:
- TestAdminProbeAuth
- TestAdminProbeRateLimitGlobal
tags:
- code
- python
---

# tests/server/test_admin_probe.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_probe.py`](../../../tests/server/test_admin_probe.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — POST /admin/probe sentinel endpoint pour validation client-side du admin token.

Tests RED-first contre `server.routers.admin.admin_probe` (NEW).

Cas couverts (spec §"Endpoint POST /admin/probe") :
- 401 sans X-Registration-Token + audit `admin_probe_failed` row inséré
- 200 avec X-Registration-Token (`{"status": "ok"}`)
- second bucket global `100/hour` (anti-rotation IP via Tor) → 429 après flood

Notes :
- `_check_admin_token` doit ajouter `await asyncio.sleep(0.5)` constant + `audit_event`
  AVANT raise 401 (forensique + anti timing). On ne teste pas le timing exact
  (flaky) mais on vérifie l'audit row + le 401.

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
  §"Endpoint POST /admin/probe"
  §"LOW + bonus" #6 (second bucket global 100/h + audit chaque échec).
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


class TestAdminProbeAuth:
    """spec test #6 — POST /admin/probe sans token → 401 + audit ; avec token → 200."""

    def test_probe_without_token_returns_401_and_audits_failed_event(
        self, client_pg_ready, db_session
    ):
        """given POST /admin/probe sans X-Registration-Token, when called, then 401 AND ≥1 row in auth_events with event_type='admin_probe_failed'.

        spec §"Endpoint POST /admin/probe" — audit chaque échec (pas sample 1/10).
        spec §LOW + bonus #6 — `auth_event` log à chaque 401.
        """
        # RED-couple: admin_probe symbol must exist.
        from sqlalchemy import select

        from server.db.models import AuthEvent
        from server.routers.admin import admin_probe  # noqa: F401

        client = client_pg_ready
        # Note: same-origin to bypass CSRF check (we test auth not CSRF here).
        r = client.post(
            "/admin/probe",
            headers={"Sec-Fetch-Site": "same-origin", "Content-Type": "application/json"},
            json={},
        )
        assert r.status_code == 401, (
            f"POST /admin/probe sans token MUST 401, got {r.status_code}: {r.text}"
        )

        # Audit event MUST be inserted on each failed probe.
        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "admin_probe_failed")
        ).scalars().all()
        assert len(events) >= 1, (
            f"expected ≥1 admin_probe_failed audit event after 401, got {len(events)} "
            "(spec §LOW + bonus #6)"
        )

    def test_probe_with_token_returns_200_status_ok(self, client_pg_ready):
        """given POST /admin/probe avec X-Registration-Token valide, when called, then 200 + body `{"status": "ok"}`.

        spec test #6 + §"Endpoint POST /admin/probe" — sentinel pour validation client-side.
        """
        from server.routers.admin import admin_probe  # noqa: F401

        client = client_pg_ready
        r = client.post(
            "/admin/probe",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Sec-Fetch-Site": "same-origin",
                "Content-Type": "application/json",
            },
            json={},
        )
        assert r.status_code == 200, (
            f"POST /admin/probe avec token MUST 200, got {r.status_code}: {r.text}"
        )
        data = r.json()
        assert data == {"status": "ok"}, (
            f"expected body `{{'status': 'ok'}}`, got {data}"
        )


class TestAdminProbeRateLimitGlobal:
    """spec §"Endpoint POST /admin/probe" — second bucket global `100/hour` anti-rotation IP."""

    def test_probe_global_bucket_caps_at_100_per_hour(
        self, client_pg_ready, monkeypatch
    ):
        """given POST /admin/probe répété (rotation IP simulée par X-Forwarded-For), when on dépasse le cap global 100/h, then 429 rate_limit_exceeded.

        spec §"Endpoint POST /admin/probe" — `@limiter.limit("100/hour", key_func=lambda r: "global")`
        Le bucket global ne se reset pas par IP rotation — donc même avec headers XFF
        différents, on hit 429 après ~100 calls.

        Pour rendre le test rapide (et pas faire 100 calls réels), on monkeypatch le
        cap global à 5/hour via env. L'impl doit lire ce cap depuis env (pattern
        existant `_email_request_composite_cap`) — sinon le test reste RED car le
        monkeypatch n'a aucun effet.
        """
        from server.routers.admin import admin_probe  # noqa: F401

        # Test hook : impl must read SAMSUNGHEALTH_RL_ADMIN_PROBE_GLOBAL_CAP env so
        # tests can lower it without flooding 100 requests. RED until impl wires it.
        monkeypatch.setenv("SAMSUNGHEALTH_RL_ADMIN_PROBE_GLOBAL_CAP", "5/hour")

        client = client_pg_ready
        last_status = None
        # 5 valid then ≥1 must 429 thanks to the global bucket.
        for i in range(8):
            r = client.post(
                "/admin/probe",
                headers={
                    "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                    "Sec-Fetch-Site": "same-origin",
                    "X-Forwarded-For": f"192.0.2.{i + 1}",  # rotate IP per call
                    "Content-Type": "application/json",
                },
                json={},
            )
            last_status = r.status_code
            if r.status_code == 429:
                break

        assert last_status == 429, (
            f"expected 429 after >5 admin/probe calls (global bucket 100/h, "
            f"reduced to 5/h via env in test), got last status {last_status}. "
            "(RED until impl wires SAMSUNGHEALTH_RL_ADMIN_PROBE_GLOBAL_CAP env override.)"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestAdminProbeAuth` (class) — lines 27-87
- `TestAdminProbeRateLimitGlobal` (class) — lines 90-135

### Imports
- `pytest`

### Exports
- `TestAdminProbeAuth`
- `TestAdminProbeRateLimitGlobal`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestAdminProbeAuth`, `TestAdminProbeRateLimitGlobal`
