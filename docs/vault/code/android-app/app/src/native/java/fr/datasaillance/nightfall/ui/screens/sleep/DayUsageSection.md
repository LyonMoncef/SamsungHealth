---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayUsageSection.kt
git_blob: ada54e1f2a1c0973e4f9afc2a9f67d32f30d3159
last_synced: '2026-05-23T19:13:13Z'
loc: 134
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayUsageSection.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayUsageSection.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayUsageSection.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
import fr.datasaillance.nightfall.data.local.usage.PackageInfoResolver
import fr.datasaillance.nightfall.viewmodel.sleep.DayUsage

/**
 * Section "Bien-être numérique" affichée sous la map dans la page Détails de la
 * journée. Top 5 apps par temps écran + total cumulé.
 */
@Composable
fun DayUsageSection(
    dayUsage: DayUsage?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resolver = remember(context) { PackageInfoResolver(context.packageManager) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Bien-être numérique",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            dayUsage == null -> UsagePlaceholder("Chargement…")
            dayUsage.rows.isEmpty() -> UsagePlaceholder(
                "Pas de collecte d'usage pour ce jour. Active 'Usage Access' dans Bien-être.",
            )
            else -> {
                Text(
                    text = "Total écran : ${formatScreenTime(dayUsage.totalForegroundMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                val top = dayUsage.rows
                    .sortedByDescending { it.totalTimeForegroundMs }
                    .take(5)
                val maxMs = top.firstOrNull()?.totalTimeForegroundMs ?: 1L
                top.forEach { row ->
                    AppRow(row = row, label = resolver.labelFor(row.packageName), maxMs = maxMs)
                }
            }
        }
    }
}

@Composable
private fun UsagePlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppRow(row: UsageDailyEntity, label: String, maxMs: Long) {
    val ratio = if (maxMs <= 0) 0f else (row.totalTimeForegroundMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatScreenTime(row.totalTimeForegroundMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.extraSmall,
                ),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
    }
}

private fun formatScreenTime(ms: Long): String {
    if (ms < 60_000L) return "${ms / 1000}s"
    val minutes = ms / 60_000L
    if (minutes < 60L) return "${minutes}min"
    val hours = minutes / 60L
    val rem = minutes % 60L
    return if (rem == 0L) "${hours}h" else "${hours}h${rem.toString().padStart(2, '0')}"
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DayUsageSection` (function) — lines 30-68
- `UsagePlaceholder` (function) — lines 70-82
- `AppRow` (function) — lines 84-125
- `formatScreenTime` (function) — lines 127-134
