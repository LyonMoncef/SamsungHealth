---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt
git_blob: 68007ca5b2d00e3ec7b4a490349451b955415f37
last_synced: '2026-05-09T18:49:36Z'
loc: 109
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
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
import fr.datasaillance.nightfall.data.local.security.NightfallKeyManager

@Database(
    entities = [
        SleepSessionEntity::class,
        SleepStageEntity::class,
        HeartRateHourlyEntity::class,
        StepsHourlyEntity::class,
        ExerciseSessionEntity::class,
        UsageDailyEntity::class, // v2 — Phase A_us usage stats
    ],
    version = 2,
    exportSchema = false,
)
abstract class NightfallDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun usageStatsDao(): UsageStatsDao

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
- `NightfallDatabase` (class) — lines 23-109
- `sleepDao` (function) — lines 37-37
- `heartRateDao` (function) — lines 38-38
- `stepsDao` (function) — lines 39-39
- `exerciseDao` (function) — lines 40-40
- `usageStatsDao` (function) — lines 41-41
- `migrate` (function) — lines 45-64
- `get` (function) — lines 77-81
- `build` (function) — lines 83-98
- `resetForTest` (function) — lines 104-107
