package com.voxmind.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.PurpleAccent
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isListening: Boolean,
    rmsLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        if (!isListening) {
            // Calm subtle line
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        val baseAmplitude = if (rmsLevel <= 0.01f) (height * 0.03f) else (rmsLevel.coerceIn(0f, 1f) * height * 0.45f)

        // Draw primary wave
        val path1 = Path()
        val path2 = Path()

        val step = 4f
        var x = 0f

        path1.moveTo(0f, centerY)
        path2.moveTo(0f, centerY)

        while (x <= width) {
            val normalX = x / width
            // Windowing function to taper at edges
            val window = sin(normalX * PI.toFloat())

            val y1 = centerY + (sin((normalX * 4f * PI.toFloat()) + phase) * baseAmplitude * window)
            val y2 = centerY + (sin((normalX * 6f * PI.toFloat()) - phase) * (baseAmplitude * 0.7f) * window)

            if (x == 0f) {
                path1.moveTo(x, y1)
                path2.moveTo(x, y2)
            } else {
                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
            }
            x += step
        }

        // Draw first glowing wave (Indigo -> Cyan)
        drawPath(
            path = path1,
            brush = Brush.horizontalGradient(
                colors = listOf(IndigoPrimary, CyanAccent, PurpleAccent)
            ),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw second lighter harmonic wave
        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(
                colors = listOf(PurpleAccent.copy(alpha = 0.6f), IndigoPrimary.copy(alpha = 0.6f))
            ),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
