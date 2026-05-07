---
type: code-source
language: python
file_path: server/security/auth_providers/__init__.py
git_blob: dd28cdb1e00ae86bcc7433343a80eae2d2c02970
last_synced: '2026-05-06T08:02:34Z'
loc: 42
annotations: []
imports:
- abc
- pydantic
exports:
- AuthProviderError
- ProviderProfile
- AuthProvider
tags:
- code
- python
---

# server/security/auth_providers/__init__.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/auth_providers/__init__.py`](../../../server/security/auth_providers/__init__.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — AuthProvider abstraction (registry-driven OAuth).

Plug-and-play interface for OAuth/OIDC providers. V2.3.2 ships GoogleAuthProvider
only; Apple Sign-in / Microsoft Entra (Phase B+) implement the same ABC.
"""
from __future__ import annotations

from abc import ABC, abstractmethod

from pydantic import BaseModel


class AuthProviderError(Exception):
    """Base error for OAuth flow failures (signature, claim, nonce, exchange...)."""


class ProviderProfile(BaseModel):
    """Normalized profile across providers (extracted from validated ID token)."""

    sub: str
    email: str
    email_verified: bool
    raw_claims: dict


class AuthProvider(ABC):
    name: str

    @abstractmethod
    def build_authorize_url(
        self, *, state: str, nonce: str, redirect_uri: str
    ) -> str: ...

    @abstractmethod
    async def exchange_code_for_tokens(
        self, *, code: str, redirect_uri: str
    ) -> dict: ...

    @abstractmethod
    async def validate_id_token(
        self, *, id_token: str, expected_nonce: str
    ) -> ProviderProfile: ...
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `AuthProvider`, `ProviderProfile`, `AuthProviderError`

### Symbols
- `AuthProviderError` (class) — lines 13-14 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `ProviderProfile` (class) — lines 17-23 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `AuthProvider` (class) — lines 26-42 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `abc`
- `pydantic`

### Exports
- `AuthProviderError`
- `ProviderProfile`
- `AuthProvider`
