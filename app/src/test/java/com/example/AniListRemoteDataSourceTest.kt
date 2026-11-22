package com.example

import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AniListRemoteDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var remoteDataSource: AniListRemoteDataSource

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val apiService = retrofit.create(AniListApiService::class.java)
        remoteDataSource = AniListRemoteDataSource(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchTrending parses GraphQL page, rankings, and pagination info`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": {
                        "Page": {
                          "pageInfo": {
                            "total": 40,
                            "currentPage": 1,
                            "lastPage": 4,
                            "hasNextPage": true
                          },
                          "media": [
                            {
                              "id": 154587,
                              "title": {
                                "english": "Frieren: Beyond Journey's End",
                                "romaji": "Sousou no Frieren",
                                "native": "葬送のフリーレン"
                              },
                              "format": "TV",
                              "status": "FINISHED",
                              "description": "An elf mage travels northward.<br><b>Great journey.</b>",
                              "season": "FALL",
                              "seasonYear": 2023,
                              "episodes": 28,
                              "duration": 24,
                              "source": "MANGA",
                              "averageScore": 91,
                              "popularity": 500000,
                              "trending": 1200,
                              "genres": ["Adventure", "Drama", "Fantasy"],
                              "tags": [
                                {"name": "Magic", "isMediaSpoiler": false},
                                {"name": "Secret Ending", "isMediaSpoiler": true}
                              ],
                              "coverImage": {
                                "extraLarge": "https://img.example/frieren_xl.jpg"
                              },
                              "bannerImage": "https://img.example/frieren_banner.jpg",
                              "studios": {
                                "nodes": [{"name": "MADHOUSE", "isAnimationStudio": true}]
                              },
                              "relations": {
                                "edges": [
                                  {"relationType": "SEQUEL", "node": {"id": 999, "type": "ANIME", "format": "TV"}}
                                ]
                              }
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = remoteDataSource.fetchTrending(page = 1, perPage = 10)
        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(1, page.currentPage)
        assertTrue(page.hasNextPage)
        assertEquals(1, page.items.size)

        val frieren = page.items.first()
        assertEquals("al_154587", frieren.id)
        assertEquals(154587, frieren.anilistId)
        assertEquals("Frieren: Beyond Journey's End", frieren.title)
        assertEquals("MADHOUSE", frieren.studio)
        assertEquals(28, frieren.totalEpisodes)
        assertEquals(9.1f, frieren.communityRating, 0.01f)
        assertEquals(1, frieren.trendingRank)
        assertEquals(2, frieren.seasonsCount)
        assertTrue(frieren.tagsCsv.contains("Magic"))
        assertFalse(frieren.tagsCsv.contains("Secret Ending"))
        assertFalse(frieren.synopsis.contains("<b>"))
    }

    @Test
    fun `searchAnime handles unknown episode counts and missing studio metadata safely`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": {
                        "Page": {
                          "pageInfo": {
                            "total": 1,
                            "currentPage": 2,
                            "lastPage": 2,
                            "hasNextPage": false
                          },
                          "media": [
                            {
                              "id": 9001,
                              "title": {
                                "english": null,
                                "romaji": "Mystery Ongoing Series"
                              },
                              "format": "TV",
                              "status": "RELEASING",
                              "episodes": null,
                              "averageScore": null,
                              "genres": [],
                              "tags": [],
                              "coverImage": { "large": "https://img.example/m.jpg" },
                              "bannerImage": null,
                              "studios": { "nodes": [] }
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val res = remoteDataSource.searchAnime(
            query = "Mystery",
            genre = "All",
            year2020Plus = true,
            format = "TV",
            sortByRating = false,
            page = 2
        )
        assertTrue(res.isSuccess)
        val page = res.getOrThrow()
        assertFalse(page.hasNextPage)
        assertEquals(2, page.currentPage)
        val item = page.items.first()
        assertEquals("Mystery Ongoing Series", item.title)
        assertNull(item.totalEpisodes)
        assertEquals("Unknown Studio", item.studio)
        // Missing bannerImage must remain empty rather than substituting portrait cover
        assertEquals("", item.bannerUrl)
        assertEquals("https://img.example/m.jpg", item.remotePosterUrl)
        assertEquals("https://img.example/m.jpg", item.coverLargeUrl)
        assertEquals("Airing", item.seasonalTag)
    }

    @Test
    fun `fetchMediaByIds parses coverImage extraLarge, large, medium, and bannerImage separately`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": {
                        "Page": {
                          "pageInfo": {
                            "total": 1,
                            "currentPage": 1,
                            "lastPage": 1,
                            "hasNextPage": false
                          },
                          "media": [
                            {
                              "id": 11061,
                              "title": { "english": "Hunter x Hunter", "romaji": "Hunter x Hunter (2011)" },
                              "format": "TV",
                              "episodes": 148,
                              "averageScore": 90,
                              "genres": ["Action", "Adventure", "Fantasy"],
                              "tags": [],
                              "coverImage": {
                                "extraLarge": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11061-y5gsT1hoHuHw.png",
                                "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx11061-y5gsT1hoHuHw.png",
                                "medium": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx11061-y5gsT1hoHuHw.png"
                              },
                              "bannerImage": "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11061-8WkkTZ6duKpq.jpg",
                              "studios": { "nodes": [{"name": "Madhouse", "isAnimationStudio": true}] }
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val res = remoteDataSource.fetchMediaByIds(listOf(11061))
        assertTrue(res.isSuccess)
        val hxh = res.getOrThrow().first()
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11061-y5gsT1hoHuHw.png",
            hxh.remotePosterUrl
        )
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx11061-y5gsT1hoHuHw.png",
            hxh.coverLargeUrl
        )
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx11061-y5gsT1hoHuHw.png",
            hxh.coverMediumUrl
        )
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11061-8WkkTZ6duKpq.jpg",
            hxh.bannerUrl
        )
    }

    @Test
    fun `http 429 rate limit retries and succeeds on second attempt`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": {
                        "Media": {
                          "id": 5114,
                          "title": { "english": "Fullmetal Alchemist: Brotherhood", "romaji": "Hagane no Renkinjutsushi" },
                          "format": "TV",
                          "episodes": 64,
                          "averageScore": 91,
                          "genres": ["Action", "Adventure"],
                          "tags": [],
                          "studios": { "nodes": [{ "name": "bones", "isAnimationStudio": true }] }
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = remoteDataSource.fetchAnimeDetails(5114)
        assertTrue(result.isSuccess)
        val anime = result.getOrThrow()
        assertEquals("Fullmetal Alchemist: Brotherhood", anime.title)
        assertEquals(64, anime.totalEpisodes)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `http 500 error returns failure Result without throwing`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = remoteDataSource.fetchPopular(page = 1)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun `graphql error payload returns failure Result gracefully`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"errors": [{"message": "Invalid query syntax", "status": 400}], "data": null}""")
        )

        val result = remoteDataSource.fetchTopRated(page = 1)
        assertTrue(result.isFailure)
        assertEquals("Invalid query syntax", result.exceptionOrNull()?.message)
    }
}
