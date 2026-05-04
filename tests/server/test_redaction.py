"""V2.3 — structlog redaction processor.

Tests RED-first contre `server.security.redaction.redact_sensitive_keys`:
remplace les valeurs des clés sensibles (password/token/jwt/...) par '[REDACTED]'.

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#38-#44)
"""
from __future__ import annotations


class TestRedactionProcessor:
    def test_redacts_password_key(self):
        """given event_dict with 'password' key, when processor runs, then value is '[REDACTED]'.

        spec #38.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "auth.login.failure", "password": "supersecret"}
        result = redact_sensitive_keys(None, "info", event)
        assert result["password"] == "[REDACTED]"

    def test_redacts_token_key(self):
        """given event_dict with 'token' key, when processor runs, then redacted.

        spec #39.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "auth.login.success", "token": "ey...jwt-here"}
        result = redact_sensitive_keys(None, "info", event)
        assert result["token"] == "[REDACTED]"

    def test_redacts_authorization_key(self):
        """given event_dict with 'authorization' key, when processor runs, then redacted.

        spec #40.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "request", "authorization": "Bearer ey..."}
        result = redact_sensitive_keys(None, "info", event)
        assert result["authorization"] == "[REDACTED]"

    def test_redacts_jwt_key(self):
        """given event_dict with 'jwt' key, when processor runs, then redacted.

        spec #41.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "test", "jwt": "ey..."}
        result = redact_sensitive_keys(None, "info", event)
        assert result["jwt"] == "[REDACTED]"

    def test_redacts_secret_key(self):
        """given event_dict with 'secret' key, when processor runs, then redacted.

        spec #42.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "test", "secret": "shhh"}
        result = redact_sensitive_keys(None, "info", event)
        assert result["secret"] == "[REDACTED]"

    def test_redacts_cookie_key(self):
        """given event_dict with 'cookie' key, when processor runs, then redacted.

        spec #43.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "test", "cookie": "session=abc"}
        result = redact_sensitive_keys(None, "info", event)
        assert result["cookie"] == "[REDACTED]"

    def test_redaction_case_insensitive(self):
        """given event_dict with 'Password' or 'AUTHORIZATION' key, when processor runs, then redacted (case-insensitive).

        spec #44 (variant).
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"Password": "x", "AUTHORIZATION": "Bearer y", "Token": "z"}
        result = redact_sensitive_keys(None, "info", event)
        assert result["Password"] == "[REDACTED]"
        assert result["AUTHORIZATION"] == "[REDACTED]"
        assert result["Token"] == "[REDACTED]"

    def test_redaction_nested_dict(self):
        """given event_dict with nested dict containing a sensitive key, when processor runs, then nested key is redacted.

        spec #44 (variant) — recursion sur dicts imbriqués.
        """
        from server.security.redaction import redact_sensitive_keys

        event = {"event": "request", "ctx": {"password": "x", "user_id": "1"}}
        result = redact_sensitive_keys(None, "info", event)
        assert result["ctx"]["password"] == "[REDACTED]"
        assert result["ctx"]["user_id"] == "1"  # non-sensitive preserved

    def test_redaction_preserves_non_sensitive_keys(self):
        """given event_dict with non-sensitive keys (event, user_id, request_id), when processor runs, then values unchanged.

        spec #44 (variant).
        """
        from server.security.redaction import redact_sensitive_keys

        event = {
            "event": "auth.login.success",
            "user_id": "00000000-0000-0000-0000-000000000001",
            "request_id": "req-abc",
            "level": "info",
        }
        result = redact_sensitive_keys(None, "info", event)
        assert result["event"] == "auth.login.success"
        assert result["user_id"] == "00000000-0000-0000-0000-000000000001"
        assert result["request_id"] == "req-abc"
        assert result["level"] == "info"
