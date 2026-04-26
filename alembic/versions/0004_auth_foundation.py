"""V2.3 auth foundation: users / refresh_tokens / auth_events + user_id FK on health tables

Revision ID: 7a3b9c0e1d24
Revises: 569d9b237062
Create Date: 2026-04-26 18:00:00.000000

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

import server.db.uuid7

# revision identifiers, used by Alembic.
revision: str = "7a3b9c0e1d24"
down_revision: Union[str, Sequence[str], None] = "569d9b237062"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


# Health tables that get a `user_id` column. The unique constraint per table
# is recreated to include user_id (so user A and user B can both have a row
# with the same timestamp).
HEALTH_TABLES_UNIQUE: list[tuple[str, str, list[str]]] = [
    ("activity_daily", "uq_activity_daily_day", ["day_date"]),
    ("activity_level", "uq_activity_level_time", ["start_time"]),
    ("blood_pressure", "uq_bp_time", ["start_time"]),
    ("ecg", "uq_ecg_window", ["start_time", "end_time"]),
    ("exercise_sessions", "uq_exercise_window", ["exercise_start", "exercise_end"]),
    ("floors_daily", "uq_floors_day", ["day_date"]),
    ("heart_rate_hourly", "uq_hr_hourly_slot", ["date", "hour"]),
    ("height", "uq_height_time", ["start_time"]),
    ("hrv", "uq_hrv_window", ["start_time", "end_time"]),
    ("mood", "uq_mood_time", ["start_time"]),
    ("respiratory_rate", "uq_respi_window", ["start_time", "end_time"]),
    ("skin_temperature", "uq_skin_temp_window", ["start_time", "end_time"]),
    ("sleep_sessions", "uq_sleep_sessions_window", ["sleep_start", "sleep_end"]),
    ("sleep_stages", "uq_sleep_stages_window", ["stage_start", "stage_end"]),
    ("spo2", "uq_spo2_window", ["start_time", "end_time"]),
    ("steps_daily", "uq_steps_daily_day", ["day_date"]),
    ("steps_hourly", "uq_steps_hourly_slot", ["date", "hour"]),
    ("stress", "uq_stress_window", ["start_time", "end_time"]),
    ("vitality_score", "uq_vitality_day", ["day_date"]),
    ("water_intake", "uq_water_time", ["start_time"]),
    ("weight", "uq_weight_time", ["start_time"]),
]


def upgrade() -> None:
    # 1. Extension citext for case-insensitive email lookup.
    op.execute("CREATE EXTENSION IF NOT EXISTS citext")

    # 2. users table.
    op.create_table(
        "users",
        sa.Column("id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("email", postgresql.CITEXT(), nullable=False),
        sa.Column("password_hash", sa.Text(), nullable=False),
        sa.Column("email_verified_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "failed_login_count",
            sa.Integer(),
            nullable=False,
            server_default="0",
        ),
        sa.Column("locked_until", sa.DateTime(timezone=True), nullable=True),
        sa.Column("last_login_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("last_login_ip", postgresql.INET(), nullable=True),
        sa.Column(
            "password_changed_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.Column(
            "is_active",
            sa.Boolean(),
            nullable=False,
            server_default=sa.text("true"),
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("email", name="uq_users_email"),
    )

    # 3. refresh_tokens.
    op.create_table(
        "refresh_tokens",
        sa.Column("id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("user_id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("jti", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column(
            "issued_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("replaced_by", server.db.uuid7.Uuid7(), nullable=True),
        sa.Column("last_used_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("user_agent", sa.Text(), nullable=True),
        sa.Column("ip", postgresql.INET(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["replaced_by"], ["refresh_tokens.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("jti", name="uq_refresh_tokens_jti"),
    )
    op.create_index("idx_refresh_tokens_user_id", "refresh_tokens", ["user_id"])
    op.create_index("idx_refresh_tokens_jti", "refresh_tokens", ["jti"], unique=True)

    # 4. auth_events.
    op.create_table(
        "auth_events",
        sa.Column("id", server.db.uuid7.Uuid7(), nullable=False),
        sa.Column("event_type", sa.Text(), nullable=False),
        sa.Column("user_id", server.db.uuid7.Uuid7(), nullable=True),
        sa.Column("email_hash", sa.Text(), nullable=True),
        sa.Column("ip", postgresql.INET(), nullable=True),
        sa.Column("user_agent", sa.Text(), nullable=True),
        sa.Column("request_id", sa.Text(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("idx_auth_events_user_id", "auth_events", ["user_id"])
    op.create_index("idx_auth_events_event_type", "auth_events", ["event_type"])

    # 5. Add user_id column + index on each health table.
    # Two unique constraints coexist:
    #   - the original (cols-without-user_id) is kept as partial index WHERE user_id IS NULL
    #     → CSV import scripts (legacy NULL user_id) keep working.
    #   - new constraint (user_id, ...cols) handles multi-user case.
    for table_name, uq_name, uq_cols in HEALTH_TABLES_UNIQUE:
        op.add_column(
            table_name,
            sa.Column("user_id", server.db.uuid7.Uuid7(), nullable=True),
        )
        op.create_foreign_key(
            f"fk_{table_name}_user_id",
            table_name,
            "users",
            ["user_id"],
            ["id"],
        )
        op.create_index(f"idx_{table_name}_user_id", table_name, ["user_id"])
        # Drop old unique, recreate as partial unique index (legacy fallback).
        op.drop_constraint(uq_name, table_name, type_="unique")
        op.create_index(
            uq_name,
            table_name,
            uq_cols,
            unique=True,
            postgresql_where=sa.text("user_id IS NULL"),
        )
        # New unique constraint with user_id prefix (multi-user).
        op.create_unique_constraint(
            f"uq_{table_name}_user_window",
            table_name,
            ["user_id", *uq_cols],
        )

    # 6. Legacy backfill: if users empty AND any health table non-empty, create
    # a `legacy@samsunghealth.local` user (is_active=false, password=random
    # impossible-to-login string) and assign all health rows to them.
    op.execute(
        """
        DO $$
        DECLARE
          legacy_id uuid;
          has_data boolean := false;
        BEGIN
          IF NOT EXISTS (SELECT 1 FROM users) THEN
            -- Detect any non-empty health table.
            IF EXISTS (SELECT 1 FROM sleep_sessions LIMIT 1)
               OR EXISTS (SELECT 1 FROM heart_rate_hourly LIMIT 1)
               OR EXISTS (SELECT 1 FROM steps_hourly LIMIT 1)
               OR EXISTS (SELECT 1 FROM exercise_sessions LIMIT 1)
               OR EXISTS (SELECT 1 FROM mood LIMIT 1)
            THEN
              has_data := true;
            END IF;

            IF has_data THEN
              legacy_id := gen_random_uuid();
              INSERT INTO users (id, email, password_hash, is_active, password_changed_at, created_at, updated_at)
              VALUES (
                legacy_id,
                'legacy@samsunghealth.local',
                '$argon2id$v=19$m=46080,t=2,p=1$LEGACYBACKFILLNOLOGINPOSSIBLE',
                false,
                now(),
                now(),
                now()
              );
              UPDATE sleep_sessions SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE sleep_stages SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE heart_rate_hourly SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE steps_hourly SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE steps_daily SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE exercise_sessions SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE mood SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE stress SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE spo2 SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE respiratory_rate SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE hrv SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE skin_temperature SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE weight SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE height SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE blood_pressure SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE water_intake SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE activity_daily SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE activity_level SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE vitality_score SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE floors_daily SET user_id = legacy_id WHERE user_id IS NULL;
              UPDATE ecg SET user_id = legacy_id WHERE user_id IS NULL;
            END IF;
          END IF;
        END $$;
        """
    )


def downgrade() -> None:
    # Reverse the unique/index/column changes on health tables.
    # NB: l'upgrade a remplacé `uq_<table>` par un INDEX partiel (pas un constraint),
    # donc on drop l'index puis on recrée le constraint d'origine.
    for table_name, uq_name, uq_cols in HEALTH_TABLES_UNIQUE:
        op.drop_constraint(f"uq_{table_name}_user_window", table_name, type_="unique")
        op.execute(f'DROP INDEX IF EXISTS {uq_name}')
        op.create_unique_constraint(uq_name, table_name, uq_cols)
        op.drop_index(f"idx_{table_name}_user_id", table_name=table_name)
        op.drop_constraint(f"fk_{table_name}_user_id", table_name, type_="foreignkey")
        op.drop_column(table_name, "user_id")

    op.drop_index("idx_auth_events_event_type", table_name="auth_events")
    op.drop_index("idx_auth_events_user_id", table_name="auth_events")
    op.drop_table("auth_events")

    op.drop_index("idx_refresh_tokens_jti", table_name="refresh_tokens")
    op.drop_index("idx_refresh_tokens_user_id", table_name="refresh_tokens")
    op.drop_table("refresh_tokens")

    op.drop_table("users")
    # citext extension left in place (harmless).
