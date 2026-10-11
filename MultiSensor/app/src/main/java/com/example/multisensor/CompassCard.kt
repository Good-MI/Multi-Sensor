package com.example.multisensor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CompassCard(
    headingDegrees: Float,
    cardinal: String,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .border(1.dp, outlineColor, MaterialTheme.shapes.medium)
            .padding(16.dp)
            .semantics {
                contentDescription = if (isAvailable) {
                    "Compass Sensor: ${headingDegrees.roundToInt()} degrees $cardinal"
                } else {
                    "Compass Sensor: Unavailable"
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "POSITION // COMPASS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isAvailable) {
                Text(
                    text = "UNAVAILABLE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Dial ring
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.8f),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    // Rotating needle (points to magnetic North)
                    rotate(degrees = -headingDegrees, pivot = center) {
                        val needleWidth = 6.dp.toPx()
                        val northPointerHeight = radius * 0.85f
                        val southPointerHeight = radius * 0.55f

                        // North needle (Solid Cyan)
                        val northPath = Path().apply {
                            moveTo(center.x, center.y - northPointerHeight)
                            lineTo(center.x - needleWidth, center.y)
                            lineTo(center.x + needleWidth, center.y)
                            close()
                        }
                        drawPath(northPath, color = primaryColor)

                        // South needle (Outline)
                        val southPath = Path().apply {
                            moveTo(center.x, center.y + southPointerHeight)
                            lineTo(center.x - needleWidth, center.y)
                            lineTo(center.x + needleWidth, center.y)
                            close()
                        }
                        drawPath(
                            southPath,
                            color = outlineColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${headingDegrees.roundToInt()}° $cardinal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Preview(name = "CompassCard Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompassCardPreviewDark() {
    MultiSensorTheme(darkTheme = true) {
        CompassCard(
            headingDegrees = 180f,
            cardinal = "S",
            isAvailable = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
