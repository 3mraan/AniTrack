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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.model.WatchStatus
import com.example.data.util.AnimeSearchMatcher
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularIconButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.watchStatusColor
import com.example.ui.components.watchStatusLabel
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

private val SortOptions = listOf("Recently Updated", "Highest Rated", "Title A-Z", "Progress %")

@Composable
fun sortOptionLabel(sortKey: String): String {
    return when (sortKey) {
        "Highest Rated" -> stringResource(R.string.sort_highest_rated)
        "Title A-Z" -> stringResource(R.string.sort_title_az)
        "Progress %" -> stringResource(R.string.sort_progress)
        else -> stringResource(R.string.sort_recent)
    }
}

@Composable
fun AnimeLibraryScreen(
    uiState: AniTrackUiState,
    isExpandedWidth: Boolean,
    onOpenSearch: () -> Unit,
    onOpenWatchlists: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onIncrementEpisode: (String) -> Unit,
    onUpdateTrackingInfo: (String, WatchStatus, Int, String) -> Unit,
    onRemoveFromLibrary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var libraryQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf<WatchStatus?>(null) }
    var selectedSort by rememberSaveable { mutableStateOf(uiState.settings.defaultSort) }
    var selectedGenre by rememberSaveable { mutableStateOf("All Genres") }
    var isGridView by rememberSaveable { mutableStateOf(uiState.settings.defaultViewGrid) }
    var editingAnime by remember { mutableStateOf<AnimeSeries?>(null) }

    LaunchedEffect(uiState.settings.defaultSort, uiState.settings.defaultViewGrid) {
        selectedSort = uiState.settings.defaultSort
        isGridView = uiState.settings.defaultViewGrid
    }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var genreMenuExpanded by remember { mutableStateOf(false) }

    val libraryAnime = remember(uiState.allAnime) {
        uiState.allAnime.filter { it.inLibrary }
    }
    val allGenres = remember(libraryAnime) {
        listOf("All Genres") + libraryAnime.flatMap { it.genres }.distinct().sorted()
    }

    val filtered = remember(libraryAnime, libraryQuery, selectedStatus, selectedSort, selectedGenre) {
        val queryFiltered = if (libraryQuery.isBlank()) {
            libraryAnime
        } else {
            AnimeSearchMatcher.rankAndFilter(libraryQuery, libraryAnime).items
        }
        queryFiltered
            .filter { selectedStatus == null || it.status == selectedStatus }
            .filter { selectedGenre == "All Genres" || it.genres.any { g -> g.equals(selectedGenre, ignoreCase = true) } }
            .let { list ->
                if (libraryQuery.isNotBlank()) {
                    list // preserve relevance ranking while typing a search query
                } else {
                    when (selectedSort) {
                        "Highest Rated" -> list.sortedByDescending { it.userRating ?: it.communityRating }
                        "Title A-Z" -> list.sortedBy { it.title }
                        "Progress %" -> list.sortedByDescending { it.progressFraction }
                        else -> list.sortedByDescending { it.updatedAt }
                    }
                }
            }
    }

    val gridColumns = if (isExpandedWidth) 3 else 2

    editingAnime?.let { target ->
        EditLibraryItemDialog(
            anime = target,
            onDismiss = { editingAnime = null },
            onSave = { status, epCount, notes ->
                onUpdateTrackingInfo(target.id, status, epCount, notes)
                editingAnime = null
            },
            onRemove = {
                onRemoveFromLibrary(target.id)
                editingAnime = null
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.library_series_count, libraryAnime.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.quick_add_anime),
                            onClick = onOpenSearch,
                            modifier = Modifier.testTag("library_search_button")
                        )
                        CircularIconButton(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = stringResource(R.string.watchlists_title),
                            onClick = onOpenWatchlists,
                            modifier = Modifier.testTag("library_watchlists_button")
                        )
                    }
                }
            }

            // Fuzzy Search within Library
            item {
                OutlinedTextField(
                    value = libraryQuery,
                    onValueChange = { libraryQuery = it },
                    placeholder = { Text(stringResource(R.string.library_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AniTrackTheme.extended.primaryText
                        )
                    },
                    trailingIcon = {
                        if (libraryQuery.isNotEmpty()) {
                            IconButton(onClick = { libraryQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_inline_search")
                )
            }

            // Status Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null },
                            label = { Text("${stringResource(R.string.action_all)} (${libraryAnime.size})") },
                            modifier = Modifier.testTag("filter_status_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(WatchStatus.entries) { status ->
                        val count = libraryAnime.count { it.status == status }
                        val selected = selectedStatus == status
                        FilterChip(
                            selected = selected,
                            onClick = { selectedStatus = if (selected) null else status },
                            label = { Text("${watchStatusLabel(status)} ($count)") },
                            modifier = Modifier.testTag("filter_status_${status.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Sort, Genre & View Toggle Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { sortMenuExpanded = true }
                                .testTag("library_sort_button"),
                            shape = MaterialTheme.shapes.medium,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sort,
                                    contentDescription = stringResource(R.string.settings_default_sorting),
                                    tint = AniTrackTheme.extended.primaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sortOptionLabel(selectedSort),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(sortOptionLabel(option)) },
                                    onClick = {
                                        selectedSort = option
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(0.9f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { genreMenuExpanded = true }
                                .testTag("library_genre_button"),
                            shape = MaterialTheme.shapes.medium,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(R.string.all_genres),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (selectedGenre == "All Genres") stringResource(R.string.all_genres) else selectedGenre,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = genreMenuExpanded,
                            onDismissRequest = { genreMenuExpanded = false }
                        ) {
                            allGenres.forEach { genre ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (genre == "All Genres") stringResource(R.string.all_genres) else genre)
                                    },
                                    onClick = {
                                        selectedGenre = genre
                                        genreMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { isGridView = !isGridView }
                            .testTag("library_view_toggle"),
                        shape = MaterialTheme.shapes.medium,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = if (isGridView) stringResource(R.string.settings_list_view) else stringResource(R.string.settings_poster_grid),
                                tint = AniTrackTheme.extended.primaryText
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
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
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = AniTrackTheme.extended.primaryText,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.empty_library_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.empty_library_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenDiscover,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("empty_library_discover_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.explore_discover))
                            }
                        }
                    }
                }
            } else if (isGridView) {
                val chunked = filtered.chunked(gridColumns)
                items(chunked) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { anime ->
                            LibraryPosterCard(
                                anime = anime,
                                onClick = { onOpenDetails(anime.id) },
                                onEdit = { editingAnime = anime },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(gridColumns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { anime ->
                    LibraryListRowCard(
                        anime = anime,
                        onClick = { onOpenDetails(anime.id) },
                        onEdit = { editingAnime = anime },
                        onIncrement = { onIncrementEpisode(anime.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPosterCard(
    anime: AnimeSeries,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLabel = anime.totalEpisodes?.toString() ?: "?"
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("library_grid_card_${anime.id}"),
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
                    withBottomScrim = true
                )
                StarRatingBadge(
                    rating = anime.userRating ?: anime.communityRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    StatusPill(status = anime.status)
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .testTag("library_edit_${anime.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                            .padding(5.dp)
                            .size(16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ep_progress_format, anime.watchedEpisodes, totalLabel),
                        style = AniTrackMonoTypography.monoSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(anime.progressFraction * 100).toInt()}%",
                        style = AniTrackMonoTypography.monoSm,
                        color = watchStatusColor(anime.status),
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { anime.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = watchStatusColor(anime.status),
                    trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LibraryListRowCard(
    anime: AnimeSeries,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onIncrement: () -> Unit
) {
    val totalLabel = anime.totalEpisodes?.toString() ?: "?"
    val canIncrement = anime.status == WatchStatus.WATCHING &&
        (anime.totalEpisodes == null || anime.watchedEpisodes < anime.totalEpisodes!!)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("library_list_card_${anime.id}"),
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
                    .size(width = 64.dp, height = 88.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StarRatingBadge(rating = anime.userRating ?: anime.communityRating)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(status = anime.status)
                    Text(
                        text = stringResource(R.string.ep_progress_format, anime.watchedEpisodes, totalLabel),
                        style = AniTrackMonoTypography.monoSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { anime.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = watchStatusColor(anime.status),
                    trackColor = AniTrackTheme.extended.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            if (canIncrement) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onIncrement),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.plus_one_episode),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditLibraryItemDialog(
    anime: AnimeSeries,
    onDismiss: () -> Unit,
    onSave: (WatchStatus, Int, String) -> Unit,
    onRemove: () -> Unit
) {
    var status by remember { mutableStateOf(anime.status) }
    var epText by remember { mutableStateOf(anime.watchedEpisodes.toString()) }
    var notes by remember { mutableStateOf(anime.personalNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AniTrackTheme.extended.surfaceContainer,
        title = {
            Text(
                stringResource(R.string.edit_anime_title, anime.title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WatchStatus.entries) { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(watchStatusLabel(st)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = epText,
                    onValueChange = { raw -> epText = raw.filter { it.isDigit() } },
                    label = {
                        Text(stringResource(R.string.episodes_watched_label, anime.totalEpisodes?.toString() ?: "∞"))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.personal_notes_label)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onRemove) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.remove_from_library), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = epText.toIntOrNull() ?: anime.watchedEpisodes
                    onSave(status, count, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
