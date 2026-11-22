package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularIconButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

private val DiscoverGenres = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Romance", "Mystery", "Horror", "Sports", "Supernatural"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    uiState: AniTrackUiState,
    isRefreshing: Boolean,
    errorMessage: String?,
    isExpandedWidth: Boolean,
    onRefresh: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenRandomPicker: () -> Unit,
    onOpenSearch: (String?) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trending = remember(uiState.allAnime) {
        uiState.allAnime
            .filter { it.trendingRank != null }
            .sortedBy { it.trendingRank }
            .ifEmpty { uiState.allAnime.take(8) }
    }
    val popular = remember(uiState.allAnime) {
        uiState.allAnime
            .sortedWith(compareBy<AnimeSeries> { it.popularityRank ?: Int.MAX_VALUE }.thenByDescending { it.communityRating })
            .take(8)
    }
    val topRated = remember(uiState.allAnime) {
        uiState.allAnime.sortedByDescending { it.communityRating }.take(8)
    }
    val seasonal = remember(uiState.allAnime) {
        uiState.allAnime.filter { it.seasonalTag != null }
            .ifEmpty { uiState.allAnime.take(4) }
    }
    val seasonalColumns = if (isExpandedWidth) 3 else 2

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.discover_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.discover_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isRefreshing) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            CircularIconButton(
                                icon = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_retry),
                                onClick = onRefresh,
                                modifier = Modifier.testTag("discover_refresh_button")
                            )
                        }
                        CircularIconButton(
                            icon = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(R.string.calendar_title),
                            onClick = onOpenCalendar,
                            modifier = Modifier.testTag("discover_calendar_button")
                        )
                        CircularIconButton(
                            icon = Icons.Filled.Casino,
                            contentDescription = stringResource(R.string.picker_title),
                            onClick = onOpenRandomPicker,
                            modifier = Modifier.testTag("discover_random_button")
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.warning.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = AniTrackTheme.extended.warning,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRefresh) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onOpenSearch(null) }
                        .testTag("discover_search_bar"),
                    shape = MaterialTheme.shapes.large,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AniTrackTheme.extended.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.discover_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.browse_by_genre),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiscoverGenres.forEach { genre ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onOpenSearch(genre) }
                                    .testTag("discover_genre_${genre.lowercase()}"),
                                shape = RoundedCornerShape(999.dp),
                                color = AniTrackTheme.extended.surfaceContainer,
                                border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.trending_now), style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(trending, key = { it.id }) { anime ->
                            DiscoverPosterCard(
                                anime = anime,
                                rankBadge = anime.trendingRank?.let { "#$it" },
                                onClick = { onOpenDetails(anime.id) }
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.popular_anime), style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(popular, key = { "pop_${it.id}" }) { anime ->
                            DiscoverPosterCard(
                                anime = anime,
                                rankBadge = null,
                                onClick = { onOpenDetails(anime.id) }
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.top_rated_classics), style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(topRated, key = { "top_${it.id}" }) { anime ->
                            DiscoverPosterCard(
                                anime = anime,
                                rankBadge = null,
                                onClick = { onOpenDetails(anime.id) }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.seasonal_upcoming), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOpenCalendar) {
                        Text(stringResource(R.string.calendar_label), color = AniTrackTheme.extended.primaryText)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AniTrackTheme.extended.primaryText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val chunkedSeasonal = seasonal.chunked(seasonalColumns)
            items(chunkedSeasonal) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { anime ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onOpenDetails(anime.id) }
                                .testTag("seasonal_card_${anime.id}"),
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                ) {
                                    AnimePosterImage(
                                        remoteUrl = anime.remotePosterUrl,
                                        fallbackUrls = anime.coverFallbackUrls,
                                        title = anime.title,
                                        contentDescription = anime.title,
                                        modifier = Modifier.fillMaxSize(),
                                        withBottomScrim = true,
                                        allowManualRetry = true
                                    )
                                    anime.seasonalTag?.let { tag ->
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    StarRatingBadge(
                                        rating = anime.communityRating,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = anime.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = anime.airingTimeLabel ?: "${anime.format} · ${anime.episodeCountLabel}",
                                        style = AniTrackMonoTypography.monoSm,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    repeat(seasonalColumns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverPosterCard(
    anime: AnimeSeries,
    rankBadge: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("discover_card_${anime.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, AniTrackTheme.extended.outline, MaterialTheme.shapes.large)
        ) {
            AnimePosterImage(
                remoteUrl = anime.remotePosterUrl,
                fallbackUrls = anime.coverFallbackUrls,
                title = anime.title,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                withBottomScrim = true,
                allowManualRetry = true
            )
            if (rankBadge != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = rankBadge,
                        style = AniTrackMonoTypography.monoSm,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            StarRatingBadge(
                rating = anime.communityRating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = anime.genres.take(2).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
