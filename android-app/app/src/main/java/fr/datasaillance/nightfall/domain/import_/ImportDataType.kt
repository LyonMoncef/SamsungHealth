package fr.datasaillance.nightfall.domain.import_

import fr.datasaillance.nightfall.R

enum class ImportDataType(
    val samsungFilenamePrefix: String,
    val apiPath: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    SLEEP(
        samsungFilenamePrefix = "com.samsung.shealth.sleep.",
        apiPath = "api/sleep/import",
        labelRes = R.string.import_type_sleep,
        iconRes = R.drawable.ic_import_sleep,
    ),
    HEART_RATE(
        samsungFilenamePrefix = "com.samsung.shealth.tracker.heart_rate.",
        apiPath = "api/heartrate/import",
        labelRes = R.string.import_type_heartrate,
        iconRes = R.drawable.ic_import_heartrate,
    ),
    STEPS(
        samsungFilenamePrefix = "com.samsung.shealth.step_daily_trend.",
        apiPath = "api/steps/import",
        labelRes = R.string.import_type_steps,
        iconRes = R.drawable.ic_import_steps,
    ),
    EXERCISE(
        samsungFilenamePrefix = "com.samsung.shealth.exercise.",
        apiPath = "api/exercise/import",
        labelRes = R.string.import_type_exercise,
        iconRes = R.drawable.ic_import_exercise,
    ),
    SLEEP_STAGE(
        samsungFilenamePrefix = "com.samsung.health.sleep_stage.",
        apiPath = "api/sleep/import-stages",
        labelRes = R.string.import_type_sleep_stage,
        iconRes = R.drawable.ic_import_sleep,
    ),
}
