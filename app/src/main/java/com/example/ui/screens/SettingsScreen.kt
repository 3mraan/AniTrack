package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AniTrackUiState
import com.example.ui.components.CircularBackButton
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

private val DefaultSortChoices = listOf("Recently Updated", "Highest Rated", "Title A-Z", "Progress %")

private data class LanguageOption(val tag: String, val nativeName: String)

private val SupportedLanguageOptions = listOf(
    LanguageOption("", ""), // System default resolved via stringResource
    LanguageOption("en", "English"),
    LanguageOption("ar", "العربية (Arabic)"),
    LanguageOption("es", "Español (Spanish)"),
    LanguageOption("fr", "Français (French)"),
    LanguageOption("de", "Deutsch (German)"),
    LanguageOption("ja", "日本語 (Japanese)"),
    LanguageOption("tr", "Türkçe (Turkish)"),
    LanguageOption("pt", "Português (Portuguese)"),
    LanguageOption("zh", "简体中文 (Chinese)")
)

@Composable
fun SettingsScreen(
    uiState: AniTrackUiState,
    onBack: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetEpisodeReminders: (Boolean) -> Unit,
    onSetReleaseNotifications: (Boolean) -> Unit,
    onSetDefaultSort: (String) -> Unit,
    onSetDefaultViewGrid: (Boolean) -> Unit,
    onSetAutoStatusSuggestions: (Boolean) -> Unit,
    onExportLibrary: ((String) -> Unit) -> Unit,
    onImportLibrary: (String) -> Unit,
    onSyncBackup: () -> Unit,
    onSelectLanguageTag: (String) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onSignOut: () -> Unit = {},
    userEmail: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = uiState.settings

    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }

    val systemLanguageLabel = stringResource(R.string.settings_language_system)
    val currentLanguageDisplay = SupportedLanguageOptions
        .firstOrNull { it.tag == settings.languageTag }
        ?.let { if (it.tag.isEmpty()) systemLanguageLabel else it.nativeName }
        ?: systemLanguageLabel

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(stringResource(R.string.settings_import)) },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    placeholder = { Text("JSON…") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportLibrary(importJsonInput)
                        showImportDialog = false
                        importJsonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.settings_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(infoDialogTitle!!) },
            text = {
                Text(
                    text = infoDialogBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularBackButton(onClick = onBack)
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Appearance
                item {
                    SettingsSectionCard(title = stringResource(R.string.settings_appearance)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(AniTrackTheme.extended.surfaceContainerHigh)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ThemeChoiceChip(
                                    label = stringResource(R.string.settings_theme_dark),
                                    icon = Icons.Filled.DarkMode,
                                    selected = settings.isDarkTheme,
                                    onClick = { onSetDarkTheme(true) },
                                    modifier = Modifier.testTag("theme_dark_button")
                                )
                                ThemeChoiceChip(
                                    label = stringResource(R.string.settings_theme_light),
                                    icon = Icons.Filled.LightMode,
                                    selected = !settings.isDarkTheme,
                                    onClick = { onSetDarkTheme(false) },
                                    modifier = Modifier.testTag("theme_light_button")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "Electric Violet",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniTrackTheme.extended.primaryText
                                )
                            }
                        }
                    }
                }

                // Language Selection (System Default on 1st Launch + Manual Switch)
                item {
                    SettingsSectionCard(title = stringResource(R.string.settings_language)) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { languageDropdownExpanded = true }
                                    .padding(vertical = 4.dp)
                                    .testTag("settings_language_picker"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = null,
                                        tint = AniTrackTheme.extended.primaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.settings_language_label),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = currentLanguageDisplay,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AniTrackTheme.extended.primaryText
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = languageDropdownExpanded,
                                onDismissRequest = { languageDropdownExpanded = false }
                            ) {
                                SupportedLanguageOptions.forEach { option ->
                                    val label = if (option.tag.isEmpty()) systemLanguageLabel else option.nativeName
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onSelectLanguageTag(option.tag)
                                            languageDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Notifications
                item {
                    SettingsSectionCard(title = stringResource(R.string.settings_notifications)) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_episode_reminders),
                            subtitle = stringResource(R.string.settings_episode_reminders_sub),
                            checked = settings.episodeReminders,
                            onCheckedChange = onSetEpisodeReminders
                        )
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_release_notifications),
                            subtitle = stringResource(R.string.settings_release_notifications_sub),
                            checked = settings.releaseNotifications,
                            onCheckedChange = onSetReleaseNotifications
                        )
                    }
                }

                // Library Preferences
                item {
                    SettingsSectionCard(title = stringResource(R.string.library_title)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sortDropdownExpanded = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_default_sorting), style = MaterialTheme.typography.titleMedium)
                            Box {
                                Text(
                                    text = sortOptionLabel(settings.defaultSort),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniTrackTheme.extended.primaryText
                                )
                                DropdownMenu(
                                    expanded = sortDropdownExpanded,
                                    onDismissRequest = { sortDropdownExpanded = false }
                                ) {
                                    DefaultSortChoices.forEach { choice ->
                                        DropdownMenuItem(
                                            text = { Text(sortOptionLabel(choice)) },
                                            onClick = {
                                                onSetDefaultSort(choice)
                                                sortDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_default_view), style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(AniTrackTheme.extended.surfaceContainerHigh)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ViewModePill(
                                    label = stringResource(R.string.settings_poster_grid),
                                    selected = settings.defaultViewGrid,
                                    onClick = { onSetDefaultViewGrid(true) }
                                )
                                ViewModePill(
                                    label = stringResource(R.string.settings_list_view),
                                    selected = !settings.defaultViewGrid,
                                    onClick = { onSetDefaultViewGrid(false) }
                                )
                            }
                        }

                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_auto_status),
                            subtitle = stringResource(R.string.settings_auto_status_sub),
                            checked = settings.autoStatusSuggestions,
                            onCheckedChange = onSetAutoStatusSuggestions
                        )
                    }
                }

                // Data & Backup
                item {
                    SettingsSectionCard(title = stringResource(R.string.settings_data)) {
                        SettingsActionRow(
                            title = stringResource(R.string.settings_export),
                            subtitle = stringResource(R.string.settings_export_sub),
                            icon = Icons.Filled.Upload,
                            onClick = {
                                onExportLibrary { jsonPayload ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "AniTrack Library Backup")
                                        putExtra(Intent.EXTRA_TEXT, jsonPayload)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export AniTrack Library"))
                                }
                            }
                        )
                        SettingsActionRow(
                            title = stringResource(R.string.settings_import),
                            subtitle = stringResource(R.string.settings_import_sub),
                            icon = Icons.Filled.Download,
                            onClick = { showImportDialog = true }
                        )
                        SettingsActionRow(
                            title = stringResource(R.string.settings_backup),
                            subtitle = settings.lastBackupLabel,
                            icon = Icons.Filled.CloudSync,
                            onClick = onSyncBackup
                        )
                    }
                }

                // Account & Quick Tutorial
                item {
                    SettingsSectionCard(title = stringResource(R.string.settings_account)) {
                        SettingsActionRow(
                            title = stringResource(R.string.tutorial_replay_title),
                            subtitle = stringResource(R.string.tutorial_replay_subtitle),
                            icon = Icons.Filled.HelpOutline,
                            onClick = onReplayTutorial,
                            modifier = Modifier.testTag("settings_replay_tutorial")
                        )
                        SettingsActionRow(
                            title = stringResource(R.string.auth_sign_out),
                            subtitle = userEmail ?: stringResource(R.string.auth_sign_out_subtitle),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = onSignOut,
                            modifier = Modifier.testTag("settings_sign_out")
                        )
                    }
                }

                // About
                item {
                    val privacyTitle = stringResource(R.string.settings_privacy)
                    val termsTitle = stringResource(R.string.settings_terms)
                    val aboutTitle = stringResource(R.string.settings_about_app)
                    val tagline = stringResource(R.string.about_tagline)
                    SettingsSectionCard(title = stringResource(R.string.settings_about)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_app_version), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "v2.5.0",
                                style = AniTrackMonoTypography.monoMd,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SettingsActionRow(
                            title = privacyTitle,
                            subtitle = stringResource(R.string.auth_feature_cloud_sync),
                            icon = Icons.Filled.Info,
                            onClick = {
                                infoDialogTitle = privacyTitle
                                infoDialogBody = "AniTrack stores your watchlist, episode progress, ratings, and preferences on-device with Room and syncs securely with your authenticated Firebase Firestore account."
                            }
                        )
                        SettingsActionRow(
                            title = termsTitle,
                            subtitle = tagline,
                            icon = Icons.Filled.Info,
                            onClick = {
                                infoDialogTitle = termsTitle
                                infoDialogBody = "AniTrack is a personal anime tracking companion. All user reviews and watchlists remain under your control and can be exported at any time."
                            }
                        )
                        SettingsActionRow(
                            title = aboutTitle,
                            subtitle = tagline,
                            icon = Icons.Filled.Info,
                            onClick = {
                                infoDialogTitle = aboutTitle
                                infoDialogBody = "AniTrack v2.5.0 — $tagline"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AniTrackTheme.extended.surfaceContainer,
            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = AniTrackTheme.extended.primaryText, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeChoiceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else AniTrackTheme.extended.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ViewModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else AniTrackTheme.extended.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
