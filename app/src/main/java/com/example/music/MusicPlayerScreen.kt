package com.example.music

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun ModernMusicPlayerScreen(
    track: Track,
    songList: List<Track>,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTrackSelected: (Track) -> Unit,
    favTracks: List<Track>,          // <-- favorites list
    onFavClick: (Track) -> Unit
) {

    val isFav = remember(track, favTracks) { track in favTracks } // check favorite state

    /** IMPROVED ROTATION LOGIC (freezes correctly when paused) **/
    val infinite = rememberInfiniteTransition()
    val baseRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // stores last rotation angle when paused
    val lastAngle = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                lastAngle.floatValue = baseRotation % 360f
                delay(40)
            }
        }
    }

    val appliedAngle =
        if (isPlaying) baseRotation % 360f else lastAngle.floatValue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /** TOP BAR **/
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown, "",
                    tint = Color.White, modifier = Modifier.size(30.dp)
                )

                Text(
                    text = track.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )


                IconButton(onClick = { onFavClick(track) }) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,  // Toggle icon
                        contentDescription = if (isFav) "Unfavorite" else "Favorite",
                        tint = if (isFav) Color.Red else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            /** CIRCULAR ALBUM ART **/
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {

                RotatingAlbumWithLottie(
                    rotationAngle = appliedAngle,
                    albumImage = track.image,
                    isPlaying = isPlaying
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            /** CONTROLS **/
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious, "",
                        tint = Color.White, modifier = Modifier.size(48.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext, "",
                        tint = Color.White, modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            /** NEXT SONGS **/

            NextSongsRow(currentTrack = track, songList = songList,
                onTrackClick  = onTrackSelected)

                Spacer(modifier = Modifier.width(12.dp))
        }
    }
}
