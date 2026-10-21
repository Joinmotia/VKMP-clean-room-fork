package com.example.data.remote

import android.content.Context
import com.example.model.AuthMethod
import com.example.model.AuthSession
import com.example.model.VkAudioTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VkAudioRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("vkmp_auth_prefs", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadSavedSession())
    val session: StateFlow<AuthSession> = _session.asStateFlow()

    // In-memory catalog of user's personal audio tracks ("Моя музыка")
    private val _userLibrary = MutableStateFlow<List<VkAudioTrack>>(emptyList())
    val userLibrary: StateFlow<List<VkAudioTrack>> = _userLibrary.asStateFlow()

    // VK Charts ("Чарты ВКонтакте")
    private val _vkCharts = MutableStateFlow<List<VkAudioTrack>>(emptyList())
    val vkCharts: StateFlow<List<VkAudioTrack>> = _vkCharts.asStateFlow()

    // Recommendations
    private val _recommendations = MutableStateFlow<List<VkAudioTrack>>(emptyList())
    val recommendations: StateFlow<List<VkAudioTrack>> = _recommendations.asStateFlow()

    init {
        initDefaultMusicCatalog()
    }

    private fun loadSavedSession(): AuthSession {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val methodStr = prefs.getString("auth_method", AuthMethod.GUEST_OFFLINE.name)
        val method = try {
            AuthMethod.valueOf(methodStr ?: AuthMethod.GUEST_OFFLINE.name)
        } catch (e: Exception) {
            AuthMethod.GUEST_OFFLINE
        }

        return AuthSession(
            isLoggedIn = isLoggedIn,
            userId = prefs.getLong("user_id", 77489201L),
            userName = prefs.getString("user_name", "Слушатель VK") ?: "Слушатель VK",
            accessToken = prefs.getString("access_token", null),
            authMethod = method,
            maxPhoneNumber = prefs.getString("max_phone", null),
            isMaxVerified = prefs.getBoolean("is_max_verified", false)
        )
    }

    private fun saveSession(session: AuthSession) {
        prefs.edit().apply {
            putBoolean("is_logged_in", session.isLoggedIn)
            putLong("user_id", session.userId)
            putString("user_name", session.userName)
            putString("access_token", session.accessToken)
            putString("auth_method", session.authMethod.name)
            putString("max_phone", session.maxPhoneNumber)
            putBoolean("is_max_verified", session.isMaxVerified)
            apply()
        }
        _session.value = session
    }

    suspend fun loginWithMax(phoneNumber: String): Boolean {
        delay(1200) // Simulate MAX messenger handshake & push verification
        val newSession = AuthSession(
            isLoggedIn = true,
            userId = 398124059L,
            userName = "Алексей (MAX ID)",
            authMethod = AuthMethod.MAX_MESSENGER,
            maxPhoneNumber = phoneNumber,
            isMaxVerified = true,
            accessToken = "vk1.a.max_verified_token_sample"
        )
        saveSession(newSession)
        return true
    }

    suspend fun loginWithToken(token: String): Boolean {
        delay(800)
        val cleanToken = token.trim()
        val newSession = AuthSession(
            isLoggedIn = true,
            userId = 249081234L,
            userName = "Пользователь VK",
            authMethod = AuthMethod.VK_TOKEN,
            accessToken = cleanToken,
            isMaxVerified = false
        )
        saveSession(newSession)
        return true
    }

    fun continueAsGuest() {
        val guestSession = AuthSession(
            isLoggedIn = true,
            userName = "Гостевой режим",
            authMethod = AuthMethod.GUEST_OFFLINE
        )
        saveSession(guestSession)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _session.value = AuthSession(isLoggedIn = false)
    }

    fun addTrackToLibrary(track: VkAudioTrack) {
        val current = _userLibrary.value.toMutableList()
        if (current.none { it.id == track.id }) {
            current.add(0, track)
            _userLibrary.value = current
        }
    }

    fun removeTrackFromLibrary(trackId: String) {
        _userLibrary.value = _userLibrary.value.filter { it.id != trackId }
    }

    private fun initDefaultMusicCatalog() {
        // High quality working streaming audio URLs with Russian and international popular hits and demo tracks
        val initialTracks = listOf(
            VkAudioTrack(
                id = "vk_audio_101",
                ownerId = 1001,
                audioId = 5001,
                title = "Малиновый закат",
                artist = "MACAN",
                durationSeconds = 178,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&q=80",
                albumTitle = "12",
                lyricsText = """
                    [00:05.00] Малиновый закат освещает наш город
                    [00:12.00] И в этих дворах каждый метр мне так дорог
                    [00:20.00] Ты помнишь те ночи, огни автострад
                    [00:28.00] И нам не вернуть это время назад...
                    [00:36.00] Скорость на максимум, ветер в висок
                    [00:44.00] Снова играет любимый басок.
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_102",
                ownerId = 1002,
                audioId = 5002,
                title = "Где прошла ты",
                artist = "Кравц & Гио Пика",
                durationSeconds = 195,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
                albumTitle = "17 мгновений осени",
                lyricsText = """
                    [00:08.00] Где прошла ты — там цветут цветы
                    [00:16.00] В тишине ночной разбивая мечты
                    [00:24.00] Белый снег летит на ладони опять
                    [00:32.00] Я устал тебя в этих снах вспоминать.
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_103",
                ownerId = 1003,
                audioId = 5003,
                title = "По барам",
                artist = "ANNA ASTI",
                durationSeconds = 212,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80",
                albumTitle = "Феникс",
                lyricsText = """
                    [00:10.00] А я всё помню, как ты уходил
                    [00:18.00] И больше не звонишь, не пишешь, не любил
                    [00:26.00] По барам пьяная одна лечу в такси
                    [00:34.00] И у ночных огней прошу: спаси.
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_104",
                ownerId = 1004,
                audioId = 5004,
                title = "Группа крови",
                artist = "Кино",
                durationSeconds = 245,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80",
                albumTitle = "Группа крови",
                lyricsText = """
                    [00:12.00] Тёплое место, но улицы ждут отпечатков наших ног
                    [00:20.00] Звёздная пыль на сапогах
                    [00:28.00] Мягкое кресло, клетчатый плед, не нажатый вовремя курок
                    [00:36.00] Солнечный день в ослепительных снах.
                    [00:44.00] Группа крови на рукаве, мой порядковый номер на рукаве
                    [00:52.00] Пожелай мне удачи в бою, пожелай мне удачи!
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_105",
                ownerId = 1005,
                audioId = 5005,
                title = "Седьмой лепесток",
                artist = "Anton Tokarev",
                durationSeconds = 188,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400&q=80",
                albumTitle = "Каверы",
                lyricsText = """
                    [00:06.00] А за окном седой туман качает ветви
                    [00:14.00] И ветер шепчет нам с тобой свои секреты
                    [00:22.00] Лети, лети, лепесток, через запад на восток!
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_106",
                ownerId = 1006,
                audioId = 5006,
                title = "Nightcall",
                artist = "Kavinsky",
                durationSeconds = 259,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80",
                albumTitle = "OutRun",
                lyricsText = """
                    [00:15.00] I'm giving you a nightcall to tell you how I feel
                    [00:27.00] I want to drive you through the night, down the hills
                    [00:40.00] There's something inside you, it's hard to explain
                    [00:52.00] They're talking about you, boy, but you're still the same.
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_107",
                ownerId = 1007,
                audioId = 5007,
                title = "Город под подошвой",
                artist = "Oxxxymiron",
                durationSeconds = 240,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80",
                albumTitle = "Горгород",
                lyricsText = """
                    [00:08.00] Весь мой рэп, если коротко — про то, что
                    [00:12.00] Уж который год который город под подошвой
                    [00:16.00] В гору, когда прёт, потом под гору, когда тошно
                    [00:20.00] Я не то что Гулливер, но всё же город под подошвой!
                """.trimIndent()
            ),
            VkAudioTrack(
                id = "vk_audio_108",
                ownerId = 1008,
                audioId = 5008,
                title = "Midnight City",
                artist = "M83",
                durationSeconds = 243,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1445985543470-41fba5c3144a?w=400&q=80",
                albumTitle = "Hurry Up, We're Dreaming",
                lyricsText = """
                    [00:18.00] Waiting in a car, waiting for a ride in the dark
                    [00:28.00] The night city grows, look and see her eyes, they glow
                    [00:38.00] The city is my church, it wraps me in the blinding twilight.
                """.trimIndent()
            )
        )

        _userLibrary.value = initialTracks

        _vkCharts.value = initialTracks.shuffled()
        _recommendations.value = initialTracks.reversed()
    }
}
