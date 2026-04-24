---
type: code-source
language: python
file_path: tests/server/test_crypto_foundation.py
git_blob: dea8a32c87401116d49bb433f3be5b91345b3ac8
last_synced: '2026-04-24T03:32:00Z'
loc: 122
annotations: []
imports:
- base64
- importlib
- secrets
- pytest
exports:
- _b64
- TestLoadEncryptionKey
- TestEncryptDecryptField
- TestEncryptedTypeDecorator
- TestBootValidation
tags:
- code
- python
---

# tests/server/test_crypto_foundation.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_crypto_foundation.py`](../../../tests/server/test_crypto_foundation.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests RED — V2.2 fondation chiffrement AES-256-GCM.

Mappé sur frontmatter tested_by: tests/server/test_crypto_foundation.py
Classes: TestLoadEncryptionKey, TestEncryptDecryptField, TestEncryptedTypeDecorator, TestBootValidation
"""
import base64
import importlib
import secrets

import pytest


def _b64(b: bytes) -> str:
    return base64.b64encode(b).decode("ascii")


class TestLoadEncryptionKey:
    def test_load_key_from_env(self, monkeypatch):
        # spec V2.2 §1
        from server.security.crypto import load_encryption_key
        raw = secrets.token_bytes(32)
        monkeypatch.setenv("SAMSUNGHEALTH_ENCRYPTION_KEY", _b64(raw))
        assert load_encryption_key() == raw

    def test_load_key_missing_env_raises(self, monkeypatch):
        # spec V2.2 §2
        from server.security.crypto import EncryptionConfigError, load_encryption_key
        monkeypatch.delenv("SAMSUNGHEALTH_ENCRYPTION_KEY", raising=False)
        with pytest.raises(EncryptionConfigError):
            load_encryption_key()

    def test_load_key_invalid_base64_raises(self, monkeypatch):
        # spec V2.2 §3
        from server.security.crypto import EncryptionConfigError, load_encryption_key
        monkeypatch.setenv("SAMSUNGHEALTH_ENCRYPTION_KEY", "not_base64!@#%")
        with pytest.raises(EncryptionConfigError):
            load_encryption_key()

    def test_load_key_wrong_length_raises(self, monkeypatch):
        # spec V2.2 §4
        from server.security.crypto import EncryptionConfigError, load_encryption_key
        monkeypatch.setenv("SAMSUNGHEALTH_ENCRYPTION_KEY", _b64(secrets.token_bytes(16)))
        with pytest.raises(EncryptionConfigError, match="32"):
            load_encryption_key()

    def test_load_key_all_zeros_raises(self, monkeypatch):
        # spec V2.2 §5
        from server.security.crypto import EncryptionConfigError, load_encryption_key
        monkeypatch.setenv("SAMSUNGHEALTH_ENCRYPTION_KEY", _b64(b"\x00" * 32))
        with pytest.raises(EncryptionConfigError, match="zero|default"):
            load_encryption_key()


class TestEncryptDecryptField:
    def test_round_trip_bytes(self):
        # spec V2.2 §6
        from server.security.crypto import decrypt_field, encrypt_field
        plaintext = b"sensitive health value"
        ct = encrypt_field(plaintext)
        assert ct != plaintext, "Le ciphertext doit différer du plaintext"
        assert isinstance(ct, bytes)
        assert decrypt_field(ct) == plaintext

    def test_round_trip_string(self):
        # spec V2.2 §7 (via TypeDecorator EncryptedString)
        from server.db.encrypted import EncryptedString
        td = EncryptedString()
        plaintext = "Bonjour 🌙 état d'âme"
        bound = td.process_bind_param(plaintext, dialect=None)
        assert isinstance(bound, bytes)
        assert plaintext.encode("utf-8") not in bound, "Le plaintext UTF-8 ne doit pas apparaître en clair"
        assert td.process_result_value(bound, dialect=None) == plaintext

    def test_tamper_detected_invalid_tag(self):
        # spec V2.2 §8
        from server.security.crypto import DecryptionError, encrypt_field
        ct = bytearray(encrypt_field(b"hello"))
        # Flip 1 byte au milieu (pas dans le nonce)
        ct[20] ^= 0xFF
        from server.security.crypto import decrypt_field
        with pytest.raises(DecryptionError):
            decrypt_field(bytes(ct))


class TestEncryptedTypeDecorator:
    def test_typedecorator_transparent_on_orm(self, schema_ready, db_session):
        # spec V2.2 §9
        from datetime import datetime, timezone

        from server.db.models import Mood

        m = Mood(
            start_time=datetime(2026, 4, 24, 9, 30, tzinfo=timezone.utc),
            mood_type=3,
            notes="weekend cool",
            emotions="calme,joie",
            factors="météo,sport",
            place="home",
            company="solo",
        )
        db_session.add(m)
        db_session.commit()
        db_session.expire_all()

        from sqlalchemy import select
        loaded = db_session.execute(select(Mood)).scalar_one()
        assert loaded.mood_type == 3
        assert loaded.notes == "weekend cool"
        assert loaded.emotions == "calme,joie"
        assert loaded.factors == "météo,sport"


class TestBootValidation:
    def test_app_boot_fails_fast_without_key(self, monkeypatch):
        # spec V2.2 §10 — pour ne pas casser server.main déjà importé,
        # on teste directement la fonction _validate_encryption_at_boot
        monkeypatch.delenv("SAMSUNGHEALTH_ENCRYPTION_KEY", raising=False)
        from server.main import _validate_encryption_at_boot
        from server.security.crypto import EncryptionConfigError
        with pytest.raises(EncryptionConfigError):
            _validate_encryption_at_boot()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_b64` (function) — lines 14-15
- `TestLoadEncryptionKey` (class) — lines 18-52
- `TestEncryptDecryptField` (class) — lines 55-83
- `TestEncryptedTypeDecorator` (class) — lines 86-111
- `TestBootValidation` (class) — lines 114-122

### Imports
- `base64`
- `importlib`
- `secrets`
- `pytest`

### Exports
- `_b64`
- `TestLoadEncryptionKey`
- `TestEncryptDecryptField`
- `TestEncryptedTypeDecorator`
- `TestBootValidation`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — classes: `TestLoadEncryptionKey`, `TestEncryptDecryptField`, `TestEncryptedTypeDecorator`, `TestBootValidation` · methods: `test_load_key_from_env`, `test_load_key_missing_env_raises`, `test_load_key_invalid_base64_raises`, `test_load_key_wrong_length_raises`, `test_load_key_all_zeros_raises`, `test_round_trip_bytes`, `test_round_trip_string`, `test_tamper_detected_invalid_tag`, `test_typedecorator_transparent_on_orm`, `test_app_boot_fails_fast_without_key`
