package fr.datasaillance.nightfall.viewmodel.radial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.local.dao.LabeledPlaceDao
import fr.datasaillance.nightfall.data.local.dao.LocationDao
import fr.datasaillance.nightfall.data.local.dao.UsageSessionDao
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.location.PlaceResolver
import fr.datasaillance.nightfall.data.local.location.toLabeledPlace
import fr.datasaillance.nightfall.dataviz.radial.RadialActivity
import fr.datasaillance.nightfall.dataviz.radial.RadialDay
import fr.datasaillance.nightfall.dataviz.radial.RadialUsageRow
import fr.datasaillance.nightfall.dataviz.radial.RadialVisit
import fr.datasaillance.nightfall.dataviz.radial.SleepStage
import fr.datasaillance.nightfall.dataviz.radial.StageInterval
import fr.datasaillance.nightfall.dataviz.radial.UsageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.data.sleep.legacyStageName

/**
 * Agrège sleep + visits + activities + usage par jour pour le `MultiDonutClock`.
 *
 * Charge la fenêtre [today - windowDays + 1, today] et expose un `Map<LocalDate, RadialDay>`.
 * Chaque RadialDay agrège les 4 sources de données dans le fuseau local.
 *
 * Le radial clock fait le rendu — ce ViewModel fait juste la transformation
 * Room entities → DTO viz light (`SleepStage` enum, `RadialVisit/Activity/UsageRow`).
 */
data class RadialUiState(
    val days: Map<LocalDate, RadialDay> = emptyMap(),
    val initialDate: LocalDate = LocalDate.now(),
    val typicalUsageHourDist: FloatArray? = null,
    val isLoading: Boolean = true,
)

class RadialClockViewModel(
    /** Sessions de sommeil (contrat v1) dont le début tombe dans [fromMs, toMs) — fournies par Health Connect. */
    private val sleepRecordsInRange: suspend (fromMs: Long, toMs: Long) -> List<SleepRecord>,
    private val locationDao: LocationDao,
    private val usageStatsDao: UsageStatsDao,
    private val labeledPlaceDao: LabeledPlaceDao? = null,
    private val usageSessionDao: UsageSessionDao? = null,
    private val windowDays: Int = 30,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadialUiState())
    val uiState: StateFlow<RadialUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { buildDaysMap() }
                .onSuccess { days ->
                    val today = clock()
                    _uiState.value = RadialUiState(
                        days = days,
                        initialDate = today,
                        typicalUsageHourDist = computeTypicalHourDist(days),
                        isLoading = false,
                    )
                }
                .onFailure { e ->
                    Timber.w("scope=radial_vm load_failed error=${e::class.simpleName} msg=${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun buildDaysMap(): Map<LocalDate, RadialDay> {
        val today = clock()
        val from = today.minusDays((windowDays - 1).toLong())
        val fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // --- Sleep stages — groupés par date (du début de la session) ---
        val sessions = runCatching { sleepRecordsInRange(fromMs, toMs) }.getOrDefault(emptyList())
        val stagesByDay: MutableMap<LocalDate, MutableList<StageInterval>> = HashMap()
        sessions.forEach { session ->
            val day = session.start.atZone(zone).toLocalDate()
            session.stages.forEach { stage ->
                // Même traduction que les autres écrans hérités (pont temporaire, Phase 4).
                val type = stageEnum(legacyStageName(stage.type)) ?: return@forEach
                stagesByDay.getOrPut(day) { mutableListOf() }.add(
                    StageInterval(type = type, startMs = stage.start.toEpochMilli(), endMs = stage.end.toEpochMilli())
                )
            }
        }

        // --- Location ---
        val allVisits = runCatching { locationDao.getVisitsInRange(fromMs, toMs) }.getOrDefault(emptyList())
        val allSegments = runCatching { locationDao.getSegmentsInRange(fromMs, toMs) }.getOrDefault(emptyList())

        // --- Labeled places — résolution des visites ancrées (lieux connus) ---
        val labeledPlaces = labeledPlaceDao
            ?.let { runCatching { it.getAll() }.getOrDefault(emptyList()) }
            ?.map { it.toLabeledPlace() }
            .orEmpty()
        val placeResolver = PlaceResolver(labeledPlaces)

        // --- Usage ---
        val allUsage = runCatching {
            usageStatsDao.getInRange(from.toString(), today.toString())
        }.getOrDefault(emptyList())

        // --- Usage sessions (horaires réels) — vide si DAO absent ou table vide ---
        val allSessions = usageSessionDao
            ?.let { runCatching { it.getSessionsInRange(fromMs, toMs) }.getOrDefault(emptyList()) }
            .orEmpty()

        val result = HashMap<LocalDate, RadialDay>()
        for (offset in 0 until windowDays) {
            val date = today.minusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val dateStr = date.toString()

            result[date] = RadialDay(
                date = date,
                sleepStages = stagesByDay[date]?.sortedBy { it.startMs }?.toList().orEmpty(),
                visits = allVisits
                    .filter { it.startMs < dayEnd && it.endMs > dayStart }
                    .map { v ->
                        val match = placeResolver.resolve(v.lat, v.lng)
                        RadialVisit(
                            startMs = v.startMs,
                            endMs = v.endMs,
                            placeName = v.placeName ?: v.address ?: "—",
                            anchored = match != null,
                            placeLabel = match?.label,
                        )
                    },
                activities = allSegments
                    .filter { it.startMs < dayEnd && it.endMs > dayStart }
                    .map { RadialActivity(it.startMs, it.endMs, it.activityType, it.distanceMeters ?: 0) },
                usageRows = allUsage
                    .filter { it.date == dateStr }
                    .map { RadialUsageRow(it.packageName, it.totalTimeForegroundMs, it.lastTimeUsedMs) },
                usageSessions = allSessions
                    .filter { it.startMs < dayEnd && it.endMs > dayStart }
                    .map { UsageSession(it.packageName, it.startMs, it.endMs) },
            )
        }
        return result
    }

    /**
     * Distribution horaire typique d'usage sur la fenêtre — 24 buckets normalisés
     * à somme = 1 (fraction du temps total). Sert de fallback Heat ring quand un
     * jour précis n'a pas de donnée d'usage.
     */
    private fun computeTypicalHourDist(days: Map<LocalDate, RadialDay>): FloatArray? {
        val buckets = FloatArray(24)
        var total = 0L
        days.values.forEach { d ->
            d.usageRows.forEach { row ->
                if (row.lastTimeUsedMs <= 0L) return@forEach
                val hour = java.time.Instant.ofEpochMilli(row.lastTimeUsedMs)
                    .atZone(zone).toLocalTime().hour.coerceIn(0, 23)
                buckets[hour] += row.totalTimeForegroundMs.toFloat()
                total += row.totalTimeForegroundMs
            }
        }
        if (total <= 0L) return null
        for (i in buckets.indices) buckets[i] = buckets[i] / total.toFloat()
        return buckets
    }

    private fun stageEnum(type: String): SleepStage? = when (type) {
        "AWAKE" -> SleepStage.AWAKE
        "REM" -> SleepStage.REM
        "LIGHT" -> SleepStage.LIGHT
        "DEEP" -> SleepStage.DEEP
        else -> null
    }
}
