package com.example.music

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var service: MusicService? = null
    private var isBound = false

    private val isPlayingFlow = MutableStateFlow(false)
    private val currentTrackFlow = MutableStateFlow(Track())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicService.MusicBinder
            service = musicBinder.getService()
            musicBinder.setMusicList(songs)

            lifecycleScope.launch {
                musicBinder.isPlaying().collect { isPlayingFlow.value = it }
            }
            lifecycleScope.launch {
                musicBinder.getCurrentTrack().collect { currentTrackFlow.value = it }
            }

            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, connection, BIND_AUTO_CREATE)

        setContent {
            val isPlaying by isPlayingFlow.collectAsState()
            val currentTrack by currentTrackFlow.collectAsState()

            ModernMusicPlayerScreen (
                track = currentTrack,
                songList = songs,
                isPlaying = isPlaying,
                onPlayPause = { service?.playPause() },
                onNext = { service?.next() },
                onPrevious = { service?.prev() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
    }
}
