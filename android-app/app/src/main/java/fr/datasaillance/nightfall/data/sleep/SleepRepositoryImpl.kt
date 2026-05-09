package fr.datasaillance.nightfall.data.sleep

import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import timber.log.Timber
import java.io.IOException

class SleepRepositoryImpl(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore
) : SleepRepository {

    override suspend fun getSessions(): Result<List<SleepSessionResponse>> {
        val token = tokenDataStore.getToken()
            ?: return Result.failure(IOException("No auth token"))
        return try {
            val sessions = api.getSleepSessions("Bearer $token", includeStages = true)
            Timber.i("sleep_sessions_fetched count=${sessions.size}")
            Result.success(sessions)
        } catch (e: retrofit2.HttpException) {
            Timber.w("sleep_fetch_http_error code=${e.code()}")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w("sleep_fetch_network_error")
            Result.failure(e)
        }
    }
}
