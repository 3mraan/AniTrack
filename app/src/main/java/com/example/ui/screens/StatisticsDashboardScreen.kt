package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.components.watchStatusColor
import com.example.ui.components.watchStatusLabel
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme
import java.util.Locale

@Composable
fun StatisticsDashboardScreen(
    uiState: AniTrackUiState,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryAnime = remember(uiState.allAnime) { uiState.allAnime.filter { it.inLibrary } }
    val totalEpisodes = remember(libraryAnime) { libraryAnime.sumOf { it.watchedEpisodes } }
    val totalHours = remember(libraryAnime) {
        libraryAnime.sumOf { it.watchedEpisodes * it.episodeDurationMinutes } / 60
    }
    val completedCount = remember(libraryAnime) { libraryAnime.count { it.status == WatchStatus.COMPLETED } }
    val watchingCount = remember(libraryAnime) { libraryAnime.count { it.status == WatchStatus.WATCHING } }
    val ratedAnime = remember(libraryAnime) { libraryAnime.mapNotNull { it.userRating } }
    val avgRating = remember(ratedAnime) {
        if (ratedAnime.isEmpty()) 0f else ratedAnime.average().toFloat()
    }

    val statusCounts = remember(libraryAnime) {
        WatchStatus.entries.map { status -> status to libraryAnime.count { it.status == status } }
    }
    val totalStatusCount = statusCounts.sumOf { it.second }.coerceAtLeast(1)

    val topGenres = remember(libraryAnime) {
        val counts = mutableMapOf<String, Int>()
        libraryAnime.forEach { anime ->
            anime.genres.forEach { g -> counts[g] = (counts[g] ?: 0) + 1 }
        }
        val maxVal = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
        counts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { Triple(it.key, it.value, it.value.toFloat() / maxVal.toFloat()) }
    }

    val highestRated = remember(libraryAnime) {
        libraryAnime.maxByOrNull { it.userRating ?: it.communityRating }
    }
    val mostWatched = remember(libraryAnime) {
        libraryAnime.filter { it.watchedEpisodes > 0 }.maxByOrNull { it.watchedEpisodes }
    }

    val monthlyData = uiState.monthlyEpisodes
    val maxMonthly = (monthlyData.maxOfOrNull { it.episodes } ?: 1).coerceAtLeast(1)
    val peakPoint = monthlyData.maxByOrNull { it.episodes }

    val ratingBuckets = uiState.ratingDistribution
    val maxRatingBucket = (ratingBuckets.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.stats_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stats_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = AniTrackTheme.extended.primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$totalHours hrs",
                                style = AniTrackMonoTypography.monoMd,
                                color = AniTrackTheme.extended.primaryText
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CoreMetricCard(
                            stringResource(R.string.stats_total_anime),
                            libraryAnime.size.toString(),
                            Modifier.weight(1f)
                        )
                        CoreMetricCard(
                            stringResource(R.string.status_completed),
                            completedCount.toString(),
                            Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CoreMetricCard(
                            stringResource(R.string.status_watching),
                            watchingCount.toString(),
                            Modifier.weight(1f)
                        )
                        CoreMetricCard(
                            stringResource(R.string.stats_episodes_watched),
                            String.format(Locale.US, "%,d", totalEpisodes),
                            Modifier.weight(1f)
                        )
                    }
                    CoreMetricCard(
                        label = stringResource(R.string.stats_average_rating, ratedAnime.size),
                        value = if (ratedAnime.isEmpty()) "—" else String.format(Locale.US, "%.1f ★", avgRating),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                        Text(stringResource(R.string.stats_status_distribution), style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(AniTrackTheme.extended.surfaceContainerHigh),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            statusCounts.forEach { (status, count) ->
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(count.toFloat() / totalStatusCount.toFloat())
                                            .fillMaxHeight()
                                            .background(watchStatusColor(status))
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            statusCounts.forEach { (status, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(watchStatusColor(status))
                                        )
                                        Text(watchStatusLabel(status), style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        text = count.toString(),
                                        style = AniTrackMonoTypography.monoMd,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Activity (Computed from EpisodeHistoryEntity)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.stats_monthly_activity), style = MaterialTheme.typography.titleLarge)
                            if (peakPoint != null && peakPoint.episodes > 0) {
                                Text(
                                    text = "${peakPoint.month} (${peakPoint.episodes})",
                                    style = AniTrackMonoTypography.monoSm,
                                    color = AniTrackTheme.extended.primaryText
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthlyData.forEach { point ->
                                val barFraction = (point.episodes.toFloat() / maxMonthly.toFloat()).coerceIn(0.1f, 1f)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = point.episodes.toString(),
                                        style = AniTrackMonoTypography.monoSm,
                                        color = if (point.isHighlight) AniTrackTheme.extended.primaryText
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .height((110 * barFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                if (point.isHighlight) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = point.month,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rating Distribution (Computed from ReviewEntity)
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.stats_rating_distribution), style = MaterialTheme.typography.titleLarge)
                        ratingBuckets.forEach { bucket ->
                            val fraction = (bucket.count.toFloat() / maxRatingBucket.toFloat()).coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${bucket.scoreBucket} ★",
                                    style = AniTrackMonoTypography.monoSm,
                                    color = AniTrackTheme.extended.warning,
                                    modifier = Modifier.widthIn(min = 38.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = AniTrackTheme.extended.warning,
                                    trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = bucket.count.toString(),
                                    style = AniTrackMonoTypography.monoSm,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (topGenres.isNotEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.stats_favorite_genres), style = MaterialTheme.typography.titleLarge)
                            topGenres.forEach { (genre, count, fraction) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(genre, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = "$count",
                                            style = AniTrackMonoTypography.monoSm,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (highestRated != null || mostWatched != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.stats_highlights), style = MaterialTheme.typography.titleLarge)
                        highestRated?.let { anime ->
                            HighlightSeriesCard(
                                label = stringResource(R.string.stats_highest_rated),
                                metric = String.format(Locale.US, "%.1f ★", anime.userRating ?: anime.communityRating),
                                icon = Icons.Filled.EmojiEvents,
                                accent = AniTrackTheme.extended.warning,
                                anime = anime,
                                onClick = { onOpenDetails(anime.id) }
                            )
                        }
                        mostWatched?.let { anime ->
                            HighlightSeriesCard(
                                label = stringResource(R.string.stats_most_watched),
                                metric = "${anime.watchedEpisodes} eps",
                                icon = Icons.Filled.LocalFireDepartment,
                                accent = MaterialTheme.colorScheme.primary,
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
private fun CoreMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = AniTrackMonoTypography.monoLg.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HighlightSeriesCard(
    label: String,
    metric: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    anime: AnimeSeries,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("stats_highlight_${anime.id}"),
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
                showTitleOnPlaceholder = false,
                modifier = Modifier
                    .size(width = 52.dp, height = 72.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
                }
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(text = metric, style = AniTrackMonoTypography.monoLg)
        }
    }
}
