package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LibraryRoute

@Serializable
data class AnimeDetailsRoute(val animeId: String)

@Serializable
data class EpisodeTrackerRoute(val animeId: String = "solo_leveling")

@Serializable
data object DiscoverRoute

@Serializable
data class SearchRoute(val initialGenre: String? = null)

@Serializable
data object StatsRoute

@Serializable
data object WatchlistsRoute

@Serializable
data class EditWatchlistRoute(val watchlistId: String? = null)

@Serializable
data class RateReviewRoute(val animeId: String)

@Serializable
data object CalendarRoute

@Serializable
data object RandomPickerRoute

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute
