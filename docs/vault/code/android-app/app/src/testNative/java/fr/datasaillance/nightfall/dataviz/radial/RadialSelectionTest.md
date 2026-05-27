---
type: code-source
language: kotlin
file_path: android-app/app/src/testNative/java/fr/datasaillance/nightfall/dataviz/radial/RadialSelectionTest.kt
git_blob: 625ae9290bbe87ba8fc02550c383ab673a12b26d
last_synced: '2026-05-27T06:16:08Z'
loc: 221
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/testNative/java/fr/datasaillance/nightfall/dataviz/radial/RadialSelectionTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/testNative/java/fr/datasaillance/nightfall/dataviz/radial/RadialSelectionTest.kt`](../../../android-app/app/src/testNative/java/fr/datasaillance/nightfall/dataviz/radial/RadialSelectionTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.dataviz.radial

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tests purs (JVM, sans Compose UI ni DB) du hit-test radial à deux étages
 * (DT-2) et de la métrique centre (DT-4). Couvre TA-1 à TA-5.
 *
 * Géométrie de test : centre (100,100). Anneaux concentriques :
 *   TIMELINE 20..40, USAGE 50..70, SLEEP 80..120.
 */
class RadialSelectionTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val cx = 100f
    private val cy = 100f
    private val geo = RadialGeometry(
        cx = cx, cy = cy,
        rSleepOuter = 120f, rSleepInner = 80f,
        rUsageOuter = 70f, rUsageInner = 50f,
        rTimelineOuter = 40f, rTimelineInner = 20f,
    )

    /** Point à `radius` px du centre, à l'heure `hour` (minuit = haut, sens horaire). */
    private fun pointAt(hour: Float, radius: Float): Offset {
        val a = ((hour / 24f) * 2f * Math.PI - Math.PI / 2).toFloat()
        return Offset(cx + radius * cos(a), cy + radius * sin(a))
    }

    /** Convertit une heure du jour en epoch ms (jour de référence, UTC). */
    private fun ms(hour: Double): Long {
        val day = LocalDate.parse("2026-05-20")
        return day.atStartOfDay(zone).toInstant().toEpochMilli() + (hour * 3_600_000L).toLong()
    }

    private val dayWithSleep = RadialDay(
        date = LocalDate.parse("2026-05-20"),
        sleepStages = listOf(
            StageInterval(SleepStage.LIGHT, ms(1.0), ms(2.0)),
            StageInterval(SleepStage.DEEP, ms(2.0), ms(3.0)),
            StageInterval(SleepStage.REM, ms(3.0), ms(4.0)),
        ),
    )

    // ── TA-1 / TA-3 : anneau touché par rayon ──────────────────────
    @Test
    fun layer_at_radius_maps_each_ring() {
        assertEquals(RadialLayer.SLEEP, layerAtRadius(pointAt(2f, 100f), geo))
        assertEquals(RadialLayer.USAGE, layerAtRadius(pointAt(2f, 60f), geo))
        assertEquals(RadialLayer.TIMELINE, layerAtRadius(pointAt(2f, 30f), geo))
    }

    @Test
    fun layer_at_radius_null_in_hub_and_outside() {
        assertNull("hub central → null", layerAtRadius(pointAt(2f, 5f), geo))
        assertNull("hors cadran → null", layerAtRadius(pointAt(2f, 200f), geo))
    }

    // ── TA-1 : tap anneau → Niveau 1 (couche entière, segment null) ─
    @Test
    fun tap_outer_ring_from_empty_selects_sleep_level1() {
        val sel = hitTestRadial(pointAt(2f, 100f), geo, current = null, day = dayWithSleep, zone = zone)
        assertEquals(RadialLayer.SLEEP, sel?.layer)
        assertNull(sel?.segmentStartMs)
    }

    @Test
    fun tap_middle_ring_selects_usage_level1() {
        val sel = hitTestRadial(pointAt(2f, 60f), geo, current = null, day = dayWithSleep, zone = zone)
        assertEquals(RadialLayer.USAGE, sel?.layer)
        assertNull(sel?.segmentStartMs)
    }

    @Test
    fun tap_inner_ring_selects_timeline_level1() {
        val sel = hitTestRadial(pointAt(2f, 30f), geo, current = null, day = dayWithSleep, zone = zone)
        assertEquals(RadialLayer.TIMELINE, sel?.layer)
        assertNull(sel?.segmentStartMs)
    }

    // ── TA-3 : tap hub / hors zone → désélection ────────────────────
    @Test
    fun tap_hub_returns_null() {
        val sel = hitTestRadial(pointAt(2f, 5f), geo,
            current = RadialSelection(RadialLayer.SLEEP), day = dayWithSleep, zone = zone)
        assertNull(sel)
    }

    @Test
    fun tap_outside_returns_null() {
        val sel = hitTestRadial(pointAt(2f, 200f), geo,
            current = RadialSelection(RadialLayer.SLEEP), day = dayWithSleep, zone = zone)
        assertNull(sel)
    }

    // ── TA-2 : re-tap segment dans couche sélectionnée → Niveau 2 ───
    @Test
    fun retap_same_layer_on_segment_returns_level2() {
        // sommeil déjà sélectionné, on retape sur le segment DEEP (heure 2.5).
        val current = RadialSelection(RadialLayer.SLEEP)
        val sel = hitTestRadial(pointAt(2.5f, 100f), geo, current = current, day = dayWithSleep, zone = zone)
        assertEquals(RadialLayer.SLEEP, sel?.layer)
        assertEquals(ms(2.0), sel?.segmentStartMs)
        assertEquals(ms(3.0), sel?.segmentEndMs)
    }

    // ── TA-4 : re-tap couche déjà en Niveau 1 sans segment → null ──
    @Test
    fun retap_same_layer_no_segment_returns_null() {
        // usage déjà sélectionné, aucune session → re-tap = désélection.
        val current = RadialSelection(RadialLayer.USAGE)
        val sel = hitTestRadial(pointAt(2f, 60f), geo, current = current, day = dayWithSleep, zone = zone)
        assertNull(sel)
    }

    @Test
    fun retap_sleep_layer_at_hour_with_no_stage_returns_null() {
        // sommeil sélectionné mais on tape à 10h (hors de tout stage) → désélection.
        val current = RadialSelection(RadialLayer.SLEEP)
        val sel = hitTestRadial(pointAt(10f, 100f), geo, current = current, day = dayWithSleep, zone = zone)
        assertNull(sel)
    }

    @Test
    fun tap_different_layer_switches_to_level1() {
        // sommeil sélectionné, on tape l'anneau timeline → bascule Niveau 1 TIMELINE.
        val current = RadialSelection(RadialLayer.SLEEP)
        val sel = hitTestRadial(pointAt(2f, 30f), geo, current = current, day = dayWithSleep, zone = zone)
        assertEquals(RadialLayer.TIMELINE, sel?.layer)
        assertNull(sel?.segmentStartMs)
    }

    // ── TA-5 : métrique centre, pas de somme visits + activities ───
    @Test
    fun center_metric_no_selection_uses_sleep_duration_not_count() {
        val day = RadialDay(
            date = LocalDate.parse("2026-05-20"),
            sleepStages = listOf(StageInterval(SleepStage.LIGHT, ms(0.0), ms(7.0))),
            visits = listOf(
                RadialVisit(ms(8.0), ms(9.0), "a"),
                RadialVisit(ms(9.0), ms(10.0), "b"),
                RadialVisit(ms(10.0), ms(11.0), "c"),
            ),
            activities = listOf(
                RadialActivity(ms(12.0), ms(13.0), "WALKING", 1000),
                RadialActivity(ms(13.0), ms(14.0), "RUNNING", 2000),
            ),
        )
        val m = centerMetric(day, selection = null)
        assertEquals("sommeil", m.label)
        assertEquals("7h00", m.value)
        // Pas de "5 déplacements" ni de somme visits + activities.
        assert(!m.value.contains("5"))
        assert(!m.label.contains("déplacement"))
    }

    @Test
    fun center_metric_timeline_counts_activities_only() {
        val day = RadialDay(
            date = LocalDate.parse("2026-05-20"),
            visits = listOf(
                RadialVisit(ms(8.0), ms(9.0), "a"),
                RadialVisit(ms(9.0), ms(10.0), "b"),
                RadialVisit(ms(10.0), ms(11.0), "c"),
            ),
            activities = listOf(
                RadialActivity(ms(12.0), ms(13.0), "WALKING", 1000),
                RadialActivity(ms(13.0), ms(14.0), "RUNNING", 2000),
            ),
        )
        val m = centerMetric(day, RadialSelection(RadialLayer.TIMELINE))
        // 2 activités uniquement, pas 5.
        assertEquals("2", m.value)
        assert(m.label.startsWith("sorties"))
        // 1+2 km — tolère le séparateur décimal selon la locale JVM.
        assert(Regex("3[.,]0 km").containsMatchIn(m.label)) { "label=${m.label}" }
    }

    @Test
    fun center_metric_usage_shows_screen_time() {
        val day = RadialDay(
            date = LocalDate.parse("2026-05-20"),
            usageRows = listOf(RadialUsageRow("app", 90 * 60_000L, ms(20.0))),
        )
        val m = centerMetric(day, RadialSelection(RadialLayer.USAGE))
        assertEquals("téléphone", m.label)
        assertEquals("1h30", m.value)
    }

    @Test
    fun center_metric_sleep_shows_sessions_count() {
        val day = RadialDay(
            date = LocalDate.parse("2026-05-20"),
            sleepStages = listOf(
                StageInterval(SleepStage.LIGHT, ms(0.0), ms(3.0)),
                // trou > 30 min → 2e session
                StageInterval(SleepStage.LIGHT, ms(4.0), ms(7.0)),
            ),
        )
        val m = centerMetric(day, RadialSelection(RadialLayer.SLEEP))
        assert(m.label.contains("2 session")) { "label=${m.label}" }
        assertEquals("6h00", m.value)
    }

    // ── DT-6 : couleur timeline binaire ancré / non labellisé ──────
    @Test
    fun timeline_visit_color_amber_when_anchored() {
        val muted = androidx.compose.ui.graphics.Color(0xFF7A9AAA)
        val anchored = RadialVisit(ms(8.0), ms(9.0), "Maison", anchored = true)
        val unlabeled = RadialVisit(ms(9.0), ms(10.0), "x", anchored = false)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD37C04), timelineVisitColor(anchored, muted))
        assertEquals(muted, timelineVisitColor(unlabeled, muted))
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RadialSelectionTest` (class) — lines 19-221
- `pointAt` (function) — lines 32-35
- `ms` (function) — lines 38-41
- `layer_at_radius_maps_each_ring` (function) — lines 53-58
- `layer_at_radius_null_in_hub_and_outside` (function) — lines 60-64
- `tap_outer_ring_from_empty_selects_sleep_level1` (function) — lines 67-72
- `tap_middle_ring_selects_usage_level1` (function) — lines 74-79
- `tap_inner_ring_selects_timeline_level1` (function) — lines 81-86
- `tap_hub_returns_null` (function) — lines 89-94
- `tap_outside_returns_null` (function) — lines 96-101
- `retap_same_layer_on_segment_returns_level2` (function) — lines 104-112
- `retap_same_layer_no_segment_returns_null` (function) — lines 115-121
- `retap_sleep_layer_at_hour_with_no_stage_returns_null` (function) — lines 123-129
- `tap_different_layer_switches_to_level1` (function) — lines 131-138
- `center_metric_no_selection_uses_sleep_duration_not_count` (function) — lines 141-162
- `center_metric_timeline_counts_activities_only` (function) — lines 164-184
- `center_metric_usage_shows_screen_time` (function) — lines 186-195
- `center_metric_sleep_shows_sessions_count` (function) — lines 197-210
- `timeline_visit_color_amber_when_anchored` (function) — lines 213-220
