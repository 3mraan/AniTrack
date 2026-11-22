package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AnimeSeries
import com.example.ui.theme.AniTrackTheme

fun isValidHttpUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    if (!trimmed.startsWith("https://", ignoreCase = true) &&
        !trimmed.startsWith("http://", ignoreCase = true)
    ) {
        return false
    }
    val parsed = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
    return !parsed.host.isNullOrBlank()
}

@Composable
fun AnimePosterImage(
    remoteUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackUrls: List<String> = emptyList(),
    title: String = contentDescription,
    contentScale: ContentScale = ContentScale.Crop,
    withBottomScrim: Boolean = false,
    showTitleOnPlaceholder: Boolean = true,
    allowManualRetry: Boolean = false
) {
    val context = LocalContext.current
    val candidates = remember(remoteUrl, fallbackUrls) {
        (listOf(remoteUrl) + fallbackUrls)
            .map { it.trim() }
            .filter { isValidHttpUrl(it) }
            .distinct()
    }

    var candidateIndex by remember(candidates) { mutableIntStateOf(0) }
    var retryCounter by remember(candidates) { mutableIntStateOf(0) }
    var isLoading by remember(candidates, candidateIndex, retryCounter) {
        mutableStateOf(candidates.isNotEmpty())
    }
    var hasAllFailed by remember(candidates) {
        mutableStateOf(candidates.isEmpty())
    }

    val currentUrl = candidates.getOrNull(candidateIndex)

    Box(modifier = modifier.background(AniTrackTheme.extended.surfaceContainerHigh)) {
        if (currentUrl != null && !hasAllFailed) {
            val request = remember(currentUrl, retryCounter, context) {
                ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .apply {
                        if (retryCounter > 0) {
                            setParameter("retry_attempt", retryCounter, memoryCacheKey = null)
                        }
                    }
                    .build()
            }

            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            isLoading = true
                        }
                        is AsyncImagePainter.State.Success -> {
                            isLoading = false
                            hasAllFailed = false
                        }
                        is AsyncImagePainter.State.Error -> {
                            if (candidateIndex + 1 < candidates.size) {
                                candidateIndex += 1
                            } else {
                                isLoading = false
                                hasAllFailed = true
                            }
                        }
                        AsyncImagePainter.State.Empty -> Unit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading && !hasAllFailed) {
            NeutralLoadingShimmer(modifier = Modifier.fillMaxSize())
        }

        if (hasAllFailed || currentUrl == null) {
            NeutralAnimePlaceholder(
                title = title,
                showTitle = showTitleOnPlaceholder,
                canRetry = allowManualRetry && candidates.isNotEmpty(),
                onRetry = {
                    candidateIndex = 0
                    hasAllFailed = false
                    isLoading = true
                    retryCounter += 1
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (withBottomScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x440B0B12),
                                Color(0xEA0B0B12)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AnimeBannerImage(
    bannerUrl: String,
    coverUrl: String,
    fallbackCoverUrls: List<String>,
    title: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val validBanner = remember(bannerUrl) { bannerUrl.trim().takeIf { isValidHttpUrl(it) } }
    val validCovers = remember(coverUrl, fallbackCoverUrls) {
        (listOf(coverUrl) + fallbackCoverUrls)
            .map { it.trim() }
            .filter { isValidHttpUrl(it) }
            .distinct()
    }

    var bannerFailed by remember(validBanner) { mutableStateOf(validBanner == null) }
    var bannerLoading by remember(validBanner) { mutableStateOf(validBanner != null) }

    Box(modifier = modifier.background(AniTrackTheme.extended.surfaceContainer)) {
        if (validBanner != null && !bannerFailed) {
            val request = remember(validBanner, context) {
                ImageRequest.Builder(context)
                    .data(validBanner)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> bannerLoading = true
                        is AsyncImagePainter.State.Success -> {
                            bannerLoading = false
                            bannerFailed = false
                        }
                        is AsyncImagePainter.State.Error -> {
                            bannerLoading = false
                            bannerFailed = true
                        }
                        AsyncImagePainter.State.Empty -> Unit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (bannerLoading) {
                NeutralLoadingShimmer(modifier = Modifier.fillMaxSize())
            }
        } else if (validCovers.isNotEmpty()) {
            // No landscape banner exists: render an ambient blurred backdrop using the same
            // anime's official cover image + centered natural aspect-ratio cover (never stretched).
            val sameAnimeCover = validCovers.first()
            val ambientReq = remember(sameAnimeCover, context) {
                ImageRequest.Builder(context)
                    .data(sameAnimeCover)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = ambientReq,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
                    .alpha(0.35f)
            )
            AnimePosterImage(
                remoteUrl = sameAnimeCover,
                fallbackUrls = validCovers.drop(1),
                title = title,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                showTitleOnPlaceholder = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
            )
        } else {
            NeutralAnimePlaceholder(
                title = title,
                showTitle = true,
                canRetry = false,
                onRetry = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun NeutralLoadingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "poster_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "poster_shimmer_alpha"
    )
    Box(
        modifier = modifier
            .background(AniTrackTheme.extended.surfaceContainerHigh.copy(alpha = alpha))
    )
}

@Composable
private fun NeutralAnimePlaceholder(
    title: String,
    showTitle: Boolean,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AniTrackTheme.extended.surfaceContainerHigh,
                        AniTrackTheme.extended.surfaceContainer
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (canRetry) Icons.Outlined.BrokenImage else Icons.Outlined.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            if (showTitle && title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (canRetry) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onRetry)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Retry image",
                        tint = AniTrackTheme.extended.primaryText,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatarImage(
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.img_avatar_ren),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape)
    )
}

private val LegacyWatchlistCoverAliases = mapOf(
    "solo" to "solo_leveling",
    "aot" to "attack_on_titan",
    "jjk" to "jujutsu_kaisen",
    "vinland" to "vinland_saga",
    "hxh" to "hunter_x_hunter",
    "demon_slayer" to "demon_slayer",
    "one_piece" to "one_piece",
    "dragon_ball" to "dragon_ball_super"
)

fun normalizeWatchlistCoverKey(coverKey: String): String {
    return LegacyWatchlistCoverAliases[coverKey] ?: coverKey
}

fun resolveWatchlistCoverAnime(
    coverKey: String,
    animeIds: List<String>,
    animeById: Map<String, AnimeSeries>
): AnimeSeries? {
    val normalized = normalizeWatchlistCoverKey(coverKey)
    return animeById[normalized]
        ?: animeIds.firstNotNullOfOrNull { animeById[it] }
}
