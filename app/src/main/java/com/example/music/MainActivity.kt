package com.example.music

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
    object Fav : BottomNavItem("Fav", Icons.Default.Favorite)
    object Library : BottomNavItem("Library", Icons.Default.LibraryMusic)
    object Search : BottomNavItem("Search", Icons.Default.Search)
}

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

            lifecycleScope.launch { musicBinder.isPlaying().collect { isPlayingFlow.value = it } }
            lifecycleScope.launch { musicBinder.getCurrentTrack().collect { currentTrackFlow.value = it } }

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
        startForegroundService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)

        setContent {
            var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

            Scaffold(
                bottomBar = {
                    BottomNavigationBar(selectedTab) { selectedTab = it }
                }
            ) { paddingValues ->
                val modifier = Modifier.padding(paddingValues)
                when (selectedTab) {
                    is BottomNavItem.Home -> {
                        val isPlaying by isPlayingFlow.collectAsState()
                        val currentTrack by currentTrackFlow.collectAsState()
                        ModernMusicPlayerScreen(
                            track = currentTrack,
                            songList = songs,
                            isPlaying = isPlaying,
                            onPlayPause = { service?.playPause() },
                            onNext = { service?.next() },
                            onPrevious = { service?.prev() }
                        )
                    }
                    is BottomNavItem.Fav -> PlaceholderScreen("Favorites", modifier)
                    is BottomNavItem.Library -> PlaceholderScreen("Library", modifier)
                    is BottomNavItem.Search -> PlaceholderScreen("Search", modifier)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
    }
}

@Composable
fun BottomNavigationBar(selectedTab: BottomNavItem, onTabSelected: (BottomNavItem) -> Unit) {
    NavigationBar(containerColor = Color.Black, tonalElevation = 4.dp) {
        listOf(BottomNavItem.Home, BottomNavItem.Fav, BottomNavItem.Library, BottomNavItem.Search)
            .forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title, tint = Color.White) },
                    label = { Text(item.title, color = Color.White) },
                    selected = selectedTab == item,
                    onClick = { onTabSelected(item) }
                )
            }
    }
}

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, color = Color.White)
    }
}
