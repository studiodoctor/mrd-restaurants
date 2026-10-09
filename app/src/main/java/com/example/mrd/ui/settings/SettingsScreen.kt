package com.example.mrd.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mrd.BuildConfig
import com.example.mrd.R
import com.example.mrd.data.remote.SimulatedNetwork
import com.example.mrd.domain.ThemeMode
import com.example.mrd.ui.theme.ThemeViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onFavoritesClick: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val networkMode by settingsViewModel.networkMode.collectAsStateWithLifecycle()
    SettingsScreen(
        themeMode = themeMode,
        onThemeModeSelected = themeViewModel::setThemeMode,
        onBack = onBack,
        onFavoritesClick = onFavoritesClick,
        networkMode = networkMode,
        onNetworkModeSelected = settingsViewModel::setNetworkMode,
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onFavoritesClick: () -> Unit,
    networkMode: SimulatedNetwork,
    onNetworkModeSelected: (SimulatedNetwork) -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
            }

            SectionTitle(stringResource(R.string.settings_section_you))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onFavoritesClick)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(text = "❤️", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_favorites),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_favorites_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeRow(
                            mode = mode,
                            selected = mode == themeMode,
                            onSelect = { onThemeModeSelected(mode) },
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                SectionTitle(stringResource(R.string.debug_menu))
                SettingsCard {
                    Column(Modifier.selectableGroup()) {
                        SimulatedNetwork.entries.forEach { mode ->
                            OptionRow(
                                emoji = when (mode) {
                                    SimulatedNetwork.ONLINE -> "📶"
                                    SimulatedNetwork.OFFLINE -> "✈️"
                                    SimulatedNetwork.SERVER_ERROR -> "🛠️"
                                },
                                label = stringResource(
                                    when (mode) {
                                        SimulatedNetwork.ONLINE -> R.string.debug_network_online
                                        SimulatedNetwork.OFFLINE -> R.string.debug_network_offline
                                        SimulatedNetwork.SERVER_ERROR -> R.string.debug_network_error
                                    },
                                ),
                                selected = mode == networkMode,
                                onSelect = { onNetworkModeSelected(mode) },
                            )
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 1.dp,
        content = content,
    )
}

@Composable
private fun ThemeRow(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit) {
    val (emoji, label) = when (mode) {
        ThemeMode.SYSTEM -> "📱" to stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> "☀️" to stringResource(R.string.theme_light)
        ThemeMode.DARK -> "🌙" to stringResource(R.string.theme_dark)
    }
    OptionRow(emoji, label, selected, onSelect)
}

@Composable
private fun OptionRow(emoji: String, label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.size(28.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // The row is the click target; the radio is purely visual (onClick = null).
        RadioButton(selected = selected, onClick = null)
    }
}
