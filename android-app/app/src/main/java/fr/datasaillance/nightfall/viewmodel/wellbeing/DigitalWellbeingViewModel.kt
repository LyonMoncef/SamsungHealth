package fr.datasaillance.nightfall.viewmodel.wellbeing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
import fr.datasaillance.nightfall.data.local.usage.PackageInfoResolver
import fr.datasaillance.nightfall.data.local.usage.UsageStatsScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

enum class WellbeingPeriod(val days: Int, val label: String) {
    TODAY(1, "Aujourd'hui"),
    LAST_7(7, "7 derniers jours"),
    LAST_30(30, "30 derniers jours"),
}

/** Stats agrégées sur la période sélectionnée pour 1 package. */
data class PeriodAppStat(
    val packageName: String,
    val displayLabel: String,
    val totalForegroundMs: Long,
    val daysWithUsage: Int,
)

data class WellbeingUiState(
    val permissionGranted: Boolean = false,
    val collectedDates: List<String> = emptyList(),
    val totalRows: Int = 0,
    val isRefreshing: Boolean = false,
    val lastCollectionAtMs: Long? = null,
    val lastCollectionEventLabel: String? = null,
    val selectedPeriod: WellbeingPeriod = WellbeingPeriod.TODAY,
    val topApps: List<PeriodAppStat> = emptyList(),
    val totalScreenTimeMs: Long = 0,
)

class DigitalWellbeingViewModel(
    private val checkPermission: () -> Boolean,
    private val dao: UsageStatsDao,
    private val packageResolver: PackageInfoResolver? = null,
    private val clock: () -> java.time.LocalDate = { java.time.LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(WellbeingUiState())
    val uiState: StateFlow<WellbeingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                val dates = dao.getCollectedDates()
                val period = _uiState.value.selectedPeriod
                val (rowsForPeriod, lastTs) = loadPeriodRows(period)
                val (topApps, total) = aggregate(rowsForPeriod)
                _uiState.value = _uiState.value.copy(
                    permissionGranted = checkPermission(),
                    collectedDates = dates,
                    totalRows = dao.count(),
                    topApps = topApps.take(15),
                    totalScreenTimeMs = total,
                    isRefreshing = false,
                    lastCollectionAtMs = lastTs,
                )
            } catch (e: Exception) {
                Timber.w("scope=wellbeing_vm refresh_error error=${e::class.simpleName}")
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun setPeriod(period: WellbeingPeriod) {
        if (_uiState.value.selectedPeriod == period) return
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        refresh()
    }

    private suspend fun loadPeriodRows(
        period: WellbeingPeriod,
    ): Pair<List<UsageDailyEntity>, Long?> {
        val today = clock()
        val from = today.minusDays((period.days - 1).toLong()).toString()
        val to = today.toString()
        val rows = runCatching { dao.getInRange(from, to) }.getOrDefault(emptyList())
        val lastTs = rows.maxOfOrNull { it.collectedAtMs }
        return rows to lastTs
    }

    private fun aggregate(rows: List<UsageDailyEntity>): Pair<List<PeriodAppStat>, Long> {
        if (rows.isEmpty()) return emptyList<PeriodAppStat>() to 0L
        val byPkg = HashMap<String, PeriodAppStat>()
        for (r in rows) {
            val existing = byPkg[r.packageName]
            byPkg[r.packageName] = if (existing == null) {
                PeriodAppStat(
                    packageName = r.packageName,
                    displayLabel = packageResolver?.labelFor(r.packageName) ?: r.packageName,
                    totalForegroundMs = r.totalTimeForegroundMs,
                    daysWithUsage = 1,
                )
            } else {
                existing.copy(
                    totalForegroundMs = existing.totalForegroundMs + r.totalTimeForegroundMs,
                    daysWithUsage = existing.daysWithUsage + 1,
                )
            }
        }
        val sorted = byPkg.values.sortedByDescending { it.totalForegroundMs }
        val total = sorted.sumOf { it.totalForegroundMs }
        return sorted to total
    }

    /**
     * Collecte les stats du jour en cours (= today). Le worker périodique
     * automatique cible J-1 le matin pour avoir une journée complète ; ce
     * bouton est l'inverse, l'utilisateur veut voir ses stats actuelles.
     */
    fun collectNow(context: Context) {
        val today = clock()
        UsageStatsScheduler.runOnce(context, target = today)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                lastCollectionEventLabel = "Collecte en cours pour $today…",
            )
            kotlinx.coroutines.delay(2000L)
            val before = _uiState.value.totalRows
            refresh()
            val delta = _uiState.value.totalRows - before
            _uiState.value = _uiState.value.copy(
                lastCollectionEventLabel = if (delta > 0)
                    "Collecte OK : +$delta lignes pour $today"
                else
                    "Collecte OK : $today (idempotent, pas de nouvelles lignes)",
            )
        }
    }

    fun backfill(context: Context, days: Int) {
        UsageStatsScheduler.backfill(context, days)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                lastCollectionEventLabel = "Backfill ${days}j en cours…",
            )
            // Backfill chaîne N workers — chacun ~50ms côté Android, on laisse
            // une marge confortable (~150ms par jour + 1s de slack).
            val expected = days * 150L + 1500L
            kotlinx.coroutines.delay(expected)
            val before = _uiState.value.totalRows
            refresh()
            val delta = _uiState.value.totalRows - before
            _uiState.value = _uiState.value.copy(
                lastCollectionEventLabel = "Backfill OK : +$delta lignes sur ${days + 1}j",
            )
        }
    }
}
