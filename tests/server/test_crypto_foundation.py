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
    def test_typedecorator_transparent_on_orm(self, schema_ready, db_session, default_user_db):
        # spec V2.2 §9 — user_id obligatoire depuis V2.3.0.1
        from datetime import datetime, timezone

        from server.db.models import Mood

        m = Mood(
            user_id=default_user_db.id,
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
