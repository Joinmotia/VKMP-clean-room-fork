package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.VkAudioTrack

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey
    val id: String,
    val ownerId: Long = 0L,
    val audioId: Long = 0L,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val streamUrl: String,
    val artworkUrl: String? = null,
    val albumTitle: String = "",
    val lyricsText: String = "",
    val localFilePath: String,
    val fileSizeBytes: Long = 0L,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toVkAudioTrack(): VkAudioTrack {
        return VkAudioTrack(
            id = id,
            ownerId = ownerId,
            audioId = audioId,
            title = title,
            artist = artist,
            durationSeconds = durationSeconds,
            streamUrl = streamUrl,
            artworkUrl = artworkUrl,
            albumTitle = albumTitle,
            lyricsText = lyricsText,
            isCached = true,
            localFilePath = localFilePath,
            downloadProgress = 1f,
            dateAdded = cachedAt
        )
    }

    companion object {
        fun fromTrack(track: VkAudioTrack, localPath: String, sizeBytes: Long): CachedTrackEntity {
            return CachedTrackEntity(
                id = track.id,
                ownerId = track.ownerId,
                audioId = track.audioId,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds,
                streamUrl = track.streamUrl,
                artworkUrl = track.artworkUrl,
                albumTitle = track.albumTitle,
                lyricsText = track.lyricsText,
                localFilePath = localPath,
                fileSizeBytes = sizeBytes,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
