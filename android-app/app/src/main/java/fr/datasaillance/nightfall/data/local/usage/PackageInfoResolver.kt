package fr.datasaillance.nightfall.data.local.usage

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Résout `packageName → label utilisateur` et `packageName → icône` via
 * `PackageManager`. Cache mémoire — un packageName peut être résolu N fois
 * pour différents jours, inutile de re-query Android à chaque fois.
 *
 * Si le package est désinstallé ou inconnu (rare — apps removed après collecte),
 * fallback sur le packageName brut côté label, `null` côté icône.
 */
open class PackageInfoResolver(private val pm: PackageManager) {

    private val labelCache = HashMap<String, String>()
    private val iconCache = HashMap<String, Bitmap?>()

    open fun labelFor(packageName: String): String {
        labelCache[packageName]?.let { return it }
        val label = runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: packageName
        labelCache[packageName] = label
        return label
    }

    /**
     * Récupère l'icône de l'app sous forme de Bitmap, prêt à être passé à
     * `Image(painter = BitmapPainter(bitmap.asImageBitmap()))` en Compose.
     * Cache via `iconCache`. Retourne `null` si l'app n'est plus installée.
     */
    open fun iconFor(packageName: String, sizePx: Int = 96): Bitmap? {
        // Cache par packageName seulement — sizePx est implicite, on assume
        // l'appelant utilise toujours la même taille (sinon créer une cache key composée).
        if (iconCache.containsKey(packageName)) return iconCache[packageName]
        val bitmap = runCatching {
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable, sizePx)
        }.getOrNull()
        iconCache[packageName] = bitmap
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
