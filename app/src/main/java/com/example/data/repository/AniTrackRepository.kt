package com.example.data.repository

import com.example.data.local.AniTrackDao
import com.example.data.local.SeedData
import com.example.data.model.AnimeEntity
import com.example.data.model.AnimeSeries
import com.example.data.model.EpisodeHistoryEntity
import com.example.data.model.LibraryExportPayload
import com.example.data.model.ReviewEntity
import com.example.data.model.UserAnimeEntity
import com.example.data.model.UserPreferencesEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistAnimeEntity
import com.example.data.model.WatchlistEntity
import com.example.data.model.WatchlistWithItems
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.remote.AniListPageResult
import com.example.data.remote.AniListRemoteDataSource
import com.example.data.remote.AuthUser
import com.example.data.remote.FirebaseSyncManager
import com.example.data.util.AnimeSearchMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniTrackRepository @Inject constructor(
    private val dao: AniTrackDao,
    private val settingsDataStore: SettingsDataStore,
    private val remoteDataSource: AniListRemoteDataSource,
    private val json: Json,
    private val firebaseSyncManager: FirebaseSyncManager? = null
) {
    val allAnime: Flow<List<AnimeSeries>> = combine(
        dao.observeAllAnimeMetadata(),
        dao.observeAllUserAnime(),
        dao.observeAllReviews()
    ) { metadataList, trackingList, reviews ->
        val trackingMap = trackingList.associateBy { it.animeId }
        val reviewMap = reviews.associateBy { it.animeId }
        metadataList.map { meta ->
            AnimeSeries(
                metadata = meta,
                tracking = trackingMap[meta.id],
                review = reviewMap[meta.id]
            )
        }.sortedByDescending { it.updatedAt }
    }

    val allEpisodeHistory: Flow<List<EpisodeHistoryEntity>> = dao.observeAllEpisodeHistory()

    val watchlists: Flow<List<WatchlistWithItems>> = combine(
        dao.observeWatchlists(),
        dao.observeAllWatchlistAnime()
    ) { lists, crossRefs ->
        val grouped = crossRefs.groupBy { it.watchlistId }
        lists.map { wl ->
            WatchlistWithItems(
                watchlist = wl,
                animeIds = grouped[wl.id]?.sortedBy { it.position }?.map { it.animeId } ?: emptyList()
            )
        }
    }

    val profile: Flow<UserProfileEntity> = dao.observeProfile().map { it ?: SeedData.defaultProfile }
    val settings: Flow<UserSettings> = settingsDataStore.settingsFlow

    fun observeAnime(animeId: String): Flow<AnimeSeries?> = combine(
        dao.observeAnimeMetadataById(animeId),
        dao.observeUserAnimeById(animeId),
        dao.observeReviewByAnimeId(animeId)
    ) { meta, tracking, review ->
        meta?.let { AnimeSeries(it, tracking, review) }
    }

    fun observeEpisodes(animeId: String): Flow<List<EpisodeHistoryEntity>> =
        dao.observeEpisodesForAnime(animeId)

    /**
     * Seeds only the public anime catalog metadata, default profile fallback, and preferences.
     * New users intentionally begin with an EMPTY personal library, empty watchlists,
     * zero episode history, and zero reviews.
     */
    suspend fun ensureSeeded() {
        val existingMeta = dao.getAllAnimeMetadataOnce()
        val seedMeta = SeedData.initialMetadata()
        if (existingMeta.isEmpty()) {
            dao.upsertAnimeMetadataList(seedMeta)
        } else {
            val seedById = seedMeta.associateBy { it.id }
            val upgraded = existingMeta.mapNotNull { existing ->
                val seed = seedById[existing.id] ?: return@mapNotNull null
                val hasLegacyUrl = existing.remotePosterUrl.contains("lh3.googleusercontent.com/aida", ignoreCase = true) ||
                    existing.remotePosterUrl.isBlank() ||
                    existing.coverLargeUrl.isBlank() ||
                    existing.bannerUrl.contains("lh3.googleusercontent.com/aida", ignoreCase = true)
                if (hasLegacyUrl) {
                    existing.copy(
                        posterKey = "",
                        remotePosterUrl = seed.remotePosterUrl,
                        coverLargeUrl = seed.coverLargeUrl,
                        coverMediumUrl = seed.coverMediumUrl,
                        bannerUrl = seed.bannerUrl
                    )
                } else {
                    null
                }
            }
            if (upgraded.isNotEmpty()) {
                dao.upsertAnimeMetadataList(upgraded)
            }
        }
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(SeedData.defaultProfile)
        }
        if (dao.getPreferencesOnce() == null) {
            dao.upsertPreferences(SeedData.defaultPreferences)
        }
    }

    // ---- Firebase User Session Sync ----

    suspend fun syncSessionForUser(user: AuthUser) {
        ensureSeeded()
        val manager = firebaseSyncManager ?: return
        val snapshot = manager.ensureUserAndFetchSnapshot(user) ?: return

        dao.clearUserAnime()
        dao.clearEpisodeHistory()
        dao.clearReviews()
        dao.clearWatchlistAnime()
        dao.clearWatchlists()
        dao.upsertProfile(snapshot.profile)

        if (snapshot.library.isNotEmpty()) {
            dao.upsertUserAnimeList(snapshot.library)
            // Ensure metadata & episode checkboxes reflect synced episode counts
            snapshot.library.forEach { tracking ->
                val meta = dao.getAnimeMetadataById(tracking.animeId)
                    ?: tracking.animeId.removePrefix("al_").toIntOrNull()?.let { anilistId ->
                        remoteDataSource.fetchAnimeDetails(anilistId).getOrNull()?.also {
                            dao.upsertAnimeMetadata(it.copy(id = tracking.animeId))
                        }
                    }
                if (meta != null && tracking.watchedEpisodes > 0) {
                    ensureEpisodesExistForAnime(meta)
                    val eps = dao.getEpisodesForAnimeOnce(meta.id)
                    eps.forEach { ep ->
                        if (ep.episodeNumber <= tracking.watchedEpisodes && !ep.watched) {
                            dao.updateEpisode(
                                ep.copy(
                                    watched = true,
                                    subtitle = "Synced",
                                    watchedAtEpoch = ep.watchedAtEpoch ?: tracking.updatedAt
                                )
                            )
                        }
                    }
                }
            }
        }
        if (snapshot.reviews.isNotEmpty()) {
            dao.insertReviews(snapshot.reviews)
        }
        if (snapshot.watchlists.isNotEmpty()) {
            val wlEntities = snapshot.watchlists.map { it.first }
            dao.insertWatchlists(wlEntities)
            val refs = snapshot.watchlists.flatMap { (wl, ids) ->
                ids.mapIndexed { idx, animeId -> WatchlistAnimeEntity(wl.id, animeId, idx) }
            }
            if (refs.isNotEmpty()) {
                dao.insertWatchlistAnime(refs)
            }
        }
        settingsDataStore.setTutorialCompleted(snapshot.tutorialCompleted)
    }

    suspend fun clearUserSessionData() {
        dao.clearUserAnime()
        dao.clearEpisodeHistory()
        dao.clearReviews()
        dao.clearWatchlistAnime()
        dao.clearWatchlists()
        dao.clearProfile()
        dao.upsertProfile(SeedData.defaultProfile)
    }

    // ---- AniList Remote Sync (Metadata Only — Never Overwrites User Progress) ----

    suspend fun refreshSeededArtworkIfNeeded(): Result<Int> {
        val existingMeta = dao.getAllAnimeMetadataOnce()
        val missingOrLegacyIds = existingMeta
            .filter { meta ->
                meta.anilistId != null && (
                    meta.remotePosterUrl.isBlank() ||
                        meta.coverLargeUrl.isBlank() ||
                        meta.remotePosterUrl.contains("lh3.googleusercontent.com/aida", ignoreCase = true) ||
                        meta.bannerUrl.contains("lh3.googleusercontent.com/aida", ignoreCase = true)
                    )
            }
            .mapNotNull { it.anilistId }
            .distinct()
        if (missingOrLegacyIds.isEmpty()) return Result.success(0)
        val res = remoteDataSource.fetchMediaByIds(missingOrLegacyIds)
        res.getOrNull()?.takeIf { it.isNotEmpty() }?.let { remoteItems ->
            cacheRemoteMetadataSafely(remoteItems)
        }
        return res.map { it.size }
    }

    suspend fun refreshDiscoverFromAniList(): Result<Int> {
        val trendingRes = remoteDataSource.fetchTrending(page = 1, perPage = 10)
        val popularRes = remoteDataSource.fetchPopular(page = 1, perPage = 10)
        val topRes = remoteDataSource.fetchTopRated(page = 1, perPage = 10)
        val seasonalRes = remoteDataSource.fetchSeasonal(page = 1, perPage = 10)

        val collected = mutableListOf<AnimeEntity>()
        trendingRes.getOrNull()?.items?.let { collected.addAll(it) }
        popularRes.getOrNull()?.items?.let { collected.addAll(it) }
        topRes.getOrNull()?.items?.let { collected.addAll(it) }
        seasonalRes.getOrNull()?.items?.let { collected.addAll(it) }

        if (collected.isEmpty()) {
            val error = trendingRes.exceptionOrNull()
                ?: popularRes.exceptionOrNull()
                ?: IllegalStateException("Unable to reach AniList")
            return Result.failure(error)
        }

        val merged = mergeDistinctPreservingRanks(collected)
        cacheRemoteMetadataSafely(merged)
        return Result.success(merged.size)
    }

    suspend fun searchAniList(
        query: String?,
        genre: String?,
        year2020Plus: Boolean,
        format: String?,
        sortByRating: Boolean,
        page: Int
    ): Result<AniListPageResult> {
        val knownTitles = dao.getAllAnimeMetadataOnce().flatMap { listOf(it.title, it.romajiTitle) }
        val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }
        val canonicalQuery = cleanQuery?.let { AnimeSearchMatcher.resolveRemoteQuery(it, knownTitles) }

        var res = remoteDataSource.searchAnime(
            query = canonicalQuery,
            genre = genre,
            year2020Plus = year2020Plus,
            format = format,
            sortByRating = sortByRating,
            page = page
        )

        // If raw query wasn't canonicalized and returned empty, retry once with trimmed original
        if (cleanQuery != null &&
            canonicalQuery != null &&
            !canonicalQuery.equals(cleanQuery, ignoreCase = true) &&
            res.getOrNull()?.items.isNullOrEmpty()
        ) {
            val fallbackRes = remoteDataSource.searchAnime(
                query = cleanQuery,
                genre = genre,
                year2020Plus = year2020Plus,
                format = format,
                sortByRating = sortByRating,
                page = page
            )
            if (fallbackRes.isSuccess && !fallbackRes.getOrNull()?.items.isNullOrEmpty()) {
                res = fallbackRes
            }
        }

        res.getOrNull()?.items?.takeIf { it.isNotEmpty() }?.let { remoteItems ->
            cacheRemoteMetadataSafely(remoteItems)
        }
        return res
    }

    suspend fun syncAnimeDetailsIfNeeded(animeId: String): Result<AnimeEntity> {
        val existing = dao.getAnimeMetadataById(animeId)
        val anilistId = existing?.anilistId ?: animeId.removePrefix("al_").toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("No AniList ID for $animeId"))

        val now = System.currentTimeMillis()
        val hasValidCachedArtwork = existing != null &&
            existing.remotePosterUrl.isNotBlank() &&
            !existing.remotePosterUrl.contains("lh3.googleusercontent.com/aida", ignoreCase = true)
        if (existing != null &&
            hasValidCachedArtwork &&
            (now - existing.lastSyncedAt) < 10 * 60_000L &&
            existing.tagsCsv.isNotBlank()
        ) {
            return Result.success(existing)
        }

        val result = remoteDataSource.fetchAnimeDetails(anilistId)
        result.getOrNull()?.let { fetched ->
            val targetId = existing?.id ?: fetched.id
            val existingSafeBanner = existing?.bannerUrl
                ?.takeUnless { it.contains("lh3.googleusercontent.com/aida", ignoreCase = true) }
                .orEmpty()
            val merged = fetched.copy(
                id = targetId,
                remotePosterUrl = fetched.remotePosterUrl.ifBlank {
                    existing?.remotePosterUrl
                        ?.takeUnless { it.contains("lh3.googleusercontent.com/aida", ignoreCase = true) }
                        .orEmpty()
                },
                coverLargeUrl = fetched.coverLargeUrl.ifBlank { existing?.coverLargeUrl.orEmpty() },
                coverMediumUrl = fetched.coverMediumUrl.ifBlank { existing?.coverMediumUrl.orEmpty() },
                bannerUrl = fetched.bannerUrl.ifBlank { existingSafeBanner },
                trendingRank = existing?.trendingRank ?: fetched.trendingRank,
                popularityRank = existing?.popularityRank ?: fetched.popularityRank,
                seasonalTag = existing?.seasonalTag ?: fetched.seasonalTag
            )
            dao.upsertAnimeMetadata(merged)
            ensureEpisodesExistForAnime(merged)
        }
        return result
    }

    suspend fun refreshAiringCalendar(): Result<Int> {
        val nowSec = System.currentTimeMillis() / 1000L
        val sevenDaysSec = nowSec + 7L * 24L * 3600L
        val res = remoteDataSource.fetchAiringCalendar(fromEpochSec = nowSec, toEpochSec = sevenDaysSec)
        res.getOrNull()?.let { items ->
            if (items.isNotEmpty()) {
                cacheRemoteMetadataSafely(items)
            }
        }
        return res.map { it.size }
    }

    private suspend fun cacheRemoteMetadataSafely(items: List<AnimeEntity>) {
        val toSave = items.map { incoming ->
            val existingById = dao.getAnimeMetadataById(incoming.id)
                ?: incoming.anilistId?.let { dao.getAnimeByAnilistId(it) }
            if (existingById != null) {
                val safeBanner = existingById.bannerUrl
                    .takeUnless { it.contains("lh3.googleusercontent.com/aida", ignoreCase = true) }
                val safePoster = existingById.remotePosterUrl
                    .takeUnless { it.contains("lh3.googleusercontent.com/aida", ignoreCase = true) }
                incoming.copy(
                    id = existingById.id,
                    remotePosterUrl = incoming.remotePosterUrl.ifBlank { safePoster.orEmpty() },
                    coverLargeUrl = incoming.coverLargeUrl.ifBlank { existingById.coverLargeUrl },
                    coverMediumUrl = incoming.coverMediumUrl.ifBlank { existingById.coverMediumUrl },
                    bannerUrl = incoming.bannerUrl.ifBlank { safeBanner.orEmpty() },
                    trendingRank = incoming.trendingRank ?: existingById.trendingRank,
                    popularityRank = incoming.popularityRank ?: existingById.popularityRank,
                    seasonalTag = incoming.seasonalTag ?: existingById.seasonalTag
                )
            } else {
                incoming
            }
        }
        dao.upsertAnimeMetadataList(toSave)
    }

    private fun mergeDistinctPreservingRanks(items: List<AnimeEntity>): List<AnimeEntity> {
        val map = linkedMapOf<String, AnimeEntity>()
        for (item in items) {
            val prev = map[item.id]
            if (prev == null) {
                map[item.id] = item
            } else {
                map[item.id] = prev.copy(
                    trendingRank = prev.trendingRank ?: item.trendingRank,
                    popularityRank = prev.popularityRank ?: item.popularityRank,
                    seasonalTag = prev.seasonalTag ?: item.seasonalTag
                )
            }
        }
        return map.values.toList()
    }

    // ---- Personal Tracking & Episode Progress ----

    suspend fun ensureEpisodesExistForAnime(meta: AnimeEntity) {
        val existing = dao.getEpisodesForAnimeOnce(meta.id)
        if (existing.isNotEmpty()) return
        val total = meta.totalEpisodes ?: 12
        val count = total.coerceIn(1, 24)
        val generated = (1..count).map { epNum ->
            EpisodeHistoryEntity(
                id = "${meta.id}_s1_ep_$epNum",
                animeId = meta.id,
                seasonNumber = 1,
                episodeNumber = epNum,
                title = if (meta.totalEpisodes != null && epNum == meta.totalEpisodes) "Season Finale" else "Episode $epNum",
                subtitle = if (epNum == 1) "Up Next" else "Episode $epNum",
                watched = false
            )
        }
        dao.insertEpisodes(generated)
    }

    suspend fun incrementEpisode(animeId: String): Pair<AnimeSeries?, Boolean> {
        val meta = dao.getAnimeMetadataById(animeId) ?: return null to false
        ensureEpisodesExistForAnime(meta)
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(
            animeId = animeId,
            status = WatchStatus.WATCHING,
            inLibrary = true
        )
        val maxEp = meta.totalEpisodes
        if (maxEp != null && current.watchedEpisodes >= maxEp) {
            val review = dao.getReviewByAnimeId(animeId)
            return AnimeSeries(meta, current, review) to false
        }

        val newWatched = current.watchedEpisodes + 1
        val reachedFinal = maxEp != null && newWatched >= maxEp
        val updatedTracking = current.copy(
            watchedEpisodes = newWatched,
            inLibrary = true,
            status = if (current.status == WatchStatus.PLANNED) WatchStatus.WATCHING else current.status,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updatedTracking)
        syncLibraryToCloud(updatedTracking, meta)

        val episodes = dao.getEpisodesForAnimeOnce(animeId)
        val targetEp = episodes.firstOrNull { it.episodeNumber == newWatched && it.seasonNumber == updatedTracking.currentSeason }
            ?: episodes.firstOrNull { !it.watched }
        if (targetEp != null) {
            dao.updateEpisode(
                targetEp.copy(
                    watched = true,
                    subtitle = "Watched ${todayLabel()}",
                    watchedAtEpoch = System.currentTimeMillis(),
                    watchedDateLabel = todayLabel()
                )
            )
        } else {
            dao.upsertEpisode(
                EpisodeHistoryEntity(
                    id = "${animeId}_s${updatedTracking.currentSeason}_ep_$newWatched",
                    animeId = animeId,
                    seasonNumber = updatedTracking.currentSeason,
                    episodeNumber = newWatched,
                    title = "Episode $newWatched",
                    subtitle = "Watched ${todayLabel()}",
                    watched = true,
                    watchedAtEpoch = System.currentTimeMillis(),
                    watchedDateLabel = todayLabel()
                )
            )
        }
        val review = dao.getReviewByAnimeId(animeId)
        return AnimeSeries(meta, updatedTracking, review) to reachedFinal
    }

    suspend fun setWatchedEpisodesManual(animeId: String, requestedCount: Int) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        ensureEpisodesExistForAnime(meta)
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, status = WatchStatus.WATCHING)
        val maxEp = meta.totalEpisodes ?: Int.MAX_VALUE
        val safeCount = requestedCount.coerceIn(0, maxEp)
        val updated = current.copy(
            watchedEpisodes = safeCount,
            inLibrary = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
        val episodes = dao.getEpisodesForAnimeOnce(animeId)
        val now = System.currentTimeMillis()
        episodes.forEach { ep ->
            val shouldWatch = ep.episodeNumber <= safeCount
            if (ep.watched != shouldWatch) {
                dao.updateEpisode(
                    ep.copy(
                        watched = shouldWatch,
                        subtitle = if (shouldWatch) "Watched ${todayLabel()}" else "Unwatched",
                        watchedAtEpoch = if (shouldWatch) (ep.watchedAtEpoch ?: now) else null,
                        watchedDateLabel = if (shouldWatch) todayLabel() else null
                    )
                )
            }
        }
    }

    suspend fun setCurrentSeason(animeId: String, seasonNumber: Int) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, status = WatchStatus.WATCHING)
        val updated = current.copy(
            currentSeason = seasonNumber.coerceIn(1, meta.seasonsCount),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun toggleEpisodeWatched(animeId: String, episodeNumber: Int) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, status = WatchStatus.WATCHING)
        val episodes = dao.getEpisodesForAnimeOnce(animeId)
        val ep = episodes.firstOrNull { it.episodeNumber == episodeNumber } ?: return
        val newWatchedState = !ep.watched
        dao.updateEpisode(
            ep.copy(
                watched = newWatchedState,
                subtitle = if (newWatchedState) "Watched ${todayLabel()}" else if (meta.totalEpisodes == episodeNumber) "Season Finale" else "Unwatched",
                watchedAtEpoch = if (newWatchedState) System.currentTimeMillis() else null,
                watchedDateLabel = if (newWatchedState) todayLabel() else null
            )
        )
        val delta = if (newWatchedState) 1 else -1
        val maxEp = meta.totalEpisodes ?: Int.MAX_VALUE
        val newCount = (current.watchedEpisodes + delta).coerceIn(0, maxEp)
        val updated = current.copy(
            watchedEpisodes = newCount,
            inLibrary = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun undoLastEpisode(animeId: String) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: return
        if (current.watchedEpisodes <= 0) return
        val episodes = dao.getEpisodesForAnimeOnce(animeId)
        val lastWatched = episodes.lastOrNull { it.watched }
        if (lastWatched != null) {
            dao.updateEpisode(
                lastWatched.copy(
                    watched = false,
                    subtitle = "Up Next",
                    watchedAtEpoch = null,
                    watchedDateLabel = null
                )
            )
        }
        val newCount = (current.watchedEpisodes - 1).coerceAtLeast(0)
        val updated = current.copy(watchedEpisodes = newCount, updatedAt = System.currentTimeMillis())
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun resetEpisodes(animeId: String) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: return
        val episodes = dao.getEpisodesForAnimeOnce(animeId)
        episodes.filter { it.watched }.forEach { ep ->
            dao.updateEpisode(
                ep.copy(
                    watched = false,
                    subtitle = if (ep.episodeNumber == 1) "Up Next" else if (meta.totalEpisodes == ep.episodeNumber) "Season Finale" else "Unwatched",
                    watchedAtEpoch = null,
                    watchedDateLabel = null
                )
            )
        }
        val updated = current.copy(watchedEpisodes = 0, updatedAt = System.currentTimeMillis())
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun toggleFavorite(animeId: String) {
        val meta = dao.getAnimeMetadataById(animeId)
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, inLibrary = true)
        val updated = current.copy(
            isFavorite = !current.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        if (meta != null) syncLibraryToCloud(updated, meta)
    }

    suspend fun toggleInLibrary(animeId: String) {
        val meta = dao.getAnimeMetadataById(animeId)
        val current = dao.getUserAnimeById(animeId)
        val updated = if (current == null) {
            UserAnimeEntity(
                animeId = animeId,
                status = WatchStatus.PLANNED,
                inLibrary = true
            )
        } else {
            current.copy(
                inLibrary = !current.inLibrary,
                updatedAt = System.currentTimeMillis()
            )
        }
        dao.upsertUserAnime(updated)
        if (meta != null) {
            ensureEpisodesExistForAnime(meta)
            syncLibraryToCloud(updated, meta)
        }
    }

    suspend fun updateStatus(animeId: String, status: WatchStatus) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId)
        val watched = if (status == WatchStatus.COMPLETED && meta.totalEpisodes != null) {
            meta.totalEpisodes
        } else {
            current.watchedEpisodes
        }
        val updated = current.copy(
            status = status,
            inLibrary = true,
            watchedEpisodes = watched,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        ensureEpisodesExistForAnime(meta)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun updateTrackingInfo(
        animeId: String,
        status: WatchStatus,
        watchedEpisodes: Int,
        personalNotes: String
    ) {
        val meta = dao.getAnimeMetadataById(animeId) ?: return
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId)
        val maxEp = meta.totalEpisodes ?: Int.MAX_VALUE
        val clampedWatched = if (status == WatchStatus.COMPLETED && meta.totalEpisodes != null) {
            meta.totalEpisodes
        } else {
            watchedEpisodes.coerceIn(0, maxEp)
        }
        val updated = current.copy(
            status = status,
            watchedEpisodes = clampedWatched,
            personalNotes = personalNotes.trim(),
            inLibrary = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        syncLibraryToCloud(updated, meta)
    }

    suspend fun updatePersonalNotes(animeId: String, notes: String) {
        val meta = dao.getAnimeMetadataById(animeId)
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, inLibrary = true)
        val updated = current.copy(
            personalNotes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAnime(updated)
        if (meta != null) syncLibraryToCloud(updated, meta)
    }

    suspend fun saveReview(
        animeId: String,
        overall: Float,
        story: Int,
        animation: Int,
        characters: Int,
        soundtrack: Int,
        reviewText: String,
        containsSpoilers: Boolean
    ) {
        val entity = ReviewEntity(
            animeId = animeId,
            overallRating = overall.coerceIn(1f, 10f),
            storyScore = story.coerceIn(1, 10),
            animationScore = animation.coerceIn(1, 10),
            charactersScore = characters.coerceIn(1, 10),
            soundtrackScore = soundtrack.coerceIn(1, 10),
            reviewText = reviewText.trim(),
            containsSpoilers = containsSpoilers,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertReview(entity)
        firebaseSyncManager?.pushReview(entity)
    }

    suspend fun deleteReview(animeId: String) {
        dao.deleteReviewByAnimeId(animeId)
        firebaseSyncManager?.deleteReview(animeId)
    }

    suspend fun toggleReminder(animeId: String) {
        val meta = dao.getAnimeMetadataById(animeId)
        val current = dao.getUserAnimeById(animeId) ?: UserAnimeEntity(animeId = animeId, inLibrary = false)
        val updated = current.copy(reminderEnabled = !current.reminderEnabled)
        dao.upsertUserAnime(updated)
        if (meta != null) syncLibraryToCloud(updated, meta)
    }

    // ---- Watchlists ----

    suspend fun saveWatchlist(
        watchlistId: String?,
        name: String,
        description: String,
        coverKey: String,
        animeIds: List<String>
    ) {
        val id = watchlistId ?: "wl_${UUID.randomUUID().toString().take(8)}"
        val entity = WatchlistEntity(
            id = id,
            name = name.trim().ifEmpty { "Untitled Watchlist" },
            description = description.trim(),
            coverKey = coverKey,
            updatedDateLabel = "Updated ${todayLabel()}",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertWatchlist(entity)
        dao.deleteWatchlistAnimeForWatchlist(id)
        val distinctIds = animeIds.distinct()
        val crossRefs = distinctIds.mapIndexed { idx, animeId ->
            WatchlistAnimeEntity(watchlistId = id, animeId = animeId, position = idx)
        }
        if (crossRefs.isNotEmpty()) {
            dao.insertWatchlistAnime(crossRefs)
        }
        firebaseSyncManager?.pushWatchlist(entity, distinctIds)
    }

    suspend fun deleteWatchlist(watchlistId: String) {
        dao.deleteWatchlistAnimeForWatchlist(watchlistId)
        dao.deleteWatchlistById(watchlistId)
        firebaseSyncManager?.deleteWatchlist(watchlistId)
    }

    suspend fun addAnimeToWatchlist(watchlistId: String, animeId: String) {
        val wl = dao.getWatchlistById(watchlistId) ?: return
        val existingRefs = dao.getWatchlistAnimeOnce(watchlistId)
        if (existingRefs.any { it.animeId == animeId }) return
        val updatedRefs = existingRefs + WatchlistAnimeEntity(
            watchlistId = watchlistId,
            animeId = animeId,
            position = existingRefs.size
        )
        dao.insertWatchlistAnime(updatedRefs)
        val updatedWl = wl.copy(
            updatedDateLabel = "Updated ${todayLabel()}",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertWatchlist(updatedWl)
        firebaseSyncManager?.pushWatchlist(updatedWl, updatedRefs.map { it.animeId })
    }

    // ---- Profile, Export/Import & Preferences ----

    suspend fun updateProfile(displayName: String, bio: String) {
        val current = dao.getProfileOnce() ?: SeedData.defaultProfile
        val updated = current.copy(
            displayName = displayName.trim().ifEmpty { current.displayName },
            bio = bio.trim()
        )
        dao.upsertProfile(updated)
        val tutorialDone = settingsDataStore.settingsFlow.firstOrNull()?.tutorialCompleted ?: false
        firebaseSyncManager?.pushProfile(updated, tutorialDone)
    }

    suspend fun setTutorialCompleted(completed: Boolean) {
        settingsDataStore.setTutorialCompleted(completed)
        firebaseSyncManager?.pushTutorialCompleted(completed)
    }

    suspend fun setLanguageTag(tag: String) {
        settingsDataStore.setLanguageTag(tag)
    }

    suspend fun exportLibraryJson(): String {
        val payload = LibraryExportPayload(
            exportedAt = System.currentTimeMillis(),
            profile = dao.getProfileOnce(),
            anime = dao.getAllAnimeMetadataOnce(),
            userAnime = dao.getAllUserAnimeOnce(),
            reviews = dao.getAllReviewsOnce(),
            watchlists = dao.getAllWatchlistsOnce(),
            watchlistAnime = dao.getAllWatchlistAnimeOnce(),
            episodeHistory = dao.getAllEpisodeHistoryOnce()
        )
        return json.encodeToString(payload)
    }

    suspend fun importLibraryJson(rawJson: String): Result<Int> = runCatching {
        val payload = json.decodeFromString<LibraryExportPayload>(rawJson)
        if (payload.anime.isNotEmpty()) dao.upsertAnimeMetadataList(payload.anime)
        if (payload.userAnime.isNotEmpty()) dao.upsertUserAnimeList(payload.userAnime)
        if (payload.reviews.isNotEmpty()) dao.insertReviews(payload.reviews)
        if (payload.watchlists.isNotEmpty()) dao.insertWatchlists(payload.watchlists)
        if (payload.watchlistAnime.isNotEmpty()) dao.insertWatchlistAnime(payload.watchlistAnime)
        if (payload.episodeHistory.isNotEmpty()) dao.insertEpisodes(payload.episodeHistory)
        payload.profile?.let { dao.upsertProfile(it) }
        payload.anime.size
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        settingsDataStore.setDarkTheme(isDark)
        syncPrefsEntity { it.copy(isDarkTheme = isDark) }
    }

    suspend fun setEpisodeReminders(enabled: Boolean) {
        settingsDataStore.setEpisodeReminders(enabled)
        syncPrefsEntity { it.copy(episodeReminders = enabled) }
    }

    suspend fun setReleaseNotifications(enabled: Boolean) {
        settingsDataStore.setReleaseNotifications(enabled)
        syncPrefsEntity { it.copy(releaseNotifications = enabled) }
    }

    suspend fun setDefaultSort(sort: String) {
        settingsDataStore.setDefaultSort(sort)
        syncPrefsEntity { it.copy(defaultSort = sort) }
    }

    suspend fun setDefaultViewGrid(isGrid: Boolean) {
        settingsDataStore.setDefaultViewGrid(isGrid)
        syncPrefsEntity { it.copy(defaultViewGrid = isGrid) }
    }

    suspend fun setAutoStatusSuggestions(enabled: Boolean) {
        settingsDataStore.setAutoStatusSuggestions(enabled)
        syncPrefsEntity { it.copy(autoStatusSuggestions = enabled) }
    }

    suspend fun syncBackupNow() {
        val label = "Synced ${todayLabel()}"
        settingsDataStore.updateBackupLabel(label)
        syncPrefsEntity { it.copy(lastBackupLabel = label) }
        val metaMap = dao.getAllAnimeMetadataOnce().associateBy { it.id }
        dao.getAllUserAnimeOnce().forEach { tracking ->
            metaMap[tracking.animeId]?.let { syncLibraryToCloud(tracking, it) }
        }
        dao.getAllReviewsOnce().forEach { firebaseSyncManager?.pushReview(it) }
        val refsMap = dao.getAllWatchlistAnimeOnce().groupBy { it.watchlistId }
        dao.getAllWatchlistsOnce().forEach { wl ->
            val ids = refsMap[wl.id]?.sortedBy { it.position }?.map { it.animeId }.orEmpty()
            firebaseSyncManager?.pushWatchlist(wl, ids)
        }
    }

    suspend fun addRecentSearch(query: String) {
        settingsDataStore.addRecentSearch(query)
    }

    private suspend fun syncLibraryToCloud(tracking: UserAnimeEntity, meta: AnimeEntity) {
        firebaseSyncManager?.pushLibraryEntry(
            tracking = tracking,
            title = meta.title,
            posterUrl = meta.remotePosterUrl
        )
    }

    private suspend fun syncPrefsEntity(transform: (UserPreferencesEntity) -> UserPreferencesEntity) {
        val current = dao.getPreferencesOnce() ?: SeedData.defaultPreferences
        dao.upsertPreferences(transform(current))
    }

    private fun todayLabel(): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    }
}
