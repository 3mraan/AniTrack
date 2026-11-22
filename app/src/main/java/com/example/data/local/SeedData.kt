package com.example.data.local

import com.example.data.model.AnimeEntity
import com.example.data.model.EpisodeHistoryEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserAnimeEntity
import com.example.data.model.UserPreferencesEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistAnimeEntity
import com.example.data.model.WatchlistEntity
import java.util.Calendar

object SeedData {

    // Official AniList CDN artwork URLs (extraLarge, large, medium, bannerImage)
    private const val SOLO_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-it355ZgzquUd.png"
    private const val SOLO_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx151807-it355ZgzquUd.png"
    private const val SOLO_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx151807-it355ZgzquUd.png"
    private const val SOLO_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-37yfQA3ym8PA.jpg"

    private const val JJK_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx145064-hSNRJM03pvv1.jpg"
    private const val JJK_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx145064-hSNRJM03pvv1.jpg"
    private const val JJK_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx145064-hSNRJM03pvv1.jpg"
    private const val JJK_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/145064-esDtAY2He7sk.jpg"

    private const val OP_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-ELSYx3yMPcKM.jpg"
    private const val OP_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx21-ELSYx3yMPcKM.jpg"
    private const val OP_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx21-ELSYx3yMPcKM.jpg"
    private const val OP_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg"

    private const val AOT_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-buvcRTBx4NSm.jpg"
    private const val AOT_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx16498-buvcRTBx4NSm.jpg"
    private const val AOT_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx16498-buvcRTBx4NSm.jpg"
    private const val AOT_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-8jpFCOcDmneX.jpg"

    private const val DEMON_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-WBsBl0ClmgYL.jpg"
    private const val DEMON_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx101922-WBsBl0ClmgYL.jpg"
    private const val DEMON_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx101922-WBsBl0ClmgYL.jpg"
    private const val DEMON_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101922-33MtJGsUSxga.jpg"

    private const val VINLAND_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101348-2fhDFPCuMNiz.jpg"
    private const val VINLAND_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx101348-2fhDFPCuMNiz.jpg"
    private const val VINLAND_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx101348-2fhDFPCuMNiz.jpg"
    private const val VINLAND_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101348-pivKKffCAwAY.jpg"

    private const val HXH_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11061-y5gsT1hoHuHw.png"
    private const val HXH_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx11061-y5gsT1hoHuHw.png"
    private const val HXH_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx11061-y5gsT1hoHuHw.png"
    private const val HXH_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11061-8WkkTZ6duKpq.jpg"

    private const val DBS_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21175-EH06qlfF8TnB.jpg"
    private const val DBS_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx21175-EH06qlfF8TnB.jpg"
    private const val DBS_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx21175-EH06qlfF8TnB.jpg"
    private const val DBS_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21175-Qo9gdGPUm4Im.jpg"

    private const val FRIEREN_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-qQTzQnEJJ3oB.jpg"
    private const val FRIEREN_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx154587-qQTzQnEJJ3oB.jpg"
    private const val FRIEREN_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx154587-qQTzQnEJJ3oB.jpg"
    private const val FRIEREN_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-ivXNJ23SM1xB.jpg"

    private const val CSM_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx127230-DdP4vAdssLoz.png"
    private const val CSM_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx127230-DdP4vAdssLoz.png"
    private const val CSM_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx127230-DdP4vAdssLoz.png"
    private const val CSM_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/127230-o8IRwCGVr9KW.jpg"

    private const val APOTHECARY_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx161645-QLbzHXiYRgV2.jpg"
    private const val APOTHECARY_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx161645-QLbzHXiYRgV2.jpg"
    private const val APOTHECARY_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx161645-QLbzHXiYRgV2.jpg"
    private const val APOTHECARY_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/161645-oqzTZYIvviWI.jpg"

    private const val HAIKYU_XL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113538-tHVE8j5mOPLu.jpg"
    private const val HAIKYU_LG = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx113538-tHVE8j5mOPLu.jpg"
    private const val HAIKYU_MD = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx113538-tHVE8j5mOPLu.jpg"
    private const val HAIKYU_BANNER = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113538-YAcNXzT07eWR.jpg"

    val defaultProfile = UserProfileEntity(
        id = 1,
        displayName = "Ren Takahashi",
        handle = "@ren_takahashi",
        bio = "Dark fantasy & seinen collector. Chasing peak animation and OSTs since 2018.",
        favoriteGenresCsv = "Dark Fantasy,Action,Seinen,Psychological"
    )

