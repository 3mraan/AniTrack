package com.example.data.remote

import com.example.data.model.AnimeEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AniListPageResult(
    val items: List<AnimeEntity>,
    val currentPage: Int,
    val hasNextPage: Boolean
)

@Singleton
class AniListRemoteDataSource @Inject constructor(
    private val apiService: AniListApiService
) {
    private val mediaFieldsFragment = """
        id
        title { english romaji native }
        format
        status
        description(asHtml: false)
        season
        seasonYear
        episodes
        duration
        source
        averageScore
        popularity
        trending
        genres
        tags { name isMediaSpoiler }
        coverImage { extraLarge large medium }
        bannerImage
        studios(isMain: true) { nodes { name isAnimationStudio } }
        nextAiringEpisode { airingAt episode }
        relations { edges { relationType node { id type format } } }
    """.trimIndent()

    private val pageQuery = """
        query (
          ${'$'}page: Int,
          ${'$'}perPage: Int,
          ${'$'}search: String,
          ${'$'}idIn: [Int],
          ${'$'}genres: [String],
          ${'$'}season: MediaSeason,
          ${'$'}seasonYear: Int,
          ${'$'}format: MediaFormat,
          ${'$'}sort: [MediaSort],
          ${'$'}yearGreater: FuzzyDateInt
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo { total currentPage lastPage hasNextPage }
            media(
              type: ANIME,
              isAdult: false,
              search: ${'$'}search,
              id_in: ${'$'}idIn,
              genre_in: ${'$'}genres,
              season: ${'$'}season,
              seasonYear: ${'$'}seasonYear,
              format: ${'$'}format,
              sort: ${'$'}sort,
              startDate_greater: ${'$'}yearGreater
            ) {
              $mediaFieldsFragment
            }
          }
        }
    """.trimIndent()

    private val detailsQuery = """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            $mediaFieldsFragment
          }
        }
    """.trimIndent()

    private val airingCalendarQuery = """
        query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}airingAtGreater: Int, ${'$'}airingAtLesser: Int) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo { total currentPage lastPage hasNextPage }
            airingSchedules(
              airingAt_greater: ${'$'}airingAtGreater,
              airingAt_lesser: ${'$'}airingAtLesser,
              sort: TIME
            ) {
              id
              airingAt
              episode
              media {
                $mediaFieldsFragment
              }
            }
          }
        }
    """.trimIndent()

    // Simple in-memory request throttle / cache to respect AniList 90 req/min rate limits
    private var lastFetchKey: String? = null
    private var lastFetchAtMs: Long = 0L
    private var cachedPageResult: AniListPageResult? = null

    suspend fun fetchTrending(page: Int = 1, perPage: Int = 12): Result<AniListPageResult> {
        return queryPage(
            cacheKey = "trending_$page",
            variables = AniListVariables(
                page = page,
                perPage = perPage,
                sort = listOf("TRENDING_DESC")
            ),
            isTrending = true
        )
    }

    suspend fun fetchPopular(page: Int = 1, perPage: Int = 12): Result<AniListPageResult> {
        return queryPage(
            cacheKey = "popular_$page",
            variables = AniListVariables(
                page = page,
                perPage = perPage,
                sort = listOf("POPULARITY_DESC")
            )
        )
    }

    suspend fun fetchTopRated(page: Int = 1, perPage: Int = 12): Result<AniListPageResult> {
        return queryPage(
            cacheKey = "top_rated_$page",
            variables = AniListVariables(
                page = page,
                perPage = perPage,
                sort = listOf("SCORE_DESC")
            )
        )
    }

    suspend fun fetchSeasonal(
        season: String = "FALL",
        seasonYear: Int = 2026,
        page: Int = 1,
        perPage: Int = 12
    ): Result<AniListPageResult> {
        return queryPage(
            cacheKey = "seasonal_${season}_${seasonYear}_$page",
            variables = AniListVariables(
                page = page,
                perPage = perPage,
                season = season,
                seasonYear = seasonYear,
                sort = listOf("POPULARITY_DESC")
            ),
            seasonalLabel = "${season.lowercase().replaceFirstChar { it.uppercase() }} $seasonYear"
        )
    }

    suspend fun searchAnime(
        query: String?,
        genre: String?,
        year2020Plus: Boolean,
        format: String?,
        sortByRating: Boolean,
        page: Int = 1,
        perPage: Int = 14
    ): Result<AniListPageResult> {
        val cleanQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val genreList = genre?.takeIf { it != "All" }?.let { listOf(it) }
        val mappedFormat = format?.takeIf { it != "All" }
        val sort = if (sortByRating) listOf("SCORE_DESC", "POPULARITY_DESC") else listOf("POPULARITY_DESC")
        return queryPage(
            cacheKey = "search_${cleanQuery}_${genre}_${year2020Plus}_${mappedFormat}_${sortByRating}_$page",
            variables = AniListVariables(
                page = page,
                perPage = perPage,
                search = cleanQuery,
                genres = genreList,
                format = mappedFormat,
                yearGreater = if (year2020Plus) 20200101 else null,
                sort = sort
            )
        )
    }

    suspend fun fetchMediaByIds(ids: List<Int>): Result<List<AnimeEntity>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return queryPage(
            cacheKey = "ids_${ids.sorted().joinToString("_")}",
            variables = AniListVariables(
                page = 1,
                perPage = ids.size.coerceIn(1, 50),
                idIn = ids
            )
        ).map { it.items }
    }

    suspend fun fetchAnimeDetails(anilistId: Int): Result<AnimeEntity> {
        return executeWithRateLimitRetry {
            val response = apiService.executeQuery(
                AniListGraphQlRequest(
                    query = detailsQuery,
                    variables = AniListVariables(id = anilistId)
                )
            )
            if (!response.isSuccessful) {
                throw AniListHttpException(response.code(), "AniList HTTP ${response.code()}")
            }
            val body = response.body() ?: throw IllegalStateException("Empty AniList response")
            if (!body.errors.isNullOrEmpty()) {
                throw IllegalStateException(body.errors.first().message ?: "AniList GraphQL error")
            }
            val media = body.data?.Media ?: throw IllegalStateException("Media #$anilistId not found")
            mapMediaToEntity(media)
        }
    }

    suspend fun fetchAiringCalendar(
        fromEpochSec: Long,
        toEpochSec: Long,
        page: Int = 1
    ): Result<List<AnimeEntity>> {
        return executeWithRateLimitRetry {
            val response = apiService.executeQuery(
                AniListGraphQlRequest(
                    query = airingCalendarQuery,
                    variables = AniListVariables(
                        page = page,
                        perPage = 16,
                        airingAtGreater = fromEpochSec,
                        airingAtLesser = toEpochSec
                    )
                )
            )
            if (!response.isSuccessful) {
                throw AniListHttpException(response.code(), "AniList HTTP ${response.code()}")
            }
            val schedules = response.body()?.data?.Page?.airingSchedules.orEmpty()
            schedules.mapNotNull { item ->
                val media = item.media ?: return@mapNotNull null
                mapMediaToEntity(
                    media = media,
                    overrideNextEp = item.episode,
                    overrideAiringAtSec = item.airingAt
                )
            }.distinctBy { it.id }
        }
    }

    private suspend fun queryPage(
        cacheKey: String,
        variables: AniListVariables,
        isTrending: Boolean = false,
        seasonalLabel: String? = null
    ): Result<AniListPageResult> {
        val now = System.currentTimeMillis()
        if (lastFetchKey == cacheKey && (now - lastFetchAtMs) < 12_000L && cachedPageResult != null) {
            return Result.success(cachedPageResult!!)
        }
        return executeWithRateLimitRetry {
            val response = apiService.executeQuery(
                AniListGraphQlRequest(query = pageQuery, variables = variables)
            )
            if (!response.isSuccessful) {
                throw AniListHttpException(response.code(), "AniList HTTP ${response.code()}")
            }
            val body = response.body() ?: throw IllegalStateException("Empty response from AniList")
            if (!body.errors.isNullOrEmpty()) {
                throw IllegalStateException(body.errors.first().message ?: "AniList GraphQL error")
            }
            val pageData = body.data?.Page ?: throw IllegalStateException("Missing Page payload")
            val pageNumber = pageData.pageInfo?.currentPage ?: (variables.page ?: 1)
            val perPage = variables.perPage ?: 12
            val mapped = pageData.media.mapIndexed { idx, media ->
                mapMediaToEntity(
                    media = media,
                    trendingRank = if (isTrending) ((pageNumber - 1) * perPage + idx + 1) else null,
                    popularityRank = (pageNumber - 1) * perPage + idx + 1,
                    seasonalTag = seasonalLabel
                )
            }
            AniListPageResult(
                items = mapped,
                currentPage = pageNumber,
                hasNextPage = pageData.pageInfo?.hasNextPage ?: false
            ).also {
                lastFetchKey = cacheKey
                lastFetchAtMs = System.currentTimeMillis()
                cachedPageResult = it
            }
        }
    }

    private suspend fun <T> executeWithRateLimitRetry(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: AniListHttpException) {
            if (e.code == 429) {
                delay(1_200L)
                runCatching { block() }
            } else {
                Result.failure(e)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    internal fun mapMediaToEntity(
        media: AniListMedia,
        trendingRank: Int? = null,
        popularityRank: Int? = null,
        seasonalTag: String? = null,
        overrideNextEp: Int? = null,
        overrideAiringAtSec: Long? = null
    ): AnimeEntity {
        val displayTitle = media.title?.english?.takeIf { it.isNotBlank() }
            ?: media.title?.romaji?.takeIf { it.isNotBlank() }
            ?: media.title?.native
            ?: "Anime #${media.id}"
        val romaji = media.title?.romaji ?: displayTitle
        val cleanSynopsis = media.description
            ?.replace(Regex("<br\\s*/?>"), "\n")
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "No synopsis available."
        val studioName = media.studios?.nodes
            ?.firstOrNull { it.isAnimationStudio }?.name
            ?: media.studios?.nodes?.firstOrNull()?.name
            ?: "Unknown Studio"
        val extraLarge = media.coverImage?.extraLarge?.takeIf { it.isNotBlank() }
        val large = media.coverImage?.large?.takeIf { it.isNotBlank() }
        val medium = media.coverImage?.medium?.takeIf { it.isNotBlank() }
        val posterUrl = extraLarge ?: large ?: medium ?: ""
        val banner = media.bannerImage?.takeIf { it.isNotBlank() } ?: ""

        val airingSec = overrideAiringAtSec ?: media.nextAiringEpisode?.airingAt
        val nextEp = overrideNextEp ?: media.nextAiringEpisode?.episode
        val airingCal = airingSec?.let {
            Calendar.getInstance().apply { timeInMillis = it * 1000L }
        }
        val airingLabel = airingSec?.let {
            val fmt = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.US)
            fmt.format(Date(it * 1000L))
        }
        val airingDay = airingCal?.get(Calendar.DAY_OF_MONTH)

        val seasonRelations = media.relations?.edges
            ?.count { edge ->
                edge.node?.type == "ANIME" &&
                    (edge.relationType == "SEQUEL" || edge.relationType == "PREQUEL")
            } ?: 0

        val tags = media.tags
            .filter { !it.isMediaSpoiler }
            .take(6)
            .joinToString(",") { it.name }

        return AnimeEntity(
            id = "al_${media.id}",
            anilistId = media.id,
            title = displayTitle,
            romajiTitle = romaji,
            year = media.seasonYear ?: 2025,
            season = media.season ?: "FALL",
            format = media.format ?: "TV",
            totalEpisodes = media.episodes,
            communityRating = ((media.averageScore ?: 75) / 10f).coerceIn(1f, 10f),
            synopsis = cleanSynopsis,
            studio = studioName,
            sourceMaterial = media.source?.lowercase()?.replace('_', ' ')
                ?.replaceFirstChar { it.uppercase() } ?: "Manga",
            episodeDurationMinutes = media.duration ?: 24,
            genresCsv = media.genres.ifEmpty { listOf("Anime") }.joinToString(","),
            tagsCsv = tags,
            posterKey = "",
            remotePosterUrl = posterUrl,
            coverLargeUrl = large ?: "",
            coverMediumUrl = medium ?: "",
            bannerUrl = banner,
            airingTimeLabel = airingLabel,
            airingDayOfMonth = airingDay,
            nextEpisodeNumber = nextEp,
            nextEpisodeTitle = nextEp?.let { "Episode $it" },
            nextAiringAtEpoch = airingSec?.times(1000L),
            trendingRank = trendingRank,
            popularityRank = popularityRank,
            seasonalTag = seasonalTag ?: if (media.status == "RELEASING") "Airing" else null,
            seasonsCount = (1 + seasonRelations).coerceIn(1, 8),
            lastSyncedAt = System.currentTimeMillis()
        )
    }
}

class AniListHttpException(val code: Int, message: String) : Exception(message)
