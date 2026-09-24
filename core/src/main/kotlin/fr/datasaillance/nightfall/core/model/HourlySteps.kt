package fr.datasaillance.nightfall.core.model

import java.time.Instant
import java.time.ZoneOffset

/**
 * Total de pas d'une tranche d'une heure, tel que Health Connect le calcule (contrat v2, spec DT-2).
 *
 * Health Connect additionne les enregistrements de la tranche en dédoublonnant les sources
 * (téléphone, montre…) selon les priorités choisies par l'utilisateur. On n'utilise plus les
 * enregistrements bruts : un seul enregistrement invalide (début = fin) faisait échouer toute la lecture.
 */
data class HourlySteps(
    val start: Instant,
    val end: Instant,
    val offset: ZoneOffset?,
    val count: Long,
    val sources: List<String>,
)
