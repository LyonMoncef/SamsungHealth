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
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
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
    ],
    version = 3,
    exportSchema = false,
)
abstract class NightfallDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun locationDao(): LocationDao

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
     * (Phase A_gps). Convergence post-merge des branches feat/usage-stats-collector
     * et feat/gps-collector — la v2 était usage_daily, on continue avec les tables
     * location en v3.
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
                .addMigrations(Migration1to2, Migration2to3)
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
