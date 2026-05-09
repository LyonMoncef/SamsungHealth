package fr.datasaillance.nightfall.data.sleep

interface SleepRepository {
    suspend fun getSessions(): Result<List<SleepSessionResponse>>
}
