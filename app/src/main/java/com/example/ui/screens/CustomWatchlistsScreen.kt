package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.isValidHttpUrl
import com.example.ui.components.resolveWatchlistCoverAnime
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

@Composable
fun CustomWatchlistsScreen(
    uiState: AniTrackUiState,
    onBack: () -> Unit,
    onCreateWatchlist: () -> Unit,
    onEditWatchlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
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
                        text = stringResource(R.string.watchlists_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = onCreateWatchlist,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("create_watchlist_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_create))
                }
            }

            val animeById = remember(uiState.allAnime) { uiState.allAnime.associateBy { it.id } }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.watchlists.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.watchlists_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
                items(uiState.watchlists, key = { it.id }) { wl ->
                    val coverAnime = remember(wl.coverKey, wl.animeIds, animeById) {
                        resolveWatchlistCoverAnime(wl.coverKey, wl.animeIds, animeById)
                    }
                    val coverRemoteUrl = coverAnime?.remotePosterUrl
                        ?: wl.coverKey.takeIf { isValidHttpUrl(it) }.orEmpty()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onEditWatchlist(wl.id) }
                            .testTag("watchlist_card_${wl.id}"),
                        shape = MaterialTheme.shapes.large,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimePosterImage(
                                remoteUrl = coverRemoteUrl,
                                fallbackUrls = coverAnime?.coverFallbackUrls ?: emptyList(),
                                title = coverAnime?.title ?: wl.name,
                                contentDescription = wl.name,
                                showTitleOnPlaceholder = false,
                                allowManualRetry = true,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 84.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = wl.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (wl.description.isNotBlank()) {
                                    Text(
                                        text = wl.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${wl.animeIds.size} anime",
                                        style = AniTrackMonoTypography.monoSm,
                                        color = AniTrackTheme.extended.primaryText
                                    )
                                    Text(
                                        text = wl.updatedDateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open watchlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
