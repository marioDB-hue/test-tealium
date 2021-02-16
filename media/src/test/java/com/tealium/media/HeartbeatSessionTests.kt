package com.tealium.media

import com.tealium.core.TealiumContext
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeartbeatSessionTest {

    @RelaxedMockK
    lateinit var mockContext: TealiumContext

    @RelaxedMockK
    lateinit var mockMediaSessionDispatcher: MediaSessionDispatcher

    private lateinit var mediaContent: MediaContent

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testHeartbeat_SessionStart() {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        verify { mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any(), any()) }
    }

    @Test
    fun testHeartbeat_HeartbeatSent() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT
        )

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        // wait 10 seconds - should record Heartbeat
        delay(10000)

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            mockMediaSessionDispatcher.track(MediaEvent.HEARTBEAT, any())
        }
    }

    @Test
    fun testHeartbeat_PlayPauseSuccess() = runBlocking {
        mediaContent = MediaContent("test_media",
                mockk(),
                mockk(),
                mockk(),
                TrackingType.HEARTBEAT,
                duration = 100)

        val media = Media(mockContext, mockMediaSessionDispatcher)
        media.startSession(mediaContent)

        // wait 10 seconds - should record Heartbeat
        delay(10000)

        media.play()
        media.pause()
        media.endSession()

        verify {
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_START, any())
            mockMediaSessionDispatcher.track(MediaEvent.HEARTBEAT, any())
            mockMediaSessionDispatcher.track(MediaEvent.PLAY, any())
            mockMediaSessionDispatcher.track(MediaEvent.PAUSE, any())
            mockMediaSessionDispatcher.track(MediaEvent.SESSION_END, any())
        }
    }
}