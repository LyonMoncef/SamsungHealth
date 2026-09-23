package fr.datasaillance.nightfall.data.local.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Gère la permission `PACKAGE_USAGE_STATS` qui est de type `appop` :
 * elle ne se demande pas via `requestPermissions()` standard mais s'active
 * manuellement par l'utilisateur dans Settings → Apps → Usage Access.
 */
class UsageStatsPermissionHelper(private val context: Context) {

    fun hasPermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION") // unsafeCheckOpNoThrow API stable, deprecation cosmétique
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Intent à lancer pour amener l'utilisateur sur l'écran Settings de gestion
     * des accès Usage. Pas de `data=` explicite (juste l'action) — Settings
     * affiche la liste de toutes les apps demandant la permission.
     *
     * Le caller doit ajouter `FLAG_ACTIVITY_NEW_TASK` si lancé depuis un Context
     * non-Activity.
     */
    fun intentToGrantPermission(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
