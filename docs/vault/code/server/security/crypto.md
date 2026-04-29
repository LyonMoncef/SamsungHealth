---
type: code-source
language: python
file_path: server/security/crypto.py
git_blob: bc96720463c14f1043dcd3e47a4469b5ac8e6ac5
last_synced: '2026-04-26T14:46:49Z'
loc: 83
annotations: []
imports:
- base64
- os
- secrets
- functools
- cryptography.exceptions
- cryptography.hazmat.primitives.ciphers.aead
- server.logging_config
exports:
- EncryptionConfigError
- DecryptionError
- load_encryption_key
- encrypt_field
- decrypt_field
- reset_key_cache
tags:
- code
- python
---

# server/security/crypto.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/crypto.py`](../../../server/security/crypto.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
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

from server.logging_config import get_logger


_log = get_logger(__name__)

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
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `load_encryption_key`, `encrypt_field`, `decrypt_field`, `EncryptionConfigError`, `DecryptionError`

### Symbols
- `EncryptionConfigError` (class) — lines 27-28 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `DecryptionError` (class) — lines 31-32 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `load_encryption_key` (function) — lines 35-55 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `encrypt_field` (function) — lines 63-67 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `decrypt_field` (function) — lines 70-78 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `reset_key_cache` (function) — lines 81-83

### Imports
- `base64`
- `os`
- `secrets`
- `functools`
- `cryptography.exceptions`
- `cryptography.hazmat.primitives.ciphers.aead`
- `server.logging_config`

### Exports
- `EncryptionConfigError`
- `DecryptionError`
- `load_encryption_key`
- `encrypt_field`
- `decrypt_field`
- `reset_key_cache`
