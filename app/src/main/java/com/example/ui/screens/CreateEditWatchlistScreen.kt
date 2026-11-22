package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.data.model.WatchlistWithItems
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.normalizeWatchlistCoverKey
import com.example.ui.theme.AniTrackTheme

private val DefaultCoverAnimeIds = listOf(
    "solo_leveling",
    "attack_on_titan",
    "jujutsu_kaisen",
    "vinland_saga"
)

@Composable
fun CreateEditWatchlistScreen(
    existingWatchlist: WatchlistWithItems?,
    allAnime: List<AnimeSeries>,
    onBack: () -> Unit,
    onSave: (id: String?, name: String, description: String, coverKey: String, animeIds: List<String>) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animeMap = remember(allAnime) { allAnime.associateBy { it.id } }
    val normalizedExistingCover = remember(existingWatchlist?.coverKey) {
        existingWatchlist?.coverKey?.let { normalizeWatchlistCoverKey(it) }
    }

    var name by rememberSaveable(existingWatchlist?.id) {
        mutableStateOf(existingWatchlist?.name ?: "")
    }
    var description by rememberSaveable(existingWatchlist?.id) {
        mutableStateOf(existingWatchlist?.description ?: "")
    }
    var coverKey by rememberSaveable(existingWatchlist?.id) {
        mutableStateOf(normalizedExistingCover ?: allAnime.firstOrNull()?.id ?: "solo_leveling")
    }
    val selectedAnimeIds = remember(existingWatchlist?.id) {
        mutableStateListOf<String>().apply {
            existingWatchlist?.animeIds?.let { addAll(it) }
        }
    }
    var showAddAnimeDialog by remember { mutableStateOf(false) }

    val coverOptions = remember(animeMap, allAnime, selectedAnimeIds.toList(), coverKey) {
        val candidateIds = (listOf(coverKey) + selectedAnimeIds + DefaultCoverAnimeIds).distinct()
        val prioritized = candidateIds.mapNotNull { animeMap[it] }.take(4)
        if (prioritized.size < 4) {
            (prioritized + allAnime.filter { it !in prioritized }).take(4)
        } else {
            prioritized
        }
    }

    if (showAddAnimeDialog) {
        val candidates = allAnime.filter { it.id !in selectedAnimeIds }
        AlertDialog(
            onDismissRequest = { showAddAnimeDialog = false },
            containerColor = AniTrackTheme.extended.surfaceContainer,
            title = { Text("Add Anime to Watchlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                if (candidates.isEmpty()) {
                    Text("All catalog series are already in this watchlist.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(candidates) { _, candidate ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        selectedAnimeIds.add(candidate.id)
                                        showAddAnimeDialog = false
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = AniTrackTheme.extended.surfaceContainerHigh,
                                border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimePosterImage(
                                        remoteUrl = candidate.remotePosterUrl,
                                        fallbackUrls = candidate.coverFallbackUrls,
                                        title = candidate.title,
                                        contentDescription = candidate.title,
                                        showTitleOnPlaceholder = false,
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 54.dp)
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                    Text(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add",
                                        tint = AniTrackTheme.extended.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAnimeDialog = false }) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularBackButton(onClick = onBack)
                    Text(
                        text = if (existingWatchlist == null) {
                            stringResource(R.string.watchlists_create)
                        } else {
                            stringResource(R.string.watchlists_edit)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = {
                        onSave(existingWatchlist?.id, name, description, coverKey, selectedAnimeIds.toList())
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_watchlist_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cover Artwork",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            coverOptions.forEach { optionAnime ->
                                val selected = optionAnime.id == coverKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(3f / 4f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else AniTrackTheme.extended.outline,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .clickable { coverKey = optionAnime.id }
                                        .testTag("cover_option_${optionAnime.id}")
                                ) {
                                    AnimePosterImage(
                                        remoteUrl = optionAnime.remotePosterUrl,
                                        fallbackUrls = optionAnime.coverFallbackUrls,
                                        title = optionAnime.title,
                                        contentDescription = "Cover ${optionAnime.title}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            repeat((4 - coverOptions.size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Watchlist Name") },
                            placeholder = { Text("e.g., Best Action Anime") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watchlist_name_input"),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("Describe the vibe or theme of this collection") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watchlist_description_input"),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Series in List (${selectedAnimeIds.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { showAddAnimeDialog = true },
                            modifier = Modifier.testTag("watchlist_add_anime_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.quick_add_anime), color = AniTrackTheme.extended.primaryText)
                        }
                    }
                }

                itemsIndexed(selectedAnimeIds, key = { idx, id -> "${id}_$idx" }) { index, animeId ->
                    val anime = animeMap[animeId]
                    if (anime != null) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnimePosterImage(
                                    remoteUrl = anime.remotePosterUrl,
                                    fallbackUrls = anime.coverFallbackUrls,
                                    title = anime.title,
                                    contentDescription = anime.title,
                                    showTitleOnPlaceholder = false,
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 60.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = anime.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${anime.format} · ${anime.episodeCountLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = selectedAnimeIds.removeAt(index)
                                            selectedAnimeIds.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = {
                                        if (index < selectedAnimeIds.lastIndex) {
                                            val item = selectedAnimeIds.removeAt(index)
                                            selectedAnimeIds.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < selectedAnimeIds.lastIndex
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                                }
                                IconButton(
                                    onClick = { selectedAnimeIds.removeAt(index) }
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove from watchlist",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                if (existingWatchlist != null) {
                    item {
                        OutlinedButton(
                            onClick = { onDelete(existingWatchlist.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_watchlist_button"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
