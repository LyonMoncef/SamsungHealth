package fr.datasaillance.nightfall.domain.import_

import fr.datasaillance.nightfall.R

enum class ImportDataType(
    val samsungFilenamePrefix: String,
    val apiPath: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    SLEEP(
        samsungFilenamePrefix = "com.samsung.health.sleep",
        apiPath = "api/sleep/import",
        labelRes = R.string.import_type_sleep,
        iconRes = R.drawable.ic_import_sleep,
    ),
    HEART_RATE(
        samsungFilenamePrefix = "com.samsung.health.heart_rate",
        apiPath = "api/heartrate/import",
        labelRes = R.string.import_type_heartrate,
        iconRes = R.drawable.ic_import_heartrate,
    ),
    STEPS(
        samsungFilenamePrefix = "com.samsung.health.step_daily_trend",
        apiPath = "api/steps/import",
        labelRes = R.string.import_type_steps,
        iconRes = R.drawable.ic_import_steps,
    ),
    EXERCISE(
        samsungFilenamePrefix = "com.samsung.health.exercise",
        apiPath = "api/exercise/import",
        labelRes = R.string.import_type_exercise,
        iconRes = R.drawable.ic_import_exercise,
    ),
}
