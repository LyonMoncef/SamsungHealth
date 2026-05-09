package fr.datasaillance.nightfall.data.local.import_

/** Codes Samsung sleep_stage → libellé canonique (mêmes valeurs que server/services/csv_import.py). */
internal val SLEEP_STAGE_MAP: Map<Int, String> = mapOf(
    40001 to "AWAKE",
    40002 to "LIGHT",
    40003 to "DEEP",
    40004 to "REM",
)

/** Codes Samsung exercise_type → libellé. Codes inconnus stockés comme `samsung_<code>`. */
internal val EXERCISE_TYPE_MAP: Map<Int, String> = mapOf(
    1001 to "running",
    1002 to "cycling",
    1007 to "walking",
    1008 to "hiking",
    3000 to "swimming",
    90001 to "indoor_cycling",
)
