package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabeledPlaceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: LabeledPlaceEntity): Long

    @Update
    suspend fun update(place: LabeledPlaceEntity)

    @Delete
    suspend fun delete(place: LabeledPlaceEntity)

    /** Flux réactif pour la liste de configuration — l'écran se met à jour en temps réel. */
    @Query("SELECT * FROM labeled_place ORDER BY category ASC, label ASC")
    fun getAllFlow(): Flow<List<LabeledPlaceEntity>>

    /** Snapshot synchrone utilisé par PlaceResolver (appelé hors UI thread). */
    @Query("SELECT * FROM labeled_place")
    suspend fun getAll(): List<LabeledPlaceEntity>

    @Query("SELECT COUNT(*) FROM labeled_place")
    suspend fun count(): Int
}
