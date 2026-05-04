---
type: code-source
language: python
file_path: tests/server/test_ip_resolution.py
git_blob: 5bb31bd6660aeef2353b919b8a07dd8c28d2ee38
last_synced: '2026-04-27T17:56:06Z'
loc: 152
annotations: []
imports:
- ipaddress
- types
- pytest
exports:
- _mock_request
- _parse_proxies
- TestRightMostUntrusted
tags:
- code
- python
---

# tests/server/test_ip_resolution.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_ip_resolution.py`](../../../tests/server/test_ip_resolution.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Right-most-untrusted IP resolution (pentester HIGH #2 fix).

Tests RED-first contre `server.security.rate_limit._resolve_client_ip` +
`_validate_trusted_proxies_at_boot` (à créer). Vérifie le pattern standard
nginx `real_ip_recursive on` / Express `trust proxy` :
walk XFF de droite à gauche, skip les CIDRs trusted, retourne le 1er IP non trusted.

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §`_key_func` IP source résolue via right-most-untrusted, §SAMSUNGHEALTH_TRUSTED_PROXIES.
Tests d'acceptation : #15, #16, #17, #18, #19, #20, #21, #22, #23.
"""
from __future__ import annotations

import ipaddress
from types import SimpleNamespace

import pytest


def _mock_request(peer: str | None, xff: str | None = None):
    """Build a minimal `Request`-like object with `.client.host` and `.headers`."""
    headers = {}
    if xff is not None:
        headers["X-Forwarded-For"] = xff
    client = SimpleNamespace(host=peer) if peer is not None else None
    return SimpleNamespace(client=client, headers=headers)


def _parse_proxies(spec_str: str) -> list:
    return [ipaddress.ip_network(s.strip(), strict=False) for s in spec_str.split(",") if s.strip()]


class TestRightMostUntrusted:
    def test_no_trusted_proxies_returns_direct_peer(self):
        """given trusted_proxies vide + XFF présent, when resolve, then retourne request.client.host (XFF ignoré).

        spec §_resolve_client_ip — mode dev / single-host : pas de trusted = pas de XFF.
        spec test #15.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="1.2.3.4", xff="9.9.9.9, 5.6.7.8")
        assert _resolve_client_ip(req, trusted_proxies=[]) == "1.2.3.4"

    def test_trusted_proxy_returns_first_untrusted_xff(self):
        """given trusted=[127.0.0.1], peer=127.0.0.1, XFF='1.2.3.4, 127.0.0.1', when resolve, then '1.2.3.4'.

        spec §_resolve_client_ip — peer trusted → walk XFF, return first untrusted from right.
        spec test #16.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="127.0.0.1", xff="1.2.3.4, 127.0.0.1")
        proxies = _parse_proxies("127.0.0.1")
        assert _resolve_client_ip(req, trusted_proxies=proxies) == "1.2.3.4"

    def test_spoofed_xff_returns_right_most_untrusted(self):
        """given XFF='1.2.3.4, 5.6.7.8, 127.0.0.1' (1.2.3.4 spoofé par attaquant), peer=127.0.0.1 trusted, when resolve, then '5.6.7.8' (right-most untrusted, PAS le spoof).

        spec §_resolve_client_ip — pattern right-most-untrusted (anti spoofing classique).
        spec test #17.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="127.0.0.1", xff="1.2.3.4, 5.6.7.8, 127.0.0.1")
        proxies = _parse_proxies("127.0.0.1")
        assert _resolve_client_ip(req, trusted_proxies=proxies) == "5.6.7.8", (
            "right-most-untrusted MUST return 5.6.7.8, NOT spoofed leftmost 1.2.3.4"
        )

    def test_untrusted_peer_ignores_xff(self):
        """given peer=8.8.8.8 (non trusted) + XFF présent, when resolve, then '8.8.8.8' (XFF ignoré, request ne vient pas via proxy).

        spec §_resolve_client_ip — direct peer NOT in trusted → request didn't come via proxy, ignore XFF.
        spec test #18.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="8.8.8.8", xff="1.2.3.4, 127.0.0.1")
        proxies = _parse_proxies("127.0.0.1")
        assert _resolve_client_ip(req, trusted_proxies=proxies) == "8.8.8.8"

    def test_all_xff_entries_trusted_falls_back_leftmost(self):
        """given XFF='127.0.0.1, 10.0.0.1' (toutes trusted), peer trusted, when resolve, then leftmost '127.0.0.1' (best-effort fallback).

        spec §_resolve_client_ip — All IPs in XFF are trusted → take leftmost (best-effort).
        spec test #19.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="127.0.0.1", xff="127.0.0.1, 10.0.0.1")
        proxies = _parse_proxies("127.0.0.1,10.0.0.0/8")
        result = _resolve_client_ip(req, trusted_proxies=proxies)
        assert result == "127.0.0.1", (
            f"fallback leftmost when all XFF trusted; got {result!r}"
        )

    def test_malformed_xff_entry_skipped(self):
        """given XFF='not-an-ip, 1.2.3.4, 127.0.0.1', peer trusted, when resolve, then skip 'not-an-ip', return '1.2.3.4'.

        spec §_resolve_client_ip — malformed entry, skip (continue loop).
        spec test #20.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="127.0.0.1", xff="not-an-ip, 1.2.3.4, 127.0.0.1")
        proxies = _parse_proxies("127.0.0.1")
        assert _resolve_client_ip(req, trusted_proxies=proxies) == "1.2.3.4"

    def test_ipv6_trusted_proxy_resolves_xff(self):
        """given trusted=[::1], peer=::1, XFF='2001:db8::1, ::1', when resolve, then '2001:db8::1'.

        spec §_resolve_client_ip — IPv6 first-class support (production behind v6 reverse proxy).
        spec test #21.
        """
        from server.security.rate_limit import _resolve_client_ip

        req = _mock_request(peer="::1", xff="2001:db8::1, ::1")
        proxies = _parse_proxies("::1")
        assert _resolve_client_ip(req, trusted_proxies=proxies) == "2001:db8::1"

    def test_boot_fail_when_trusted_proxies_malformed(self, monkeypatch):
        """given SAMSUNGHEALTH_TRUSTED_PROXIES='not-an-ip', when boot validator runs, then raises ConfigError.

        spec §SAMSUNGHEALTH_TRUSTED_PROXIES — Validation au boot (entry format invalide).
        spec test #22.
        """
        from server.security.rate_limit import _validate_trusted_proxies_at_boot

        monkeypatch.setenv("SAMSUNGHEALTH_TRUSTED_PROXIES", "not-an-ip")
        monkeypatch.setenv("SAMSUNGHEALTH_ENV", "test")
        with pytest.raises(Exception) as excinfo:
            _validate_trusted_proxies_at_boot()
        # ConfigError ou ValueError — il faut juste que ça lève.
        assert "trusted_proxies" in str(excinfo.value).lower() or "not-an-ip" in str(
            excinfo.value
        ) or "config" in type(excinfo.value).__name__.lower(), (
            f"expected ConfigError-like exception for malformed CIDR, got {excinfo.value!r}"
        )

    def test_boot_fail_when_production_without_trusted_proxies(self, monkeypatch):
        """given SAMSUNGHEALTH_ENV=production + TRUSTED_PROXIES vide, when boot validator runs, then raises ConfigError (pentester HIGH #4 fix).

        spec §SAMSUNGHEALTH_TRUSTED_PROXIES — boot fail si prod ET TRUSTED_PROXIES vide.
        spec test #23.
        """
        from server.security.rate_limit import _validate_trusted_proxies_at_boot

        monkeypatch.setenv("SAMSUNGHEALTH_ENV", "production")
        monkeypatch.delenv("SAMSUNGHEALTH_TRUSTED_PROXIES", raising=False)
        with pytest.raises(Exception):
            _validate_trusted_proxies_at_boot()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_mock_request` (function) — lines 20-26
- `_parse_proxies` (function) — lines 29-30
- `TestRightMostUntrusted` (class) — lines 33-152

### Imports
- `ipaddress`
- `types`
- `pytest`

### Exports
- `_mock_request`
- `_parse_proxies`
- `TestRightMostUntrusted`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — classes: `TestRightMostUntrusted`
