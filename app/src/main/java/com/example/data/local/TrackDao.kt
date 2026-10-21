package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM cached_tracks ORDER BY cachedAt DESC")
    fun getAllCachedTracks(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getCachedTrackById(trackId: String): CachedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTrack(track: CachedTrackEntity)

    @Query("DELETE FROM cached_tracks WHERE id = :trackId")
    suspend fun deleteCachedTrack(trackId: String)

    @Query("DELETE FROM cached_tracks")
    suspend fun clearAllCache()

    @Query("SELECT SUM(fileSizeBytes) FROM cached_tracks")
    fun getTotalCacheSizeBytes(): Flow<Long?>
}
