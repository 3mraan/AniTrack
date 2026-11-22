package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AniTrackDatabase
import com.example.data.local.SeedData
import com.example.data.model.WatchStatus
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListRemoteDataSource
import com.example.data.remote.FirebaseSyncManager
import com.example.data.repository.AniTrackRepository
import com.example.data.util.AnimeSearchMatcher
import com.example.ui.AniTrackViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AniTrackRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AniTrackDatabase
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: AniTrackRepository

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AniTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val remoteDataSource = AniListRemoteDataSource(retrofit.create(AniListApiService::class.java))
        repository = AniTrackRepository(
            dao = database.aniTrackDao(),
            settingsDataStore = SettingsDataStore(context),
            remoteDataSource = remoteDataSource,
            firebaseSyncManager = FirebaseSyncManager(context),
            json = json
        )
    }

    @After
    fun tearDown() {
        database.close()
        mockWebServer.shutdown()
    }

    @Test
    fun `ensureSeeded populates catalog metadata and default profile while starting personal library empty`() = runTest {
        repository.ensureSeeded()

        val allAnime = repository.allAnime.first()
        val watchlists = repository.watchlists.first()
        val history = repository.allEpisodeHistory.first()
        val profile = repository.profile.first()

        assertTrue(allAnime.isNotEmpty())
        assertTrue("New user personal library must start empty", allAnime.none { it.inLibrary })
        assertTrue("New user watchlists must start empty", watchlists.isEmpty())
        assertTrue("New user episode history must start empty", history.isEmpty())
        assertEquals("Anime Fan", profile.displayName)
        assertNotNull(allAnime.firstOrNull { it.id == "solo_leveling" })
    }

    @Test
    fun `offline-first AniList sync updates metadata without overwriting user progress or review`() = runTest {
        repository.ensureSeeded()
        repository.setWatchedEpisodesManual("solo_leveling", 9)
        repository.saveReview(
            animeId = "solo_leveling",
            overall = 9.5f,
            story = 9,
            animation = 10,
            characters = 9,
            soundtrack = 10,
            reviewText = "Personal review must survive API sync.",
            containsSpoilers = false
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": {
                        "Media": {
                          "id": 151807,
                          "title": { "english": "Solo Leveling", "romaji": "Ore dake Level Up na Ken" },
                          "format": "TV",
                          "status": "FINISHED",
                          "description": "Updated synopsis from AniList.",
                          "seasonYear": 2024,
                          "episodes": 12,
                          "averageScore": 85,
                          "genres": ["Action", "Adventure", "Fantasy"],
                          "tags": [{"name": "Dungeon", "isMediaSpoiler": false}],
                          "studios": { "nodes": [{"name": "A-1 Pictures", "isAnimationStudio": true}] }
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        // Force stale timestamp so syncAnimeDetailsIfNeeded executes network request
        val existingMeta = database.aniTrackDao().getAnimeMetadataById("solo_leveling")!!
        database.aniTrackDao().upsertAnimeMetadata(existingMeta.copy(lastSyncedAt = 0L, tagsCsv = ""))

        val syncRes = repository.syncAnimeDetailsIfNeeded("solo_leveling")
        assertTrue(syncRes.isSuccess)

        val afterSync = repository.observeAnime("solo_leveling").first()!!
        assertEquals("Updated synopsis from AniList.", afterSync.synopsis)
        assertEquals(9, afterSync.watchedEpisodes)
        assertEquals(9.5f, afterSync.userRating ?: 0f, 0.01f)
        assertEquals("Personal review must survive API sync.", afterSync.personalReview)
    }

    @Test
    fun `episode tracking increments, clamps invalid manual values, undoes, and resets`() = runTest {
        repository.ensureSeeded()

        val initial = repository.observeAnime("solo_leveling").first()!!
        val startCount = initial.watchedEpisodes

        val (afterInc, _) = repository.incrementEpisode("solo_leveling")
        assertEquals(startCount + 1, afterInc?.watchedEpisodes)

        // Attempt invalid over-limit manual progress -> clamped to totalEpisodes (12)
        repository.setWatchedEpisodesManual("solo_leveling", 999)
        assertEquals(12, repository.observeAnime("solo_leveling").first()!!.watchedEpisodes)

        // Attempt invalid negative manual progress -> clamped to 0
        repository.setWatchedEpisodesManual("solo_leveling", -5)
        assertEquals(0, repository.observeAnime("solo_leveling").first()!!.watchedEpisodes)

        repository.setWatchedEpisodesManual("solo_leveling", 4)
        repository.undoLastEpisode("solo_leveling")
        assertEquals(3, repository.observeAnime("solo_leveling").first()!!.watchedEpisodes)

        repository.resetEpisodes("solo_leveling")
        assertEquals(0, repository.observeAnime("solo_leveling").first()!!.watchedEpisodes)
    }

    @Test
    fun `updateStatus COMPLETED fills total episodes and updatePersonalNotes persists`() = runTest {
        repository.ensureSeeded()

        repository.updateStatus("solo_leveling", WatchStatus.COMPLETED)
        repository.updatePersonalNotes("solo_leveling", "Rewatch before Season 2")

        val updated = repository.observeAnime("solo_leveling").first()!!
        assertEquals(WatchStatus.COMPLETED, updated.status)
        assertEquals(12, updated.watchedEpisodes)
        assertEquals("Rewatch before Season 2", updated.personalNotes)
    }

    @Test
    fun `watchlist CRUD creates, reorders entries, adds anime, and deletes cleanly`() = runTest {
        repository.ensureSeeded()

        repository.saveWatchlist(
            watchlistId = null,
            name = "Weekend Marathon",
            description = "High energy action",
            coverKey = "jjk",
            animeIds = listOf("jujutsu_kaisen", "solo_leveling")
        )

        val created = repository.watchlists.first().first { it.name == "Weekend Marathon" }
        assertEquals(listOf("jujutsu_kaisen", "solo_leveling"), created.animeIds)

        repository.saveWatchlist(
            watchlistId = created.id,
            name = "Weekend Marathon Updated",
            description = "Reordered list",
            coverKey = "solo",
            animeIds = listOf("solo_leveling", "jujutsu_kaisen")
        )
        val reordered = repository.watchlists.first().first { it.id == created.id }
        assertEquals("Weekend Marathon Updated", reordered.name)
        assertEquals(listOf("solo_leveling", "jujutsu_kaisen"), reordered.animeIds)

        repository.deleteWatchlist(created.id)
        assertNull(repository.watchlists.first().firstOrNull { it.id == created.id })
    }

    @Test
    fun `reviews save and delete update database state`() = runTest {
        repository.ensureSeeded()

        repository.saveReview(
            animeId = "vinland_saga",
            overall = 9.0f,
            story = 10,
            animation = 9,
            characters = 10,
            soundtrack = 9,
            reviewText = "Masterpiece character arc.",
            containsSpoilers = true
        )
        val saved = repository.observeAnime("vinland_saga").first()!!
        assertEquals(9.0f, saved.userRating ?: 0f, 0.01f)
        assertTrue(saved.containsSpoilers)

        repository.deleteReview("vinland_saga")
        val afterDelete = repository.observeAnime("vinland_saga").first()!!
        assertNull(afterDelete.userRating)
        assertEquals("", afterDelete.personalReview)
    }

    @Test
    fun `statistics calculations derive monthly activity and rating distribution from local records`() = runTest {
        repository.ensureSeeded()
        repository.toggleInLibrary("solo_leveling")
        repository.incrementEpisode("solo_leveling")
        repository.saveReview(
            animeId = "solo_leveling",
            overall = 9.0f,
            story = 9,
            animation = 10,
            characters = 9,
            soundtrack = 9,
            reviewText = "Awesome adaptation.",
            containsSpoilers = false
        )

        val allAnime = repository.allAnime.first()
        val history = repository.allEpisodeHistory.first()

        val monthly = AniTrackViewModel.computeMonthlyActivity(history)
        val dist = AniTrackViewModel.computeRatingDistribution(allAnime)

        assertEquals(6, monthly.size)
        assertTrue(monthly.sumOf { it.episodes } > 0)
        assertEquals(5, dist.size)
        assertTrue(dist.sumOf { it.count } > 0)
    }

    @Test
    fun `fuzzy search matcher resolves typos and Arabic anime titles accurately`() = runTest {
        repository.ensureSeeded()
        val catalog = repository.allAnime.first()
        val titles = catalog.map { it.title }

        val typoSolo = AnimeSearchMatcher.rankAndFilter("Solo Levelng", catalog).items
        assertTrue(typoSolo.isNotEmpty())
        assertEquals("solo_leveling", typoSolo.first().id)

        val typoJjk = AnimeSearchMatcher.rankAndFilter("Jujutsu Kiasen", catalog).items
        assertTrue(typoJjk.isNotEmpty())
        assertEquals("jujutsu_kaisen", typoJjk.first().id)

        val typoAot = AnimeSearchMatcher.rankAndFilter("Atack on Titan", catalog).items
        assertTrue(typoAot.isNotEmpty())
        assertEquals("attack_on_titan", typoAot.first().id)

        val arabicSolo = AnimeSearchMatcher.rankAndFilter("سولو ليفلنج", catalog).items
        assertTrue(arabicSolo.isNotEmpty())
        assertEquals("solo_leveling", arabicSolo.first().id)
        assertEquals("Solo Leveling", AnimeSearchMatcher.resolveRemoteQuery("سولو ليفلنج", titles))

        val arabicOnePiece = AnimeSearchMatcher.rankAndFilter("ون بيس", catalog).items
        assertTrue(arabicOnePiece.isNotEmpty())
        assertEquals("one_piece", arabicOnePiece.first().id)

        val arabicAot = AnimeSearchMatcher.rankAndFilter("هجوم العمالقة", catalog).items
        assertTrue(arabicAot.isNotEmpty())
        assertEquals("attack_on_titan", arabicAot.first().id)

        val arabicDemon = AnimeSearchMatcher.rankAndFilter("ديمون سلاير", catalog).items
        assertTrue(arabicDemon.isNotEmpty())
        assertEquals("demon_slayer", arabicDemon.first().id)
    }

    @Test
    fun `exportLibraryJson and importLibraryJson preserve tracking and metadata`() = runTest {
        repository.ensureSeeded()
        repository.updatePersonalNotes("solo_leveling", "Export verification note")

        val exportedJson = repository.exportLibraryJson()
        assertTrue(exportedJson.contains("Export verification note"))

        // Clear and re-import into a fresh in-memory DB
        val secondDb = Room.inMemoryDatabaseBuilder(context, AniTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val secondRepo = AniTrackRepository(
            dao = secondDb.aniTrackDao(),
            settingsDataStore = SettingsDataStore(context),
            remoteDataSource = AniListRemoteDataSource(
                Retrofit.Builder()
                    .baseUrl(mockWebServer.url("/"))
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(AniListApiService::class.java)
            ),
            firebaseSyncManager = FirebaseSyncManager(context),
            json = json
        )
        val importResult = secondRepo.importLibraryJson(exportedJson)
        assertTrue(importResult.isSuccess)
        val restored = secondRepo.observeAnime("solo_leveling").first()
        assertNotNull(restored)
        assertEquals("Export verification note", restored?.personalNotes)
        secondDb.close()
    }

    @Test
    fun `seeded catalog and 7 key anime use distinct official AniList URLs with zero AI artwork`() {
        val metadata = SeedData.initialMetadata()
        val byId = metadata.associateBy { it.id }

        val requiredIds = listOf(
            "solo_leveling",
            "hunter_x_hunter",
            "one_piece",
            "jujutsu_kaisen",
            "attack_on_titan",
            "demon_slayer",
            "dragon_ball_super"
        )
        requiredIds.forEach { id ->
            val anime = byId[id]
            assertNotNull("Expected seeded anime $id", anime)
            assertTrue("Missing official cover for $id", anime!!.remotePosterUrl.startsWith("https://s4.anilist.co/"))
            assertTrue("Missing official large cover for $id", anime.coverLargeUrl.startsWith("https://s4.anilist.co/"))
            assertTrue("Missing official medium cover for $id", anime.coverMediumUrl.startsWith("https://s4.anilist.co/"))
            assertTrue("Missing official banner for $id", anime.bannerUrl.startsWith("https://s4.anilist.co/"))
            assertFalse("AI URL still present for $id", anime.remotePosterUrl.contains("lh3.googleusercontent.com/aida"))
            assertFalse("AI banner still present for $id", anime.bannerUrl.contains("lh3.googleusercontent.com/aida"))
        }

        // Verify none of the anime reuse another anime's poster or banner
        val distinctPosters = metadata.map { it.remotePosterUrl }.toSet()
        val distinctBanners = metadata.map { it.bannerUrl }.toSet()
        assertEquals("Every anime must have its own distinct poster URL", metadata.size, distinctPosters.size)
        assertEquals("Every anime must have its own distinct banner URL", metadata.size, distinctBanners.size)

        // Verify critical non-swapped AniList media IDs in artwork URLs
        assertTrue(byId["hunter_x_hunter"]!!.remotePosterUrl.contains("11061"))
        assertTrue(byId["one_piece"]!!.remotePosterUrl.contains("21"))
        assertTrue(byId["dragon_ball_super"]!!.remotePosterUrl.contains("21175"))
        assertTrue(byId["demon_slayer"]!!.remotePosterUrl.contains("101922"))
        assertTrue(byId["solo_leveling"]!!.remotePosterUrl.contains("151807"))
        assertTrue(byId["jujutsu_kaisen"]!!.remotePosterUrl.contains("145064"))
        assertTrue(byId["attack_on_titan"]!!.remotePosterUrl.contains("16498"))
    }

    @Test
    fun `ensureSeeded upgrades legacy AI-generated artwork URLs without modifying user tracking or reviews`() = runTest {
        val dao = database.aniTrackDao()
        val seedMeta = SeedData.initialMetadata()
        val legacyMeta = seedMeta.map {
            it.copy(
                posterKey = "aot",
                remotePosterUrl = "https://lh3.googleusercontent.com/aida/legacy_ai_poster_${it.id}",
                coverLargeUrl = "",
                coverMediumUrl = "",
                bannerUrl = "https://lh3.googleusercontent.com/aida/legacy_ai_banner_${it.id}"
            )
        }
        val tracking = SeedData.initialUserTracking()
        val trackingMap = tracking.associateBy { it.animeId }
        dao.upsertAnimeMetadataList(legacyMeta)
        dao.upsertUserAnimeList(tracking)
        dao.insertReviews(SeedData.initialReviews())
        dao.insertEpisodes(SeedData.initialEpisodesFor(seedMeta, trackingMap))
        dao.insertWatchlists(SeedData.initialWatchlists())
        dao.insertWatchlistAnime(SeedData.initialWatchlistAnime())

        // User custom progress/notes before upgrade
        repository.setWatchedEpisodesManual("hunter_x_hunter", 99)
        repository.updatePersonalNotes("hunter_x_hunter", "Chimera Ant arc peak")

        // Run ensureSeeded -> should replace legacy AI URLs with official AniList URLs
        repository.ensureSeeded()

        val upgradedHxH = repository.observeAnime("hunter_x_hunter").first()!!
        assertTrue(upgradedHxH.remotePosterUrl.startsWith("https://s4.anilist.co/"))
        assertTrue(upgradedHxH.remotePosterUrl.contains("11061"))
        assertTrue(upgradedHxH.coverLargeUrl.contains("11061"))
        assertTrue(upgradedHxH.coverMediumUrl.contains("11061"))
        assertTrue(upgradedHxH.bannerUrl.contains("11061"))
        // User progress, notes, review, and watchlists remain intact
        assertEquals(99, upgradedHxH.watchedEpisodes)
        assertEquals("Chimera Ant arc peak", upgradedHxH.personalNotes)
        assertNotNull(upgradedHxH.review)
        assertTrue(repository.watchlists.first().isNotEmpty())
    }
}
