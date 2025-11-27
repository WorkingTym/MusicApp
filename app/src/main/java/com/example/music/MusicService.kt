package com.example.music

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

const val PREV = "previous"
const val NEXT = "next"
const val PLAY_PAUSE = "play_pause"

class MusicService : Service() {

    private var mediaPlayer = MediaPlayer()

    private val currentTrack = MutableStateFlow(Track())
    private val maxDuration = MutableStateFlow(0f)
    private val currentDuration = MutableStateFlow(0f)
    private val isPlaying = MutableStateFlow(false)

    private var musiclist = mutableListOf<Track>()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    private lateinit var mediaSession: MediaSessionCompat
    private var isForegroundService = false

    companion object {
        const val CHANNEL_ID = "music_channel"
    }

    inner class MusicBinder : Binder() {
        fun getService() = this@MusicService
        fun setMusicList(list: List<Track>) {
            musiclist = list.toMutableList()
        }

        fun currentDuration() = currentDuration
        fun maxDuration() = maxDuration
        fun isPlaying() = isPlaying
        fun getCurrentTrack() = currentTrack
    }

    private val binder = MusicBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "music")

        if (songs.isNotEmpty()) {
            currentTrack.value = songs[0]
            isPlaying.value = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PREV -> prev()
            NEXT -> next()
            PLAY_PAUSE -> playPause()
        }
        return START_STICKY
    }

    // -------------------- DURATION UPDATES --------------------

    fun updateDurations() {
        job?.cancel()
        job = scope.launch {
            maxDuration.update { mediaPlayer.duration.toFloat() }

            while (isPlaying.value) {
                currentDuration.update { mediaPlayer.currentPosition.toFloat() }
                delay(1000)
            }
        }
    }

    // -------------------- PLAYER CONTROLS --------------------

    fun playPause() {
        if (mediaPlayer.isPlaying) {
            // ✅ PAUSE — keep current position
            mediaPlayer.pause()
            isPlaying.value = false

        } else {
            // ✅ RESUME — no reset, no re-prepare
            if (!mediaPlayer.isPlaying) {

                // Only prepare FIRST time after app launch
                if (mediaPlayer.currentPosition == 0) {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(this, getRawUri(currentTrack.value.id))
                    mediaPlayer.prepare()   // synchronous is OK for local raw files
                }

                mediaPlayer.start()
                isPlaying.value = true
            }
        }

        sendNotification(currentTrack.value)
    }


    fun prev() {
        restartPlayer()

        val index = musiclist.indexOf(currentTrack.value)
        val prevIndex = if (index <= 0) musiclist.lastIndex else index - 1
        val prevItem = musiclist[prevIndex]

        currentTrack.update { prevItem }

        mediaPlayer.setDataSource(this, getRawUri(prevItem.id))
        mediaPlayer.prepareAsync()

        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            isPlaying.update { true }
            sendNotification(prevItem)
            updateDurations()
        }
    }

    fun next() {
        restartPlayer()

        val index = musiclist.indexOf(currentTrack.value)
        val nextIndex = (index + 1) % musiclist.size
        val nextItem = musiclist[nextIndex]

        currentTrack.update { nextItem }

        mediaPlayer.setDataSource(this, getRawUri(nextItem.id))
        mediaPlayer.prepareAsync()

        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            isPlaying.update { true }
            sendNotification(nextItem)
            updateDurations()
        }
    }

//    private fun play(track: Track) {
//        restartPlayer()
//
//        mediaPlayer.setDataSource(this, getRawUri(track.id))
//        mediaPlayer.prepareAsync()
//
//        mediaPlayer.setOnPreparedListener {
//            mediaPlayer.start()
//            isPlaying.update { true }
//            sendNotification(track)
//            updateDurations()
//        }
//    }

    private fun restartPlayer() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
    }

    // -------------------- NOTIFICATION --------------------

    private fun buildNotification(track: Track): Notification {

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession.sessionToken)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(style)
            .setContentTitle(track.name)
            .setContentText(track.desc)
            .addAction(
                R.drawable.baseline_arrow_circle_left_24,
                "previous",
                createPrevPendingIntent()
            )
            .addAction(
                R.drawable.baseline_arrow_circle_right_24,
                "next",
                createNextPendingIntent()
            )
            .addAction(
                if (isPlaying.value)
                    R.drawable.baseline_pause_circle_24
                else
                    R.drawable.baseline_play_circle_24,
                "play_pause",
                createPlayPausePendingIntent()
            )
            .setSmallIcon(track.image)
            .setLargeIcon(BitmapFactory.decodeResource(resources, track.image))
            .build()
    }

    private fun sendNotification(track: Track) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (!isForegroundService) {
            startForeground(1, buildNotification(track))
            isForegroundService = true
        } else {
            nm.notify(1, buildNotification(track))
        }
    }

    // -------------------- HELPERS --------------------

    private fun getRawUri(id: Int) =
        "android.resource://${packageName}/$id".toUri()

    fun createPrevPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { action = PREV }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createNextPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { action = NEXT }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createPlayPausePendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { action = PLAY_PAUSE }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // -------------------- CLEANUP --------------------

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        mediaPlayer.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
