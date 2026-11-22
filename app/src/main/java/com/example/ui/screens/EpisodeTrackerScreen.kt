package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.model.EpisodeHistoryEntity
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

@Composable
fun EpisodeTrackerScreen(
    anime: AnimeSeries?,
    episodes: List<EpisodeHistoryEntity>,
    watchingSeries: List<AnimeSeries>,
    onBack: () -> Unit,
    onSelectAnime: (String) -> Unit,
    onSelectSeason: (String, Int) -> Unit,
    onSetManualProgress: (String, Int) -> Unit,
    onReset: (String) -> Unit,
    onMarkNext: (String) -> Unit,
    onUndoLast: (String) -> Unit,
    onToggleEpisode: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (anime == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularBackButton(onClick = onBack)
                Text(
                    text = stringResource(R.string.tracker_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tracker_empty_series),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    var showManualDialog by remember { mutableStateOf(false) }
    var manualInput by remember(anime.watchedEpisodes) { mutableStateOf(anime.watchedEpisodes.toString()) }
    var inputError by remember { mutableStateOf<String?>(null) }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(stringResource(R.string.tracker_update_manually)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { raw ->
                            manualInput = raw.filter { it.isDigit() }
                            inputError = null
                        },
                        label = {
                            Text(stringResource(R.string.episodes_watched_label, anime.totalEpisodes?.toString() ?: "∞"))
                        },
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = manualInput.toIntOrNull()
                        val maxEp = anime.totalEpisodes
                        when {
                            parsed == null || parsed < 0 -> inputError = "Enter a valid non-negative number"
                            maxEp != null && parsed > maxEp -> inputError = "Cannot exceed $maxEp"
                            else -> {
                                onSetManualProgress(anime.id, parsed)
                                showManualDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val remainingLabel = anime.totalEpisodes?.let { total ->
        "${(total - anime.watchedEpisodes).coerceAtLeast(0)} left"
    } ?: "∞"
    val canMarkNext = anime.totalEpisodes == null || anime.watchedEpisodes < anime.totalEpisodes!!

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularBackButton(onClick = onBack)
                    Text(
                        text = stringResource(R.string.tracker_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = { onReset(anime.id) },
                    modifier = Modifier.testTag("tracker_reset_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (watchingSeries.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(watchingSeries, key = { it.id }) { series ->
                        FilterChip(
                            selected = series.id == anime.id,
                            onClick = { onSelectAnime(series.id) },
                            label = { Text(series.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (anime.seasonsCount > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items((1..anime.seasonsCount).toList()) { seasonNum ->
                        FilterChip(
                            selected = anime.currentSeason == seasonNum,
                            onClick = { onSelectSeason(anime.id, seasonNum) },
                            label = { Text("Season $seasonNum") }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimePosterImage(
                                    remoteUrl = anime.remotePosterUrl,
                                    fallbackUrls = anime.coverFallbackUrls,
                                    title = anime.title,
                                    contentDescription = anime.title,
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 98.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = anime.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${anime.format} · Season ${anime.currentSeason} · ${anime.episodeCountLabel}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${anime.watchedEpisodes} Watched · $remainingLabel (${(anime.progressFraction * 100).toInt()}%)",
                                        style = AniTrackMonoTypography.monoMd,
                                        color = AniTrackTheme.extended.primaryText
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { anime.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                                strokeCap = StrokeCap.Round
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onMarkNext(anime.id) },
                                    enabled = canMarkNext,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("tracker_mark_next_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (canMarkNext) {
                                            stringResource(R.string.tracker_mark_next)
                                        } else {
                                            stringResource(R.string.tracker_all_watched)
                                        }
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onUndoLast(anime.id) },
                                    enabled = anime.watchedEpisodes > 0,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("tracker_undo_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last", modifier = Modifier.size(18.dp))
                                }
                                OutlinedButton(
                                    onClick = { showManualDialog = true },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("tracker_manual_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Set episode manually", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.tracker_episodes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(episodes, key = { it.id }) { ep ->
                    EpisodeRowItem(
                        episode = ep,
                        onToggle = { onToggleEpisode(anime.id, ep.episodeNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: EpisodeHistoryEntity,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onToggle)
            .testTag("episode_row_${episode.episodeNumber}"),
        shape = MaterialTheme.shapes.large,
        color = if (episode.watched) AniTrackTheme.extended.surfaceContainerLow else AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (episode.watched) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else AniTrackTheme.extended.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (episode.watched) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else AniTrackTheme.extended.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", episode.episodeNumber),
                    style = AniTrackMonoTypography.monoMd,
                    color = if (episode.watched) AniTrackTheme.extended.primaryText else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (episode.subtitle == "Up Next") AniTrackTheme.extended.primaryText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (episode.watched) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (episode.watched) MaterialTheme.colorScheme.primary else AniTrackTheme.extended.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (episode.watched) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Watched",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
