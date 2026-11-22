package com.example.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SeedData
import com.example.data.model.AnimeSeries
import com.example.data.model.EpisodeHistoryEntity
import com.example.data.model.MonthlyEpisodePoint
import com.example.data.model.RatingBucketPoint
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistWithItems
import com.example.data.preferences.UserSettings
import com.example.data.remote.AuthUser
import com.example.data.remote.FirebaseSyncManager
import com.example.data.repository.AniTrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class AniTrackUiState(
    val allAnime: List<AnimeSeries> = emptyList(),
    val watchlists: List<WatchlistWithItems> = emptyList(),
    val profile: UserProfileEntity = SeedData.defaultProfile,
    val settings: UserSettings = UserSettings(),
    val monthlyEpisodes: List<MonthlyEpisodePoint> = emptyList(),
    val ratingDistribution: List<RatingBucketPoint> = emptyList(),
    val isLoaded: Boolean = false
)

data class SearchUiState(
    val remoteItems: List<AnimeSeries> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val hasActiveRemoteSearch: Boolean = false
)

@HiltViewModel
class AniTrackViewModel @Inject constructor(
    private val repository: AniTrackRepository,
    private val firebaseSyncManager: FirebaseSyncManager
) : ViewModel() {

    private val baseFlows = combine(
        repository.allAnime,
        repository.watchlists,
        repository.profile,
        repository.settings
    ) { anime, watchlists, profile, settings ->
        Quadruple(anime, watchlists, profile, settings)
    }

    val uiState: StateFlow<AniTrackUiState> = combine(
        baseFlows,
        repository.allEpisodeHistory
    ) { (anime, watchlists, profile, settings), history ->
        AniTrackUiState(
            allAnime = anime,
            watchlists = watchlists,
            profile = profile,
            settings = settings,
            monthlyEpisodes = computeMonthlyActivity(history),
            ratingDistribution = computeRatingDistribution(anime),
            isLoaded = anime.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AniTrackUiState()
    )

    val authUser: StateFlow<AuthUser?> = firebaseSyncManager.authUser

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _forceShowTutorial = MutableStateFlow(false)
    private val _dismissedTutorialThisSession = MutableStateFlow(false)

    val showTutorial: StateFlow<Boolean> = combine(
        authUser,
        uiState,
        _forceShowTutorial,
        _dismissedTutorialThisSession
    ) { user, state, forced, dismissed ->
        if (user == null) {
            false
        } else if (forced) {
            true
        } else {
            !dismissed && state.isLoaded && !state.settings.tutorialCompleted
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _autoCompletePromptAnime = MutableStateFlow<AnimeSeries?>(null)
    val autoCompletePromptAnime: StateFlow<AnimeSeries?> = _autoCompletePromptAnime.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _discoverRefreshing = MutableStateFlow(false)
    val discoverRefreshing: StateFlow<Boolean> = _discoverRefreshing.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError: StateFlow<String?> = _discoverError.asStateFlow()

    private val _calendarSyncing = MutableStateFlow(false)
    val calendarSyncing: StateFlow<Boolean> = _calendarSyncing.asStateFlow()

    private var lastSyncedUid: String? = null
    private var silentSignInAttempted = false

    init {
        viewModelScope.launch {
            authUser.collectLatest { user ->
                if (user != null && user.uid != lastSyncedUid) {
                    lastSyncedUid = user.uid
                    _dismissedTutorialThisSession.value = false
                    repository.syncSessionForUser(user)
                } else if (user == null) {
                    lastSyncedUid = null
                }
            }
        }
    }

    fun observeAnime(animeId: String): Flow<AnimeSeries?> = repository.observeAnime(animeId)

    fun observeEpisodes(animeId: String): Flow<List<EpisodeHistoryEntity>> = repository.observeEpisodes(animeId)

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(2_400)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    // ---- Authentication & Onboarding Tutorial ----

    fun attemptSilentSignInOnce(activityContext: Context) {
        if (silentSignInAttempted) return
        silentSignInAttempted = true
        viewModelScope.launch {
            firebaseSyncManager.attemptSilentGoogleSignIn(activityContext)
        }
    }

    fun signInWithGoogle(activityContext: Context, cancelledMessage: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = firebaseSyncManager.signInWithGoogleInteractive(activityContext)
            res.onFailure { err ->
                _authError.value = if (err is GetCredentialCancellationException) {
                    cancelledMessage
                } else {
                    err.message ?: cancelledMessage
                }
            }
            _authLoading.value = false
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            _authLoading.value = true
            _forceShowTutorial.value = false
            _dismissedTutorialThisSession.value = false
            firebaseSyncManager.signOut(activityContext)
            repository.clearUserSessionData()
            _authLoading.value = false
        }
    }

    fun completeTutorial() {
        _forceShowTutorial.value = false
        _dismissedTutorialThisSession.value = true
        viewModelScope.launch {
            repository.setTutorialCompleted(true)
        }
    }

    fun replayTutorial() {
        _dismissedTutorialThisSession.value = false
        _forceShowTutorial.value = true
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch {
            repository.setLanguageTag(tag)
            val localeList = if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    // ---- AniList Remote Actions ----

    fun refreshDiscoverFeed() {
        viewModelScope.launch {
            _discoverRefreshing.value = true
            _discoverError.value = null
            val result = repository.refreshDiscoverFromAniList()
            result.onFailure { err ->
                _discoverError.value = err.message ?: "Offline: showing cached catalog"
            }.onSuccess { count ->
                showToast("Synced $count series from AniList")
            }
            _discoverRefreshing.value = false
        }
    }

    fun syncAnimeDetails(animeId: String) {
        viewModelScope.launch {
            repository.syncAnimeDetailsIfNeeded(animeId)
        }
    }

    fun refreshCalendarFeed() {
        viewModelScope.launch {
            _calendarSyncing.value = true
            val res = repository.refreshAiringCalendar()
            res.onSuccess { count ->
                if (count > 0) showToast("Updated $count airing schedules")
            }.onFailure {
                showToast("Offline: showing cached release schedule")
            }
            _calendarSyncing.value = false
        }
    }

    fun performRemoteSearch(
        query: String,
        genre: String,
        year2020Plus: Boolean,
        format: String,
        sortByRating: Boolean,
        page: Int = 1
    ) {
        viewModelScope.launch {
            if (page == 1) {
                _searchState.value = _searchState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    hasActiveRemoteSearch = true
                )
            } else {
                _searchState.value = _searchState.value.copy(
                    isLoadingMore = true,
                    errorMessage = null
                )
            }

            val result = repository.searchAniList(
                query = query,
                genre = genre,
                year2020Plus = year2020Plus,
                format = format,
                sortByRating = sortByRating,
                page = page
            )

            result.onSuccess { pageResult ->
                val allMap = uiState.value.allAnime.associateBy { it.id }
                val wrapped = pageResult.items.map { meta ->
                    allMap[meta.id] ?: AnimeSeries(metadata = meta, tracking = null, review = null)
                }
                val combined = if (page == 1) {
                    wrapped
                } else {
                    (_searchState.value.remoteItems + wrapped).distinctBy { it.id }
                }
                _searchState.value = SearchUiState(
                    remoteItems = combined,
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    currentPage = pageResult.currentPage,
                    hasNextPage = pageResult.hasNextPage,
                    hasActiveRemoteSearch = true
                )
            }.onFailure { err ->
                _searchState.value = _searchState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = err.message ?: "Unable to reach AniList"
                )
            }
        }
    }

    fun clearRemoteSearch() {
        _searchState.value = SearchUiState()
    }

    // ---- Tracking & Episode Actions ----

    fun incrementEpisode(animeId: String) {
        viewModelScope.launch {
            val (updated, reachedFinal) = repository.incrementEpisode(animeId)
            if (updated != null) {
                showToast("${updated.title} · Ep ${updated.watchedEpisodes} logged")
                if (reachedFinal && updated.status != WatchStatus.COMPLETED && uiState.value.settings.autoStatusSuggestions) {
                    _autoCompletePromptAnime.value = updated
                }
            }
        }
    }

    fun setWatchedEpisodesManual(animeId: String, count: Int) {
        viewModelScope.launch {
            repository.setWatchedEpisodesManual(animeId, count)
            showToast("Progress updated")
        }
    }

    fun setCurrentSeason(animeId: String, season: Int) {
        viewModelScope.launch {
            repository.setCurrentSeason(animeId, season)
        }
    }

    fun toggleEpisodeWatched(animeId: String, episodeNumber: Int) {
        viewModelScope.launch {
            repository.toggleEpisodeWatched(animeId, episodeNumber)
        }
    }

    fun markNextEpisode(animeId: String) {
        incrementEpisode(animeId)
    }

    fun undoLastEpisode(animeId: String) {
        viewModelScope.launch {
            repository.undoLastEpisode(animeId)
            showToast("Last episode unmarked")
        }
    }

    fun resetEpisodes(animeId: String) {
        viewModelScope.launch {
            repository.resetEpisodes(animeId)
            showToast("Episode progress reset")
        }
    }

    fun dismissAutoCompletePrompt() {
        _autoCompletePromptAnime.value = null
    }

    fun confirmAutoComplete() {
        val anime = _autoCompletePromptAnime.value ?: return
        _autoCompletePromptAnime.value = null
        updateStatus(anime.id, WatchStatus.COMPLETED)
    }

    fun toggleFavorite(animeId: String) {
        viewModelScope.launch { repository.toggleFavorite(animeId) }
    }

    fun toggleInLibrary(animeId: String) {
        viewModelScope.launch {
            val existing = uiState.value.allAnime.firstOrNull { it.id == animeId }
            repository.toggleInLibrary(animeId)
            if (existing != null) {
                showToast(if (existing.inLibrary) "Removed from library" else "Added to library")
            }
        }
    }

    fun updateStatus(animeId: String, status: WatchStatus) {
        viewModelScope.launch {
            repository.updateStatus(animeId, status)
            showToast("Status set to ${status.label}")
        }
    }

    fun updateTrackingInfo(
        animeId: String,
        status: WatchStatus,
        watchedEpisodes: Int,
        personalNotes: String
    ) {
        viewModelScope.launch {
            repository.updateTrackingInfo(animeId, status, watchedEpisodes, personalNotes)
            showToast("Series tracking updated")
        }
    }

    fun updatePersonalNotes(animeId: String, notes: String) {
        viewModelScope.launch {
            repository.updatePersonalNotes(animeId, notes)
            showToast("Personal notes saved")
        }
    }

    fun saveReview(
        animeId: String,
        overall: Float,
        story: Int,
        animation: Int,
        characters: Int,
        soundtrack: Int,
        reviewText: String,
        containsSpoilers: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveReview(
                animeId = animeId,
                overall = overall,
                story = story,
                animation = animation,
                characters = characters,
                soundtrack = soundtrack,
                reviewText = reviewText,
                containsSpoilers = containsSpoilers
            )
            showToast("Rating & review saved")
            onComplete()
        }
    }

    fun deleteReview(animeId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteReview(animeId)
            showToast("Review deleted")
            onComplete()
        }
    }

    fun toggleReminder(animeId: String) {
        viewModelScope.launch {
            val current = uiState.value.allAnime.firstOrNull { it.id == animeId }
            repository.toggleReminder(animeId)
            if (current != null) {
                val enabled = !current.reminderEnabled
                showToast(if (enabled) "Reminder enabled for ${current.title}" else "Reminder muted")
            }
        }
    }

    fun saveWatchlist(
        watchlistId: String?,
        name: String,
        description: String,
        coverKey: String,
        animeIds: List<String>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveWatchlist(watchlistId, name, description, coverKey, animeIds)
            showToast("Watchlist saved")
            onSaved()
        }
    }

    fun deleteWatchlist(watchlistId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWatchlist(watchlistId)
            showToast("Watchlist deleted")
            onDeleted()
        }
    }

    fun addAnimeToWatchlist(watchlistId: String, animeId: String) {
        viewModelScope.launch {
            repository.addAnimeToWatchlist(watchlistId, animeId)
            showToast("Added to watchlist")
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfile(displayName, bio)
            showToast("Profile updated")
        }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch { repository.addRecentSearch(query) }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(isDark) }
    }

    fun setEpisodeReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setEpisodeReminders(enabled) }
    }

    fun setReleaseNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setReleaseNotifications(enabled) }
    }

    fun setDefaultSort(sort: String) {
        viewModelScope.launch { repository.setDefaultSort(sort) }
    }

    fun setDefaultViewGrid(isGrid: Boolean) {
        viewModelScope.launch { repository.setDefaultViewGrid(isGrid) }
    }

    fun setAutoStatusSuggestions(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoStatusSuggestions(enabled) }
    }

    fun syncBackupNow() {
        viewModelScope.launch {
            repository.syncBackupNow()
            showToast("Backup settings synced")
        }
    }

    fun exportLibraryJson(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val payload = repository.exportLibraryJson()
            onExportReady(payload)
        }
    }

    fun importLibraryJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importLibraryJson(rawJson)
            result.onSuccess { count ->
                showToast("Imported $count anime series")
            }.onFailure {
                showToast("Invalid JSON backup format")
            }
        }
    }

    companion object {
        fun computeMonthlyActivity(history: List<EpisodeHistoryEntity>): List<MonthlyEpisodePoint> {
            val monthOrder = listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov")
            val counts = monthOrder.associateWith { 0 }.toMutableMap()
            val fmt = SimpleDateFormat("MMM", Locale.US)

            history.filter { it.watched && it.watchedAtEpoch != null }.forEach { ep ->
                val label = fmt.format(Date(ep.watchedAtEpoch!!))
                if (label in counts) {
                    counts[label] = (counts[label] ?: 0) + 1
                } else {
                    val cal = Calendar.getInstance().apply { timeInMillis = ep.watchedAtEpoch }
                    val fallbackKey = monthOrder[cal.get(Calendar.MONTH) % monthOrder.size]
                    counts[fallbackKey] = (counts[fallbackKey] ?: 0) + 1
                }
            }
            val peak = counts.values.maxOrNull() ?: 0
            return monthOrder.map { m ->
                val value = counts[m] ?: 0
                MonthlyEpisodePoint(month = m, episodes = value, isHighlight = value > 0 && value == peak)
            }
        }

        fun computeRatingDistribution(anime: List<AnimeSeries>): List<RatingBucketPoint> {
            val scores = anime.mapNotNull { it.userRating }
            val buckets = (6..10).associateWith { 0 }.toMutableMap()
            scores.forEach { score ->
                val bucket = score.roundToInt().coerceIn(6, 10)
                buckets[bucket] = (buckets[bucket] ?: 0) + 1
            }
            return (6..10).map { b -> RatingBucketPoint(scoreBucket = b, count = buckets[b] ?: 0) }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
