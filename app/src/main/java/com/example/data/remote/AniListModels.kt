package com.example.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AniListGraphQlRequest(
    val query: String,
    val variables: AniListVariables
)

@Serializable
data class AniListVariables(
    val page: Int? = null,
    val perPage: Int? = null,
    val search: String? = null,
    val id: Int? = null,
    val idIn: List<Int>? = null,
    val genres: List<String>? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val format: String? = null,
    val sort: List<String>? = null,
    val yearGreater: Int? = null,
    val airingAtGreater: Long? = null,
    val airingAtLesser: Long? = null
)

@Serializable
data class AniListResponse(
    val data: AniListData? = null,
    val errors: List<AniListError>? = null
)

@Serializable
data class AniListError(
    val message: String? = null,
    val status: Int? = null
)

@Serializable
data class AniListData(
    val Page: AniListPage? = null,
    val Media: AniListMedia? = null
)

@Serializable
data class AniListPage(
    val pageInfo: AniListPageInfo? = null,
    val media: List<AniListMedia> = emptyList(),
    val airingSchedules: List<AniListAiringScheduleItem> = emptyList()
)

@Serializable
data class AniListPageInfo(
    val total: Int? = null,
    val currentPage: Int? = null,
    val lastPage: Int? = null,
    val hasNextPage: Boolean = false
)

@Serializable
data class AniListMedia(
    val id: Int,
    val title: AniListTitle? = null,
    val format: String? = null,
    val status: String? = null,
    val description: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val source: String? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val trending: Int? = null,
    val genres: List<String> = emptyList(),
    val tags: List<AniListTag> = emptyList(),
    val coverImage: AniListCoverImage? = null,
    val bannerImage: String? = null,
    val studios: AniListStudioConnection? = null,
    val nextAiringEpisode: AniListNextAiring? = null,
    val relations: AniListRelationConnection? = null
)

@Serializable
data class AniListTitle(
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null
)

@Serializable
data class AniListTag(
    val name: String,
    val isMediaSpoiler: Boolean = false
)

@Serializable
data class AniListCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class AniListStudioConnection(
    val nodes: List<AniListStudio> = emptyList()
)

@Serializable
data class AniListStudio(
    val name: String,
    val isAnimationStudio: Boolean = false
)

@Serializable
data class AniListNextAiring(
    val airingAt: Long,
    val episode: Int
)

@Serializable
data class AniListRelationConnection(
    val edges: List<AniListRelationEdge> = emptyList()
)

@Serializable
data class AniListRelationEdge(
    val relationType: String? = null,
    val node: AniListRelationNode? = null
)

@Serializable
data class AniListRelationNode(
    val id: Int,
    val type: String? = null,
    val format: String? = null
)

@Serializable
data class AniListAiringScheduleItem(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: AniListMedia? = null
)
