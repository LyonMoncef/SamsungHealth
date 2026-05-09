---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt
git_blob: 5d6ad70cec67f65f6ae189d1a8c591aa4dd4e1a7
last_synced: '2026-05-09T19:12:26Z'
loc: 140
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import fr.datasaillance.nightfall.data.local.dao.ExerciseDao
import fr.datasaillance.nightfall.data.local.dao.HeartRateDao
import fr.datasaillance.nightfall.data.local.dao.LocationDao
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import fr.datasaillance.nightfall.data.local.security.NightfallKeyManager

@Database(
    entities = [
        SleepSessionEntity::class,
        SleepStageEntity::class,
        HeartRateHourlyEntity::class,
        StepsHourlyEntity::class,
        ExerciseSessionEntity::class,
        LocationVisitEntity::class,    // v2 — Phase A_gps
        ActivitySegmentEntity::class,  // v2 — Phase A_gps
    ],
    version = 2,
    exportSchema = false,
)
abstract class NightfallDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun locationDao(): LocationDao

    /**
     * Migration v1 → v2 : ajoute les tables `location_visits` + `activity_segments`
     * (Phase A_gps). À noter : la branche soeur `feat/usage-stats-collector` fait sa
     * propre v1→v2 avec `usage_daily`. La consolidation finale (intégration des 2
     * branches sur main) bumpera à v3 avec les 3 tables.
     */
    object Migration1to2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // location_visits
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `location_visits` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `start_ms` INTEGER NOT NULL,
                    `end_ms` INTEGER NOT NULL,
                    `lat` REAL NOT NULL,
                    `lng` REAL NOT NULL,
                    `place_id` TEXT,
                    `place_name` TEXT,
                    `address` TEXT,
                    `confidence` TEXT,
                    `source` TEXT NOT NULL DEFAULT 'takeout',
                    `imported_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_location_visits_start_ms_end_ms_lat_lng` ON `location_visits` (`start_ms`, `end_ms`, `lat`, `lng`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_visits_start_ms` ON `location_visits` (`start_ms`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_visits_place_id` ON `location_visits` (`place_id`)")

            // activity_segments
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_segments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `start_ms` INTEGER NOT NULL,
                    `end_ms` INTEGER NOT NULL,
                    `start_lat` REAL NOT NULL,
                    `start_lng` REAL NOT NULL,
                    `end_lat` REAL NOT NULL,
                    `end_lng` REAL NOT NULL,
                    `activity_type` TEXT NOT NULL,
                    `distance_m` INTEGER,
                    `confidence` TEXT,
                    `source` TEXT NOT NULL DEFAULT 'takeout',
                    `imported_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_activity_segments_start_ms_end_ms_activity_type` ON `activity_segments` (`start_ms`, `end_ms`, `activity_type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_segments_start_ms` ON `activity_segments` (`start_ms`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_segments_activity_type` ON `activity_segments` (`activity_type`)")
        }
    }

    companion object {
        private const val DB_NAME = "nightfall.db"

        @Volatile
        private var instance: NightfallDatabase? = null

        /**
         * DB de production avec SQLCipher (clé Android Keystore).
         * Singleton pour partager une seule connexion entre tous les ViewModels.
         */
        fun get(context: Context): NightfallDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): NightfallDatabase {
            val keyManager = NightfallKeyManager(context.applicationContext)
            val factory: SupportSQLiteOpenHelper.Factory =
                net.sqlcipher.database.SupportFactory(keyManager.getOrCreatePassphrase())
            return Room.databaseBuilder(
                context.applicationContext,
                NightfallDatabase::class.java,
                DB_NAME,
            )
                .openHelperFactory(factory)
                .addMigrations(Migration1to2)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Reset l'instance (utilisé en test pour isoler les fixtures).
         * Ne pas appeler en production.
         */
        internal fun resetForTest() {
            instance?.close()
            instance = null
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallDatabase` (class) — lines 24-140
- `sleepDao` (function) — lines 39-39
- `heartRateDao` (function) — lines 40-40
- `stepsDao` (function) — lines 41-41
- `exerciseDao` (function) — lines 42-42
- `locationDao` (function) — lines 43-43
- `migrate` (function) — lines 52-97
- `get` (function) — lines 110-114
- `build` (function) — lines 116-129
- `resetForTest` (function) — lines 135-138
