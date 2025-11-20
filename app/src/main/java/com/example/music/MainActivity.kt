package com.example.music

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val isPlayingFlow = MutableStateFlow(false)
    private val maxDurationFlow = MutableStateFlow(0f)
    private val currentDurationFlow = MutableStateFlow(0f)
    private val currentTrackFlow = MutableStateFlow(Track())

    private var service: MusicService? = null
    private var isBound = false
    private val songsList = songs

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as? MusicService.MusicBinder ?: return
            service = musicBinder.getService()
            musicBinder.setMusicList(songsList)

            // collect flows from binder inside lifecycleScope and update local MutableStateFlows
            lifecycleScope.launch {
                musicBinder.isPlaying().collect { isPlayingFlow.value = it }
            }
            lifecycleScope.launch {
                musicBinder.maxDuration().collect { maxDurationFlow.value = it }
            }
            lifecycleScope.launch {
                musicBinder.currentDuration().collect { currentDurationFlow.value = it }
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

        // Start UI
        setContent {
            // collect flows into Compose states
            val isPlaying by isPlayingFlow.collectAsState()
            val currentTrack by currentTrackFlow.collectAsState()

            ModernMusicPlayerScreen(
                track = currentTrack,
                isPlaying = isPlaying,
                onPlayPause = {
                    val intent = Intent(this, MusicService::class.java)
                    ContextCompat.startForegroundService(this, intent)
                    bindService(intent, connection, BIND_AUTO_CREATE)

                    if (isBound) {
                        service?.playPause()
                    }
                },
                onNext = {
                    // request next track via service
                    service?.next()
                },
                onPrevious = {
                    service?.prev()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
