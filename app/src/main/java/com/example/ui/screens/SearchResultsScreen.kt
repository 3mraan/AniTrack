package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.util.AnimeSearchMatcher
import com.example.ui.AniTrackUiState
import com.example.ui.SearchUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

private val FormatOptions = listOf("All", "TV", "MOVIE", "OVA")

@Composable
fun SearchResultsScreen(
    uiState: AniTrackUiState,
    searchState: SearchUiState,
    initialGenre: String?,
    isExpandedWidth: Boolean,
    onBack: () -> Unit,
    onPerformSearch: (query: String, genre: String, year2020Plus: Boolean, format: String, sortByRating: Boolean, page: Int) -> Unit,
    onClearRemoteSearch: () -> Unit,
    onAddRecentSearch: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGenre by rememberSaveable { mutableStateOf(initialGenre ?: "All") }
    var year2020Plus by rememberSaveable { mutableStateOf(false) }
    var selectedFormat by rememberSaveable { mutableStateOf("All") }
    var sortByRating by rememberSaveable { mutableStateOf(true) }
    var genreMenuExpanded by remember { mutableStateOf(false) }
    var formatMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialGenre) {
        if (!initialGenre.isNullOrBlank()) {
            onPerformSearch(query, initialGenre, year2020Plus, selectedFormat, sortByRating, 1)
        }
    }

    val availableGenres = remember(uiState.allAnime) {
        (listOf("All", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sports", "Supernatural") +
            uiState.allAnime.flatMap { it.genres }).distinct()
    }

    val fuzzyLocalOutput = remember(uiState.allAnime, query, selectedGenre, year2020Plus, selectedFormat, sortByRating) {
        val preFiltered = uiState.allAnime.filter { anime ->
            val matchesGenre = selectedGenre == "All" ||
                anime.genres.any { it.equals(selectedGenre, ignoreCase = true) }
            val matchesYear = !year2020Plus || anime.year >= 2020
            val matchesFormat = selectedFormat == "All" || anime.format.equals(selectedFormat, ignoreCase = true)
            matchesGenre && matchesYear && matchesFormat
        }
        val ranked = AnimeSearchMatcher.rankAndFilter(query, preFiltered)
        if (query.isBlank()) {
            ranked.copy(
                items = if (sortByRating) ranked.items.sortedByDescending { it.communityRating }
                else ranked.items.sortedBy { it.title }
            )
        } else {
            ranked
        }
    }

    val localResults = fuzzyLocalOutput.items
    val displayedResults = if (searchState.hasActiveRemoteSearch && searchState.errorMessage == null && searchState.remoteItems.isNotEmpty()) {
        searchState.remoteItems
    } else {
        localResults
    }

    val columns = if (isExpandedWidth) 3 else 2

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularBackButton(onClick = onBack)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input_field"),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AniTrackTheme.extended.primaryText
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                onClearRemoteSearch()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) onAddRecentSearch(query)
                            onPerformSearch(query, selectedGenre, year2020Plus, selectedFormat, sortByRating, 1)
                        }
                    ),
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        focusedContainerColor = AniTrackTheme.extended.surfaceContainer,
                        unfocusedContainerColor = AniTrackTheme.extended.surfaceContainer
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter & Sort Chips + Live AniList Search trigger
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = searchState.hasActiveRemoteSearch,
                                onClick = {
                                    if (query.isNotBlank()) onAddRecentSearch(query)
                                    onPerformSearch(query, selectedGenre, year2020Plus, selectedFormat, sortByRating, 1)
                                },
                                label = { Text(stringResource(R.string.search_anilist_chip)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        item {
                            Box {
                                FilterChip(
                                    selected = selectedGenre != "All",
                                    onClick = { genreMenuExpanded = true },
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.genre_prefix,
                                                if (selectedGenre == "All") stringResource(R.string.action_all) else selectedGenre
                                            )
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = genreMenuExpanded,
                                    onDismissRequest = { genreMenuExpanded = false }
                                ) {
                                    availableGenres.forEach { genre ->
                                        DropdownMenuItem(
                                            text = { Text(if (genre == "All") stringResource(R.string.action_all) else genre) },
                                            onClick = {
                                                selectedGenre = genre
                                                genreMenuExpanded = false
                                                onPerformSearch(query, genre, year2020Plus, selectedFormat, sortByRating, 1)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Box {
                                FilterChip(
                                    selected = selectedFormat != "All",
                                    onClick = { formatMenuExpanded = true },
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.format_prefix,
                                                if (selectedFormat == "All") stringResource(R.string.action_all) else selectedFormat
                                            )
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = formatMenuExpanded,
                                    onDismissRequest = { formatMenuExpanded = false }
                                ) {
                                    FormatOptions.forEach { fmt ->
                                        DropdownMenuItem(
                                            text = { Text(if (fmt == "All") stringResource(R.string.action_all) else fmt) },
                                            onClick = {
                                                selectedFormat = fmt
                                                formatMenuExpanded = false
                                                onPerformSearch(query, selectedGenre, year2020Plus, fmt, sortByRating, 1)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            FilterChip(
                                selected = year2020Plus,
                                onClick = {
                                    year2020Plus = !year2020Plus
                                    onPerformSearch(query, selectedGenre, year2020Plus, selectedFormat, sortByRating, 1)
                                },
                                label = { Text("2020+") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortByRating,
                                onClick = { sortByRating = !sortByRating },
                                label = {
                                    Text(
                                        if (sortByRating) stringResource(R.string.sort_rating_chip)
                                        else stringResource(R.string.sort_popular_chip)
                                    )
                                }
                            )
                        }
                    }
                }

                // Smart Typo / Arabic Match Hint Banner
                val fuzzyHint = fuzzyLocalOutput.resolvedQueryHint
                if (query.isNotBlank() && fuzzyHint != null && localResults.isNotEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fuzzy_match_hint_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = null,
                                    tint = AniTrackTheme.extended.primaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.fuzzy_match_hint, query.trim(), fuzzyHint),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AniTrackTheme.extended.primaryText
                                )
                            }
                        }
                    }
                }

                // Recent Searches
                if (uiState.settings.recentSearches.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.recent_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(uiState.settings.recentSearches) { recent ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable {
                                            query = recent
                                            onPerformSearch(recent, selectedGenre, year2020Plus, selectedFormat, sortByRating, 1)
                                        },
                                    shape = RoundedCornerShape(999.dp),
                                    color = AniTrackTheme.extended.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                                ) {
                                    Text(
                                        text = recent,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline/API error banner with cached fallback notice
                if (searchState.errorMessage != null) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.error_search_title),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.cached_results_fallback,
                                            localResults.size,
                                            searchState.errorMessage
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        onPerformSearch(query, selectedGenre, year2020Plus, selectedFormat, sortByRating, 1)
                                    }
                                ) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                }

                if (searchState.isLoading) {
                    item { LoadingSkeletonGrid(columns = columns) }
                } else if (displayedResults.isEmpty()) {
                    item {
                        SearchStatusCard(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.empty_search_title),
                            subtitle = stringResource(R.string.empty_search_subtitle),
                            actionLabel = stringResource(R.string.reset_filters),
                            onAction = {
                                query = ""
                                selectedGenre = "All"
                                selectedFormat = "All"
                                year2020Plus = false
                                onClearRemoteSearch()
                            }
                        )
                    }
                } else {
                    val chunked = displayedResults.chunked(columns)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { anime ->
                                SearchAnimeCard(
                                    anime = anime,
                                    onClick = {
                                        if (query.isNotBlank()) onAddRecentSearch(query)
                                        onOpenDetails(anime.id)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (searchState.hasNextPage) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        onPerformSearch(
                                            query,
                                            selectedGenre,
                                            year2020Plus,
                                            selectedFormat,
                                            sortByRating,
                                            searchState.currentPage + 1
                                        )
                                    },
                                    enabled = !searchState.isLoadingMore,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("search_load_more_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (searchState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(stringResource(R.string.load_more_page, searchState.currentPage + 1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAnimeCard(
    anime: AnimeSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("search_result_${anime.id}"),
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
                StarRatingBadge(
                    rating = anime.communityRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
                if (anime.inLibrary) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        StatusPill(status = anime.status)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
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
                    text = "${anime.year} · ${anime.studio}",
                    style = AniTrackMonoTypography.monoSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LoadingSkeletonGrid(columns: Int) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(columns) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        color = AniTrackTheme.extended.surfaceContainer.copy(alpha = alpha),
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(AniTrackTheme.extended.surfaceContainerHigh)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AniTrackTheme.extended.surfaceContainerHigh)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = AniTrackTheme.extended.surfaceContainer,
        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AniTrackTheme.extended.primaryText,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAction,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}
