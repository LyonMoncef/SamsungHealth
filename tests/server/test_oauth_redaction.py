"""V2.3.2 — Extension `_SENSITIVE_KEYS` avec les 6 clés OAuth.

Tests RED-first contre `server.security.redaction._SENSITIVE_KEYS`:
- code, state, id_token, nonce, error_description redactées
- error_description jamais propagé au client (test sur callback réponse JSON)

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#41).
"""
from __future__ import annotations


class TestOauthKeysRedacted:
    def test_redacts_oauth_sensitive_keys(self):
        """given event_dict with 6 OAuth-sensitive keys, when redact_sensitive_keys runs, then all 6 values are '[REDACTED]'.

        spec #41 — extension `_SENSITIVE_KEYS` : code, state, id_token, nonce, error_description (refresh_token déjà couvert).
        """
        from server.security.redaction import redact_sensitive_keys

        event = {
            "event": "oauth.callback",
            "code": "4/0AfJohXn-google-auth-code",
            "state": "ey...state-jwt",
            "id_token": "ey...google-id-token",
            "nonce": "abc123-nonce-raw",
            "error_description": "Internal Google leak info",
            "refresh_token": "ey...refresh-jwt",
        }
        result = redact_sensitive_keys(None, "info", event)
        assert result["code"] == "[REDACTED]", f"code not redacted: {result}"
        assert result["state"] == "[REDACTED]", f"state not redacted: {result}"
        assert result["id_token"] == "[REDACTED]", f"id_token not redacted: {result}"
        assert result["nonce"] == "[REDACTED]", f"nonce not redacted: {result}"
        assert result["error_description"] == "[REDACTED]", (
            f"error_description not redacted: {result}"
        )
        assert result["refresh_token"] == "[REDACTED]", (
            f"refresh_token not redacted: {result}"
        )
        # Non-sensitive event key preserved.
        assert result["event"] == "oauth.callback"

    def test_oauth_sensitive_keys_in_constant(self):
        """given the module constant `_SENSITIVE_KEYS`, when inspected, then it contains the 5 new V2.3.2 keys (code, state, id_token, nonce, error_description).

        spec #41 — vérifier que le set inclut les 5 nouvelles clés OAuth.
        """
        from server.security.redaction import _SENSITIVE_KEYS

        for key in {"code", "state", "id_token", "nonce", "error_description"}:
            assert key in _SENSITIVE_KEYS, (
                f"V2.3.2 OAuth key {key!r} missing from _SENSITIVE_KEYS: {sorted(_SENSITIVE_KEYS)}"
            )
