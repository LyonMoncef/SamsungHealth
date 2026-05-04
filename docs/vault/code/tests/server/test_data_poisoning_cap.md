---
type: code-source
language: python
file_path: tests/server/test_data_poisoning_cap.py
git_blob: 786b3d1a57307630db758a40a778e3467ca7a243
last_synced: '2026-04-27T17:56:06Z'
loc: 79
annotations: []
imports:
- pytest
exports:
- TestApiPostCap
tags:
- code
- python
---

# tests/server/test_data_poisoning_cap.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_data_poisoning_cap.py`](../../../tests/server/test_data_poisoning_cap.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Cap data-poisoning sur POST /api/* (1000/h par user, monkeypatché à 5 en test).

Tests RED-first contre `server.routers.sleep` + slowapi `@limiter.limit("1000/hour", key_func=_user_id_key)`.
On monkeypatch le cap via env pour garder le test rapide.

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §Limites par endpoint — POST /api/* INSERT routes santé : 1000/h par user.
Tests d'acceptation : #11.
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


class TestApiPostCap:
    def test_post_sleep_under_cap_succeeds(self, client_pg_ready, monkeypatch):
        """given cap monkeypatché à 5/min, when 5 POST /api/sleep, then les 5 OK (200/201).

        spec §Limites par endpoint — POST /api/* cap par user (sub-cap OK).
        spec test #11 (volet sous-cap).
        """
        # Monkeypatch le cap pour rendre le test rapide.
        monkeypatch.setenv("SAMSUNGHEALTH_RL_API_POST_CAP", "5/minute")

        client = client_pg_ready
        ok_count = 0
        for i in range(5):
            r = client.post(
                "/api/sleep",
                json={
                    "sessions": [
                        {
                            "sleep_start": f"2026-04-{20+i:02d}T22:00:00Z",
                            "sleep_end": f"2026-04-{21+i:02d}T06:00:00Z",
                        }
                    ]
                },
            )
            if r.status_code in (200, 201):
                ok_count += 1
        assert ok_count == 5, (
            f"expected 5/5 successful POSTs under cap, got {ok_count}"
        )

    def test_post_sleep_over_cap_returns_429(self, client_pg_ready, monkeypatch):
        """given cap 5/min, when 6e POST /api/sleep, then 429.

        spec §Limites par endpoint — data poisoning self-DoS cap.
        spec test #11 (volet over-cap).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_RL_API_POST_CAP", "5/minute")

        client = client_pg_ready
        for i in range(5):
            client.post(
                "/api/sleep",
                json={
                    "sessions": [
                        {
                            "sleep_start": f"2026-04-{15+i:02d}T22:00:00Z",
                            "sleep_end": f"2026-04-{16+i:02d}T06:00:00Z",
                        }
                    ]
                },
            )
        r = client.post(
            "/api/sleep",
            json={
                "sessions": [
                    {"sleep_start": "2026-04-26T22:00:00Z", "sleep_end": "2026-04-27T06:00:00Z"}
                ]
            },
        )
        assert r.status_code == 429, (
            f"expected 429 on 6th POST (cap=5), got {r.status_code}: {r.text}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestApiPostCap` (class) — lines 18-79

### Imports
- `pytest`

### Exports
- `TestApiPostCap`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — classes: `TestApiPostCap`
