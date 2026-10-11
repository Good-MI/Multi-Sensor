package com.example.multisensor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BallArena(
    ballOffset: Offset,
    isAvailable: Boolean,
    onSizeChanged: (width: Float, height: Float, density: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val outlineColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, outlineColor, MaterialTheme.shapes.large)
            .onSizeChanged { size ->
                onSizeChanged(size.width.toFloat(), size.height.toFloat(), density.density)
            }
            .semantics {
                contentDescription = if (isAvailable) {
                    "Ball Arena: Motion accelerometer canvas"
                } else {
                    "Ball Arena: Accelerometer unavailable"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isAvailable) {
            Text(
                text = "ACCELEROMETER UNAVAILABLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = outlineColor.copy(alpha = 0.5f)

                // Subtle crosshair grid
                drawLine(
                    color = gridColor,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )

                // Ball
                val radiusPx = MultiSensorViewModel.BALL_RADIUS_DP * density.density
                val center = if (ballOffset == Offset.Zero) {
                    Offset(size.width / 2f, size.height / 2f)
                } else {
                    ballOffset
                }

                // Inner core
                drawCircle(
                    color = primaryColor,
                    radius = radiusPx,
                    center = center
                )
                
                // Outer glow
                val glowRadius = radiusPx * 2.5f
                val glowBrush = ShaderBrush(
                    RadialGradientShader(
                        center = center,
                        radius = glowRadius,
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                        colorStops = listOf(0f, 1f)
                    )
                )
                drawCircle(
                    brush = glowBrush,
                    radius = glowRadius,
                    center = center
                )
            }
        }
    }
}

@Preview(name = "BallArena Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BallArenaPreviewDark() {
    MultiSensorTheme(darkTheme = true) {
        BallArena(
            ballOffset = Offset(150f, 150f),
            isAvailable = true,
            onSizeChanged = { _, _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}
