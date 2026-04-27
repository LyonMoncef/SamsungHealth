---
type: code-source
language: python
file_path: alembic/versions/0008_users_last_failed_login.py
git_blob: b6f71988d01643c7b0c5fd171c7ca0839bceca14
last_synced: '2026-04-27T17:56:06Z'
loc: 35
annotations: []
imports:
- typing
- alembic
exports:
- upgrade
- downgrade
tags:
- code
- python
---

# alembic/versions/0008_users_last_failed_login.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/versions/0008_users_last_failed_login.py`](../../../alembic/versions/0008_users_last_failed_login.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — users.last_failed_login_at TIMESTAMPTZ NULL

Revision ID: 1b4c5d6e7f83
Revises: 0a3b4c5d6e72
Create Date: 2026-04-26 12:00:00.000000

Adds `last_failed_login_at` column for cleanup_stale_failed_login_counts cron
(reset failed_login_count to 0 if no fail in last 24h).
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op


# revision identifiers, used by Alembic.
revision: str = "1b4c5d6e7f83"
down_revision: Union[str, Sequence[str], None] = "0a3b4c5d6e72"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column(
            "last_failed_login_at",
            sa.DateTime(timezone=True),
            nullable=True,
        ),
    )


def downgrade() -> None:
    op.drop_column("users", "last_failed_login_at")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `upgrade`, `downgrade`

### Symbols
- `upgrade` (function) — lines 23-31 · **Specs**: [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout|2026-04-26-v2.3.3.1-rate-limit-lockout]]
- `downgrade` (function) — lines 34-35 · **Specs**: [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout|2026-04-26-v2.3.3.1-rate-limit-lockout]]

### Imports
- `typing`
- `alembic`

### Exports
- `upgrade`
- `downgrade`
