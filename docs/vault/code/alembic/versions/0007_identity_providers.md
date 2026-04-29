---
type: code-source
language: python
file_path: alembic/versions/0007_identity_providers.py
git_blob: 14fcd82608a56d594e610839dd96b88fb42e9707
last_synced: '2026-04-27T07:34:23Z'
loc: 81
annotations: []
imports:
- typing
- alembic
- sqlalchemy.dialects
- server.db.uuid7
exports:
- upgrade
- downgrade
tags:
- code
- python
---

# alembic/versions/0007_identity_providers.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/versions/0007_identity_providers.py`](../../../alembic/versions/0007_identity_providers.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 identity_providers + verification_tokens.payload

Revision ID: 0a3b4c5d6e72
Revises: 9d2e3f5a6b71
Create Date: 2026-04-26 23:30:00.000000

Creates `identity_providers` table (Google OAuth + future Apple/MS) +
adds `payload jsonb NULL` column to `verification_tokens` (used to carry
oauth_link_confirm context until the user clicks the email link).
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

import server.db.uuid7

# revision identifiers, used by Alembic.
revision: str = "0a3b4c5d6e72"
down_revision: Union[str, Sequence[str], None] = "9d2e3f5a6b71"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "identity_providers",
        sa.Column("id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("user_id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("provider", sa.Text(), nullable=False),
        sa.Column("provider_sub", sa.Text(), nullable=False),
        sa.Column("provider_email", sa.Text(), nullable=True),
        sa.Column(
            "email_verified",
            sa.Boolean(),
            nullable=False,
            server_default=sa.text("false"),
        ),
        sa.Column(
            "linked_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("last_used_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("raw_claims", postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "provider", "provider_sub", name="uq_identity_providers_provider_sub"
        ),
        sa.UniqueConstraint(
            "user_id", "provider", name="uq_identity_providers_user_provider"
        ),
    )
    op.create_index(
        "idx_identity_providers_user_id", "identity_providers", ["user_id"]
    )
    op.create_index(
        "idx_identity_providers_provider_sub",
        "identity_providers",
        ["provider", "provider_sub"],
    )

    # Add payload jsonb to verification_tokens (used for oauth_link_confirm).
    op.add_column(
        "verification_tokens",
        sa.Column(
            "payload", postgresql.JSONB(astext_type=sa.Text()), nullable=True
        ),
    )


def downgrade() -> None:
    op.drop_column("verification_tokens", "payload")
    op.drop_index(
        "idx_identity_providers_provider_sub", table_name="identity_providers"
    )
    op.drop_index("idx_identity_providers_user_id", table_name="identity_providers")
    op.drop_table("identity_providers")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `upgrade`, `downgrade`

### Symbols
- `upgrade` (function) — lines 26-72 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `downgrade` (function) — lines 75-81 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `typing`
- `alembic`
- `sqlalchemy.dialects`
- `server.db.uuid7`

### Exports
- `upgrade`
- `downgrade`
