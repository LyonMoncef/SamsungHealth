---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt
git_blob: af7e0b93becb3cfbfd5eefe46dcf1b11579ce6dc
last_synced: '2026-05-27T05:17:18Z'
loc: 256
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
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import fr.datasaillance.nightfall.data.local.dao.ExerciseDao
import fr.datasaillance.nightfall.data.local.dao.HeartRateDao
import fr.datasaillance.nightfall.data.local.dao.LabeledPlaceDao
import fr.datasaillance.nightfall.data.local.dao.LocationDao
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.dao.UsageSessionDao
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationPathEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
import fr.datasaillance.nightfall.data.local.entity.usage.UsageSessionEntity
import fr.datasaillance.nightfall.data.local.security.NightfallKeyManager

@Database(
    entities = [
        SleepSessionEntity::class,
        SleepStageEntity::class,
        HeartRateHourlyEntity::class,
        StepsHourlyEntity::class,
        ExerciseSessionEntity::class,
        UsageDailyEntity::class,       // v2 — Phase A_us usage stats
        LocationVisitEntity::class,    // v3 — Phase A_gps location visits
        ActivitySegmentEntity::class,  // v3 — Phase A_gps activity segments
        LocationPathEntity::class,     // v4 — timelinePath waypoints GPS (trajets réalistes)
        UsageSessionEntity::class,     // v5 — Phase B_us sessions foreground intra-journée
        LabeledPlaceEntity::class,     // v6 — Phase B_lp lieux labellisés configurables
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NightfallDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun locationDao(): LocationDao
    abstract fun labeledPlaceDao(): LabeledPlaceDao

    /** Migration v1 → v2 : ajoute la table `usage_daily` (Phase A_us). */
    object Migration1to2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `usage_daily` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` TEXT NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `total_time_foreground_ms` INTEGER NOT NULL,
                    `total_time_visible_ms` INTEGER NOT NULL DEFAULT 0,
                    `total_time_fgs_ms` INTEGER NOT NULL DEFAULT 0,
                    `last_time_used_ms` INTEGER NOT NULL DEFAULT 0,
                    `app_launch_count` INTEGER NOT NULL DEFAULT 0,
                    `collected_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_daily_date_package_name` ON `usage_daily` (`date`, `package_name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_daily_date` ON `usage_daily` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_daily_package_name` ON `usage_daily` (`package_name`)")
        }
    }

    /**
     * Migration v2 → v3 : ajoute les tables `location_visits` + `activity_segments`
     * (Phase A_gps). Convergence post-merge des 2 branches long-lived — la v2
     * était usage_daily, on continue avec les tables location en v3.
     */
    object Migration2to3 : Migration(2, 3) {
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

    /**
     * Migration v3 → v4 : ajoute la table `location_paths` pour stocker les waypoints
     * `timelinePath` du nouveau format Google Takeout (trajets GPS détaillés).
     */
    object Migration3to4 : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `location_paths` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `start_ms` INTEGER NOT NULL,
                    `end_ms` INTEGER NOT NULL,
                    `points_json` TEXT NOT NULL,
                    `point_count` INTEGER NOT NULL,
                    `source` TEXT NOT NULL DEFAULT 'takeout',
                    `imported_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_location_paths_start_ms_end_ms` ON `location_paths` (`start_ms`, `end_ms`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_paths_start_ms` ON `location_paths` (`start_ms`)")
        }
    }

    /**
     * Migration v4 → v5 : ajoute la table `usage_session` (Phase B_us — sessions
     * foreground intra-journée). Additive — aucun ALTER destructif sur l'existant.
     */
    object Migration4to5 : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `usage_session` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `start_ms` INTEGER NOT NULL,
                    `end_ms` INTEGER NOT NULL,
                    `duration_ms` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `source` TEXT NOT NULL DEFAULT 'events',
                    `collected_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_session_package_name_start_ms` ON `usage_session` (`package_name`, `start_ms`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_date` ON `usage_session` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_start_ms` ON `usage_session` (`start_ms`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_package_name` ON `usage_session` (`package_name`)")
        }
    }

    /**
     * Migration v5 → v6 : ajoute la table `labeled_place` (Phase B_lp — lieux
     * labellisés configurables). Additive — aucun ALTER destructif sur l'existant.
     */
    object Migration5to6 : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `labeled_place` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `label` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `lat` REAL NOT NULL,
                    `lng` REAL NOT NULL,
                    `radius_meters` INTEGER NOT NULL DEFAULT 150,
                    `created_at_ms` INTEGER NOT NULL,
                    `updated_at_ms` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_labeled_place_category` ON `labeled_place` (`category`)")
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
                .addMigrations(Migration1to2, Migration2to3, Migration3to4, Migration4to5, Migration5to6)
                // Fallback safety : si une migration future foire ou si l'utilisateur
                // a une DB v0 inattendue, on rebuild from scratch plutôt que crasher.
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
- `NightfallDatabase` (class) — lines 32-256
- `sleepDao` (function) — lines 52-52
- `heartRateDao` (function) — lines 53-53
- `stepsDao` (function) — lines 54-54
- `exerciseDao` (function) — lines 55-55
- `usageStatsDao` (function) — lines 56-56
- `usageSessionDao` (function) — lines 57-57
- `locationDao` (function) — lines 58-58
- `labeledPlaceDao` (function) — lines 59-59
- `migrate` (function) — lines 63-82
- `migrate` (function) — lines 91-136
- `migrate` (function) — lines 144-160
- `migrate` (function) — lines 168-187
- `migrate` (function) — lines 195-211
- `get` (function) — lines 224-228
- `build` (function) — lines 230-245
- `resetForTest` (function) — lines 251-254
