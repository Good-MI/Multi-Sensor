package com.example.multisensor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MultiSensorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MultiSensorTheme(darkTheme = uiState.isDarkMode) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        uiState = uiState,
                        onArenaSizeChanged = viewModel::onArenaSizeChanged,
                        onThemeModeSelected = viewModel::selectThemeMode,
                        onToggleMute = viewModel::toggleMute
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Requirement C: Explicit SensorController lifecycle registration
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Requirement C: Explicit SensorController lifecycle unregistration
        viewModel.onPause()
    }
}

@Composable
fun MainScreen(
    uiState: UiState,
    onArenaSizeChanged: (Float, Float, Float) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val outlineColor = MaterialTheme.colorScheme.outline

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Row 1: Title and Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MULTI SENSOR",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (uiState.isMuted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
                            )
                            .border(1.dp, if (!uiState.isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .clickable { onToggleMute() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .semantics {
                                contentDescription = if (uiState.isMuted) "Unmute audio and haptics" else "Mute audio and haptics"
                            }
                    ) {
                        Text(
                            text = if (uiState.isMuted) "MUTED" else "SOUND",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!uiState.isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                }
                
                // Row 2: Theme Selector Segmented Control
                ThemeModeSelector(
                    selectedMode = uiState.themeMode,
                    onModeSelected = onThemeModeSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ball Arena (Motion - Accelerometer)
            BallArena(
                ballOffset = uiState.ballOffset,
                isAvailable = uiState.sensorAvailability.hasAccelerometer,
                onSizeChanged = onArenaSizeChanged
            )

            // Sensor Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LightCard(
                    lux = uiState.lux,
                    isDarkMode = uiState.isDarkMode,
                    isAvailable = uiState.sensorAvailability.hasLightSensor,
                    modifier = Modifier.weight(1f)
                )

                CompassCard(
                    headingDegrees = uiState.headingDegrees,
                    cardinal = uiState.cardinal,
                    isAvailable = uiState.sensorAvailability.hasMagnetometer &&
                            uiState.sensorAvailability.hasAccelerometer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer: Raw Accelerometer Data
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .border(1.dp, outlineColor, MaterialTheme.shapes.medium)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Raw accelerometer telemetry"
                    }
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RAW ACCELEROMETER TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "X: %+06.2f   Y: %+06.2f   Z: %+06.2f",
                                uiState.rawAccelerometer.x,
                                uiState.rawAccelerometer.y,
                                uiState.rawAccelerometer.z
                            ),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    // Mini waveform graphic
                    androidx.compose.foundation.Canvas(modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path()
                        val w = size.width
                        val h = size.height
                        path.moveTo(0f, h / 2f)
                        path.quadraticTo(w * 0.1f, h * 0.1f, w * 0.2f, h / 2f)
                        path.quadraticTo(w * 0.3f, h * 0.9f, w * 0.4f, h / 2f)
                        path.quadraticTo(w * 0.5f, h * 0.2f, w * 0.6f, h / 2f)
                        path.quadraticTo(w * 0.7f, h * 0.8f, w * 0.8f, h / 2f)
                        path.lineTo(w, h / 2f)
                        
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(ThemeMode.AUTO, ThemeMode.LIGHT, ThemeMode.DARK)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Preview(name = "MainScreen Light", showBackground = true)
@Composable
private fun MainScreenPreviewLight() {
    MultiSensorTheme(darkTheme = false) {
        MainScreen(
            uiState = UiState(
                isDarkMode = false,
                headingDegrees = 45f,
                cardinal = "NE",
                lux = 120f,
                sensorAvailability = SensorAvailability(
                    hasAccelerometer = true,
                    hasMagnetometer = true,
                    hasLightSensor = true
                )
            ),
            onArenaSizeChanged = { _, _, _ -> },
            onThemeModeSelected = {},
            onToggleMute = {}
        )
    }
}

@Preview(name = "MainScreen Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MainScreenPreviewDark() {
    MultiSensorTheme(darkTheme = true) {
        MainScreen(
            uiState = UiState(
                isDarkMode = true,
                headingDegrees = 270f,
                cardinal = "W",
                lux = 10f,
                sensorAvailability = SensorAvailability(
                    hasAccelerometer = true,
                    hasMagnetometer = true,
                    hasLightSensor = true
                )
            ),
            onArenaSizeChanged = { _, _, _ -> },
            onThemeModeSelected = {},
            onToggleMute = {}
        )
    }
}
