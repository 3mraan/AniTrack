package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class WatchStatus(val label: String) {
    WATCHING("Watching"),
    COMPLETED("Completed"),
    PLANNED("Planned"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped")
}

@Serializable
@Entity(tableName = "anime_metadata")
data class AnimeEntity(
    @PrimaryKey val id: String,
    val anilistId: Int? = null,
    val title: String,
    val romajiTitle: String,
    val year: Int,
    val season: String = "FALL",
    val format: String,
    val totalEpisodes: Int?,
    val communityRating: Float,
    val synopsis: String,
    val studio: String,
    val sourceMaterial: String,
    val episodeDurationMinutes: Int,
    val genresCsv: String,
    val tagsCsv: String = "",
    val posterKey: String,
    val remotePosterUrl: String,
    val coverLargeUrl: String = "",
    val coverMediumUrl: String = "",
    val bannerUrl: String,
    val airingTimeLabel: String? = null,
    val airingDayOfMonth: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val nextEpisodeTitle: String? = null,
    val nextAiringAtEpoch: Long? = null,
    val trendingRank: Int? = null,
    val popularityRank: Int? = null,
    val seasonalTag: String? = null,
    val seasonsCount: Int = 1,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "user_anime")
data class UserAnimeEntity(
    @PrimaryKey val animeId: String,
    val status: WatchStatus = WatchStatus.PLANNED,
    val watchedEpisodes: Int = 0,
    val currentSeason: Int = 1,
    val inLibrary: Boolean = true,
    val isFavorite: Boolean = false,
    val reminderEnabled: Boolean = false,
    val personalNotes: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "episode_history")
data class EpisodeHistoryEntity(
    @PrimaryKey val id: String,
    val animeId: String,
    val seasonNumber: Int = 1,
    val episodeNumber: Int,
    val title: String,
    val subtitle: String,
    val watched: Boolean,
    val watchedAtEpoch: Long? = null,
    val watchedDateLabel: String? = null
)

@Serializable
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val animeId: String,
    val overallRating: Float,
    val storyScore: Int,
    val animationScore: Int,
    val charactersScore: Int,
    val soundtrackScore: Int,
    val reviewText: String,
    val containsSpoilers: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverKey: String,
    val updatedDateLabel: String,
    val updatedAt: Long
)

@Serializable
@Entity(
    tableName = "watchlist_anime",
    primaryKeys = ["watchlistId", "animeId"]
)
data class WatchlistAnimeEntity(
    val watchlistId: String,
    val animeId: String,
    val position: Int
)

@Serializable
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkTheme: Boolean = true,
    val episodeReminders: Boolean = true,
    val releaseNotifications: Boolean = true,
    val defaultSort: String = "Recently Updated",
    val defaultViewGrid: Boolean = true,
    val autoStatusSuggestions: Boolean = true,
    val lastBackupLabel: String = "Synced Today",
    val recentSearchesCsv: String = "Solo Leveling|Vinland Saga"
)

@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val handle: String,
    val bio: String,
    val favoriteGenresCsv: String
) {
    val favoriteGenres: List<String>
        get() = favoriteGenresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * Combined UI projection merging external metadata, user tracking state, and review.
 */
data class AnimeSeries(
    val metadata: AnimeEntity,
    val tracking: UserAnimeEntity?,
    val review: ReviewEntity?
) {
    val id: String get() = metadata.id
    val anilistId: Int? get() = metadata.anilistId
    val title: String get() = metadata.title
    val romajiTitle: String get() = metadata.romajiTitle
    val year: Int get() = metadata.year
    val season: String get() = metadata.season
    val format: String get() = metadata.format
    val totalEpisodes: Int? get() = metadata.totalEpisodes
    val watchedEpisodes: Int get() = tracking?.watchedEpisodes ?: 0
    val currentSeason: Int get() = tracking?.currentSeason ?: 1
    val status: WatchStatus get() = tracking?.status ?: WatchStatus.PLANNED
    val inLibrary: Boolean get() = tracking?.inLibrary ?: false
    val isFavorite: Boolean get() = tracking?.isFavorite ?: false
    val reminderEnabled: Boolean get() = tracking?.reminderEnabled ?: false
    val personalNotes: String get() = tracking?.personalNotes ?: ""
    val updatedAt: Long get() = tracking?.updatedAt ?: metadata.lastSyncedAt
    val communityRating: Float get() = metadata.communityRating
    val userRating: Float? get() = review?.overallRating
    val storyScore: Int get() = review?.storyScore ?: 8
    val animationScore: Int get() = review?.animationScore ?: 8
    val charactersScore: Int get() = review?.charactersScore ?: 8
    val soundtrackScore: Int get() = review?.soundtrackScore ?: 8
    val personalReview: String get() = review?.reviewText ?: ""
    val containsSpoilers: Boolean get() = review?.containsSpoilers ?: false
    val synopsis: String get() = metadata.synopsis
    val studio: String get() = metadata.studio
    val sourceMaterial: String get() = metadata.sourceMaterial
    val episodeDurationMinutes: Int get() = metadata.episodeDurationMinutes
    val genres: List<String>
        get() = metadata.genresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val tags: List<String>
        get() = metadata.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val posterKey: String get() = metadata.posterKey
    val remotePosterUrl: String get() = metadata.remotePosterUrl
    val coverLargeUrl: String get() = metadata.coverLargeUrl
    val coverMediumUrl: String get() = metadata.coverMediumUrl
    val coverFallbackUrls: List<String>
        get() = listOf(coverLargeUrl, coverMediumUrl)
            .filter { it.isNotBlank() && it != remotePosterUrl }
            .distinct()
    val bannerUrl: String get() = metadata.bannerUrl
    val airingTimeLabel: String? get() = metadata.airingTimeLabel
    val airingDayOfMonth: Int? get() = metadata.airingDayOfMonth
    val nextEpisodeNumber: Int? get() = metadata.nextEpisodeNumber
    val nextEpisodeTitle: String? get() = metadata.nextEpisodeTitle
    val trendingRank: Int? get() = metadata.trendingRank
    val popularityRank: Int? get() = metadata.popularityRank
    val seasonalTag: String? get() = metadata.seasonalTag
    val seasonsCount: Int get() = metadata.seasonsCount

    val progressFraction: Float
        get() {
            val total = totalEpisodes ?: return if (watchedEpisodes > 0) 0.5f else 0f
            return if (total <= 0) 0f else (watchedEpisodes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    val episodeCountLabel: String
        get() = totalEpisodes?.let { "$it eps" } ?: "Ongoing"
}

data class WatchlistWithItems(
    val watchlist: WatchlistEntity,
    val animeIds: List<String>
) {
    val id: String get() = watchlist.id
    val name: String get() = watchlist.name
    val description: String get() = watchlist.description
    val coverKey: String get() = watchlist.coverKey
    val updatedDateLabel: String get() = watchlist.updatedDateLabel
    val updatedAt: Long get() = watchlist.updatedAt
}

data class MonthlyEpisodePoint(
    val month: String,
    val episodes: Int,
    val isHighlight: Boolean = false
)

data class RatingBucketPoint(
    val scoreBucket: Int,
    val count: Int
)

@Serializable
data class LibraryExportPayload(
    val exportedAt: Long,
    val profile: UserProfileEntity?,
    val anime: List<AnimeEntity>,
    val userAnime: List<UserAnimeEntity>,
    val reviews: List<ReviewEntity>,
    val watchlists: List<WatchlistEntity>,
    val watchlistAnime: List<WatchlistAnimeEntity>,
    val episodeHistory: List<EpisodeHistoryEntity> = emptyList()
)
