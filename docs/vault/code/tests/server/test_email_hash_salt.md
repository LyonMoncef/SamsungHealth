---
type: code-source
language: python
file_path: tests/server/test_email_hash_salt.py
git_blob: 357d2d8ace8653c964ea5dc44af80a8960d26afe
last_synced: '2026-05-06T08:02:35Z'
loc: 70
annotations: []
imports:
- pytest
exports:
- TestHmacEmailHash
tags:
- code
- python
---

# tests/server/test_email_hash_salt.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_email_hash_salt.py`](../../../tests/server/test_email_hash_salt.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — HMAC-SHA256 email hash with server salt (anti-dictionary).

Tests RED-first contre `server.security.email_outbound.hmac_email_hash` +
`server.security.auth._validate_email_hash_salt_at_boot`.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import pytest


_VALID_SALT = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"


@pytest.fixture(autouse=True)
def _set_salt(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_HASH_SALT", _VALID_SALT)
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")


class TestHmacEmailHash:
    def test_hash_is_64_chars_hex(self):
        """given hmac_email_hash('alice@x.com'), when called, then result is 64-char hex (sha256 output).

        spec §Email = log + table dédiée — hmac_sha256 64 chars hex.
        spec §Test d'acceptation #22 — Hash a 64 chars hex.
        """
        from server.security.email_outbound import hmac_email_hash

        h = hmac_email_hash("alice@example.com")
        assert isinstance(h, str), f"expected str, got {type(h).__name__}"
        assert len(h) == 64, f"expected 64 chars hex, got {len(h)}: {h!r}"
        assert all(c in "0123456789abcdef" for c in h), f"non-hex chars in hash: {h!r}"

    def test_hash_stable_for_same_email(self):
        """given hmac_email_hash called twice with same email, when compared, then identical.

        spec §Test d'acceptation #22 — 2 appels avec même email → même hash.
        """
        from server.security.email_outbound import hmac_email_hash

        h1 = hmac_email_hash("stable@example.com")
        h2 = hmac_email_hash("stable@example.com")
        assert h1 == h2, f"hash must be stable: {h1!r} vs {h2!r}"

        # Case-insensitive (spec : email.lower() before HMAC).
        h_upper = hmac_email_hash("STABLE@example.com")
        assert h_upper == h1, "hash must lowercase email before HMAC"

        # Different email → different hash.
        h_other = hmac_email_hash("other@example.com")
        assert h_other != h1, "different emails must produce different hashes"

    def test_boot_fails_without_email_hash_salt_env(self, monkeypatch):
        """given env var SAMSUNGHEALTH_EMAIL_HASH_SALT unset OR < 32 bytes, when boot validator runs, then raises.

        spec §Test d'acceptation #21 — Boot fails sans SAMSUNGHEALTH_EMAIL_HASH_SALT (≥ 32 bytes).
        """
        from server.security.auth import _validate_email_hash_salt_at_boot

        # Unset → must raise.
        monkeypatch.delenv("SAMSUNGHEALTH_EMAIL_HASH_SALT", raising=False)
        with pytest.raises(Exception):
            _validate_email_hash_salt_at_boot()

        # Too short (< 32 chars) → must raise.
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_HASH_SALT", "tooshort")
        with pytest.raises(Exception):
            _validate_email_hash_salt_at_boot()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestHmacEmailHash` (class) — lines 22-70

### Imports
- `pytest`

### Exports
- `TestHmacEmailHash`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestHmacEmailHash` · methods: `test_hash_is_64_chars_hex`, `test_hash_stable_for_same_email`, `test_boot_fails_without_email_hash_salt_env`
