package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.WatchStatus
import com.example.ui.AniTrackViewModel
import com.example.ui.components.AniTrackBottomBar
import com.example.ui.components.AniTrackNavigationRail
import com.example.ui.components.AniTrackToastBanner
import com.example.ui.components.AutoCompletePromptDialog
import com.example.ui.components.OnboardingTutorialOverlay
import com.example.ui.components.TopLevelDestination
import com.example.ui.navigation.AnimeDetailsRoute
import com.example.ui.navigation.CalendarRoute
import com.example.ui.navigation.DiscoverRoute
import com.example.ui.navigation.EditWatchlistRoute
import com.example.ui.navigation.EpisodeTrackerRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.ProfileRoute
import com.example.ui.navigation.RandomPickerRoute
import com.example.ui.navigation.RateReviewRoute
import com.example.ui.navigation.SearchRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.StatsRoute
import com.example.ui.navigation.WatchlistsRoute
import com.example.ui.screens.AnimeCalendarScreen
import com.example.ui.screens.AnimeDetailsScreen
import com.example.ui.screens.AnimeLibraryScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateEditWatchlistScreen
import com.example.ui.screens.CustomWatchlistsScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.EpisodeTrackerScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.RandomAnimePickerScreen
import com.example.ui.screens.RatingsReviewsScreen
import com.example.ui.screens.SearchResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsDashboardScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.AniTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val widthSizeClass = windowSizeClass.widthSizeClass
            val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact
            val isExpandedWidth = widthSizeClass == WindowWidthSizeClass.Expanded

            val viewModel: AniTrackViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val authUser by viewModel.authUser.collectAsStateWithLifecycle()
            val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
            val authError by viewModel.authError.collectAsStateWithLifecycle()
            val showTutorial by viewModel.showTutorial.collectAsStateWithLifecycle()
            val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
            val autoCompleteAnime by viewModel.autoCompletePromptAnime.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.attemptSilentSignInOnce(this@MainActivity)
            }

            AniTrackTheme(darkTheme = uiState.settings.isDarkTheme) {
                val currentUser = authUser
                if (currentUser == null) {
                    AuthScreen(
                        isLoading = authLoading,
                        errorMessage = authError,
                        onGoogleSignIn = {
                            viewModel.signInWithGoogle(
                                activityContext = this@MainActivity,
                                cancelledMessage = getString(R.string.auth_cancelled)
                            )
                        },
                        onDismissError = { viewModel.clearAuthError() }
                    )
                } else {
                    key(currentUser.uid) {
                        AniTrackRoot(
                            viewModel = viewModel,
                            userEmail = currentUser.email,
                            showTutorial = showTutorial,
                            useNavigationRail = useNavigationRail,
                            isExpandedWidth = isExpandedWidth,
                            toastMessage = toastMessage,
                            autoCompleteTitle = autoCompleteAnime?.title
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AniTrackRoot(
    viewModel: AniTrackViewModel,
    userEmail: String?,
    showTutorial: Boolean,
    useNavigationRail: Boolean,
    isExpandedWidth: Boolean,
    toastMessage: String?,
    autoCompleteTitle: String?
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val discoverRefreshing by viewModel.discoverRefreshing.collectAsStateWithLifecycle()
    val discoverError by viewModel.discoverError.collectAsStateWithLifecycle()
    val calendarSyncing by viewModel.calendarSyncing.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val currentTab = when {
        currentDest?.hasRoute<HomeRoute>() == true -> TopLevelDestination.HOME
        currentDest?.hasRoute<LibraryRoute>() == true -> TopLevelDestination.LIBRARY
        currentDest?.hasRoute<DiscoverRoute>() == true -> TopLevelDestination.DISCOVER
        currentDest?.hasRoute<StatsRoute>() == true -> TopLevelDestination.STATS
        currentDest?.hasRoute<ProfileRoute>() == true -> TopLevelDestination.PROFILE
        else -> null
    }

    val navigateToTab: (TopLevelDestination) -> Unit = { dest ->
        val route: Any = when (dest) {
            TopLevelDestination.HOME -> HomeRoute
            TopLevelDestination.LIBRARY -> LibraryRoute
            TopLevelDestination.DISCOVER -> DiscoverRoute
            TopLevelDestination.STATS -> StatsRoute
            TopLevelDestination.PROFILE -> ProfileRoute
        }
        navController.navigate(route) {
            popUpTo<HomeRoute> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showTutorial) {
        OnboardingTutorialOverlay(
            onDismissComplete = { viewModel.completeTutorial() },
            onOpenDiscover = {
                viewModel.completeTutorial()
                navigateToTab(TopLevelDestination.DISCOVER)
            }
        )
    }

    if (autoCompleteTitle != null) {
        AutoCompletePromptDialog(
            animeTitle = autoCompleteTitle,
            onConfirmComplete = { viewModel.confirmAutoComplete() },
            onDismiss = { viewModel.dismissAutoCompletePrompt() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavigationRail && currentTab != null) {
                    AniTrackBottomBar(
                        currentTab = currentTab,
                        onSelectTab = navigateToTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && currentTab != null) {
                    AniTrackNavigationRail(
                        currentTab = currentTab,
                        onSelectTab = navigateToTab,
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<HomeRoute> {
                        HomeDashboardScreen(
                            uiState = uiState,
                            onOpenCalendar = { navController.navigate(CalendarRoute) },
                            onOpenProfile = { navigateToTab(TopLevelDestination.PROFILE) },
                            onOpenStats = { navigateToTab(TopLevelDestination.STATS) },
                            onOpenSearch = { navController.navigate(SearchRoute()) },
                            onOpenEpisodeTracker = { animeId -> navController.navigate(EpisodeTrackerRoute(animeId)) },
                            onOpenDiscover = { navigateToTab(TopLevelDestination.DISCOVER) },
                            onOpenLibrary = { navigateToTab(TopLevelDestination.LIBRARY) },
                            onOpenRandomPicker = { navController.navigate(RandomPickerRoute) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            onIncrementEpisode = { animeId -> viewModel.incrementEpisode(animeId) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<LibraryRoute> {
                        AnimeLibraryScreen(
                            uiState = uiState,
                            isExpandedWidth = isExpandedWidth,
                            onOpenSearch = { navController.navigate(SearchRoute()) },
                            onOpenWatchlists = { navController.navigate(WatchlistsRoute) },
                            onOpenDiscover = { navigateToTab(TopLevelDestination.DISCOVER) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            onIncrementEpisode = { animeId -> viewModel.incrementEpisode(animeId) },
                            onUpdateTrackingInfo = { id, status, eps, notes ->
                                viewModel.updateTrackingInfo(id, status, eps, notes)
                            },
                            onRemoveFromLibrary = { id -> viewModel.toggleInLibrary(id) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<AnimeDetailsRoute> { entry ->
                        val args = entry.toRoute<AnimeDetailsRoute>()
                        val animeFlow = remember(args.animeId) { viewModel.observeAnime(args.animeId) }
                        val anime by animeFlow.collectAsStateWithLifecycle(
                            initialValue = uiState.allAnime.firstOrNull { it.id == args.animeId }
                        )
                        AnimeDetailsScreen(
                            anime = anime,
                            allAnime = uiState.allAnime,
                            watchlists = uiState.watchlists,
                            onSyncDetails = { viewModel.syncAnimeDetails(it) },
                            onBack = { navController.navigateUp() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onToggleInLibrary = { viewModel.toggleInLibrary(it) },
                            onUpdateStatus = { id, status -> viewModel.updateStatus(id, status) },
                            onUpdatePersonalNotes = { id, notes -> viewModel.updatePersonalNotes(id, notes) },
                            onOpenEpisodeTracker = { id -> navController.navigate(EpisodeTrackerRoute(id)) },
                            onOpenRateReview = { id -> navController.navigate(RateReviewRoute(id)) },
                            onOpenGenreSearch = { genre -> navController.navigate(SearchRoute(initialGenre = genre)) },
                            onAddToWatchlist = { wlId, animeId -> viewModel.addAnimeToWatchlist(wlId, animeId) },
                            onOpenAnimeDetails = { id -> navController.navigate(AnimeDetailsRoute(id)) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<EpisodeTrackerRoute> { entry ->
                        val args = entry.toRoute<EpisodeTrackerRoute>()
                        val animeFlow = remember(args.animeId) { viewModel.observeAnime(args.animeId) }
                        val episodesFlow = remember(args.animeId) { viewModel.observeEpisodes(args.animeId) }
                        val anime by animeFlow.collectAsStateWithLifecycle(
                            initialValue = uiState.allAnime.firstOrNull { it.id == args.animeId }
                        )
                        val episodes by episodesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                        val watchingSeries = remember(uiState.allAnime) {
                            uiState.allAnime.filter { it.status == WatchStatus.WATCHING }
                        }
                        EpisodeTrackerScreen(
                            anime = anime,
                            episodes = episodes,
                            watchingSeries = watchingSeries,
                            onBack = { navController.navigateUp() },
                            onSelectAnime = { nextId ->
                                navController.navigate(EpisodeTrackerRoute(nextId)) {
                                    popUpTo(EpisodeTrackerRoute(args.animeId)) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onSelectSeason = { id, season -> viewModel.setCurrentSeason(id, season) },
                            onSetManualProgress = { id, count -> viewModel.setWatchedEpisodesManual(id, count) },
                            onReset = { viewModel.resetEpisodes(it) },
                            onMarkNext = { viewModel.markNextEpisode(it) },
                            onUndoLast = { viewModel.undoLastEpisode(it) },
                            onToggleEpisode = { id, epNum -> viewModel.toggleEpisodeWatched(id, epNum) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<DiscoverRoute> {
                        DiscoverScreen(
                            uiState = uiState,
                            isRefreshing = discoverRefreshing,
                            errorMessage = discoverError,
                            isExpandedWidth = isExpandedWidth,
                            onRefresh = { viewModel.refreshDiscoverFeed() },
                            onOpenCalendar = { navController.navigate(CalendarRoute) },
                            onOpenRandomPicker = { navController.navigate(RandomPickerRoute) },
                            onOpenSearch = { genre -> navController.navigate(SearchRoute(initialGenre = genre)) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<SearchRoute> { entry ->
                        val args = entry.toRoute<SearchRoute>()
                        SearchResultsScreen(
                            uiState = uiState,
                            searchState = searchState,
                            initialGenre = args.initialGenre,
                            isExpandedWidth = isExpandedWidth,
                            onBack = { navController.navigateUp() },
                            onPerformSearch = { q, genre, y2020, fmt, sortRating, page ->
                                viewModel.performRemoteSearch(q, genre, y2020, fmt, sortRating, page)
                            },
                            onClearRemoteSearch = { viewModel.clearRemoteSearch() },
                            onAddRecentSearch = { viewModel.addRecentSearch(it) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<StatsRoute> {
                        StatisticsDashboardScreen(
                            uiState = uiState,
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<WatchlistsRoute> {
                        CustomWatchlistsScreen(
                            uiState = uiState,
                            onBack = { navController.navigateUp() },
                            onCreateWatchlist = { navController.navigate(EditWatchlistRoute(null)) },
                            onEditWatchlist = { wlId -> navController.navigate(EditWatchlistRoute(wlId)) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<EditWatchlistRoute> { entry ->
                        val args = entry.toRoute<EditWatchlistRoute>()
                        val existing = uiState.watchlists.firstOrNull { it.id == args.watchlistId }
                        CreateEditWatchlistScreen(
                            existingWatchlist = existing,
                            allAnime = uiState.allAnime,
                            onBack = { navController.navigateUp() },
                            onSave = { id, name, desc, coverKey, animeIds ->
                                viewModel.saveWatchlist(id, name, desc, coverKey, animeIds) {
                                    navController.navigateUp()
                                }
                            },
                            onDelete = { id ->
                                viewModel.deleteWatchlist(id) {
                                    navController.navigateUp()
                                }
                            },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<RateReviewRoute> { entry ->
                        val args = entry.toRoute<RateReviewRoute>()
                        val animeFlow = remember(args.animeId) { viewModel.observeAnime(args.animeId) }
                        val anime by animeFlow.collectAsStateWithLifecycle(
                            initialValue = uiState.allAnime.firstOrNull { it.id == args.animeId }
                        )
                        RatingsReviewsScreen(
                            anime = anime,
                            onBack = { navController.navigateUp() },
                            onSaveReview = { id, overall, story, anim, chars, ost, text, spoilers ->
                                viewModel.saveReview(id, overall, story, anim, chars, ost, text, spoilers) {
                                    navController.navigateUp()
                                }
                            },
                            onDeleteReview = { id ->
                                viewModel.deleteReview(id) {
                                    navController.navigateUp()
                                }
                            },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<CalendarRoute> {
                        AnimeCalendarScreen(
                            uiState = uiState,
                            isSyncing = calendarSyncing,
                            onBack = { navController.navigateUp() },
                            onRefreshCalendar = { viewModel.refreshCalendarFeed() },
                            onToggleReminder = { viewModel.toggleReminder(it) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<RandomPickerRoute> {
                        RandomAnimePickerScreen(
                            uiState = uiState,
                            onBack = { navController.navigateUp() },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }

                    composable<ProfileRoute> {
                        UserProfileScreen(
                            uiState = uiState,
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenLibrary = { navigateToTab(TopLevelDestination.LIBRARY) },
                            onOpenWatchlists = { navController.navigate(WatchlistsRoute) },
                            onOpenWatchlistEdit = { wlId -> navController.navigate(EditWatchlistRoute(wlId)) },
                            onOpenDetails = { animeId -> navController.navigate(AnimeDetailsRoute(animeId)) },
                            onUpdateProfile = { name, bio -> viewModel.updateProfile(name, bio) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            uiState = uiState,
                            onBack = { navController.navigateUp() },
                            onSetDarkTheme = { viewModel.setDarkTheme(it) },
                            onSetEpisodeReminders = { viewModel.setEpisodeReminders(it) },
                            onSetReleaseNotifications = { viewModel.setReleaseNotifications(it) },
                            onSetDefaultSort = { viewModel.setDefaultSort(it) },
                            onSetDefaultViewGrid = { viewModel.setDefaultViewGrid(it) },
                            onSetAutoStatusSuggestions = { viewModel.setAutoStatusSuggestions(it) },
                            onExportLibrary = { callback -> viewModel.exportLibraryJson(callback) },
                            onImportLibrary = { viewModel.importLibraryJson(it) },
                            onSyncBackup = { viewModel.syncBackupNow() },
                            onSelectLanguageTag = { tag -> viewModel.setLanguageTag(tag) },
                            onReplayTutorial = { viewModel.replayTutorial() },
                            onSignOut = { viewModel.signOut(context) },
                            userEmail = userEmail,
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }
        }

        AniTrackToastBanner(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (currentTab != null && !useNavigationRail) 96.dp else 24.dp)
        )
    }
}
