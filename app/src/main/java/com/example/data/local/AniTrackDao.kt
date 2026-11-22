package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AnimeEntity
import com.example.data.model.EpisodeHistoryEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserAnimeEntity
import com.example.data.model.UserPreferencesEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistAnimeEntity
import com.example.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AniTrackDao {

    // Metadata
    @Query("SELECT * FROM anime_metadata")
    fun observeAllAnimeMetadata(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime_metadata WHERE id = :id")
    fun observeAnimeMetadataById(id: String): Flow<AnimeEntity?>

    @Query("SELECT * FROM anime_metadata WHERE id = :id")
    suspend fun getAnimeMetadataById(id: String): AnimeEntity?

    @Query("SELECT * FROM anime_metadata WHERE anilistId = :anilistId LIMIT 1")
    suspend fun getAnimeByAnilistId(anilistId: Int): AnimeEntity?

    @Query("SELECT * FROM anime_metadata")
    suspend fun getAllAnimeMetadataOnce(): List<AnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnimeMetadataList(items: List<AnimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnimeMetadata(item: AnimeEntity)

    // User Tracking
    @Query("SELECT * FROM user_anime")
    fun observeAllUserAnime(): Flow<List<UserAnimeEntity>>

    @Query("SELECT * FROM user_anime WHERE animeId = :animeId")
    fun observeUserAnimeById(animeId: String): Flow<UserAnimeEntity?>

    @Query("SELECT * FROM user_anime WHERE animeId = :animeId")
    suspend fun getUserAnimeById(animeId: String): UserAnimeEntity?

    @Query("SELECT * FROM user_anime")
    suspend fun getAllUserAnimeOnce(): List<UserAnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAnime(tracking: UserAnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAnimeList(items: List<UserAnimeEntity>)

    @Query("DELETE FROM user_anime WHERE animeId = :animeId")
    suspend fun deleteUserAnimeById(animeId: String)

    // Episode History
    @Query("SELECT * FROM episode_history")
    fun observeAllEpisodeHistory(): Flow<List<EpisodeHistoryEntity>>

    @Query("SELECT * FROM episode_history WHERE animeId = :animeId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodesForAnime(animeId: String): Flow<List<EpisodeHistoryEntity>>

    @Query("SELECT * FROM episode_history WHERE animeId = :animeId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForAnimeOnce(animeId: String): List<EpisodeHistoryEntity>

    @Query("SELECT * FROM episode_history")
    suspend fun getAllEpisodeHistoryOnce(): List<EpisodeHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: EpisodeHistoryEntity)

    @Update
    suspend fun updateEpisode(episode: EpisodeHistoryEntity)

    // Reviews
    @Query("SELECT * FROM reviews")
    fun observeAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE animeId = :animeId")
    fun observeReviewByAnimeId(animeId: String): Flow<ReviewEntity?>

    @Query("SELECT * FROM reviews WHERE animeId = :animeId")
    suspend fun getReviewByAnimeId(animeId: String): ReviewEntity?

    @Query("SELECT * FROM reviews")
    suspend fun getAllReviewsOnce(): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Query("DELETE FROM reviews WHERE animeId = :animeId")
    suspend fun deleteReviewByAnimeId(animeId: String)

    // Watchlists
    @Query("SELECT * FROM watchlists ORDER BY updatedAt DESC")
    fun observeWatchlists(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getWatchlistById(id: String): WatchlistEntity?

    @Query("SELECT * FROM watchlists")
    suspend fun getAllWatchlistsOnce(): List<WatchlistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlist(watchlist: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlists(watchlists: List<WatchlistEntity>)

    @Query("DELETE FROM watchlists WHERE id = :id")
    suspend fun deleteWatchlistById(id: String)

    // Watchlist Cross-References
    @Query("SELECT * FROM watchlist_anime ORDER BY position ASC")
    fun observeAllWatchlistAnime(): Flow<List<WatchlistAnimeEntity>>

    @Query("SELECT * FROM watchlist_anime")
    suspend fun getAllWatchlistAnimeOnce(): List<WatchlistAnimeEntity>

    @Query("SELECT * FROM watchlist_anime WHERE watchlistId = :watchlistId ORDER BY position ASC")
    suspend fun getWatchlistAnimeOnce(watchlistId: String): List<WatchlistAnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistAnime(items: List<WatchlistAnimeEntity>)

    @Query("DELETE FROM watchlist_anime WHERE watchlistId = :watchlistId")
    suspend fun deleteWatchlistAnimeForWatchlist(watchlistId: String)

    // User Preferences Entity
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun observePreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: UserPreferencesEntity)

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    // Session Isolation Clears (keeps shared anime_metadata cache intact)
    @Query("DELETE FROM user_anime")
    suspend fun clearUserAnime()

    @Query("DELETE FROM episode_history")
    suspend fun clearEpisodeHistory()

    @Query("DELETE FROM reviews")
    suspend fun clearReviews()

    @Query("DELETE FROM watchlists")
    suspend fun clearWatchlists()

    @Query("DELETE FROM watchlist_anime")
    suspend fun clearWatchlistAnime()

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}
