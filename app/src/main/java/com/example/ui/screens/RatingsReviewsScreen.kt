package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.ui.components.CircularBackButton
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme
import kotlin.math.roundToInt

@Composable
fun RatingsReviewsScreen(
    anime: AnimeSeries?,
    onBack: () -> Unit,
    onSaveReview: (
        animeId: String,
        overall: Float,
        story: Int,
        animation: Int,
        characters: Int,
        soundtrack: Int,
        reviewText: String,
        containsSpoilers: Boolean
    ) -> Unit,
    onDeleteReview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (anime == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.loading_series), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var overall by rememberSaveable(anime.id) {
        mutableFloatStateOf(anime.userRating ?: 8.5f)
    }
    var story by rememberSaveable(anime.id) { mutableIntStateOf(anime.storyScore) }
    var animation by rememberSaveable(anime.id) { mutableIntStateOf(anime.animationScore) }
    var characters by rememberSaveable(anime.id) { mutableIntStateOf(anime.charactersScore) }
    var soundtrack by rememberSaveable(anime.id) { mutableIntStateOf(anime.soundtrackScore) }
    var reviewText by rememberSaveable(anime.id) { mutableStateOf(anime.personalReview) }
    var containsSpoilers by rememberSaveable(anime.id) { mutableStateOf(anime.containsSpoilers) }

    val hasExistingReview = anime.userRating != null || anime.personalReview.isNotBlank()

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularBackButton(onClick = onBack)
                Column {
                    Text(
                        text = stringResource(R.string.reviews_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AniTrackTheme.extended.primaryText
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.reviews_overall), style = MaterialTheme.typography.titleLarge)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = AniTrackTheme.extended.warning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f / 10", overall),
                                        style = AniTrackMonoTypography.monoLg
                                    )
                                }
                            }
                            Slider(
                                value = overall,
                                onValueChange = { raw ->
                                    overall = ((raw * 2f).roundToInt() / 2f).coerceIn(1f, 10f)
                                },
                                valueRange = 1f..10f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("overall_rating_slider")
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.reviews_breakdown), style = MaterialTheme.typography.titleLarge)
                            CategorySliderRow(stringResource(R.string.reviews_story), story) { story = it }
                            CategorySliderRow(stringResource(R.string.reviews_animation), animation) { animation = it }
                            CategorySliderRow(stringResource(R.string.reviews_characters), characters) { characters = it }
                            CategorySliderRow(stringResource(R.string.reviews_soundtrack), soundtrack) { soundtrack = it }
                        }
                    }
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AniTrackTheme.extended.surfaceContainer,
                        border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.reviews_written), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = reviewText,
                                onValueChange = { reviewText = it },
                                placeholder = { Text(stringResource(R.string.reviews_written)) },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("review_text_input"),
                                shape = MaterialTheme.shapes.medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reviews_contains_spoilers),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = containsSpoilers,
                                    onCheckedChange = { containsSpoilers = it },
                                    modifier = Modifier.testTag("spoiler_switch")
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onSaveReview(
                                    anime.id,
                                    overall,
                                    story,
                                    animation,
                                    characters,
                                    soundtrack,
                                    reviewText,
                                    containsSpoilers
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_review_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.reviews_save), style = MaterialTheme.typography.labelLarge)
                        }

                        if (hasExistingReview) {
                            OutlinedButton(
                                onClick = { onDeleteReview(anime.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("delete_review_button"),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.reviews_delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$value / 10",
                style = AniTrackMonoTypography.monoMd,
                color = AniTrackTheme.extended.primaryText
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}
