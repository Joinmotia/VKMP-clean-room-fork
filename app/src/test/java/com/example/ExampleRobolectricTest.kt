package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedTrackEntity
import com.example.model.VkAudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VKMP", appName)
    }

    @Test
    fun `test track duration format`() {
        val track = VkAudioTrack(
            id = "test_1",
            title = "Test Song",
            artist = "Test Artist",
            durationSeconds = 195,
            streamUrl = "https://example.com/audio.mp3"
        )
        assertEquals("3:15", track.durationFormatted)
    }

    @Test
    fun `test cached track entity conversion`() {
        val track = VkAudioTrack(
            id = "test_2",
            title = "Sample Track",
            artist = "Sample Artist",
            durationSeconds = 120,
            streamUrl = "https://example.com/sample.mp3"
        )
        val entity = CachedTrackEntity.fromTrack(track, "/data/audio/sample.mp3", 4096000L)
        val converted = entity.toVkAudioTrack()

        assertEquals("test_2", converted.id)
        assertTrue(converted.isCached)
        assertEquals("/data/audio/sample.mp3", converted.localFilePath)
    }
}
