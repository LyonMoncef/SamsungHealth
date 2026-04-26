"""V2.3.0.1 cleanup: user_id NOT NULL on health tables (after legacy backfill)

Revision ID: 8c1d2e4f5a90
Revises: 7a3b9c0e1d24
Create Date: 2026-04-26 22:00:00.000000

Suite naturelle de 0004 (V2.3 auth foundation). 0004 a :
- ajouté `user_id UUID NULL` sur 21 tables santé,
- backfill des rows existantes vers le user `legacy@samsunghealth.local`,
- créé un index partiel `WHERE user_id IS NULL` pour préserver l'unicité historique
  pendant que les scripts CSV (qui n'avaient pas encore notion de user_id) tournent.

V2.3.0.1 ferme la fenêtre de tolérance :
- Vérifie qu'aucune row n'a `user_id IS NULL` (safety, fail si oui),
- ALTER COLUMN ... SET NOT NULL sur les 21 tables,
- Drop l'index partiel devenu inutile.

Les scripts CSV ont été mis à jour dans le même PR pour ne plus produire de rows NULL.
"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = "8c1d2e4f5a90"
down_revision: Union[str, Sequence[str], None] = "7a3b9c0e1d24"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


# Doit rester aligné avec HEALTH_TABLES_UNIQUE de 0004_auth_foundation.py.
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
    # 1. Safety check : aucune row ne doit rester avec user_id IS NULL.
    # Si oui, l'opérateur doit reset password du user legacy et ré-assigner
    # les rows manuellement avant de re-tenter la migration.
    table_names = [t for t, _, _ in HEALTH_TABLES_UNIQUE]
    union_query = " UNION ALL ".join(
        f"SELECT '{name}' AS table_name, COUNT(*) AS n "
        f"FROM {name} WHERE user_id IS NULL"
        for name in table_names
    )
    op.execute(
        f"""
        DO $$
        DECLARE
          orphan_row record;
          orphan_total bigint := 0;
          orphan_detail text := '';
        BEGIN
          FOR orphan_row IN
            SELECT table_name, n FROM ({union_query}) s WHERE n > 0
          LOOP
            orphan_total := orphan_total + orphan_row.n;
            orphan_detail := orphan_detail
              || format(' %s=%s', orphan_row.table_name, orphan_row.n);
          END LOOP;
          IF orphan_total > 0 THEN
            RAISE EXCEPTION 'V2.3.0.1: % rows still have user_id IS NULL. Detail:%',
              orphan_total, orphan_detail
              USING HINT = 'Re-run V2.3 backfill or assign rows to a user before retrying.';
          END IF;
        END $$;
        """
    )

    # 2. ALTER COLUMN ... SET NOT NULL + drop l'index partiel devenu inutile.
    for table_name, uq_name, _uq_cols in HEALTH_TABLES_UNIQUE:
        op.alter_column(table_name, "user_id", nullable=False)
        op.drop_index(uq_name, table_name=table_name)


def downgrade() -> None:
    for table_name, uq_name, uq_cols in HEALTH_TABLES_UNIQUE:
        op.alter_column(table_name, "user_id", nullable=True)
        op.create_index(
            uq_name,
            table_name,
            uq_cols,
            unique=True,
            postgresql_where="user_id IS NULL",
        )
