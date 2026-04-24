"""SQLAlchemy TypeDecorator wrappers pour chiffrement transparent (V2.2)."""
from __future__ import annotations

from sqlalchemy import LargeBinary
from sqlalchemy.types import TypeDecorator

from server.security.crypto import decrypt_field, encrypt_field


class EncryptedBytes(TypeDecorator):
    """Stocke `bytes` chiffrés en BYTEA. Les valeurs Python restent des bytes."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, (bytes, bytearray, memoryview)):
            raise TypeError(f"EncryptedBytes attend bytes, got {type(value).__name__}")
        return encrypt_field(bytes(value))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return decrypt_field(bytes(value))


class EncryptedString(TypeDecorator):
    """Stocke `str` chiffrés en BYTEA. Sérialise UTF-8."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, str):
            raise TypeError(f"EncryptedString attend str, got {type(value).__name__}")
        return encrypt_field(value.encode("utf-8"))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return decrypt_field(bytes(value)).decode("utf-8")


class EncryptedInt(TypeDecorator):
    """Stocke `int` chiffrés en BYTEA. Sérialise via str(int).encode('ascii')."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, int) or isinstance(value, bool):
            raise TypeError(f"EncryptedInt attend int, got {type(value).__name__}")
        return encrypt_field(str(value).encode("ascii"))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return int(decrypt_field(bytes(value)).decode("ascii"))


class EncryptedFloat(TypeDecorator):
    """Stocke `float` chiffrés en BYTEA. Sérialise via repr(float).encode('ascii') (préserve IEEE 754)."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if isinstance(value, int) and not isinstance(value, bool):
            value = float(value)
        if not isinstance(value, float):
            raise TypeError(f"EncryptedFloat attend float, got {type(value).__name__}")
        return encrypt_field(repr(value).encode("ascii"))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return float(decrypt_field(bytes(value)).decode("ascii"))
