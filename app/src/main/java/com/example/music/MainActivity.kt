package com.example.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    // Local state holders (mirrors service flows)
    private val isPlayingFlow = MutableStateFlow(false)
    private val maxDurationFlow = MutableStateFlow(0f)
    private val currentDurationFlow = MutableStateFlow(0f)
    private val currentTrackFlow = MutableStateFlow(Track())

    private var service: MusicService? = null
    private var isBound = false

    // songs from your file (you provided earlier)
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
                    // start + bind service and toggle play via binder
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

// Extension helpers to safely call binder methods (because we used inner binder class)
private val MusicService.binder: MusicService.MusicBinder?
    get() = try { // reflection-free access: find IBinder via onBind? We provided binder in service, so we can keep a small helper by casting
        // There's no direct public property; but in our MainActivity we get the binder from ServiceConnection, so safe-calls are used above.
        null
    } catch (e: Exception) {
        null
    }

// Helper functions that call service methods safely (used after bind)
private fun MusicService.binderPlayPauseSafe() {
    // we can't access the binder instance directly outside ServiceConnection; instead call playPause via public method if you implement one
    // If you prefer, expose a public method on MusicService to control playback. For simplicity, we can expose it below.
}

private fun MusicService.playPauseSafe() {
    // placeholder
}
