package fr.datasaillance.nightfall.data.local.usage

import android.content.pm.PackageManager

/**
 * Résout `packageName → label utilisateur` via `PackageManager.getApplicationLabel`.
 * Cache mémoire — un packageName peut être résolu N fois pour différents jours,
 * inutile de re-query Android à chaque fois.
 *
 * Si le package est désinstallé ou inconnu (rare — apps removed après collecte),
 * fallback sur le packageName brut.
 */
open class PackageInfoResolver(private val pm: PackageManager) {

    private val cache = HashMap<String, String>()

    open fun labelFor(packageName: String): String {
        cache[packageName]?.let { return it }
        val label = runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: packageName
        cache[packageName] = label
        return label
    }
}
