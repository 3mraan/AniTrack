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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.model.WatchStatus
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularIconButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    uiState: AniTrackUiState,
    onOpenCalendar: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEpisodeTracker: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRandomPicker: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onIncrementEpisode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryAnime = uiState.allAnime.filter { it.inLibrary }
    val watchingList = libraryAnime.filter { it.status == WatchStatus.WATCHING }
    val completedCount = libraryAnime.count { it.status == WatchStatus.COMPLETED }
    val plannedCount = libraryAnime.count { it.status == WatchStatus.PLANNED }
    val totalEpisodesWatched = uiState.allAnime.sumOf { it.watchedEpisodes }
    val recentAnime = libraryAnime.sortedByDescending { it.updatedAt }.take(3)
    val recommendedAnime = uiState.allAnime
        .filter { !it.inLibrary || it.status == WatchStatus.PLANNED }
        .ifEmpty { uiState.allAnime }
        .sortedByDescending { it.communityRating }
        .take(3)

    val firstName = uiState.profile.displayName.substringBefore(" ").ifBlank { "Ren" }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.greeting_evening, firstName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularIconButton(
                            icon = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(R.string.calendar_title),
                            onClick = onOpenCalendar,
                            modifier = Modifier.testTag("home_calendar_button")
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenProfile)
                                .testTag("home_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarImage(
                                contentDescription = stringResource(R.string.profile_title),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.your_journey),
                        actionLabel = stringResource(R.string.full_stats),
                        onAction = onOpenStats
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JourneyStatCard(
                            label = stringResource(R.string.status_watching),
                            value = watchingList.size.toString(),
                            icon = Icons.Filled.PlayArrow,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        JourneyStatCard(
                            label = stringResource(R.string.status_completed),
                            value = completedCount.toString(),
                            icon = Icons.Filled.CheckCircle,
                            accent = AniTrackTheme.extended.success,
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JourneyStatCard(
                            label = stringResource(R.string.status_planned),
                            value = plannedCount.toString(),
                            icon = Icons.Filled.Schedule,
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        JourneyStatCard(
                            label = stringResource(R.string.episodes_label),
                            value = String.format(Locale.US, "%,d", totalEpisodesWatched),
                            icon = Icons.Filled.Star,
                            accent = AniTrackTheme.extended.warning,
                            onClick = onOpenStats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = stringResource(R.string.quick_add_anime),
                        icon = Icons.Filled.Add,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_anime")
                    )
                    QuickActionTile(
                        label = stringResource(R.string.quick_update_ep),
                        icon = Icons.Filled.Update,
                        accent = AniTrackTheme.extended.success,
                        onClick = {
                            val targetId = watchingList.firstOrNull()?.id
                                ?: libraryAnime.firstOrNull()?.id
                            if (targetId != null) {
                                onOpenEpisodeTracker(targetId)
                            } else {
                                onOpenSearch()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_update_ep")
                    )
                    QuickActionTile(
                        label = stringResource(R.string.quick_browse),
                        icon = Icons.Filled.Explore,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenDiscover,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_browse")
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.continue_watching),
                        actionLabel = stringResource(R.string.track_all),
                        onAction = {
                            val targetId = watchingList.firstOrNull()?.id
                                ?: libraryAnime.firstOrNull()?.id
                                ?: uiState.allAnime.firstOrNull()?.id
                                ?: "solo_leveling"
                            onOpenEpisodeTracker(targetId)
                        }
                    )
                    if (watchingList.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .clickable(onClick = onOpenDiscover)
                                .testTag("empty_watching_cta")
                        ) {
                            Text(
                                text = stringResource(R.string.empty_watching_cta),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(watchingList, key = { it.id }) { anime ->
                                ContinueWatchingCard(
                                    anime = anime,
                                    onOpenDetails = { onOpenDetails(anime.id) },
                                    onIncrement = { onIncrementEpisode(anime.id) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.recently_added),
                        actionLabel = stringResource(R.string.see_library),
                        onAction = onOpenLibrary
                    )
                    if (recentAnime.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .clickable(onClick = onOpenSearch)
                        ) {
                            Text(
                                text = stringResource(R.string.empty_recent_cta),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            recentAnime.forEach { anime ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onOpenDetails(anime.id) }
                                        .testTag("recent_card_${anime.id}"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(3f / 4f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .border(1.dp, AniTrackTheme.extended.outline, MaterialTheme.shapes.medium)
                                    ) {
                                        AnimePosterImage(
                                            remoteUrl = anime.remotePosterUrl,
                                            fallbackUrls = anime.coverFallbackUrls,
                                            title = anime.title,
                                            contentDescription = anime.title,
                                            modifier = Modifier.fillMaxSize(),
                                            withBottomScrim = true
                                        )
                                        StarRatingBadge(
                                            rating = anime.communityRating,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                        )
                                    }
                                    Text(
                                        text = anime.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            repeat((3 - recentAnime.size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.recommended_for_you),
                        actionLabel = stringResource(R.string.pick_random),
                        leadingIcon = Icons.Filled.Casino,
                        onAction = onOpenRandomPicker
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recommendedAnime.forEach { anime ->
                            RecommendedSeriesCard(
                                anime = anime,
                                onClick = { onOpenDetails(anime.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onAction) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = AniTrackTheme.extended.primaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = AniTrackTheme.extended.primaryText
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AniTrackTheme.extended.primaryText,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun JourneyStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = AniTrackMonoTypography.monoLg.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    anime: AnimeSeries,
    onOpenDetails: () -> Unit,
    onIncrement: () -> Unit
) {
    val canIncrement = anime.totalEpisodes == null || anime.watchedEpisodes < anime.totalEpisodes!!
    val epDenominator = anime.totalEpisodes?.toString() ?: "?"

    Surface(
        modifier = Modifier
            .width(276.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenDetails),
        shape = MaterialTheme.shapes.large,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimePosterImage(
                    remoteUrl = anime.remotePosterUrl,
                    fallbackUrls = anime.coverFallbackUrls,
                    title = anime.title,
                    contentDescription = anime.title,
                    modifier = Modifier
                        .size(width = 62.dp, height = 84.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.ep_progress_format, anime.watchedEpisodes, epDenominator),
                        style = AniTrackMonoTypography.monoMd,
                        color = AniTrackTheme.extended.primaryText
                    )
                    LinearProgressIndicator(
                        progress = { anime.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            Button(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("increment_ep_${anime.id}"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (canIncrement) stringResource(R.string.plus_one_episode) else stringResource(R.string.season_complete),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RecommendedSeriesCard(
    anime: AnimeSeries,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("recommended_card_${anime.id}"),
        shape = MaterialTheme.shapes.large,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimePosterImage(
                remoteUrl = anime.remotePosterUrl,
                fallbackUrls = anime.coverFallbackUrls,
                title = anime.title,
                contentDescription = anime.title,
                modifier = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.genres.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${anime.format} · ${anime.episodeCountLabel}",
                        style = AniTrackMonoTypography.monoSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StarRatingBadge(rating = anime.communityRating)
                }
            }
            Icon(
                imageVector = Icons.Filled.BookmarkAdd,
                contentDescription = anime.title,
                tint = AniTrackTheme.extended.primaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
