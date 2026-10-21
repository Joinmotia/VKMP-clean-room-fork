package com.example.model

data class VkAudioTrack(
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
    val isCached: Boolean = false,
    val localFilePath: String? = null,
    val downloadProgress: Float = -1f, // -1: not downloading, 0..1: downloading, 1.0: done
    val dateAdded: Long = System.currentTimeMillis()
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class EqualizerPreset(val titleRu: String) {
    FLAT("Обычный"),
    BASS_BOOST("Усиление баса"),
    VOCAL("Вокал"),
    ROCK("Рок"),
    ELECTRONIC("Электроника"),
    ACOUSTIC("Акустика")
}
