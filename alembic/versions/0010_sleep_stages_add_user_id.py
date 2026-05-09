"""sleep_stages — add user_id column, fix unique constraint

Revision ID: 3d6e7f8a9b02
Revises: 2c5d6e7f8a91
Create Date: 2026-05-09 09:30:00.000000

The initial migration created sleep_stages with a unique constraint on
(stage_start, stage_end) and no user_id column. The ORM model expects
user_id (FK → users.id) and a unique constraint on (user_id, stage_start,
stage_end). This migration brings the schema in sync with the model.
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "3d6e7f8a9b02"
down_revision: Union[str, None] = "2c5d6e7f8a91"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add user_id column (nullable first so existing rows don't break)
    op.add_column(
        "sleep_stages",
        sa.Column("user_id", sa.Uuid(), nullable=True),
    )

    # 2. Backfill user_id from the parent sleep_session
    op.execute(
        """
        UPDATE sleep_stages ss
        SET user_id = (
            SELECT sl.user_id FROM sleep_sessions sl WHERE sl.id = ss.session_id
        )
        """
    )

    # 3. Make user_id NOT NULL now that it's populated
    op.alter_column("sleep_stages", "user_id", nullable=False)

    # 4. Add FK constraint
    op.create_foreign_key(
        "fk_sleep_stages_user_id",
        "sleep_stages",
        "users",
        ["user_id"],
        ["id"],
    )

    # 5. Drop old unique constraint (stage_start, stage_end) — wrong scope
    op.drop_constraint("uq_sleep_stages_window", "sleep_stages", type_="unique")

    # 6. Create correct unique constraint (user_id, stage_start, stage_end)
    op.create_unique_constraint(
        "uq_sleep_stages_window",
        "sleep_stages",
        ["user_id", "stage_start", "stage_end"],
    )

    # 7. Add index on user_id for query performance
    op.create_index("idx_sleep_stages_user_id", "sleep_stages", ["user_id"])


def downgrade() -> None:
    op.drop_index("idx_sleep_stages_user_id", table_name="sleep_stages")
    op.drop_constraint("uq_sleep_stages_window", "sleep_stages", type_="unique")
    op.create_unique_constraint(
        "uq_sleep_stages_window",
        "sleep_stages",
        ["stage_start", "stage_end"],
    )
    op.drop_constraint("fk_sleep_stages_user_id", "sleep_stages", type_="foreignkey")
    op.drop_column("sleep_stages", "user_id")
