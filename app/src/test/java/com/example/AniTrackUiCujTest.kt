package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedData
import com.example.data.model.AnimeSeries
import com.example.data.model.WatchlistWithItems
import com.example.ui.AniTrackUiState
import com.example.ui.AniTrackViewModel
import com.example.ui.SearchUiState
import com.example.ui.components.AniTrackBottomBar
import com.example.ui.components.AnimeBannerImage
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.OnboardingTutorialOverlay
import com.example.ui.components.TopLevelDestination
import com.example.ui.components.isValidHttpUrl
import com.example.ui.components.normalizeWatchlistCoverKey
import com.example.ui.components.resolveWatchlistCoverAnime
import com.example.ui.screens.AnimeLibraryScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.RatingsReviewsScreen
import com.example.ui.screens.SearchResultsScreen
import com.example.ui.screens.StatisticsDashboardScreen
import com.example.ui.theme.AniTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h1200dp")
class AniTrackUiCujTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildSeededUiState(): AniTrackUiState {
        val metadata = SeedData.initialMetadata()
        val trackingMap = SeedData.initialUserTracking().associateBy { it.animeId }
        val reviewMap = SeedData.initialReviews().associateBy { it.animeId }
        val series = metadata.map { meta ->
            AnimeSeries(
                metadata = meta,
                tracking = trackingMap[meta.id],
                review = reviewMap[meta.id]
            )
        }
        val episodes = SeedData.initialEpisodesFor(metadata, trackingMap)
        val crossRefs = SeedData.initialWatchlistAnime().groupBy { it.watchlistId }
        val watchlists = SeedData.initialWatchlists().map { wl ->
            WatchlistWithItems(
                watchlist = wl,
                animeIds = crossRefs[wl.id]?.sortedBy { it.position }?.map { it.animeId } ?: emptyList()
            )
        }
        return AniTrackUiState(
            allAnime = series,
            watchlists = watchlists,
            profile = SeedData.defaultProfile,
            monthlyEpisodes = AniTrackViewModel.computeMonthlyActivity(episodes),
            ratingDistribution = AniTrackViewModel.computeRatingDistribution(series),
            isLoaded = true
        )
    }

    @Test
    fun `application resources expose AniTrack branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AniTrack", context.getString(R.string.app_name))
    }

    @Test
    fun `bottom navigation bar exposes all five tabs and dispatches selection`() {
        var selected = TopLevelDestination.HOME
        composeTestRule.setContent {
            AniTrackTheme {
                AniTrackBottomBar(
                    currentTab = selected,
                    onSelectTab = { selected = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_library").performClick()
        assertEquals(TopLevelDestination.LIBRARY, selected)
        composeTestRule.onNodeWithTag("nav_discover").performClick()
        assertEquals(TopLevelDestination.DISCOVER, selected)
        composeTestRule.onNodeWithTag("nav_stats").performClick()
        assertEquals(TopLevelDestination.STATS, selected)
        composeTestRule.onNodeWithTag("nav_profile").performClick()
        assertEquals(TopLevelDestination.PROFILE, selected)
    }

    @Test
    fun `home dashboard renders user journey stats and triggers increment episode`() {
        val state = buildSeededUiState()
        var incrementedId: String? = null
        composeTestRule.setContent {
            AniTrackTheme {
                HomeDashboardScreen(
                    uiState = state,
                    onOpenCalendar = {},
                    onOpenProfile = {},
                    onOpenStats = {},
                    onOpenSearch = {},
                    onOpenEpisodeTracker = {},
                    onOpenDiscover = {},
                    onOpenLibrary = {},
                    onOpenRandomPicker = {},
                    onOpenDetails = {},
                    onIncrementEpisode = { incrementedId = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Continue Watching").assertIsDisplayed()
        composeTestRule.onNodeWithTag("increment_ep_solo_leveling").performClick()
        assertEquals("solo_leveling", incrementedId)
    }

    @Test
    fun `library screen filters by status and toggles grid list view`() {
        val state = buildSeededUiState()
        composeTestRule.setContent {
            AniTrackTheme {
                AnimeLibraryScreen(
                    uiState = state,
                    isExpandedWidth = false,
                    onOpenSearch = {},
                    onOpenWatchlists = {},
                    onOpenDiscover = {},
                    onOpenDetails = {},
                    onIncrementEpisode = {},
                    onUpdateTrackingInfo = { _, _, _, _ -> },
                    onRemoveFromLibrary = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_view_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Solo Leveling").assertIsDisplayed()

        // Filter to Completed via FilterChip testTag
        composeTestRule.onNodeWithTag("filter_status_completed").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Attack on Titan").assertIsDisplayed()
    }

    @Test
    fun `search screen displays offline retry banner on network error`() {
        val state = buildSeededUiState()
        var retried = false
        composeTestRule.setContent {
            AniTrackTheme {
                SearchResultsScreen(
                    uiState = state,
                    searchState = SearchUiState(
                        isLoading = false,
                        errorMessage = "Unable to reach AniList",
                        hasActiveRemoteSearch = true
                    ),
                    initialGenre = null,
                    isExpandedWidth = false,
                    onBack = {},
                    onPerformSearch = { _, _, _, _, _, _ -> retried = true },
                    onClearRemoteSearch = {},
                    onAddRecentSearch = {},
                    onOpenDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun `statistics dashboard renders real DB metrics and charts`() {
        val state = buildSeededUiState()
        composeTestRule.setContent {
            AniTrackTheme {
                StatisticsDashboardScreen(
                    uiState = state,
                    onOpenDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Status Distribution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly Activity").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ratings and reviews screen saves and deletes review`() {
        val state = buildSeededUiState()
        val solo = state.allAnime.first { it.id == "solo_leveling" }
        var savedText = ""
        var deletedId: String? = null

        composeTestRule.setContent {
            AniTrackTheme {
                RatingsReviewsScreen(
                    anime = solo,
                    onBack = {},
                    onSaveReview = { _, _, _, _, _, _, text, _ -> savedText = text },
                    onDeleteReview = { deletedId = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("review_text_input").performScrollTo().performTextInput(" Great!")
        composeTestRule.onNodeWithTag("save_review_button").performScrollTo().performClick()
        assertTrue(savedText.contains("Great!"))

        composeTestRule.onNodeWithTag("delete_review_button").performScrollTo().performClick()
        assertEquals("solo_leveling", deletedId)
    }

    @Test
    fun `image URL validator accepts only http and https URLs`() {
        assertTrue(isValidHttpUrl("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11061-y5gsT1hoHuHw.png"))
        assertTrue(isValidHttpUrl("http://img.example/poster.jpg"))
        assertFalse(isValidHttpUrl(""))
        assertFalse(isValidHttpUrl("   "))
        assertFalse(isValidHttpUrl("notaurl"))
        assertFalse(isValidHttpUrl("ftp://example.com/poster.png"))
        assertFalse(isValidHttpUrl(null))
    }

    @Test
    fun `poster shows neutral title placeholder when image URL is missing or invalid`() {
        composeTestRule.setContent {
            AniTrackTheme {
                AnimePosterImage(
                    remoteUrl = "",
                    fallbackUrls = listOf("invalid-scheme://bad"),
                    title = "Hunter x Hunter",
                    contentDescription = "Hunter x Hunter"
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hunter x Hunter").assertIsDisplayed()
    }

    @Test
    fun `banner image falls back to neutral title placeholder when both banner and cover are missing`() {
        composeTestRule.setContent {
            AniTrackTheme {
                AnimeBannerImage(
                    bannerUrl = "",
                    coverUrl = "",
                    fallbackCoverUrls = emptyList(),
                    title = "Solo Leveling",
                    contentDescription = "Solo Leveling banner"
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
    }

    @Test
    fun `watchlist cover resolver maps legacy aliases and series ids to real AniList artwork`() {
        val state = buildSeededUiState()
        val byId = state.allAnime.associateBy { it.id }

        assertEquals("attack_on_titan", normalizeWatchlistCoverKey("aot"))
        assertEquals("solo_leveling", normalizeWatchlistCoverKey("solo"))
        assertEquals("jujutsu_kaisen", normalizeWatchlistCoverKey("jjk"))
        assertEquals("vinland_saga", normalizeWatchlistCoverKey("vinland"))

        val legacyResolved = resolveWatchlistCoverAnime("aot", emptyList(), byId)
        assertNotNull(legacyResolved)
        assertEquals("Attack on Titan", legacyResolved?.title)
        assertTrue(legacyResolved!!.remotePosterUrl.contains("16498"))

        val listFallbackResolved = resolveWatchlistCoverAnime("unknown_key", listOf("one_piece"), byId)
        assertNotNull(listFallbackResolved)
        assertEquals("One Piece", listFallbackResolved?.title)
        assertTrue(listFallbackResolved!!.remotePosterUrl.contains("21"))
    }

    @Test
    fun `auth screen switches between Sign In and Create Account and triggers Google Sign-In`() {
        var googleClicks = 0
        composeTestRule.setContent {
            AniTrackTheme {
                AuthScreen(
                    isLoading = false,
                    errorMessage = null,
                    onGoogleSignIn = { googleClicks++ },
                    onDismissError = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_tab_signup").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Start Your Journey").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_google_button").performClick()
        assertEquals(1, googleClicks)
    }

    @Test
    fun `onboarding tutorial walks through 3 steps and completes cleanly`() {
        var completed = false
        composeTestRule.setContent {
            AniTrackTheme {
                OnboardingTutorialOverlay(
                    onDismissComplete = { completed = true },
                    onOpenDiscover = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Library Starts Fresh").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tutorial_next_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How to Add a New Anime").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tutorial_next_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How to Log Watched Episodes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tutorial_finish_btn").performClick()
        assertTrue(completed)
    }
}