    val defaultPreferences = UserPreferencesEntity(id = 1)

    fun initialMetadata(): List<AnimeEntity> {
        val staleSync = 0L
        return listOf(
            AnimeEntity(
                id = "solo_leveling",
                anilistId = 151807,
                title = "Solo Leveling",
                romajiTitle = "Ore dake Level Up na Ken",
                year = 2024,
                season = "WINTER",
                format = "TV",
                totalEpisodes = 12,
                communityRating = 8.6f,
                synopsis = "In a world where hunters must battle deadly monsters to protect humanity, Sung Jinwoo, known as the weakest hunter of all mankind, survives a hidden double dungeon and gains a mysterious system that allows him to level up without limits.",
                studio = "A-1 Pictures",
                sourceMaterial = "Web Novel / Manhwa",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Dark Fantasy,Adventure",
                tagsCsv = "Dungeon,Overpowered,Leveling,Anti-Hero",
                posterKey = "",
                remotePosterUrl = SOLO_XL,
                coverLargeUrl = SOLO_LG,
                coverMediumUrl = SOLO_MD,
                bannerUrl = SOLO_BANNER,
                airingTimeLabel = "Today · 18:30 JST",
                airingDayOfMonth = 21,
                nextEpisodeNumber = 11,
                nextEpisodeTitle = "A Knight Who Defends an Empty Throne",
                trendingRank = 1,
                popularityRank = 1,
                seasonalTag = "Airing Sat",
                seasonsCount = 2,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "jujutsu_kaisen",
                anilistId = 145064,
                title = "Jujutsu Kaisen S2",
                romajiTitle = "Jujutsu Kaisen: Shibuya Jihen",
                year = 2023,
                season = "SUMMER",
                format = "TV",
                totalEpisodes = 23,
                communityRating = 8.8f,
                synopsis = "Yuji Itadori and the Tokyo Jujutsu High sorcerers confront an unprecedented curse conspiracy sealed deep beneath Shibuya Station on Halloween night.",
                studio = "MAPPA",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Supernatural,Dark Fantasy",
                tagsCsv = "Shounen,Curses,Urban Fantasy,Martial Arts",
                posterKey = "",
                remotePosterUrl = JJK_XL,
                coverLargeUrl = JJK_LG,
                coverMediumUrl = JJK_MD,
                bannerUrl = JJK_BANNER,
                airingTimeLabel = "Thu, Nov 27 · 23:56 JST",
                airingDayOfMonth = 27,
                nextEpisodeNumber = 18,
                nextEpisodeTitle = "Shibuya Incident",
                trendingRank = 2,
                popularityRank = 2,
                seasonalTag = "Airing Thu",
                seasonsCount = 2,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "one_piece",
                anilistId = 21,
                title = "One Piece",
                romajiTitle = "One Piece: Egghead Arc",
                year = 1999,
                season = "FALL",
                format = "TV",
                totalEpisodes = 1122,
                communityRating = 8.9f,
                synopsis = "Monkey D. Luffy and the Straw Hat Pirates arrive at Dr. Vegapunk's futuristic island laboratory while world powers converge on the Grand Line.",
                studio = "Toei Animation",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Adventure,Fantasy,Comedy",
                tagsCsv = "Pirates,Super Power,Ensemble Cast",
                posterKey = "",
                remotePosterUrl = OP_XL,
                coverLargeUrl = OP_LG,
                coverMediumUrl = OP_MD,
                bannerUrl = OP_BANNER,
                airingTimeLabel = "Sun, Nov 22 · 09:30 JST",
                airingDayOfMonth = 22,
                nextEpisodeNumber = 1105,
                nextEpisodeTitle = "Egghead Island Arc",
                trendingRank = 4,
                popularityRank = 3,
                seasonalTag = "Airing Sun",
                seasonsCount = 1,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "attack_on_titan",
                anilistId = 16498,
                title = "Attack on Titan",
                romajiTitle = "Shingeki no Kyojin",
                year = 2023,
                season = "SPRING",
                format = "TV",
                totalEpisodes = 89,
                communityRating = 9.1f,
                synopsis = "Humanity fights for survival behind towering concentric walls against colossal titans while uncovering the truth of their isolated world.",
                studio = "WIT Studio / MAPPA",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Dark Fantasy,Drama,Mystery",
                tagsCsv = "Post-Apocalyptic,Military,Tragedy,Gore",
                posterKey = "",
                remotePosterUrl = AOT_XL,
                coverLargeUrl = AOT_LG,
                coverMediumUrl = AOT_MD,
                bannerUrl = AOT_BANNER,
                seasonsCount = 4,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "demon_slayer",
                anilistId = 101922,
                title = "Demon Slayer",
                romajiTitle = "Kimetsu no Yaiba",
                year = 2024,
                season = "SPRING",
                format = "TV",
                totalEpisodes = 55,
                communityRating = 8.7f,
                synopsis = "Tanjiro Kamado joins the Demon Slayer Corps to avenge his family and find a cure for his sister Nezuko.",
                studio = "ufotable",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Fantasy,Supernatural",
                tagsCsv = "Swordplay,Demons,Historical",
                posterKey = "",
                remotePosterUrl = DEMON_XL,
                coverLargeUrl = DEMON_LG,
                coverMediumUrl = DEMON_MD,
                bannerUrl = DEMON_BANNER,
                trendingRank = 3,
                popularityRank = 4,
                seasonsCount = 4,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "vinland_saga",
                anilistId = 101348,
                title = "Vinland Saga",
                romajiTitle = "Vinland Saga",
                year = 2023,
                season = "SUMMER",
                format = "TV",
                totalEpisodes = 48,
                communityRating = 8.8f,
                synopsis = "Raised on battlefields across eleventh-century Northern Europe, young Thorfinn seeks vengeance before discovering what it truly means to be a warrior.",
                studio = "WIT Studio / MAPPA",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Adventure,Drama",
                tagsCsv = "Vikings,Historical,Seinen,Revenge",
                posterKey = "",
                remotePosterUrl = VINLAND_XL,
                coverLargeUrl = VINLAND_LG,
                coverMediumUrl = VINLAND_MD,
                bannerUrl = VINLAND_BANNER,
                seasonsCount = 2,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "hunter_x_hunter",
                anilistId = 11061,
                title = "Hunter x Hunter",
                romajiTitle = "Hunter x Hunter (2011)",
                year = 2011,
                season = "FALL",
                format = "TV",
                totalEpisodes = 148,
                communityRating = 9.0f,
                synopsis = "Gon Freecss embarks on a perilous journey to become a licensed Hunter and track down the father who left him behind.",
                studio = "Madhouse",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 23,
                genresCsv = "Action,Adventure,Fantasy",
                tagsCsv = "Cultivation,Tournament,Super Power",
                posterKey = "",
                remotePosterUrl = HXH_XL,
                coverLargeUrl = HXH_LG,
                coverMediumUrl = HXH_MD,
                bannerUrl = HXH_BANNER,
                seasonsCount = 1,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "dragon_ball_super",
                anilistId = 21175,
                title = "Dragon Ball Super",
                romajiTitle = "Dragon Ball Super",
                year = 2024,
                season = "FALL",
                format = "TV",
                totalEpisodes = 131,
                communityRating = 8.4f,
                synopsis = "Goku and allies face multiverse tournaments and cosmic deities beyond the Seventh Universe.",
                studio = "Toei Animation",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 23,
                genresCsv = "Action,Adventure,Sports",
                tagsCsv = "Martial Arts,Super Power,Aliens",
                posterKey = "",
                remotePosterUrl = DBS_XL,
                coverLargeUrl = DBS_LG,
                coverMediumUrl = DBS_MD,
                bannerUrl = DBS_BANNER,
                airingTimeLabel = "Fri, Nov 20 · 23:40 JST",
                airingDayOfMonth = 20,
                nextEpisodeNumber = 19,
                nextEpisodeTitle = "Tournament of Power Prelude",
                trendingRank = 5,
                popularityRank = 5,
                seasonalTag = "Upcoming",
                seasonsCount = 1,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "frieren_beyond",
                anilistId = 154587,
                title = "Frieren: Beyond Journey's End",
                romajiTitle = "Sousou no Frieren",
                year = 2023,
                season = "FALL",
                format = "TV",
                totalEpisodes = 28,
                communityRating = 9.3f,
                synopsis = "After a ten-year quest to defeat the Demon King, the elven mage Frieren sets out to understand the fleeting lives of her former comrades.",
                studio = "Madhouse",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Adventure,Drama,Fantasy",
                tagsCsv = "Magic,Elf,Iyashikei,Travel",
                posterKey = "",
                remotePosterUrl = FRIEREN_XL,
                coverLargeUrl = FRIEREN_LG,
                coverMediumUrl = FRIEREN_MD,
                bannerUrl = FRIEREN_BANNER,
                seasonsCount = 1,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "chainsaw_man",
                anilistId = 127230,
                title = "Chainsaw Man",
                romajiTitle = "Chainsaw Man",
                year = 2022,
                season = "FALL",
                format = "TV",
                totalEpisodes = 12,
                communityRating = 8.5f,
                synopsis = "Impoverished devil hunter Denji merges with his chainsaw devil Pochita to fight fiends in Tokyo's Public Safety Division.",
                studio = "MAPPA",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Action,Horror,Supernatural",
                tagsCsv = "Gore,Anti-Hero,Demons",
                posterKey = "",
                remotePosterUrl = CSM_XL,
                coverLargeUrl = CSM_LG,
                coverMediumUrl = CSM_MD,
                bannerUrl = CSM_BANNER,
                seasonsCount = 1,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "apothecary_diaries",
                anilistId = 161645,
                title = "The Apothecary Diaries",
                romajiTitle = "Kusuriya no Hitorigoto",
                year = 2024,
                season = "FALL",
                format = "TV",
                totalEpisodes = 24,
                communityRating = 8.9f,
                synopsis = "A sharp-witted herbalist kidnapped into the imperial inner palace unravels medical mysteries and court intrigues.",
                studio = "OLM / TOHO animation",
                sourceMaterial = "Light Novel",
                episodeDurationMinutes = 24,
                genresCsv = "Mystery,Drama,Romance",
                tagsCsv = "Medicine,Historical,Detective",
                posterKey = "",
                remotePosterUrl = APOTHECARY_XL,
                coverLargeUrl = APOTHECARY_LG,
                coverMediumUrl = APOTHECARY_MD,
                bannerUrl = APOTHECARY_BANNER,
                seasonsCount = 2,
                lastSyncedAt = staleSync
            ),
            AnimeEntity(
                id = "haikyu_battle",
                anilistId = 113538,
                title = "Haikyu!! To The Top",
                romajiTitle = "Haikyuu!!",
                year = 2020,
                season = "WINTER",
                format = "TV",
                totalEpisodes = 25,
                communityRating = 8.8f,
                synopsis = "Karasuno High's volleyball squad competes at the national tournament against Japan's fiercest rivals.",
                studio = "Production I.G",
                sourceMaterial = "Manga",
                episodeDurationMinutes = 24,
                genresCsv = "Sports,Drama,Comedy",
                tagsCsv = "Volleyball,Teamwork,School Club",
                posterKey = "",
                remotePosterUrl = HAIKYU_XL,
                coverLargeUrl = HAIKYU_LG,
                coverMediumUrl = HAIKYU_MD,
                bannerUrl = HAIKYU_BANNER,
                seasonsCount = 4,
                lastSyncedAt = staleSync
            )
        )
    }

