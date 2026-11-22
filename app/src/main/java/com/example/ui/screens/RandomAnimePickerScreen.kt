package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WatchStatus
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.StarRatingBadge
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme
import kotlin.random.Random

private val PickerFilters = listOf("Any Genre", "Action", "Dark Fantasy", "12–24 Eps", "50+ Eps")

@Composable
fun RandomAnimePickerScreen(
    uiState: AniTrackUiState,
    onBack: () -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var plannedOnly by rememberSaveable { mutableStateOf(true) }
    var selectedFilter by rememberSaveable { mutableStateOf("Any Genre") }
    var rollCounter by rememberSaveable { mutableIntStateOf(0) }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }

    val basePool = remember(uiState.allAnime, plannedOnly) {
        if (plannedOnly) {
            uiState.allAnime.filter { it.status == WatchStatus.PLANNED }.ifEmpty { uiState.allAnime }
        } else {
            uiState.allAnime
        }
    }

    val pool = remember(basePool, selectedFilter) {
        basePool.filter { anime ->
            val eps = anime.totalEpisodes ?: 12
            when (selectedFilter) {
                "Action" -> anime.genres.any { it.equals("Action", ignoreCase = true) }
                "Dark Fantasy" -> anime.genres.any { it.equals("Dark Fantasy", ignoreCase = true) }
                "12–24 Eps" -> eps in 12..24
                "50+ Eps" -> eps >= 50
                else -> true
            }
        }.ifEmpty { basePool }
    }

    val currentAnime = remember(pool, pickedId) {
        pool.firstOrNull { it.id == pickedId } ?: pool.firstOrNull()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Column {
                        Text(
                            text = stringResource(R.string.picker_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (plannedOnly) stringResource(R.string.status_planned) else stringResource(R.string.action_all),
                            style = MaterialTheme.typography.labelMedium,
                            color = AniTrackTheme.extended.primaryText
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Casino,
                        contentDescription = null,
                        tint = AniTrackTheme.extended.primaryText
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = plannedOnly,
                        onClick = {
                            plannedOnly = !plannedOnly
                            pickedId = null
                            rollCounter++
                        },
                        label = {
                            Text(if (plannedOnly) stringResource(R.string.status_planned) else stringResource(R.string.action_all))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
                items(PickerFilters) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = {
                            selectedFilter = filter
                            pickedId = pool.randomOrNull()?.id
                            rollCounter++
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (currentAnime != null) {
                AnimatedContent(
                    targetState = currentAnime to rollCounter,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.94f)).togetherWith(fadeOut())
                    },
                    label = "random_picker_card",
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) { (anime, _) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimePosterImage(
                                remoteUrl = anime.remotePosterUrl,
                                fallbackUrls = anime.coverFallbackUrls,
                                title = anime.title,
                                contentDescription = anime.title,
                                allowManualRetry = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x220B0B12),
                                                Color(0x550B0B12),
                                                Color(0xF00B0B12)
                                            )
                                        )
                                    )
                            )
                            StarRatingBadge(
                                rating = anime.communityRating,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = "${anime.format} · ${anime.episodeCountLabel}",
                                        style = AniTrackMonoTypography.monoSm,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = anime.title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = anime.genres.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val others = pool.filter { it.id != currentAnime.id }.ifEmpty { pool }
                            pickedId = others[Random.nextInt(others.size)].id
                            rollCounter++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("select_another_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.picker_select_another))
                    }
                    OutlinedButton(
                        onClick = { onOpenDetails(currentAnime.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("picker_open_details_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.picker_open_details))
                    }
                }
            }
        }
    }
}
