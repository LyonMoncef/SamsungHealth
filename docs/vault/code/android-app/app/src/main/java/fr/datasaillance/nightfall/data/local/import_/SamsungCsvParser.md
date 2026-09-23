---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/SamsungCsvParser.kt
git_blob: 80721f33a9cf73b4f2dc32a8a6338e320607b148
last_synced: '2026-05-09T15:30:15Z'
loc: 109
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/SamsungCsvParser.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/SamsungCsvParser.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/SamsungCsvParser.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.import_

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parser des CSV Samsung Health. Port Kotlin de `server/services/csv_import.py::parse_samsung_csv`.
 *
 * Particularités du format Samsung :
 * - BOM UTF-8 (U+FEFF) en début de fichier — la décode UTF-8 standard ne le mange pas, on doit le strip.
 * - Lignes commençant par `#` = commentaires/metadata → ignorées.
 * - Première ligne après les commentaires = ligne metadata `namespace,user_id,version` (le user_id est un entier).
 * - 2e ligne (ou 1re si pas de metadata) = header CSV.
 *
 * Détecte la ligne metadata par la présence d'un entier en 2e champ.
 */
object SamsungCsvParser {

    private const val UTF8_BOM = '﻿'

    /**
     * Parse le contenu binaire d'un CSV Samsung. Retourne une liste de `Map<String, String>`
     * (équivalent du `list[dict]` Python) — clés = noms de colonnes du header.
     *
     * @throws IllegalArgumentException si le contenu est mal formé (ex: lignes incohérentes)
     */
    fun parse(rawBytes: ByteArray): List<Map<String, String>> {
        val text = rawBytes.toString(Charsets.UTF_8).removePrefix(UTF8_BOM.toString())
        val lines = text.lineSequence()
            .filter { !it.startsWith("#") }
            .toList()
        if (lines.isEmpty()) return emptyList()

        // Détecte la ligne metadata Samsung (`com.samsung.<x>,<userid_int>,<version>`)
        val firstParts = lines[0].split(",", limit = 3)
        val withoutMetadata = if (firstParts.size >= 2 && firstParts[1].trim().toIntOrNull() != null) {
            lines.drop(1)
        } else {
            lines
        }
        if (withoutMetadata.isEmpty()) return emptyList()

        val header = parseCsvRow(withoutMetadata[0])
        val rows = mutableListOf<Map<String, String>>()
        for (i in 1 until withoutMetadata.size) {
            val raw = withoutMetadata[i]
            if (raw.isBlank()) continue
            val cells = parseCsvRow(raw)
            // Tolérant aux lignes avec extra cellule(s) finale(s) — on ignore les surplus.
            val map = LinkedHashMap<String, String>(header.size)
            for ((idx, name) in header.withIndex()) {
                map[name] = if (idx < cells.size) cells[idx] else ""
            }
            rows.add(map)
        }
        return rows
    }

    /**
     * Parse une ligne CSV avec gestion des champs entre guillemets (RFC 4180 minimal).
     * Suffisant pour Samsung Health qui n'utilise pas de quoting complexe.
     */
    private fun parseCsvRow(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i += 2; continue
                }
                c == '"' -> { inQuotes = !inQuotes; i++; continue }
                c == ',' && !inQuotes -> { out.add(sb.toString()); sb.clear(); i++; continue }
                else -> { sb.append(c); i++ }
            }
        }
        out.add(sb.toString())
        return out
    }

    /**
     * Parse un timestamp Samsung Health en epoch millis UTC.
     * Format : `yyyy-MM-dd HH:mm:ss[.SSS]`. Retourne null si invalide.
     *
     * Note : Samsung exporte en heure locale "naïve" sans tz, mais sépare le `time_offset`
     * dans une colonne dédiée. On considère ici la valeur comme UTC pour cohérence avec
     * l'implémentation serveur (cf. `_parse_ts` en Python).
     */
    fun parseTimestampToMs(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        return try {
            val dt = if ("." in v) {
                LocalDateTime.parse(v, FMT_WITH_MS)
            } else {
                LocalDateTime.parse(v, FMT_NO_MS)
            }
            dt.toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private val FMT_WITH_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val FMT_NO_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `parse` (function) — lines 28-58
- `parseCsvRow` (function) — lines 64-82
- `parseTimestampToMs` (function) — lines 92-105
