package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistWithItems
import com.example.ui.components.AnimeBannerImage
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.CircularIconButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.watchStatusLabel
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailsScreen(
    anime: AnimeSeries?,
    allAnime: List<AnimeSeries>,
    watchlists: List<WatchlistWithItems>,
    onSyncDetails: (String) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleInLibrary: (String) -> Unit,
    onUpdateStatus: (String, WatchStatus) -> Unit,
    onUpdatePersonalNotes: (String, String) -> Unit,
    onOpenEpisodeTracker: (String) -> Unit,
    onOpenRateReview: (String) -> Unit,
    onOpenGenreSearch: (String) -> Unit,
    onAddToWatchlist: (String, String) -> Unit,
    onOpenAnimeDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (anime == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.loading_series), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LaunchedEffect(anime.id) {
        onSyncDetails(anime.id)
    }

    var statusMenuOpen by remember { mutableStateOf(false) }
    var showWatchlistPicker by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesDraft by remember(anime.personalNotes) { mutableStateOf(anime.personalNotes) }
    var spoilersRevealed by rememberSaveable(anime.id) { mutableStateOf(false) }

    val similar = remember(anime, allAnime) {
        allAnime.filter { other ->
            other.id != anime.id && other.genres.any { it in anime.genres }
        }.take(6)
    }

    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(stringResource(R.string.personal_notes_label)) },
            text = {
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    placeholder = { Text(stringResource(R.string.details_no_notes)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePersonalNotes(anime.id, notesDraft)
                        showNotesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showWatchlistPicker) {
        AlertDialog(
            onDismissRequest = { showWatchlistPicker = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(stringResource(R.string.watchlists_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                if (watchlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.watchlists_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        watchlists.forEach { wl ->
                            val alreadyIn = anime.id in wl.animeIds
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onAddToWatchlist(wl.id, anime.id)
                                        showWatchlistPicker = false
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = AniTrackTheme.extended.surfaceContainerHigh,
                                border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(wl.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${wl.animeIds.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (alreadyIn) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AniTrackTheme.extended.success
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWatchlistPicker = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    val totalLabel = anime.totalEpisodes?.toString() ?: "?"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    AnimeBannerImage(
                        bannerUrl = anime.bannerUrl,
                        coverUrl = anime.remotePosterUrl,
                        fallbackCoverUrls = anime.coverFallbackUrls,
                        title = anime.title,
                        contentDescription = "${anime.title} banner",
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x880B0B12),
                                        Color(0x440B0B12),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularBackButton(
                            onClick = onBack,
                            containerColor = Color(0xAA12121C),
                            contentColor = Color.White
                        )
                        CircularIconButton(
                            icon = if (anime.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            onClick = { onToggleFavorite(anime.id) },
                            containerColor = Color(0xAA12121C),
                            contentColor = if (anime.isFavorite) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.testTag("details_favorite_button")
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-28).dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AnimePosterImage(
                        remoteUrl = anime.remotePosterUrl,
                        fallbackUrls = anime.coverFallbackUrls,
                        title = anime.title,
                        contentDescription = anime.title,
                        allowManualRetry = true,
                        modifier = Modifier
                            .size(width = 112.dp, height = 152.dp)
                            .clip(MaterialTheme.shapes.large)
                            .border(2.dp, AniTrackTheme.extended.outlineVariant, MaterialTheme.shapes.large)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box {
                            StatusPill(
                                status = anime.status,
                                onClick = { statusMenuOpen = true },
                                modifier = Modifier.testTag("details_status_pill")
                            )
                            DropdownMenu(
                                expanded = statusMenuOpen,
                                onDismissRequest = { statusMenuOpen = false }
                            ) {
                                WatchStatus.entries.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(watchStatusLabel(st)) },
                                        onClick = {
                                            onUpdateStatus(anime.id, st)
                                            statusMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = anime.romajiTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${anime.year} · ${anime.format} · ${anime.episodeCountLabel}",
                            style = AniTrackMonoTypography.monoSm,
                            color = AniTrackTheme.extended.primaryText
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.details_community),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = AniTrackTheme.extended.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = String.format("%.1f", anime.communityRating),
                                    style = AniTrackMonoTypography.monoLg,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onOpenRateReview(anime.id) }
                            .testTag("details_your_rating_card"),
                        shape = MaterialTheme.shapes.large,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.details_your_rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTrackTheme.extended.primaryText
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = anime.userRating?.let { String.format("%.1f", it) }
                                        ?: stringResource(R.string.details_tap_to_rate),
                                    style = AniTrackMonoTypography.monoLg,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Genres & Tags
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    anime.genres.forEach { genre ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onOpenGenreSearch(genre) },
                            shape = RoundedCornerShape(999.dp),
                            color = AniTrackTheme.extended.surfaceContainerHigh,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    anime.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = AniTrackTheme.extended.primaryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onOpenEpisodeTracker(anime.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("details_update_progress_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.details_update_progress, anime.watchedEpisodes, totalLabel))
                    }
                    OutlinedButton(
                        onClick = { onToggleInLibrary(anime.id) },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("details_library_toggle_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Icon(
                            imageVector = if (anime.inLibrary) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkBorder,
                            contentDescription = if (anime.inLibrary) "In Library" else "Add to Library",
                            tint = if (anime.inLibrary) AniTrackTheme.extended.success else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = { showWatchlistPicker = true },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("details_add_watchlist_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = "Add to Watchlist",
                            tint = AniTrackTheme.extended.primaryText
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    shape = MaterialTheme.shapes.large,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.details_synopsis), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = anime.synopsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.details_information), style = MaterialTheme.typography.titleLarge)
                        InfoRow(stringResource(R.string.details_studio), anime.studio)
                        InfoRow(stringResource(R.string.details_source), anime.sourceMaterial)
                        InfoRow(
                            stringResource(R.string.details_duration),
                            stringResource(R.string.details_min_per_ep, anime.episodeDurationMinutes)
                        )
                        InfoRow(stringResource(R.string.details_seasons), "${anime.seasonsCount}")
                    }
                }
            }

            // Personal Notes Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    shape = MaterialTheme.shapes.large,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.personal_notes_label), style = MaterialTheme.typography.titleLarge)
                            TextButton(
                                onClick = { showNotesDialog = true },
                                modifier = Modifier.testTag("details_edit_notes_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (anime.personalNotes.isBlank()) stringResource(R.string.details_add_note)
                                    else stringResource(R.string.details_edit_note)
                                )
                            }
                        }
                        Text(
                            text = anime.personalNotes.ifBlank { stringResource(R.string.details_no_notes) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (anime.personalNotes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Personal Review Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.details_personal_review), style = MaterialTheme.typography.titleLarge)
                            TextButton(
                                onClick = { onOpenRateReview(anime.id) },
                                modifier = Modifier.testTag("details_edit_review_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AniTrackTheme.extended.primaryText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.details_edit_review), color = AniTrackTheme.extended.primaryText)
                            }
                        }

                        if (anime.personalReview.isBlank()) {
                            Text(
                                text = stringResource(R.string.details_no_review),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (anime.containsSpoilers && !spoilersRevealed) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = AniTrackTheme.extended.surfaceContainerHigh,
                                border = BorderStroke(1.dp, AniTrackTheme.extended.warning.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.details_spoiler_warning),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AniTrackTheme.extended.warning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { spoilersRevealed = true }) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.details_reveal))
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = anime.personalReview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (similar.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.details_similar_anime), style = MaterialTheme.typography.titleLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(similar, key = { it.id }) { item ->
                                Column(
                                    modifier = Modifier
                                        .width(116.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onOpenAnimeDetails(item.id) },
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(3f / 4f)
                                            .clip(MaterialTheme.shapes.medium)
                                    ) {
                                        AnimePosterImage(
                                            remoteUrl = item.remotePosterUrl,
                                            fallbackUrls = item.coverFallbackUrls,
                                            title = item.title,
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        StarRatingBadge(
                                            rating = item.communityRating,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
