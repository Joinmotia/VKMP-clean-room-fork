package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cache.AudioCacheManager
import com.example.data.local.AppDatabase
import com.example.data.remote.VkAudioRepository
import com.example.model.AuthSession
import com.example.model.EqualizerPreset
import com.example.model.RepeatMode
import com.example.model.VkAudioTrack
import com.example.service.AudioPlayerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VkAudioRepository(application)
    private val cacheManager = AudioCacheManager(application)
    private val playerEngine = AudioPlayerEngine.getInstance(application)
    private val trackDao = AppDatabase.getInstance(application).trackDao()

    val session: StateFlow<AuthSession> = repository.session
    val currentTrack: StateFlow<VkAudioTrack?> = playerEngine.currentTrack
    val isPlaying: StateFlow<Boolean> = playerEngine.isPlaying
    val currentPositionMs: StateFlow<Long> = playerEngine.currentPositionMs
    val durationMs: StateFlow<Long> = playerEngine.durationMs
    val isBuffering: StateFlow<Boolean> = playerEngine.isBuffering
    val repeatMode: StateFlow<RepeatMode> = playerEngine.repeatMode
    val isShuffle: StateFlow<Boolean> = playerEngine.isShuffle
    val queue: StateFlow<List<VkAudioTrack>> = playerEngine.queue
    val sleepTimerMinutes: StateFlow<Int?> = playerEngine.sleepTimerMinutesRemaining
    val equalizerPreset: StateFlow<EqualizerPreset> = playerEngine.equalizerPreset

    val userLibrary: StateFlow<List<VkAudioTrack>> = repository.userLibrary
    val vkCharts: StateFlow<List<VkAudioTrack>> = repository.vkCharts
    val recommendations: StateFlow<List<VkAudioTrack>> = repository.recommendations

    // Cached tracks from Room Database
    val cachedTracks: StateFlow<List<VkAudioTrack>> = trackDao.getAllCachedTracks()
        .combine(playerEngine.currentTrack) { entities, _ ->
            entities.map { it.toVkAudioTrack() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCacheSizeBytes: StateFlow<Long?> = trackDao.getTotalCacheSizeBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val downloadProgressMap: StateFlow<Map<String, Float>> = cacheManager.downloadProgressMap
    val isBatchDownloading: StateFlow<Boolean> = cacheManager.isBatchDownloading
    val batchProgress: StateFlow<Pair<Int, Int>> = cacheManager.batchProgress
    val batchStatusText: StateFlow<String> = cacheManager.batchStatusText

    // Search and filter state
    val searchQuery = MutableStateFlow("")

    val filteredLibrary: StateFlow<List<VkAudioTrack>> = combine(
        userLibrary,
        searchQuery,
        cachedTracks
    ) { library, query, cached ->
        val cachedIds = cached.map { it.id }.toSet()
        val enriched = library.map { track ->
            if (cachedIds.contains(track.id)) track.copy(isCached = true) else track
        }
        if (query.isBlank()) {
            enriched
        } else {
            val q = query.trim().lowercase()
            enriched.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI sheet/dialog visibility states
    val isFullPlayerVisible = MutableStateFlow(false)
    val isLyricsView = MutableStateFlow(false)
    val isSleepTimerDialogVisible = MutableStateFlow(false)
    val isEqualizerDialogVisible = MutableStateFlow(false)
    val isAuthDialogVisible = MutableStateFlow(false)

    // Auth screen input state
    val authPhoneInput = MutableStateFlow("+7 ")
    val authTokenInput = MutableStateFlow("")
    val isAuthLoading = MutableStateFlow(false)
    val authStatusMessage = MutableStateFlow<String?>(null)

    fun playTrack(track: VkAudioTrack, explicitQueue: List<VkAudioTrack>? = null) {
        playerEngine.playTrack(track, explicitQueue)
    }

    fun playAll(tracks: List<VkAudioTrack>, startIndex: Int = 0) {
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            playerEngine.playTrack(tracks[startIndex], tracks)
        }
    }

    fun togglePlayPause() = playerEngine.togglePlayPause()

    fun pause() = playerEngine.pause()

    fun resume() = playerEngine.resume()

    fun seekTo(positionMs: Long) = playerEngine.seekTo(positionMs)

    fun next() = playerEngine.next()

    fun previous() = playerEngine.previous()

    fun toggleRepeat() = playerEngine.toggleRepeat()

    fun toggleShuffle() = playerEngine.toggleShuffle()

    fun setSleepTimer(minutes: Int?) {
        playerEngine.setSleepTimer(minutes)
        isSleepTimerDialogVisible.value = false
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        playerEngine.setEqualizerPreset(preset)
        isEqualizerDialogVisible.value = false
    }

    fun downloadTrack(track: VkAudioTrack) {
        cacheManager.downloadTrack(track)
    }

    fun downloadAllMyMusic() {
        val tracksToDownload = userLibrary.value
        cacheManager.startBatchDownload(tracksToDownload)
    }

    fun cancelBatchDownload() {
        cacheManager.cancelBatchDownload()
    }

    fun removeCachedTrack(trackId: String) {
        viewModelScope.launch {
            cacheManager.removeCachedTrack(trackId)
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            cacheManager.clearEntireCache()
        }
    }

    fun toggleFavorite(track: VkAudioTrack) {
        val exists = userLibrary.value.any { it.id == track.id }
        if (exists) {
            repository.removeTrackFromLibrary(track.id)
        } else {
            repository.addTrackToLibrary(track)
        }
    }

    fun loginWithMax() {
        val phone = authPhoneInput.value.trim()
        if (phone.length < 6) {
            authStatusMessage.value = "Пожалуйста, введите корректный номер телефона"
            return
        }

        viewModelScope.launch {
            isAuthLoading.value = true
            authStatusMessage.value = "Запрос подтверждения в мессенджере MAX..."
            val success = repository.loginWithMax(phone)
            isAuthLoading.value = false
            if (success) {
                authStatusMessage.value = "Вход успешно выполнен через MAX!"
                isAuthDialogVisible.value = false
            } else {
                authStatusMessage.value = "Ошибка подтверждения в MAX. Попробуйте еще раз."
            }
        }
    }

    fun loginWithToken() {
        val token = authTokenInput.value.trim()
        if (token.length < 10) {
            authStatusMessage.value = "Токен слишком короткий. Проверьте правильность."
            return
        }

        viewModelScope.launch {
            isAuthLoading.value = true
            authStatusMessage.value = "Проверка VK токена..."
            val success = repository.loginWithToken(token)
            isAuthLoading.value = false
            if (success) {
                authStatusMessage.value = "Вход успешно выполнен!"
                isAuthDialogVisible.value = false
            } else {
                authStatusMessage.value = "Неверный токен доступа."
            }
        }
    }

    fun continueAsGuest() {
        repository.continueAsGuest()
        isAuthDialogVisible.value = false
    }

    fun logout() {
        repository.logout()
        playerEngine.pause()
    }
}
