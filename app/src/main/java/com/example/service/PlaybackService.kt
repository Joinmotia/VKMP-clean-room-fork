package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.VkAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlaybackService : Service() {
    private lateinit var playerEngine: AudioPlayerEngine
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var trackJob: Job? = null
    private var isPlayingJob: Job? = null

    companion object {
        const val CHANNEL_ID = "vkmp_playback_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_PLAY = "com.example.vkmp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.vkmp.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.vkmp.ACTION_NEXT"
        const val ACTION_PREV = "com.example.vkmp.ACTION_PREV"
        const val ACTION_STOP = "com.example.vkmp.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playerEngine = AudioPlayerEngine.getInstance(this)
        createNotificationChannel()

        mediaSession = MediaSession(this, "VKMPMediaSession").apply {
            isActive = true
        }

        trackJob = scope.launch {
            playerEngine.currentTrack.collect { track ->
                updateNotification(track, playerEngine.isPlaying.value)
            }
        }

        isPlayingJob = scope.launch {
            playerEngine.isPlaying.collect { isPlaying ->
                updateNotification(playerEngine.currentTrack.value, isPlaying)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playerEngine.resume()
            ACTION_PAUSE -> playerEngine.pause()
            ACTION_NEXT -> playerEngine.next()
            ACTION_PREV -> playerEngine.previous()
            ACTION_STOP -> {
                playerEngine.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_playback_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(track: VkAudioTrack?, isPlaying: Boolean) {
        if (track == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlaybackService::class.java).apply { action = playPauseAction },
            PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(if (track.isCached) "Офлайн кеш" else "VK Музыка")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Назад", prevIntent)
            .addAction(playPauseIcon, if (isPlaying) "Пауза" else "Играть", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Вперед", nextIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        trackJob?.cancel()
        isPlayingJob?.cancel()
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
