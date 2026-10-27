package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CompassCalibration
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ThemeMode
import com.example.ui.AeroPulseUiState
import com.example.ui.AeroPulseViewModel
import com.example.ui.NavigationTab
import com.example.ui.screens.ActivityWindowsScreen
import com.example.ui.screens.InstrumentSensorsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsAndSecurityScreen
import com.example.ui.screens.StationsDirectoryScreen
import com.example.ui.screens.TelemetryDashboardScreen
import com.example.ui.theme.AeroPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: AeroPulseViewModel = viewModel(
                factory = AeroPulseViewModel.provideFactory(context)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val useDarkTheme = when (uiState.preferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(uiState.preferences.hasCompletedOnboarding, uiState.preferences.autoSyncOnLaunch) {
                if (uiState.preferences.hasCompletedOnboarding && uiState.preferences.autoSyncOnLaunch) {
                    viewModel.syncLiveTelemetry()
                }
            }

            AeroPulseTheme(darkTheme = useDarkTheme) {
                AeroPulseAppRoot(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun AeroPulseAppRoot(
    uiState: AeroPulseUiState,
    viewModel: AeroPulseViewModel
) {
    if (!uiState.preferences.hasCompletedOnboarding) {
        OnboardingScreen(
            onCompleteOnboarding = viewModel::completeOnboarding
        )
        return
    }

    if (uiState.currentTab != NavigationTab.TELEMETRY) {
        BackHandler {
            viewModel.selectTab(NavigationTab.TELEMETRY)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val selected = uiState.currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val selected = uiState.currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name}")
                            )
                        }
                    }
                }

                when (uiState.currentTab) {
                    NavigationTab.TELEMETRY -> TelemetryDashboardScreen(
                        uiState = uiState,
                        onSyncLiveTelemetry = viewModel::syncLiveTelemetry,
                        onSelectStation = viewModel::selectPrimaryStation,
                        onSelectChartMetric = viewModel::selectChartMetric,
                        onSaveFieldNotes = viewModel::saveStationNotes,
                        onDismissBanner = viewModel::clearBanner,
                        onNavigateToWindows = { viewModel.selectTab(NavigationTab.WINDOWS) },
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTab.WINDOWS -> ActivityWindowsScreen(
                        uiState = uiState,
                        onSelectProfile = viewModel::selectActivityProfile,
                        onCreateCustomProfile = viewModel::validateAndSaveActivityProfile,
                        onDeleteProfile = viewModel::deleteActivityProfile,
                        onClearValidationError = viewModel::clearValidationError,
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTab.STATIONS -> StationsDirectoryScreen(
                        uiState = uiState,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                        onSortOptionChange = viewModel::updateSortOption,
                        onSelectPrimaryStation = viewModel::selectPrimaryStation,
                        onToggleStationFavorite = viewModel::toggleStationFavorite,
                        onDeleteStation = viewModel::deleteStation,
                        onSearchRemoteLocations = viewModel::searchRemoteLocations,
                        onAddStationFromCandidate = viewModel::addStationFromCandidate,
                        onValidateAndAddManualStation = viewModel::validateAndAddManualStation,
                        onPopulateFromGps = viewModel::populateCoordinatesFromGps,
                        onClearValidationError = viewModel::clearValidationError,
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTab.INSTRUMENT -> InstrumentSensorsScreen(
                        uiState = uiState,
                        onCalibrateFromActiveStation = viewModel::calibrateSeaLevelPressureFromStation,
                        onAdjustSeaLevelReference = viewModel::adjustSeaLevelReferenceHpa,
                        onRecordBarometerSnapshot = viewModel::recordBarometerLog,
                        onClearBarometerLogs = viewModel::clearBarometerLogs,
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTab.SETTINGS -> SettingsAndSecurityScreen(
                        uiState = uiState,
                        onSelectTempUnit = viewModel::setTemperatureUnit,
                        onSelectWindUnit = viewModel::setWindSpeedUnit,
                        onSelectPressureUnit = viewModel::setPressureUnit,
                        onSelectThemeMode = viewModel::setThemeMode,
                        onToggleAutoSync = viewModel::setAutoSyncOnLaunch,
                        onReopenOnboarding = viewModel::reopenOnboarding,
                        onResetAllData = viewModel::resetAllData,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: NavigationTab, selected: Boolean): ImageVector = when (tab) {
    NavigationTab.TELEMETRY -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    NavigationTab.WINDOWS -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
    NavigationTab.STATIONS -> if (selected) Icons.Filled.Public else Icons.Outlined.Public
    NavigationTab.INSTRUMENT -> if (selected) Icons.Filled.CompassCalibration else Icons.Outlined.CompassCalibration
    NavigationTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