    fun initialUserTracking(): List<UserAnimeEntity> {
        val now = 1795267500000L
        return listOf(
            UserAnimeEntity("solo_leveling", WatchStatus.WATCHING, 10, 1, true, true, true, "Rewatch boss fight ep 6 before S2 drops.", now - 1000L, now - 1000L),
            UserAnimeEntity("jujutsu_kaisen", WatchStatus.WATCHING, 17, 2, true, false, true, "Shibuya arc episode 17 animation is unmatched.", now - 2000L, now - 2000L),
            UserAnimeEntity("one_piece", WatchStatus.WATCHING, 1104, 1, true, false, false, "Catching up on Egghead weekly.", now - 3000L, now - 3000L),
            UserAnimeEntity("attack_on_titan", WatchStatus.COMPLETED, 89, 4, true, true, false, "All-time favorite ending.", now - 4000L, now - 4000L),
            UserAnimeEntity("demon_slayer", WatchStatus.COMPLETED, 55, 4, true, false, false, "", now - 5000L, now - 5000L),
            UserAnimeEntity("vinland_saga", WatchStatus.PLANNED, 0, 1, true, true, false, "Weekend binge plan with friends.", now - 6000L, now - 6000L),
            UserAnimeEntity("hunter_x_hunter", WatchStatus.ON_HOLD, 76, 1, true, false, false, "Paused right after Greed Island.", now - 7000L, now - 7000L),
            UserAnimeEntity("dragon_ball_super", WatchStatus.PLANNED, 18, 1, false, false, false, "", now - 8000L, now - 8000L),
            UserAnimeEntity("frieren_beyond", WatchStatus.COMPLETED, 28, 1, true, true, false, "Comfort rewatch favorite.", now - 9000L, now - 9000L),
            UserAnimeEntity("chainsaw_man", WatchStatus.DROPPED, 4, 1, true, false, false, "Might pick back up before the movie.", now - 10000L, now - 10000L),
            UserAnimeEntity("apothecary_diaries", WatchStatus.COMPLETED, 24, 2, true, false, false, "", now - 11000L, now - 11000L),
            UserAnimeEntity("haikyu_battle", WatchStatus.COMPLETED, 25, 4, true, false, false, "", now - 12000L, now - 12000L)
        )
    }

