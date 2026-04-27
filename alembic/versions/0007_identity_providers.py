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
