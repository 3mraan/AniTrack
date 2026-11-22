package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WatchStatus
import com.example.ui.AniTrackUiState
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.CircularBackButton
import com.example.ui.components.CircularIconButton
import com.example.ui.theme.AniTrackMonoTypography
import com.example.ui.theme.AniTrackTheme

private enum class CalendarFilter(val label: String) {
    MY_WATCHING("My Watching"),
    ALL_SEASONAL("All Seasonal"),
    REMINDERS_ON("Reminders On")
}

private val MonthLabels = listOf("October 2026", "November 2026", "December 2026")
private val WeekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
fun AnimeCalendarScreen(
    uiState: AniTrackUiState,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onRefreshCalendar: () -> Unit,
    onToggleReminder: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var monthIndex by rememberSaveable { mutableIntStateOf(1) }
    var selectedFilter by rememberSaveable { mutableStateOf(CalendarFilter.ALL_SEASONAL) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }

    val airingAnime = remember(uiState.allAnime) {
        uiState.allAnime.filter { it.airingDayOfMonth != null }
    }

    val filteredReleases = remember(airingAnime, selectedFilter, selectedDay) {
        airingAnime
            .filter { anime ->
                when (selectedFilter) {
                    CalendarFilter.MY_WATCHING -> anime.status == WatchStatus.WATCHING
                    CalendarFilter.ALL_SEASONAL -> true
                    CalendarFilter.REMINDERS_ON -> anime.reminderEnabled
                }
            }
            .filter { anime ->
                selectedDay == null || anime.airingDayOfMonth == selectedDay
            }
            .sortedBy { it.airingDayOfMonth }
    }

    val airingDays = remember(airingAnime) {
        airingAnime.mapNotNull { it.airingDayOfMonth }.toSet()
    }

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
                    Column {
                        Text(
                            text = stringResource(R.string.calendar_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = MonthLabels[monthIndex],
                            style = MaterialTheme.typography.labelLarge,
                            color = AniTrackTheme.extended.primaryText
                        )
                    }
                }
                if (isSyncing) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                } else {
                    CircularIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_retry),
                        onClick = onRefreshCalendar,
                        modifier = Modifier.testTag("calendar_sync_button")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CalendarFilter.entries) { filter ->
                            val filterLabel = when (filter) {
                                CalendarFilter.MY_WATCHING -> stringResource(R.string.status_watching)
                                CalendarFilter.ALL_SEASONAL -> stringResource(R.string.action_all)
                                CalendarFilter.REMINDERS_ON -> stringResource(R.string.settings_episode_reminders)
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = {
                                    selectedFilter = filter
                                    selectedDay = null
                                },
                                label = { Text(filterLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
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
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (monthIndex > 0) monthIndex-- },
                                    enabled = monthIndex > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                                }
                                Text(MonthLabels[monthIndex], style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = { if (monthIndex < MonthLabels.lastIndex) monthIndex++ },
                                    enabled = monthIndex < MonthLabels.lastIndex
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                WeekDays.forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val weeks = (1..30).toList().chunked(7)
                            weeks.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    week.forEach { dayNum ->
                                        val isToday = monthIndex == 1 && dayNum == 21
                                        val isSelected = selectedDay == dayNum
                                        val hasRelease = monthIndex == 1 && dayNum in airingDays
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                    color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedDay = if (selectedDay == dayNum) null else dayNum
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = AniTrackMonoTypography.monoMd,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (hasRelease) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else AniTrackTheme.extended.success
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(7 - week.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
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
                        Text(
                            text = if (selectedDay != null) "Releases on Nov $selectedDay" else "Upcoming Episodes",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (selectedDay != null) {
                            Text(
                                text = "Show All",
                                style = MaterialTheme.typography.labelLarge,
                                color = AniTrackTheme.extended.primaryText,
                                modifier = Modifier.clickable { selectedDay = null }
                            )
                        }
                    }
                }

                if (filteredReleases.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No scheduled episodes match this filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(filteredReleases, key = { it.id }) { anime ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onOpenDetails(anime.id) }
                                .testTag("calendar_release_${anime.id}"),
                            shape = MaterialTheme.shapes.large,
                            color = AniTrackTheme.extended.surfaceContainer,
                            border = BorderStroke(1.dp, AniTrackTheme.extended.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimePosterImage(
                                    remoteUrl = anime.remotePosterUrl,
                                    fallbackUrls = anime.coverFallbackUrls,
                                    title = anime.title,
                                    contentDescription = anime.title,
                                    showTitleOnPlaceholder = false,
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 74.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = anime.airingTimeLabel ?: "TBA",
                                        style = AniTrackMonoTypography.monoSm,
                                        color = AniTrackTheme.extended.primaryText
                                    )
                                    Text(
                                        text = "${anime.title} · Ep ${anime.nextEpisodeNumber ?: (anime.watchedEpisodes + 1)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = anime.nextEpisodeTitle ?: "New Episode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleReminder(anime.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("calendar_reminder_${anime.id}")
                                ) {
                                    Icon(
                                        imageVector = if (anime.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                                        contentDescription = if (anime.reminderEnabled) "Mute reminder" else "Enable reminder",
                                        tint = if (anime.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
