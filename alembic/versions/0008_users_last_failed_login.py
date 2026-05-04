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