    fun initialReviews(): List<ReviewEntity> {
        val now = 1795267500000L
        return listOf(
            ReviewEntity("solo_leveling", 9.0f, 8, 10, 8, 9, "Hiroyuki Sawano's soundtrack elevates every dungeon boss fight. Pacing is tight and the shadow extraction scenes are unforgettable.", false, now),
            ReviewEntity("jujutsu_kaisen", 8.9f, 9, 10, 9, 9, "Shibuya Incident delivers relentless choreography, kinetic camerawork, and high-stakes domain clashes.", false, now),
            ReviewEntity("one_piece", 9.2f, 10, 9, 10, 9, "Egghead Island brings crisp sakuga bursts and centuries-old Void Century mysteries to the surface.", false, now),
            ReviewEntity("attack_on_titan", 9.5f, 10, 9, 9, 10, "A generational epic from Shiganshina's walls to the final battle. Every motif and foreshadowing beat lands.", false, now),
            ReviewEntity("demon_slayer", 8.7f, 8, 10, 8, 9, "Ufotable's compositing and lighting make every breathing technique look hand-painted in motion.", false, now),
            ReviewEntity("vinland_saga", 8.8f, 9, 9, 10, 8, "Queued up for a full weekend marathon—everyone praises Farmland Arc's character study.", false, now),
            ReviewEntity("hunter_x_hunter", 9.2f, 10, 9, 10, 9, "Paused right after Greed Island before diving into the Chimera Ant arc.", false, now),
            ReviewEntity("frieren_beyond", 9.6f, 10, 10, 10, 10, "Quietly devastating meditation on time, memory, and companionship with Evan Call's orchestral score.", false, now),
            ReviewEntity("chainsaw_man", 7.4f, 7, 9, 8, 8, "Incredible cinematic direction; might revisit before the Reze arc film.", false, now),
            ReviewEntity("apothecary_diaries", 9.1f, 9, 9, 10, 9, "Maomao solving court poison mysteries is endlessly charismatic.", false, now),
            ReviewEntity("haikyu_battle", 8.8f, 9, 9, 10, 9, "Peak sports tension—every rally feels earned.", false, now)
        )
    }

