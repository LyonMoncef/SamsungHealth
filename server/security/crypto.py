"""AES-256-GCM at-rest encryption helper (V2.2 fondation).

Lit la clé maître depuis env var `SAMSUNGHEALTH_ENCRYPTION_KEY` (base64 32 bytes).
Validation stricte au boot — fail-fast si absente, invalide ou zéro.
"""
from __future__ import annotations

import base64
import os
import secrets
from functools import lru_cache

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


KEY_ENV_VAR = "SAMSUNGHEALTH_ENCRYPTION_KEY"
NONCE_BYTES = 12
KEY_BYTES = 32
_ZERO_KEY = b"\x00" * KEY_BYTES


class EncryptionConfigError(RuntimeError):
    """Clé de chiffrement absente, mal formée ou interdite."""


class DecryptionError(RuntimeError):
    """Échec de déchiffrement (tampering, clé tournée, data corrompue)."""


def load_encryption_key() -> bytes:
    """Charge et valide la clé maître depuis l'env var. Raise EncryptionConfigError sinon."""
    raw = os.environ.get(KEY_ENV_VAR)
    if not raw:
        raise EncryptionConfigError(
            f"Env var {KEY_ENV_VAR} absente. Génère via : "
            f'python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"'
        )
    try:
        decoded = base64.b64decode(raw, validate=True)
    except (ValueError, base64.binascii.Error) as exc:
        raise EncryptionConfigError(f"{KEY_ENV_VAR} n'est pas du base64 valide") from exc
    if len(decoded) != KEY_BYTES:
        raise EncryptionConfigError(
            f"{KEY_ENV_VAR} doit décoder en exactement {KEY_BYTES} bytes, got {len(decoded)}"
        )
    if decoded == _ZERO_KEY:
        raise EncryptionConfigError(
            f"{KEY_ENV_VAR} = zero key forbidden (default/uninit dangereuse)"
        )
    return decoded


@lru_cache(maxsize=1)
def _aesgcm() -> AESGCM:
    return AESGCM(load_encryption_key())


def encrypt_field(plaintext: bytes) -> bytes:
    """Chiffre AES-256-GCM. Retourne nonce(12) || ciphertext_avec_tag."""
    nonce = secrets.token_bytes(NONCE_BYTES)
    ct = _aesgcm().encrypt(nonce, plaintext, associated_data=None)
    return nonce + ct


def decrypt_field(blob: bytes) -> bytes:
    """Déchiffre AES-256-GCM. Raise DecryptionError si tampering / clé invalide."""
    if len(blob) < NONCE_BYTES + 16:  # nonce + min tag
        raise DecryptionError("blob trop court")
    nonce, ct = blob[:NONCE_BYTES], blob[NONCE_BYTES:]
    try:
        return _aesgcm().decrypt(nonce, ct, associated_data=None)
    except InvalidTag as exc:
        raise DecryptionError("decryption failed") from exc


def reset_key_cache() -> None:
    """Test-only — invalide le cache de la clé après monkeypatch env."""
    _aesgcm.cache_clear()
