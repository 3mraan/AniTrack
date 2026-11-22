package com.example.data.util

import com.example.data.model.AnimeSeries
import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

data class FuzzySearchOutput(
    val items: List<AnimeSeries>,
    val resolvedQueryHint: String? = null
)

object AnimeSearchMatcher {

    private val aliasMap: Map<String, String> = mapOf(
        // Solo Leveling
        "سولو ليفلنج" to "Solo Leveling",
        "سولو ليفلينج" to "Solo Leveling",
        "سولو لفلنج" to "Solo Leveling",
        "سولو ليفيلنج" to "Solo Leveling",
        "سولو" to "Solo Leveling",
        "solo levelng" to "Solo Leveling",
        "solo levling" to "Solo Leveling",
        "solo lvling" to "Solo Leveling",
        "sololeveling" to "Solo Leveling",
        "sl" to "Solo Leveling",

        // Jujutsu Kaisen
        "جوجوتسو" to "Jujutsu Kaisen",
        "جوجوتسو كايسن" to "Jujutsu Kaisen",
        "جوجيتسو" to "Jujutsu Kaisen",
        "jjk" to "Jujutsu Kaisen",
        "jujutsu kiasen" to "Jujutsu Kaisen",
        "jujutsu kasen" to "Jujutsu Kaisen",
        "jujutsu" to "Jujutsu Kaisen",

        // One Piece
        "ون بيس" to "One Piece",
        "وان بيس" to "One Piece",
        "one peice" to "One Piece",
        "onepiece" to "One Piece",
        "1 piece" to "One Piece",

        // Attack on Titan
        "هجوم العمالقه" to "Attack on Titan",
        "العمالقه" to "Attack on Titan",
        "aot" to "Attack on Titan",
        "snk" to "Attack on Titan",
        "atack on titan" to "Attack on Titan",
        "attack on titn" to "Attack on Titan",
        "shingeki" to "Attack on Titan",

        // Demon Slayer
        "ديمون سلاير" to "Demon Slayer",
        "قاتل الشياطين" to "Demon Slayer",
        "كيميتسو" to "Demon Slayer",
        "demon slyer" to "Demon Slayer",
        "demonslayer" to "Demon Slayer",
        "kimetsu" to "Demon Slayer",

        // Hunter x Hunter
        "هنتر" to "Hunter x Hunter",
        "القناص" to "Hunter x Hunter",
        "هنتر اكس هنتر" to "Hunter x Hunter",
        "hxh" to "Hunter x Hunter",
        "hunter hunter" to "Hunter x Hunter",

        // Dragon Ball Super
        "دراغون بول" to "Dragon Ball Super",
        "دراجون بول" to "Dragon Ball Super",
        "دراغون بول سوبر" to "Dragon Ball Super",
        "كرات التنين" to "Dragon Ball Super",
        "dbs" to "Dragon Ball Super",
        "dragonball" to "Dragon Ball Super",

        // Vinland Saga
        "فينلاند" to "Vinland Saga",
        "فينلاند ساجا" to "Vinland Saga",
        "ملحمه فينلاند" to "Vinland Saga",
        "vinlnd" to "Vinland Saga",

        // Frieren
        "فريرن" to "Frieren: Beyond Journey's End",
        "فريرين" to "Frieren: Beyond Journey's End",
        "سوسو نو فريرن" to "Frieren: Beyond Journey's End",
        "freiren" to "Frieren: Beyond Journey's End",
        "freren" to "Frieren: Beyond Journey's End",

        // Chainsaw Man
        "تشينسو مان" to "Chainsaw Man",
        "رجل المنشار" to "Chainsaw Man",
        "csm" to "Chainsaw Man",
        "chainsawman" to "Chainsaw Man",

        // The Apothecary Diaries
        "الصيدلانيه" to "The Apothecary Diaries",
        "يوميات الصيدلانيه" to "The Apothecary Diaries",
        "apothekary" to "The Apothecary Diaries",

        // Haikyu
        "هايكيو" to "Haikyu",
        "haikyuu" to "Haikyu",

        // Additional global hits for Arabic -> AniList query mapping
        "ناروتو" to "Naruto",
        "بليتش" to "Bleach",
        "ديث نوت" to "Death Note",
        "مذكره الموت" to "Death Note",
        "طوكيو غول" to "Tokyo Ghoul",
        "بوكو نو هيرو" to "My Hero Academia",
        "اكاديميه بطلي" to "My Hero Academia",
        "بلاك كلوفر" to "Black Clover",
        "فل ميتال" to "Fullmetal Alchemist",
        "الكيميائي المعدني" to "Fullmetal Alchemist",
        "سباي اكس فاميلي" to "Spy x Family",
        "بلو لوك" to "Blue Lock",
        "دان دا دان" to "Dandadan",
        "كود غياس" to "Code Geass",
        "شتاينز جيت" to "Steins;Gate",
        "ري زيرو" to "Re:Zero",
        "موب سايكو" to "Mob Psycho 100",
        "ون بنش مان" to "One Punch Man",
        "رجل اللكمه الواحده" to "One Punch Man"
    ).mapKeys { normalize(it.key) }

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            val type = Character.getType(ch)
            // Skip Latin combining accents and Arabic harakat / tatweel
            if (type == Character.NON_SPACING_MARK.toInt() || ch == '\u0640') continue
            val mapped = when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                else -> ch.lowercaseChar()
            }
            if (mapped.isLetterOrDigit() || mapped.isWhitespace()) {
                sb.append(mapped)
            } else {
                sb.append(' ')
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Resolves an Arabic, shorthand, or typoed search query to the canonical English title
     * when sending a remote GraphQL query to AniList.
     */
    fun resolveRemoteQuery(rawQuery: String, knownTitles: List<String>): String {
        val norm = normalize(rawQuery)
        if (norm.isBlank()) return ""

        // 1. Exact alias hit
        aliasMap[norm]?.let { return it }

        // 2. Partial alias hit inside multi-word Arabic/Latin string
        for ((aliasKey, canonical) in aliasMap) {
            if (aliasKey.length >= 3 && (norm.contains(aliasKey) || aliasKey.contains(norm))) {
                return canonical
            }
        }

        // 3. Fuzzy match against known catalog titles (helps fix 1-2 character typos)
        val bestLocal = knownTitles
            .mapNotNull { title ->
                val normTitle = normalize(title)
                if (normTitle.contains(norm)) return@mapNotNull title to 1.0
                val sim = bestSimilarity(norm, normTitle)
                if (sim >= 0.72) title to sim else null
            }
            .maxByOrNull { it.second }

        return bestLocal?.first ?: rawQuery.trim()
    }

    fun rankAndFilter(query: String, items: List<AnimeSeries>): FuzzySearchOutput {
        val normQuery = normalize(query)
        if (normQuery.isBlank()) return FuzzySearchOutput(items = items, resolvedQueryHint = null)

        val aliasCanonical = aliasMap[normQuery]
            ?: aliasMap.entries.firstOrNull { (k, _) ->
                k.length >= 3 && normQuery.contains(k)
            }?.value
        val effectiveNorm = aliasCanonical?.let(::normalize) ?: normQuery

        val scored = items.mapNotNull { series ->
            val score = scoreSeries(normQuery, effectiveNorm, series)
            if (score > 0.0) series to score else null
        }.sortedWith(
            compareByDescending<Pair<AnimeSeries, Double>> { it.second }
                .thenByDescending { it.first.communityRating }
        )

        val matchedItems = scored.map { it.first }
        val top = matchedItems.firstOrNull()
        val showHint = if (top != null) {
            val topNorm = normalize(top.title)
            if (!topNorm.contains(normQuery)) {
                aliasCanonical ?: top.title
            } else {
                null
            }
        } else {
            aliasCanonical
        }

        return FuzzySearchOutput(
            items = matchedItems,
            resolvedQueryHint = showHint
        )
    }

    private fun scoreSeries(rawNorm: String, effectiveNorm: String, series: AnimeSeries): Double {
        val candidates = listOf(
            series.title,
            series.romajiTitle,
            series.studio,
            series.genres.joinToString(" ")
        ).filter { it.isNotBlank() }.map(::normalize)

        val titleNorm = candidates.firstOrNull().orEmpty()
        val romajiNorm = candidates.getOrNull(1).orEmpty()

        // 1. Direct containment on raw or canonicalized query
        if (titleNorm == effectiveNorm || romajiNorm == effectiveNorm) return 100.0
        if (titleNorm.startsWith(effectiveNorm) || romajiNorm.startsWith(effectiveNorm)) return 95.0
        if (titleNorm.contains(effectiveNorm) || romajiNorm.contains(effectiveNorm)) return 90.0
        if (candidates.any { it.contains(rawNorm) || it.contains(effectiveNorm) }) return 80.0

        // 2. Token-by-token Damerau-Levenshtein fuzzy similarity (handles typos/dropped letters/swapped letters)
        val simFull = maxOf(
            bestSimilarity(rawNorm, titleNorm),
            bestSimilarity(rawNorm, romajiNorm),
            bestSimilarity(effectiveNorm, titleNorm),
            bestSimilarity(effectiveNorm, romajiNorm)
        )
        if (simFull >= 0.58) {
            return 50.0 + simFull * 35.0
        }

        // 3. Ordered subsequence check for shorthand like "slvl" -> "solo leveling"
        if (rawNorm.length >= 3 && isSubsequence(rawNorm.replace(" ", ""), titleNorm.replace(" ", ""))) {
            return 45.0
        }

        return 0.0
    }

    private fun bestSimilarity(queryNorm: String, targetNorm: String): Double {
        if (queryNorm.isBlank() || targetNorm.isBlank()) return 0.0
        if (targetNorm.contains(queryNorm)) return 1.0

        val qNoSpace = queryNorm.replace(" ", "")
        val tNoSpace = targetNorm.replace(" ", "")
        val wholeDist = damerauLevenshtein(qNoSpace, tNoSpace)
        val wholeMax = maxOf(qNoSpace.length, tNoSpace.length).coerceAtLeast(1)
        val wholeSim = 1.0 - (wholeDist.toDouble() / wholeMax.toDouble())

        val qTokens = queryNorm.split(" ").filter { it.length >= 2 }
        val tTokens = targetNorm.split(" ").filter { it.length >= 2 }
        if (qTokens.isEmpty() || tTokens.isEmpty()) return wholeSim

        var tokenSum = 0.0
        for (qt in qTokens) {
            val bestTokenSim = tTokens.maxOf { tt ->
                if (tt.startsWith(qt) || qt.startsWith(tt)) {
                    0.92
                } else {
                    val d = damerauLevenshtein(qt, tt)
                    val m = maxOf(qt.length, tt.length).coerceAtLeast(1)
                    1.0 - (d.toDouble() / m.toDouble())
                }
            }
            tokenSum += bestTokenSim
        }
        val avgTokenSim = tokenSum / qTokens.size.toDouble()
        return maxOf(wholeSim, avgTokenSim)
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.isEmpty()) return true
        var i = 0
        for (ch in haystack) {
            if (ch == needle[i]) {
                i++
                if (i == needle.length) return true
            }
        }
        return false
    }

    fun damerauLevenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = best
            }
        }
        return dp[a.length][b.length]
    }
}
