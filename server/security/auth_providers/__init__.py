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
