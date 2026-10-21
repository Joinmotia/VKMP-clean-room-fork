package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.example.data.cache.AudioCacheManager
import com.example.model.EqualizerPreset
import com.example.model.RepeatMode
import com.example.model.VkAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerEngine private constructor(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cacheManager = AudioCacheManager(context)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // State Flows
    private val _currentTrack = MutableStateFlow<VkAudioTrack?>(null)
    val currentTrack: StateFlow<VkAudioTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<VkAudioTrack>>(emptyList())
    val queue: StateFlow<List<VkAudioTrack>> = _queue.asStateFlow()

    private val _sleepTimerMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesRemaining: StateFlow<Int?> = _sleepTimerMinutesRemaining.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(EqualizerPreset.FLAT)
    val equalizerPreset: StateFlow<EqualizerPreset> = _equalizerPreset.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerEngine? = null

        fun getInstance(context: Context): AudioPlayerEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayerEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun playTrack(track: VkAudioTrack, newQueue: List<VkAudioTrack>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
        } else if (_queue.value.none { it.id == track.id }) {
            _queue.value = listOf(track) + _queue.value
        }

        _currentTrack.value = track
        prepareAndPlay(track)
    }

    private fun prepareAndPlay(track: VkAudioTrack) {
        releasePlayer()
        _isBuffering.value = true
        _currentPositionMs.value = 0L

        try {
            val localFile = cacheManager.getLocalFileForTrack(track.id)
            val playSource = if (localFile.exists() && localFile.length() > 1024) {
                localFile.absolutePath
            } else {
                // If not cached, auto-cache on the fly
                cacheManager.downloadTrack(track)
                track.streamUrl
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(playSource)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    _durationMs.value = mp.duration.toLong()
                    initAudioEffects(mp.audioSessionId)
                    mp.start()
                    _isPlaying.value = true
                    startPositionTicker()
                    PlaybackService.start(context)
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    _isBuffering.value = false
                    _isPlaying.value = false
                    stopPositionTicker()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                applyPresetToEqualizer(_equalizerPreset.value)
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
                if (_equalizerPreset.value == EqualizerPreset.BASS_BOOST) {
                    setStrength(1000.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerPreset.value = preset
        applyPresetToEqualizer(preset)
    }

    private fun applyPresetToEqualizer(preset: EqualizerPreset) {
        equalizer?.let { eq ->
            try {
                when (preset) {
                    EqualizerPreset.FLAT -> {
                        for (i in 0 until eq.numberOfBands) {
                            eq.setBandLevel(i.toShort(), 0)
                        }
                        bassBoost?.setStrength(0.toShort())
                    }
                    EqualizerPreset.BASS_BOOST -> {
                        eq.setBandLevel(0, 700)
                        eq.setBandLevel(1, 400)
                        bassBoost?.setStrength(1000.toShort())
                    }
                    EqualizerPreset.VOCAL -> {
                        eq.setBandLevel(2, 600)
                        eq.setBandLevel(3, 500)
                        bassBoost?.setStrength(100.toShort())
                    }
                    EqualizerPreset.ROCK -> {
                        eq.setBandLevel(0, 500)
                        eq.setBandLevel(1, 200)
                        eq.setBandLevel(4, 600)
                    }
                    EqualizerPreset.ELECTRONIC -> {
                        eq.setBandLevel(0, 600)
                        eq.setBandLevel(1, 400)
                        eq.setBandLevel(4, 500)
                    }
                    EqualizerPreset.ACOUSTIC -> {
                        eq.setBandLevel(1, 300)
                        eq.setBandLevel(2, 400)
                        eq.setBandLevel(3, 300)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopPositionTicker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startPositionTicker()
        } ?: run {
            _currentTrack.value?.let { prepareAndPlay(it) }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val nextIndex = when {
            _isShuffle.value -> q.indices.filter { it != currentIndex }.randomOrNull() ?: 0
            currentIndex in 0 until q.size - 1 -> currentIndex + 1
            _repeatMode.value == RepeatMode.ALL -> 0
            else -> null
        }

        nextIndex?.let { playTrack(q[it]) }
    }

    fun previous() {
        if (_currentPositionMs.value > 3000L) {
            seekTo(0)
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val prevIndex = when {
            currentIndex > 0 -> currentIndex - 1
            _repeatMode.value == RepeatMode.ALL -> q.size - 1
            else -> 0
        }
        playTrack(q[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesRemaining.value = minutes

        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                var remaining = minutes
                while (remaining > 0 && isActive) {
                    delay(60_000L)
                    remaining--
                    _sleepTimerMinutesRemaining.value = remaining
                }
                pause()
                _sleepTimerMinutesRemaining.value = null
            }
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                next()
            }
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
    }

    private fun releasePlayer() {
        stopPositionTicker()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
    }
}
