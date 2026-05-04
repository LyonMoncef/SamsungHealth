---
type: code-source
language: python
file_path: server/security/passwords.py
git_blob: 62e1047f29b0ad028a0a487e890c3d8acc40ba72
last_synced: '2026-04-26T22:07:14Z'
loc: 139
annotations: []
imports: []
exports:
- WeakPasswordError
- validate_password_strength
tags:
- code
- python
---

# server/security/passwords.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/passwords.py`](../../../server/security/passwords.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Password strength validation (min length + top-100 leaked blocklist).

Used by both `/auth/register` (V2.3) and `/auth/password/reset/confirm` (V2.3.1)
for consistency. All blocklist entries are ≥ 12 chars (else already caught by
the length check; keeps the data structure minimal).
"""
from __future__ import annotations


_MIN_PASSWORD_LENGTH = 12


# Top-100 leaked passwords ≥ 12 chars (HaveIBeenPwned / NIST SP 800-63B style).
# Keeps only entries that pass the length floor — under-12 strings are already
# rejected by the length check, no need to duplicate.
_PASSWORD_BLOCKLIST: frozenset[str] = frozenset(
    {
        "password1234",
        "password12345",
        "password123456",
        "password1234567",
        "password12345678",
        "password123!!!",
        "password1!!!!",
        "passw0rd1234",
        "p@ssword1234",
        "passwordpassword",
        "qwerty123456",
        "qwerty1234567",
        "qwerty12345678",
        "qwertyuiop12",
        "qwertyuiop123",
        "1234567890ab",
        "12345678901234",
        "123456789012",
        "1234567890123",
        "abcdefg12345",
        "abcdefgh1234",
        "abcdefghij12",
        "abcd12345678",
        "letmein123456",
        "letmein12345678",
        "letmeinplease",
        "iloveyou1234",
        "iloveyou12345",
        "iloveyou2026",
        "iloveyou2025",
        "iloveyou2024",
        "admin1234567",
        "admin12345678",
        "administrator",
        "administrator1",
        "welcome12345",
        "welcome123456",
        "welcomeback1",
        "monkey123456",
        "monkey1234567",
        "dragon123456",
        "dragon1234567",
        "master123456",
        "master1234567",
        "freedom12345",
        "freedom123456",
        "shadow123456",
        "superman1234",
        "batman123456",
        "trustno12345",
        "sunshine1234",
        "princess1234",
        "princess12345",
        "football1234",
        "football12345",
        "baseball1234",
        "baseball12345",
        "hello1234567",
        "hello12345678",
        "summer123456",
        "summer20262025",
        "winter123456",
        "spring123456",
        "autumn123456",
        "samsung12345",
        "samsung123456",
        "samsunghealth",
        "samsunghealth1",
        "google1234567",
        "google123456",
        "facebook1234",
        "facebook12345",
        "linkedin1234",
        "linkedin12345",
        "passw0rd1234!!",
        "changeme1234",
        "changeme12345",
        "qazwsxedc123",
        "zxcvbnm12345",
        "asdfghjkl123",
        "1qaz2wsx3edc",
        "1qaz2wsx3edc4",
        "qwer1234asdf",
        "qwer1234zxcv",
        "iloveyou123456!",
        "letmein123456!",
        "welcome2026abc",
        "welcome2025abc",
        "welcome2024abc",
        "password2026",
        "password2025",
        "password2024",
        "myp@ssword12",
        "mypassword12",
        "mypassword123",
        "mypassword1234",
        "iloveyousoooo",
        "iloveyouforever",
        "thisisapassword",
        "thereisnopassword",
        "loginpassword",
        "loginpassword1",
        "test1234test",
        "testtesttest",
    }
)


class WeakPasswordError(ValueError):
    """Raised when a password fails strength validation (length or blocklist)."""


def validate_password_strength(pwd: str) -> None:
    """Validate `pwd`. Raise WeakPasswordError if too short or in blocklist.

    Used at register and password reset confirm. Blocklist match is case-sensitive
    (lowercase canonical entries — leaked password lists are lowercase by convention).
    """
    if not isinstance(pwd, str) or len(pwd) < _MIN_PASSWORD_LENGTH:
        raise WeakPasswordError("password_too_short")
    if pwd in _PASSWORD_BLOCKLIST or pwd.lower() in _PASSWORD_BLOCKLIST:
        raise WeakPasswordError("password_in_blocklist")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `_PASSWORD_BLOCKLIST`, `validate_password_strength`, `WeakPasswordError`

### Symbols
- `WeakPasswordError` (class) — lines 126-127 · **Specs**: [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]]
- `validate_password_strength` (function) — lines 130-139 · **Specs**: [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]]

### Exports
- `WeakPasswordError`
- `validate_password_strength`
