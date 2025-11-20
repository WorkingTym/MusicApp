package com.example.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun RotatingAlbumWithLottie(
    rotationAngle: Float,
    albumImage: Int,
    isPlaying: Boolean
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(420.dp)
    ) {

        // 🔥 BACKGROUND LOTTIE ANIMATION

        if (isPlaying) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.Asset("voice.json")
            )

            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(400.dp)
            )
        }

            // 🔵 OPTIONAL: Progress ring
            Canvas(modifier = Modifier.size(220.dp)) {
                drawArc(
                    color = Color(0xFF4C4CFF),
                    startAngle = -90f,
                    sweepAngle = 250f,
                    useCenter = false,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
            }

            // 🎵 ROTATING ALBUM ART
            Image(
                painter = painterResource(id = albumImage),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .rotate(rotationAngle),
                contentScale = ContentScale.Crop
            )
        }
}
