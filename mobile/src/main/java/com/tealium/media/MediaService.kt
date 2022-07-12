package com.tealium.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.tealium.core.Tealium
import com.tealium.media.segments.Ad
import com.tealium.media.segments.AdBreak
import com.tealium.media.segments.Chapter
import com.tealium.media.v2.MediaMetadata
import com.tealium.media.v2.MediaModule
import com.tealium.media.v2.plugins.MilestonePlugin
import com.tealium.media.v2.plugins.SummaryPlugin
import com.tealium.mobile.BuildConfig

class MediaService : Service() {

    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null

    private var binder = LocalBinder()

    private val mediaContent: MediaContent = MediaContent(
            "Tealium Sample Media Content",
            StreamType.VOD,
            MediaType.VIDEO,
            QoE(1),
            trackingType = TrackingType.FULL_PLAYBACK,
            duration = 120
    )

    override fun onCreate() {
        super.onCreate()
        startPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        mediaSession?.release()
        playerNotificationManager?.setPlayer(null)
        player?.release()
        player = null

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        player?.addListener(createListener())

        buildMediaSource()?.let {
            player?.prepare(it)
        }

        playerNotificationManager =
            PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setChannelNameResourceId(R.string.app_name)
                .setMediaDescriptionAdapter(createMediaDescriptionManager())
                .setNotificationListener(createNotificationListener())
                .build()
        playerNotificationManager?.setPlayer(player)

        val playbackState = PlaybackStateCompat.Builder()
        playbackState.setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)

        mediaSession = MediaSessionCompat(this, "Tealium Sample Media").apply {
            isActive = true
            setPlaybackState(playbackState.build())
            playerNotificationManager?.setMediaSessionToken(sessionToken)
        }
        startSession()
    }

    private fun buildMediaSource(): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(this, "Sample")
        val uri = RawResourceDataSource.buildRawResourceUri(com.tealium.mobile.R.raw.tealium)
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
    }

    private fun createListener(): Player.Listener { //Player.EventListener
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                when (isPlaying) {
                    true -> play()
                    false -> pause()
                }
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_BUFFERING -> {
                    }
                    ExoPlayer.STATE_ENDED -> endSession()
                    ExoPlayer.STATE_READY -> {
                    }
                    ExoPlayer.STATE_IDLE -> println("Idle")
                    else -> print("unknownState$playbackState")
                }
            }
        }
    }

    private fun createMediaDescriptionManager(): PlayerNotificationManager.MediaDescriptionAdapter {
        return object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return null
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return "Tealium Sample Media"
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return "Tealium Sample Media"
            }

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                return null
            }
        }
    }

    private fun createNotificationListener(): PlayerNotificationManager.NotificationListener {
        return object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                stopSelf()
            }

            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                startForeground(notificationId, notification)
            }
        }
    }

    private fun getMediaModule2() : MediaModule? {
        return Tealium[BuildConfig.TEALIUM_INSTANCE]?.modules?.getModule(MediaModule::class.java)
    }

    fun startSession() {
//        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.startSession(mediaContent)
        getMediaModule2()?.createSession(
            MediaMetadata(
                mediaContent.customId,
                mediaContent.name,
                mediaContent.duration,
                mediaContent.streamType,
                mediaContent.mediaType,
                System.currentTimeMillis(),
                mediaContent.playerName,
                mediaContent.channelName
            ),
            setOf(
                SummaryPlugin.Factory,
                MilestonePlugin.Factory.configure(1000L)
            )
        )?.startSession()
    }

    fun endSession() {
//        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.endSession()
        getMediaModule2()?.session?.endSession()
    }

    fun play() {
//        Tealium[BuildConfig.TEALIUM_INSTANCE]?.media?.play()
        getMediaModule2()?.session?.play()
    }

    fun pause() {
        getMediaModule2()?.session?.pause()
    }

    fun startAdBreak() {
        getMediaModule2()?.session?.startAdBreak(AdBreak("Ad Break 1"))
    }

    fun endAdBreak() {
        getMediaModule2()?.session?.endAdBreak()
    }

    fun startAd() {
        getMediaModule2()?.session?.startAd(Ad("Ad  1"))
    }

    fun endAd() {
        getMediaModule2()?.session?.endAd()
    }

    fun startChapter() {
        getMediaModule2()?.session?.startChapter(Chapter("Chapter 1", 3000.toDouble()))
    }

    fun endChapter() {
        getMediaModule2()?.session?.endChapter()
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@MediaService

        val player
            get() = this@MediaService.player
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "playback_channel"
    }
}