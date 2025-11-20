package com.example.music

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri

const val PREV = "previous"
const val NEXT = "next"
const val PLAY_PAUSE = "play_pause"


class MusicService : Service() {

    private var mediaPlayer = MediaPlayer()
    private val currentTrack = MutableStateFlow<Track>(Track())
    private val maxDuration= MutableStateFlow(0f)
    private val currentDuration= MutableStateFlow(0f)
    private val scope= CoroutineScope(Dispatchers.Main)

    private var musiclist = mutableListOf<Track>()
    private val isPlaying= MutableStateFlow<Boolean>(false)
    val binder = MusicBinder()

    companion object {
        const val CHANNEL_ID = "music_channel"
    }

    inner class MusicBinder : Binder() {
        fun getService() = this@MusicService
        fun setMusicList(list: List<Track>) {
            this@MusicService.musiclist = list.toMutableList()
        }
        fun currentDuration()=this@MusicService.currentDuration
        fun maxDuration()=this@MusicService.maxDuration
        fun isPlaying()=this@MusicService.isPlaying
        fun getCurrentTrack()=this@MusicService.currentTrack
    }

    private var job:Job?=null

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
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
        intent?.let {
            when (intent.action) {
                PREV -> {
                    prev()
                }

                NEXT -> {
                    next()
                }

                PLAY_PAUSE -> {
                    playPause()
                }

                else -> {
                    currentTrack.update { songs[0] }
                    play(currentTrack.value)
                }
            }
        }
        return START_STICKY
    }

    fun updateDurations(){
        job=scope.launch {
            if(mediaPlayer.isPlaying.not()) return@launch
            maxDuration.update { mediaPlayer.duration.toFloat() }

            while (true){
                currentDuration.update { mediaPlayer.currentPosition.toFloat() }
                delay(1000)
            }


        }
    }

    fun prev() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musiclist.indexOf(currentTrack.value)
        val prevIndex = if (index < 0) musiclist.size.minus(1) else index.minus(1)
        val prevItem = musiclist[prevIndex]
        currentTrack.update { prevItem }
        mediaPlayer.setDataSource(this, getRawUri(currentTrack.value.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack.value)
            updateDurations()
        }

    }

    fun next() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musiclist.indexOf(currentTrack.value)
        val nextIndex = index.plus(1).mod(musiclist.size)
        val nextItem = musiclist[nextIndex]
        currentTrack.update { nextItem }
        mediaPlayer.setDataSource(this, getRawUri(nextItem.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack.value)
            updateDurations()
        }
    }

    fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        sendNotification(currentTrack.value)
    }

    private fun play(track: Track) {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, getRawUri(track.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            sendNotification(track)
            mediaPlayer.start()
//            sendNotification(track)
            updateDurations()
        }
    }

    private fun getRawUri(id: Int) = "android.resource://${packageName}/${id}".toUri()

    private fun sendNotification(track: Track) {
        val session= MediaSessionCompat(this,"music")
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(session.sessionToken)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(style)
            .setContentTitle(track.name)
            .setContentText(track.desc)
            .addAction(
                R.drawable.baseline_arrow_circle_left_24,
                "previous",
                createPrevPendingIntent()
            )
            .addAction(R.drawable.baseline_arrow_circle_right_24, "next", createNextPendingIntent())
            .addAction(
                if (mediaPlayer.isPlaying) R.drawable.baseline_pause_circle_24 else R.drawable.baseline_play_circle_24,
                "play_pause",
                createPlayPausePendingIntent()
            )
            .setSmallIcon(track.image)
            .setLargeIcon(BitmapFactory.decodeResource(resources, track.image))
            .build()
        isPlaying.update { true }
        startForeground(1, notification)

    }

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


    fun createPrevPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PREV
        }
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createNextPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = NEXT
        }
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createPlayPausePendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PLAY_PAUSE
        }
        return PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}