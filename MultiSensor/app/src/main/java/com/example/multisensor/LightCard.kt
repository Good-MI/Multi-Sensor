package com.example.multisensor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun LightCard(
    lux: Float,
    isDarkMode: Boolean,
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
                    "Light Sensor: ${String.format(Locale.US, "%.1f", lux)} Lux, Active mode: ${if (isDarkMode) "Dark" else "Light"}"
                } else {
                    "Light Sensor: Unavailable"
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "ENVIRONMENT // LIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isAvailable) {
                Text(
                    text = "UNAVAILABLE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", lux)} LX",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp, 24.dp)
                            .background(primaryColor)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isDarkMode) "THRESHOLD: DARK" else "THRESHOLD: LIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Preview(name = "LightCard Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LightCardPreviewDark() {
    MultiSensorTheme(darkTheme = true) {
        LightCard(
            lux = 150.0f,
            isDarkMode = false,
            isAvailable = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
