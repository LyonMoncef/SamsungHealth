---
type: code-source
language: python
file_path: alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py
git_blob: 72b32caa09b7b57b142a02c8d458e14cfcad97a8
last_synced: '2026-04-29T20:50:45Z'
loc: 69
annotations: []
imports:
- typing
- alembic
- sqlalchemy.dialects
exports:
- upgrade
- downgrade
tags:
- code
- python
---

# alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py`](../../../alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 3 RGPD — auth_events.{ip_hash,meta} + verification_tokens.purpose CHECK

Revision ID: 2c5d6e7f8a91
Revises: 1b4c5d6e7f83
Create Date: 2026-04-28 14:00:00.000000

Three changes :
- `auth_events.ip_hash` (TEXT NULL) — HMAC-SHA256 truncated 16-hex of client IP
  (cohérent V2.3.3.1, RGPD-friendly cross-event identifier).
- `auth_events.meta` (JSONB NULL) — application-level audit payload, capped
  4KB at insertion via `audit_event()` helper.
- `verification_tokens.purpose` CHECK constraint — whitelists the four valid
  purposes (defense in depth on top of the application check, blocks
  cross-purpose token reuse at the DB level — pentester MED).

Phase 3 spec : docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md §11.
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql


# revision identifiers, used by Alembic.
revision: str = "2c5d6e7f8a91"
down_revision: Union[str, Sequence[str], None] = "1b4c5d6e7f83"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


# Whitelist of valid `verification_tokens.purpose` values across V2.3 → Phase 3.
_VALID_PURPOSES = (
    "email_verification",
    "password_reset",
    "oauth_link_confirm",
    "account_export_confirm",
    "account_erase_confirm",
)


def upgrade() -> None:
    # 1) auth_events.ip_hash (TEXT NULL).
    op.add_column(
        "auth_events",
        sa.Column("ip_hash", sa.Text(), nullable=True),
    )
    # 2) auth_events.meta (JSONB NULL).
    op.add_column(
        "auth_events",
        sa.Column("meta", postgresql.JSONB(astext_type=sa.Text()), nullable=True),
    )
    # 3) verification_tokens.purpose CHECK constraint.
    purposes_sql = ", ".join(f"'{p}'" for p in _VALID_PURPOSES)
    op.create_check_constraint(
        "ck_verification_tokens_purpose",
        "verification_tokens",
        f"purpose IN ({purposes_sql})",
    )


def downgrade() -> None:
    op.drop_constraint(
        "ck_verification_tokens_purpose",
        "verification_tokens",
        type_="check",
    )
    op.drop_column("auth_events", "meta")
    op.drop_column("auth_events", "ip_hash")
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `upgrade` (function) — lines 42-59
- `downgrade` (function) — lines 62-69

### Imports
- `typing`
- `alembic`
- `sqlalchemy.dialects`

### Exports
- `upgrade`
- `downgrade`
