---
type: code-source
language: python
file_path: alembic/versions/0006_verification_tokens.py
git_blob: 8e0a23c348f3e850eed9e7d4a35c1ce9e57f4cdd
last_synced: '2026-04-26T22:07:13Z'
loc: 95
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

# alembic/versions/0006_verification_tokens.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/versions/0006_verification_tokens.py`](../../../alembic/versions/0006_verification_tokens.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 verification tokens (email verification + password reset)

Revision ID: 9d2e3f5a6b71
Revises: 8c1d2e4f5a90
Create Date: 2026-04-26 23:00:00.000000

Creates `verification_tokens` table + 1-active-per-purpose unique partial
index + anti-flip-back trigger preventing UPDATE that re-NULLs consumed_at.
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

import server.db.uuid7

# revision identifiers, used by Alembic.
revision: str = "9d2e3f5a6b71"
down_revision: Union[str, Sequence[str], None] = "8c1d2e4f5a90"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "verification_tokens",
        sa.Column("id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("user_id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("token_hash", sa.Text(), nullable=False),
        sa.Column("purpose", sa.Text(), nullable=False),
        sa.Column(
            "issued_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("consumed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("ip", postgresql.INET(), nullable=True),
        sa.Column("user_agent", sa.Text(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token_hash", name="uq_verification_tokens_token_hash"),
        sa.CheckConstraint(
            "consumed_at IS NULL OR consumed_at >= issued_at",
            name="consumed_after_issued",
        ),
    )
    op.create_index(
        "idx_verification_tokens_user_id", "verification_tokens", ["user_id"]
    )
    op.create_index(
        "idx_verification_tokens_purpose", "verification_tokens", ["purpose"]
    )
    # 1 seul token actif (consumed_at IS NULL) par (user, purpose). Race protection.
    op.create_index(
        "uq_verification_tokens_active_per_purpose",
        "verification_tokens",
        ["user_id", "purpose"],
        unique=True,
        postgresql_where=sa.text("consumed_at IS NULL"),
    )

    # Anti flip-back trigger (pentester reco) : consumed_at peut être set, jamais re-NULLé.
    op.execute(
        """
        CREATE OR REPLACE FUNCTION verification_tokens_no_unconsume()
        RETURNS TRIGGER AS $$
        BEGIN
          IF OLD.consumed_at IS NOT NULL AND NEW.consumed_at IS NULL THEN
            RAISE EXCEPTION 'verification_tokens.consumed_at cannot be NULLed (was %)', OLD.consumed_at;
          END IF;
          RETURN NEW;
        END $$ LANGUAGE plpgsql;
        """
    )
    op.execute(
        """
        CREATE TRIGGER trg_verification_tokens_no_unconsume
          BEFORE UPDATE ON verification_tokens
          FOR EACH ROW EXECUTE FUNCTION verification_tokens_no_unconsume();
        """
    )


def downgrade() -> None:
    op.execute("DROP TRIGGER IF EXISTS trg_verification_tokens_no_unconsume ON verification_tokens")
    op.execute("DROP FUNCTION IF EXISTS verification_tokens_no_unconsume()")
    op.drop_index(
        "uq_verification_tokens_active_per_purpose", table_name="verification_tokens"
    )
    op.drop_index("idx_verification_tokens_purpose", table_name="verification_tokens")
    op.drop_index("idx_verification_tokens_user_id", table_name="verification_tokens")
    op.drop_table("verification_tokens")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `upgrade`, `downgrade`

### Symbols
- `upgrade` (function) — lines 25-84 · **Specs**: [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]]
- `downgrade` (function) — lines 87-95 · **Specs**: [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]]

### Imports
- `typing`
- `alembic`
- `sqlalchemy.dialects`
- `server.db.uuid7`

### Exports
- `upgrade`
- `downgrade`
