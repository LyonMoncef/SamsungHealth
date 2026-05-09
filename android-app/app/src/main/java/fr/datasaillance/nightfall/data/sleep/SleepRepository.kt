package fr.datasaillance.nightfall.data.sleep

import java.time.LocalDate

interface SleepRepository {
    suspend fun getSessions(
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<SleepSessionResponse>>
}
