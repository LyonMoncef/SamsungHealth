---
type: code-source
language: python
file_path: tests/server/test_return_to_validator.py
git_blob: 3362269ac170237acae194feea53a7719881f9ea
last_synced: '2026-05-06T08:02:35Z'
loc: 137
annotations: []
imports:
- pytest
exports:
- TestReturnToBypass
tags:
- code
- python
---

# tests/server/test_return_to_validator.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_return_to_validator.py`](../../../tests/server/test_return_to_validator.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — return_to whitelist validator (anti open-redirect).

Tests RED-first contre `server.security.auth_providers.return_to.validate_return_to`.
1 test par bypass classique listé dans la spec section "return_to whitelist".

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#28-#35).
"""
from __future__ import annotations

import pytest


_ALLOWED = {"https://app.samsunghealth.com", "http://localhost:3000"}


class TestReturnToBypass:
    def test_subdomain_bypass_rejected(self):
        """given 'https://allowed.com.evil.com', when validate_return_to runs with allowed={allowed.com}, then InvalidReturnTo(reason='not_in_whitelist').

        spec #28 — match exact origin (pas startswith/endswith/contains).
        """
        from server.security.auth_providers.return_to import (
            InvalidReturnTo,
            validate_return_to,
        )

        with pytest.raises(InvalidReturnTo) as exc_info:
            validate_return_to(
                "https://app.samsunghealth.com.evil.com",
                _ALLOWED,
            )
        assert exc_info.value.reason == "not_in_whitelist"

    def test_protocol_relative_rejected(self):
        """given '//evil.com', when validate_return_to runs, then InvalidReturnTo(reason='bad_scheme').

        spec #29 — protocol-relative URL → bad_scheme.
        """
        from server.security.auth_providers.return_to import (
            InvalidReturnTo,
            validate_return_to,
        )

        with pytest.raises(InvalidReturnTo) as exc_info:
            validate_return_to("//evil.com", _ALLOWED)
        assert exc_info.value.reason == "bad_scheme"

    def test_userinfo_present_rejected(self):
        """given 'https://app.samsunghealth.com@evil.com', when validate_return_to runs, then InvalidReturnTo(reason='userinfo_present').

        spec #30 — userinfo (@) interdit (auth basic-style url bypass).
        """
        from server.security.auth_providers.return_to import (
            InvalidReturnTo,
            validate_return_to,
        )

        with pytest.raises(InvalidReturnTo) as exc_info:
            validate_return_to(
                "https://app.samsunghealth.com@evil.com",
                _ALLOWED,
            )
        assert exc_info.value.reason == "userinfo_present"

    def test_idn_homograph_rejected_after_punycode(self):
        """given 'https://samsünghealth.com' (cyrillic ü), when validate_return_to runs, then InvalidReturnTo(reason='not_in_whitelist') AFTER punycode normalization.

        spec #31 — IDN punycode anti-homograph.
        """
        from server.security.auth_providers.return_to import (
            InvalidReturnTo,
            validate_return_to,
        )

        with pytest.raises(InvalidReturnTo) as exc_info:
            validate_return_to(
                "https://samsünghealth.com",
                _ALLOWED,
            )
        # The validator may report bad_idna or not_in_whitelist depending on resolution;
        # spec says: punycode normalize then exact match → not_in_whitelist.
        assert exc_info.value.reason == "not_in_whitelist"

    def test_javascript_scheme_rejected(self):
        """given 'javascript:alert(1)', when validate_return_to runs, then InvalidReturnTo(reason='bad_scheme').

        spec #32 — XSS-via-redirect avec scheme javascript:.
        """
        from server.security.auth_providers.return_to import (
            InvalidReturnTo,
            validate_return_to,
        )

        with pytest.raises(InvalidReturnTo) as exc_info:
            validate_return_to("javascript:alert(1)", _ALLOWED)
        assert exc_info.value.reason == "bad_scheme"

    def test_fragment_stripped_path_query_preserved(self):
        """given 'https://app.samsunghealth.com/path?q=1#hash', when validate_return_to runs, then returns 'https://app.samsunghealth.com/path?q=1' (fragment stripped, path+query kept).

        spec #33 — fragment stripped, path/query préservés.
        """
        from server.security.auth_providers.return_to import validate_return_to

        result = validate_return_to(
            "https://app.samsunghealth.com/path?q=1#hash",
            _ALLOWED,
        )
        assert result == "https://app.samsunghealth.com/path?q=1", (
            f"fragment must be stripped, got: {result!r}"
        )

    def test_empty_returns_none(self):
        """given empty string or None, when validate_return_to runs, then returns None (no redirect).

        spec #34 — vide → None.
        """
        from server.security.auth_providers.return_to import validate_return_to

        assert validate_return_to(None, _ALLOWED) is None
        assert validate_return_to("", _ALLOWED) is None

    def test_whitelisted_origin_exact_match_passes(self):
        """given 'https://app.samsunghealth.com' (whitelisted), when validate_return_to runs, then returns the URL with default '/' path.

        spec #35 — whitelisted exact → OK.
        """
        from server.security.auth_providers.return_to import validate_return_to

        result = validate_return_to(
            "https://app.samsunghealth.com",
            _ALLOWED,
        )
        # spec algo step 7 = urlunsplit avec parts.path or "/" → trailing slash.
        assert result == "https://app.samsunghealth.com/", (
            f"whitelisted origin must pass with default path /, got: {result!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestReturnToBypass` (class) — lines 16-137

### Imports
- `pytest`

### Exports
- `TestReturnToBypass`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — classes: `TestReturnToBypass`
