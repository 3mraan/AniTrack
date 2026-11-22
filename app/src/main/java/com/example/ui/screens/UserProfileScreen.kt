package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WatchStatus
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularIconButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    uiState: AniTrackUiState,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWatchlists: () -> Unit,
    onOpenWatchlistEdit: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    val libraryAnime = remember(uiState.allAnime) { uiState.allAnime.filter { it.inLibrary } }
    val completedCount = remember(libraryAnime) { libraryAnime.count { it.status == WatchStatus.COMPLETED } }
    val totalEpisodes = remember(uiState.allAnime) { uiState.allAnime.sumOf { it.watchedEpisodes } }
    val ratedScores = remember(libraryAnime) { libraryAnime.mapNotNull { it.userRating } }
    val meanScore = remember(ratedScores) {
        if (ratedScores.isEmpty()) 0f else ratedScores.average().toFloat()
    }
    val favoriteAnime = remember(uiState.allAnime) {
        uiState.allAnime.filter { it.isFavorite }.take(3)
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var editBio by remember(profile.bio) { mutableStateOf(profile.bio) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text(stringResource(R.string.profile_edit), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.profile_display_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text(stringResource(R.string.profile_bio)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(editName, editBio)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
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
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = profile.handle,
                            style = AniTrackMonoTypography.monoMd,
                            color = AniTrackTheme.extended.primaryText
                        )
                    }
                    CircularIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("profile_settings_button")
                    )
                }
            }

            // Identity Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = AniTrackTheme.extended.surfaceContainer,
                    border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarImage(
                                contentDescription = profile.displayName,
                                modifier = Modifier.size(68.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = profile.bio.ifBlank { stringResource(R.string.about_tagline) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatColumn(stringResource(R.string.status_completed), completedCount.toString())
                            ProfileStatColumn(stringResource(R.string.episodes_label), String.format(Locale.US, "%,d", totalEpisodes))
                            ProfileStatColumn(stringResource(R.string.reviews_overall), String.format(Locale.US, "%.1f", meanScore))
                        }

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("edit_profile_button"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.profile_edit))
                        }
                    }
                }
            }

            // Favorite Genres
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.stats_favorite_genres),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.favoriteGenres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AniTrackTheme.extended.primaryText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Favorite Anime 3-col Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.profile_favorite_anime),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onOpenLibrary) {
                            Text(stringResource(R.string.profile_view_all), color = AniTrackTheme.extended.primaryText)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        favoriteAnime.forEach { anime ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onOpenDetails(anime.id) },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .clip(MaterialTheme.shapes.medium)
                                ) {
                                    AnimePosterImage(
                                        remoteUrl = anime.remotePosterUrl,
                                        fallbackUrls = anime.coverFallbackUrls,
                                        title = anime.title,
                                        contentDescription = anime.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    StarRatingBadge(
                                        rating = anime.userRating ?: anime.communityRating,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    )
                                }
                                Text(
                                    text = anime.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat((3 - favoriteAnime.size).coerceAtLeast(0)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Personal Collections
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.profile_collections),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onOpenWatchlists,
                            modifier = Modifier.testTag("profile_manage_watchlists")
                        ) {
                            Text(stringResource(R.string.profile_manage), color = AniTrackTheme.extended.primaryText)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.watchlists.take(3).forEach { wl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable { onOpenWatchlistEdit(wl.id) },
                                shape = MaterialTheme.shapes.large,
                                color = AniTrackTheme.extended.surfaceContainer,
                                border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(wl.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${wl.animeIds.size}",
                                            style = AniTrackMonoTypography.monoSm,
                                            color = AniTrackTheme.extended.primaryText
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = wl.name,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ProfileStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AniTrackMonoTypography.monoLg.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
