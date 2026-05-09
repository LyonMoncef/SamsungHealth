---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/NightfallDatabase.kt
git_blob: d8cedd6c16b58107a3e3c29e7448e2729928e583
last_synced: '2026-05-09T15:08:38Z'
loc: 76
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
import androidx.sqlite.db.SupportSQLiteOpenHelper
import fr.datasaillance.nightfall.data.local.dao.ExerciseDao
import fr.datasaillance.nightfall.data.local.dao.HeartRateDao
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import fr.datasaillance.nightfall.data.local.security.NightfallKeyManager

@Database(
    entities = [
        SleepSessionEntity::class,
        SleepStageEntity::class,
        HeartRateHourlyEntity::class,
        StepsHourlyEntity::class,
        ExerciseSessionEntity::class,
    ],
    version = 1,
    exportSchema = false, // Phase A v1 — pas de migration legacy à archiver. À activer + plugin Room quand on attaquera v2.
)
abstract class NightfallDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun exerciseDao(): ExerciseDao

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
                .fallbackToDestructiveMigration() // v1 — pas de migration legacy à gérer
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
- `NightfallDatabase` (class) — lines 19-76
- `sleepDao` (function) — lines 32-32
- `heartRateDao` (function) — lines 33-33
- `stepsDao` (function) — lines 34-34
- `exerciseDao` (function) — lines 35-35
- `get` (function) — lines 47-51
- `build` (function) — lines 53-65
- `resetForTest` (function) — lines 71-74
