---
type: code-source
language: python
file_path: alembic/versions/0002_encrypt_mood.py
git_blob: 51e62e7d8407147a3bbc06c14a360a43a70a28ad
last_synced: '2026-04-24T03:44:10Z'
loc: 78
annotations: []
imports:
- typing
- alembic
- server.db.encrypted
exports:
- upgrade
- downgrade
tags:
- code
- python
---

# alembic/versions/0002_encrypt_mood.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/versions/0002_encrypt_mood.py`](../../../alembic/versions/0002_encrypt_mood.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""encrypt mood Art.9 fields (V2.2)

Revision ID: cd52629cf7b7
Revises: ffc3fa072656
Create Date: 2026-04-24 05:36:27.729447

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

from server.db.encrypted import EncryptedInt, EncryptedString


# revision identifiers, used by Alembic.
revision: str = 'cd52629cf7b7'
down_revision: Union[str, Sequence[str], None] = 'ffc3fa072656'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """V2.2 — chiffrement Art.9 sur mood. Assume fresh DB (rows TEXT/INT droppées via USING NULL)."""
    op.add_column('mood', sa.Column('mood_type_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('emotions_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('factors_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('notes_crypto_v', sa.Integer(), server_default='1', nullable=False))
    # Conversion type TEXT/INT → BYTEA : USING NULL pour vider les anciennes valeurs (fresh DB assumée).
    # Toute donnée mood pré-V2.2 doit être ré-importée via le pipeline CSV.
    op.alter_column('mood', 'mood_type',
                    existing_type=sa.INTEGER(),
                    type_=EncryptedInt(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'emotions',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'factors',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'notes',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')


def downgrade() -> None:
    """V2.2 — downgrade ne préserve PAS les données chiffrées (perte irrémédiable)."""
    op.alter_column('mood', 'notes',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'factors',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'emotions',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'mood_type',
                    existing_type=EncryptedInt(),
                    type_=sa.INTEGER(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.drop_column('mood', 'notes_crypto_v')
    op.drop_column('mood', 'factors_crypto_v')
    op.drop_column('mood', 'emotions_crypto_v')
    op.drop_column('mood', 'mood_type_crypto_v')
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `upgrade`, `downgrade`

### Symbols
- `upgrade` (function) — lines 23-50 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `downgrade` (function) — lines 53-78 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]

### Imports
- `typing`
- `alembic`
- `server.db.encrypted`

### Exports
- `upgrade`
- `downgrade`
