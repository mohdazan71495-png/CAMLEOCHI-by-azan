package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryEmerald

private data class OnboardingPage(
    val badge: String,
    val title: String,
    val description: String,
    val highlights: List<String>,
    val icon: ImageVector
)

private val onboardingPages = listOf(
    OnboardingPage(
        badge = "STAGE 01 // ATMOSPHERIC TELEMETRY",
        title = "Hyper-Local Microclimate & Solar Intelligence",
        description = "Monitor 24-hour temperature curves, surface & sea-level barometric pressure, dew point, wind gusts, and US AQI across alpine and urban field stations.",
        highlights = listOf(
            "Offline-first Room SQLite persistence with instant station switching",
            "Live HTTPS sync with Open-Meteo Forecast, Air Quality & Geocoding APIs",
            "Golden Hour, Blue Hour, and Solar Noon horizon calculations"
        ),
        icon = Icons.Default.WbSunny
    ),
    OnboardingPage(
        badge = "STAGE 02 // ACTIVITY WINDOW ENGINE",
        title = "Go / Caution / Hold Readiness Scoring",
        description = "Evaluate weather envelopes against custom thresholds for Trail Running, Deep-Sky Astrophotography, Aerial Drone Flights, and Endurance Cycling.",
        highlights = listOf(
            "0–100 composite readiness score with limiting factor diagnostics",
            "Hour-by-hour optimal window finder across the next 24 hours",
            "Create and validate custom activity safety rules"
        ),
        icon = Icons.Default.Radar
    ),
    OnboardingPage(
        badge = "STAGE 03 // HARDWARE ALTIMETER DECK",
        title = "On-Device Barometer & QNH Calibration",
        description = "Read live pressure, ambient light, and humidity sensors directly from your Android hardware and calibrate sea-level QNH against known station elevations.",
        highlights = listOf(
            "International barometric formula altitude computation",
            "Persistent barometer snapshot log stored locally on-device",
            "Zero tracking, zero cloud accounts, complete local privacy"
        ),
        icon = Icons.Default.CompassCalibration
    )
)

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val page = onboardingPages[currentStep]

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header with Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AEROPULSE // v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    TextButton(
                        onClick = onCompleteOnboarding,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip Intro")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Visual Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ObsidianNight
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_atmosphere),
                            contentDescription = "AeroPulse atmospheric observatory illustration",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricCyan.copy(alpha = 0.25f), SolarAmber.copy(alpha = 0.25f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = page.title,
                                tint = SolarAmber,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedContent(
                    targetState = page,
                    label = "onboarding_page_transition"
                ) { targetPage ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = targetPage.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = SolarAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetPage.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = targetPage.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        targetPage.highlights.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TelemetryEmerald)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step Indicators + Navigation Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        onboardingPages.indices.forEach { idx ->
                            val isSelected = idx == currentStep
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .size(width = if (isSelected) 28.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) ElectricCyan
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("onboarding_back_button")
                            ) {
                                Text("Back")
                            }
                        }
                        Button(
                            onClick = {
                                if (currentStep < onboardingPages.lastIndex) {
                                    currentStep++
                                } else {
                                    onCompleteOnboarding()
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(52.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = if (currentStep < onboardingPages.lastIndex) "Next Stage" else "Launch Field Station"
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}
