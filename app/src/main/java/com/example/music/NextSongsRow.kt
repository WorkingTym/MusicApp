package com.example.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music.Track

@Composable
fun NextSongsRow(
    currentTrack: Track,
    songList: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Next Songs",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        val currentIndex = songList.indexOfFirst { it.id == currentTrack.id }
        val nextTracks = remember(currentTrack) {
            if (currentIndex != -1) {
                val list = mutableListOf<Track>()
                for (i in 1 until songList.size) {
                    list.add(songList[(currentIndex + i) % songList.size])
                }
                list
            } else emptyList()
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(nextTracks) { _, track ->
                val albumBitmap: ImageBitmap = runCatching {
                    ImageBitmap.imageResource(id = track.image)
                }.getOrElse {
                    ImageBitmap.imageResource(R.drawable.ek_mulakat)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTrackClick(track) }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = albumBitmap,
                        contentDescription = track.name,
                        contentScale = ContentScale.Crop, // <--- Make image fill the size
                        modifier = Modifier
                            .size(120.dp)

                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = track.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