    fun initialEpisodesFor(metadataList: List<AnimeEntity>, trackingMap: Map<String, UserAnimeEntity>): List<EpisodeHistoryEntity> {
        val result = mutableListOf<EpisodeHistoryEntity>()
        val soloTitles = listOf(
            1 to "I'm Used to It",
            2 to "If I Had One More Chance",
            3 to "It's Like a Game",
            4 to "I've Gotta Get Stronger",
            5 to "A Pretty Good Deal",
            6 to "The Real Hunt Begins",
            7 to "Let's See How Far I Can Go",
            8 to "This Is Frustrating",
            9 to "You've Been Hiding Your Skills",
            10 to "What Is This, a Picnic?",
            11 to "A Knight Who Defends an Empty Throne",
            12 to "Arise"
        )

        // Spread historical watched timestamps across Jun–Nov 2026 so Monthly Activity chart is computed from real DB rows!
        val monthTimestamps = listOf(
            epochFor(2026, Calendar.JUNE, 14),
            epochFor(2026, Calendar.JULY, 18),
            epochFor(2026, Calendar.AUGUST, 12),
            epochFor(2026, Calendar.SEPTEMBER, 21),
            epochFor(2026, Calendar.OCTOBER, 15),
            epochFor(2026, Calendar.NOVEMBER, 19)
        )
        var cursor = 0

        for (anime in metadataList) {
            val watchedCount = trackingMap[anime.id]?.watchedEpisodes ?: 0
            val total = anime.totalEpisodes ?: 12
            if (anime.id == "solo_leveling") {
                soloTitles.forEach { (num, title) ->
                    val watched = num <= watchedCount
                    val watchedEpoch = if (watched) epochFor(2026, Calendar.NOVEMBER, (6 + num).coerceAtMost(19)) else null
                    val subtitle = when {
                        watched && num == 9 -> "Watched Nov 16, 2026"
                        watched && num == 10 -> "Watched Nov 19, 2026"
                        watched -> "Watched Nov ${(6 + num).coerceAtMost(19)}, 2026"
                        num == 11 -> "Up Next"
                        num == 12 -> "Season Finale"
                        else -> "Unwatched"
                    }
                    result.add(
                        EpisodeHistoryEntity(
                            id = "${anime.id}_s1_ep_$num",
                            animeId = anime.id,
                            seasonNumber = 1,
                            episodeNumber = num,
                            title = title,
                            subtitle = subtitle,
                            watched = watched,
                            watchedAtEpoch = watchedEpoch,
                            watchedDateLabel = if (watched) "Nov ${(6 + num).coerceAtMost(19)}, 2026" else null
                        )
                    )
                }
            } else {
                val count = total.coerceAtMost(24)
                val startEp = if (total > 24) (watchedCount - 11).coerceAtLeast(1) else 1
                for (offset in 0 until count) {
                    val epNum = startEp + offset
                    if (epNum > total) break
                    val watched = epNum <= watchedCount
                    val isUpNext = !watched && epNum == watchedCount + 1
                    val title = when {
                        anime.id == "jujutsu_kaisen" && epNum == 17 -> "Thunderclap, Part 2"
                        anime.id == "jujutsu_kaisen" && epNum == 18 -> "Right and Wrong"
                        anime.id == "one_piece" && epNum == 1105 -> "A Beautiful Act of Treason!"
                        epNum == total -> "Season Finale"
                        else -> "Episode $epNum"
                    }
                    // Weight August higher naturally so real DB aggregation reflects a summer binge peak
                    val bucketIndex = when {
                        cursor % 7 in listOf(2, 3) -> 2 // August peak
                        else -> cursor % monthTimestamps.size
                    }
                    cursor++
                    val epEpoch = if (watched) monthTimestamps[bucketIndex] else null
                    result.add(
                        EpisodeHistoryEntity(
                            id = "${anime.id}_s1_ep_$epNum",
                            animeId = anime.id,
                            seasonNumber = 1,
                            episodeNumber = epNum,
                            title = title,
                            subtitle = when {
                                watched -> "Watched"
                                isUpNext -> "Up Next"
                                epNum == total -> "Finale"
                                else -> "Episode $epNum"
                            },
                            watched = watched,
                            watchedAtEpoch = epEpoch,
                            watchedDateLabel = if (watched) "2026" else null
                        )
                    )
                }
            }
        }
        return result
    }

