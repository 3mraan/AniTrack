# AniTrack — Your Anime. Your Journey.

Native Android anime tracking and management application built in Kotlin and Jetpack Compose from the AniTrack Stitch mobile UI/UX design system.

## Overview & Identity

- **Application Name**: AniTrack
- **Tagline**: Your Anime. Your Journey.
- **Package / Application ID**: `com.aistudio.anitrack.kvnqzx` (`namespace = "com.example"`)
- **Theme**: Premium Dark UI (`#0B0B12`) with Electric Violet (`#8B5CF6`) accent, full Light theme support, and bundled `Mulish`, `Instrument Sans`, and `Spline Sans Mono` typography.

## Architecture & Technology Stack

- **UI**: Jetpack Compose + Material 3 + Adaptive `WindowSizeClass` (`NavigationBar` on phones, `NavigationRail` + multi-column grids on tablets/foldables)
- **Navigation**: Type-safe Navigation Compose (`@Serializable` route keys) across all 14 screens
- **State & DI**: MVVM + Repository pattern with Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`) + `StateFlow` / `collectAsStateWithLifecycle`
- **Local Persistence (Offline-First)**:
  - **Room Database** (`AniTrackDatabase` v2) with 8 normalized entities:
    - `AnimeEntity` (`anime_metadata`)
    - `UserAnimeEntity` (`user_anime` — personal status, watched episodes, favorites, reminders, personal notes)
    - `EpisodeHistoryEntity` (`episode_history` — per-episode watch timestamps & seasonal logs)
    - `ReviewEntity` (`reviews` — overall + category scores, review text, spoiler warning flag)
    - `WatchlistEntity` (`watchlists`)
    - `WatchlistAnimeEntity` (`watchlist_anime` — ordered positions)
    - `UserPreferencesEntity` (`user_preferences`)
    - `UserProfileEntity` (`user_profile`)
  - **Preferences DataStore** (`SettingsDataStore`) synced with `UserPreferencesEntity`
- **Remote Metadata API**:
  - **AniList GraphQL API** (`https://graphql.anilist.co/`) via Retrofit + OkHttp + `kotlinx.serialization`
  - Supports Trending, Popular, Top Rated, Seasonal, Live Search with filters & pagination, Anime Details (`Media` tags, studios, relations), and Airing Schedule queries
  - Built-in rate-limit handling (429 backoff retry + 12-second in-memory query throttle) and offline cache fallback that never overwrites user progress
- **Image Loading**: Coil (`AsyncImage`) with local bundled poster/avatar fallbacks

## All 14 Implemented Screens

1. **Home Dashboard** (`HomeDashboardScreen`): Personal journey metrics, quick actions, `Continue Watching` with `+1 Ep` logging & auto-completion prompts, `Recently Added`, and `Recommended For You`.
2. **Anime Library** (`AnimeLibraryScreen`): Filter by status (`All`, `Watching`, `Completed`, `Planned`, `On Hold`, `Dropped`), inline search, genre filter, sorting, Poster Grid / List View toggle, quick tracking edits, and library removal.
3. **Anime Details** (`AnimeDetailsScreen`): Hero banner & poster, AniList sync, status selector, community vs. personal scores, genre pills, library/favorite toggles, watchlist assignment, synopsis, studio & tags metadata, personal notes editor, spoiler-protected review preview, and related/similar titles.
4. **Episode Tracker** (`EpisodeTrackerScreen`): Series switcher, season selector, progress overview, `Mark Next Watched`, `Undo`, `Reset`, manual progress entry dialog with bounds validation, unknown episode count handling, and per-episode checkoff history.
5. **Discover** (`DiscoverScreen`): Genre chips, `Trending Now`, `Popular Right Now`, `Top Rated Classics`, `Seasonal & Upcoming`, manual AniList sync, and offline cached fallback banner.
6. **Search Results** (`SearchResultsScreen`): Live AniList GraphQL search with debounced submission, Genre / 2020+ / Format (`TV`, `MOVIE`, `OVA`) / Rating sort filters, `Load More` pagination, recent search chips, and Loading / Empty / Error + Retry / Results states.
7. **Statistics Dashboard** (`StatisticsDashboardScreen`): Database-derived totals (`Total Anime`, `Completed`, `Watching`, `Episodes`), watch-hours calculation, multi-segment Status Breakdown, 6-month `Monthly Activity` bar chart, `Rating Distribution` (6★–10★), top genres, and highlights.
8. **Custom Watchlists** (`CustomWatchlistsScreen`): Overview of personal collections with cover art, counts, and creation navigation.
9. **Create / Edit Watchlist** (`CreateEditWatchlistScreen`): Create, rename, edit description, choose cover art, add/remove anime, reorder entries (`Move Up` / `Move Down`), and delete watchlists.
10. **Ratings & Reviews** (`RatingsReviewsScreen`): `1.0–10.0` overall slider, 4 category ratings (`Story`, `Animation`, `Characters`, `Soundtrack`), written review, spoiler toggle, and Delete Review action.
11. **Anime Calendar** (`AnimeCalendarScreen`): Monthly airing calendar grid, AniList airing schedule sync, upcoming episode cards, and personal episode reminder toggles.
12. **Random Anime Picker** (`RandomAnimePickerScreen`): Picks from Planned collection (or Full Catalog toggle) with genre and episode-length filters (`Short <= 12`, `Standard <= 26`, `Long 27+`).
13. **User Profile** (`UserProfileScreen`): Display name, handle, avatar, editable bio & name, live stats, favorite genres, favorite anime 3-column grid, and personal watchlists.
14. **Settings** (`SettingsScreen`): Dark/Light theme toggle, notification toggles, default library sort/layout/auto-complete preferences, JSON export/import backup, and app info dialogs.

## Build & Test Instructions

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (`compileSdk = 36`, `minSdk = 26`) with `ANDROID_SDK_ROOT` configured
- Gradle `9.3.1` (use system `gradle` or compatible wrapper)

### Commands
- **Build Debug APK**:
  ```bash
  gradle :app:assembleDebug
  ```
- **Run Unit & Robolectric Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Lint Check**:
  ```bash
  gradle :app:lintDebug
  ```
