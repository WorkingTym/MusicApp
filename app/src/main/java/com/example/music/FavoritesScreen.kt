package com.example.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FavoritesScreen(
    favTracks: List<Track>,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favTracks.isEmpty()) {
        PlaceholderScreen("No Favorites", modifier)
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(favTracks) { track ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(track) }
                        .padding(12.dp)
                ) {
                    val albumBitmap = runCatching { ImageBitmap.imageResource(track.image) }
                        .getOrElse { ImageBitmap.imageResource(R.drawable.ek_mulakat) }

                    Image(
                        bitmap = albumBitmap,
                        contentDescription = track.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = track.name, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
