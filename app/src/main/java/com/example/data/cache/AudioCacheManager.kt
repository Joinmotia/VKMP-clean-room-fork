package com.example.data.cache

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CachedTrackEntity
import com.example.model.VkAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AudioCacheManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val trackDao = db.trackDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.filesDir, "vkmp_audio_cache").apply {
        if (!exists()) mkdirs()
    }

    // Active individual download progress map: trackId -> 0.0f..1.0f
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    // Batch download state
    private val _isBatchDownloading = MutableStateFlow(false)
    val isBatchDownloading: StateFlow<Boolean> = _isBatchDownloading.asStateFlow()

    private val _batchProgress = MutableStateFlow(0 to 0) // completed to total
    val batchProgress: StateFlow<Pair<Int, Int>> = _batchProgress.asStateFlow()

    private val _batchStatusText = MutableStateFlow("")
    val batchStatusText: StateFlow<String> = _batchStatusText.asStateFlow()

    private var batchJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getLocalFileForTrack(trackId: String): File {
        val sanitized = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return File(cacheDir, "$sanitized.mp3")
    }

    suspend fun isTrackCached(trackId: String): Boolean {
        val file = getLocalFileForTrack(trackId)
        return file.exists() && file.length() > 1024
    }

    /**
     * Download and cache a single track asynchronously
     */
    fun downloadTrack(track: VkAudioTrack, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val success = downloadTrackInternal(track)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(success)
            }
        }
    }

    private suspend fun downloadTrackInternal(track: VkAudioTrack): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getLocalFileForTrack(track.id)
        if (targetFile.exists() && targetFile.length() > 1024) {
            // Already cached, ensure recorded in Room
            trackDao.insertCachedTrack(
                CachedTrackEntity.fromTrack(track, targetFile.absolutePath, targetFile.length())
            )
            return@withContext true
        }

        val tempFile = File(cacheDir, "${targetFile.name}.download")
        try {
            updateProgress(track.id, 0.05f)

            val request = Request.Builder()
                .url(track.streamUrl)
                .header("User-Agent", "VKMP/2.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                updateProgress(track.id, -1f)
                return@withContext false
            }

            val body = response.body ?: run {
                updateProgress(track.id, -1f)
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input: InputStream ->
                FileOutputStream(tempFile).use { output: FileOutputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            updateProgress(track.id, progress)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists() && tempFile.length() > 512) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                // Save to Room DB
                trackDao.insertCachedTrack(
                    CachedTrackEntity.fromTrack(track, targetFile.absolutePath, targetFile.length())
                )
                updateProgress(track.id, 1.0f)
                return@withContext true
            } else {
                tempFile.delete()
                updateProgress(track.id, -1f)
                return@withContext false
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            updateProgress(track.id, -1f)
            return@withContext false
        }
    }

    private fun updateProgress(trackId: String, progress: Float) {
        val current = _downloadProgressMap.value.toMutableMap()
        if (progress < 0f || progress >= 1f) {
            current.remove(trackId)
        } else {
            current[trackId] = progress
        }
        _downloadProgressMap.value = current
    }

    /**
     * Start batch download for all tracks in "Моя музыка"
     */
    fun startBatchDownload(tracks: List<VkAudioTrack>) {
        if (_isBatchDownloading.value) return

        batchJob = scope.launch {
            _isBatchDownloading.value = true
            val uncachedTracks = tracks.filter { !isTrackCached(it.id) }
            val total = uncachedTracks.size

            if (total == 0) {
                _batchStatusText.value = "Все треки уже скачаны!"
                _isBatchDownloading.value = false
                return@launch
            }

            _batchProgress.value = 0 to total
            var completed = 0

            for (track in uncachedTracks) {
                if (!batchJob?.isActive!!) break
                _batchStatusText.value = "Загрузка: ${track.artist} — ${track.title} (${completed + 1}/$total)"
                val success = downloadTrackInternal(track)
                if (success) {
                    completed++
                    _batchProgress.value = completed to total
                }
            }

            _batchStatusText.value = "Загрузка завершена ($completed/$total)"
            _isBatchDownloading.value = false
        }
    }

    fun cancelBatchDownload() {
        batchJob?.cancel()
        _isBatchDownloading.value = false
        _batchStatusText.value = "Загрузка отменена"
        _downloadProgressMap.value = emptyMap()
    }

    suspend fun removeCachedTrack(trackId: String) = withContext(Dispatchers.IO) {
        val file = getLocalFileForTrack(trackId)
        if (file.exists()) file.delete()
        trackDao.deleteCachedTrack(trackId)
    }

    suspend fun clearEntireCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        trackDao.clearAllCache()
    }
}
