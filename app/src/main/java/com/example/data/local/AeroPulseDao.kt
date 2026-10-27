package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AeroPulseDao {

    @Query("SELECT * FROM field_stations ORDER BY isPrimary DESC, isFavorite DESC, name ASC")
    fun observeAllStations(): Flow<List<FieldStationEntity>>

    @Query("SELECT * FROM field_stations")
    suspend fun getAllStationsOnce(): List<FieldStationEntity>

    @Query("SELECT COUNT(*) FROM field_stations")
    suspend fun getStationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: FieldStationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<FieldStationEntity>)

    @Update
    suspend fun updateStation(station: FieldStationEntity)

    @Query("DELETE FROM field_stations WHERE id = :stationId")
    suspend fun deleteStationById(stationId: Long)

    @Query("UPDATE field_stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun setStationFavorite(stationId: Long, isFavorite: Boolean)

    @Query("UPDATE field_stations SET fieldNotes = :notes WHERE id = :stationId")
    suspend fun updateStationNotes(stationId: Long, notes: String)

    @Query("UPDATE field_stations SET isPrimary = 0")
    suspend fun clearAllPrimaryFlags()

    @Query("UPDATE field_stations SET isPrimary = 1 WHERE id = :stationId")
    suspend fun markStationAsPrimary(stationId: Long)

    @Transaction
    suspend fun selectPrimaryStation(stationId: Long) {
        clearAllPrimaryFlags()
        markStationAsPrimary(stationId)
    }

    // Activity Profiles
    @Query("SELECT * FROM activity_profiles ORDER BY isBuiltIn DESC, name ASC")
    fun observeActivityProfiles(): Flow<List<ActivityProfileEntity>>

    @Query("SELECT COUNT(*) FROM activity_profiles")
    suspend fun getActivityProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityProfile(profile: ActivityProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityProfiles(profiles: List<ActivityProfileEntity>)

    @Query("DELETE FROM activity_profiles WHERE id = :profileId")
    suspend fun deleteActivityProfileById(profileId: Long)

    // Barometer Logs
    @Query("SELECT * FROM barometer_logs ORDER BY timestampMillis DESC LIMIT 50")
    fun observeBarometerLogs(): Flow<List<BarometerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarometerLog(entry: BarometerLogEntity)

    @Query("DELETE FROM barometer_logs")
    suspend fun clearBarometerLogs()

    @Query("DELETE FROM field_stations")
    suspend fun deleteAllStations()

    @Query("DELETE FROM activity_profiles")
    suspend fun deleteAllProfiles()
}
