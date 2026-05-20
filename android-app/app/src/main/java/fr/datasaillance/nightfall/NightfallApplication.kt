package fr.datasaillance.nightfall

import android.app.Application
import fr.datasaillance.nightfall.data.local.usage.UsageStatsScheduler
import timber.log.Timber

class NightfallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Phase B_us : worker quotidien (idempotent via uniqueWorkName).
        // Le worker no-op silencieusement si la permission UsageStats est absente.
        UsageStatsScheduler.schedulePeriodic(this)
    }
}