    fun initialWatchlists(): List<WatchlistEntity> {
        val now = 1795267500000L
        return listOf(
            WatchlistEntity("wl_favorites", "My Favorite Anime", "All-time personal masterpieces with unforgettable OSTs and direction.", "attack_on_titan", "Updated Nov 20, 2026", now - 1000L),
            WatchlistEntity("wl_action", "Best Action Anime", "High-octane sakuga fights, intense choreography, and unforgettable boss battles.", "solo_leveling", "Updated Nov 14, 2026", now - 2000L),
            WatchlistEntity("wl_later", "Watch Later", "Must-watch classics and seasonal shows queued for weekends.", "vinland_saga", "Updated Nov 11, 2026", now - 3000L),
            WatchlistEntity("wl_villains", "Best Anime Villains", "Iconic antagonists who steal every scene they appear in.", "jujutsu_kaisen", "Updated Oct 28, 2026", now - 4000L),
            WatchlistEntity("wl_underrated", "Underrated Gems", "Hidden treasures across mystery, drama, and fantasy.", "hunter_x_hunter", "Updated Oct 12, 2026", now - 5000L)
        )
    }

    fun initialWatchlistAnime(): List<WatchlistAnimeEntity> {
        val mapping = mapOf(
            "wl_favorites" to listOf("attack_on_titan", "solo_leveling", "frieren_beyond", "vinland_saga", "jujutsu_kaisen"),
            "wl_action" to listOf("solo_leveling", "jujutsu_kaisen", "demon_slayer"),
            "wl_later" to listOf("vinland_saga", "dragon_ball_super"),
            "wl_villains" to listOf("jujutsu_kaisen", "hunter_x_hunter", "attack_on_titan"),
            "wl_underrated" to listOf("apothecary_diaries", "haikyu_battle")
        )
        return mapping.flatMap { (wlId, ids) ->
            ids.mapIndexed { idx, animeId -> WatchlistAnimeEntity(wlId, animeId, idx) }
        }
    }

    private fun epochFor(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
