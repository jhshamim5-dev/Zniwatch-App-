package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class ScheduleDayInfo(
    val timestamp: Long,
    val date: String,
    val wday: String,
    val dayName: String,
    val isActive: Boolean,
    var totalItems: Int = 0
)

data class AnikotoScheduleItem(
    val id: String,
    val tipId: String?,
    val title: String,
    val japaneseTitle: String?,
    val time: String,
    val episode: String,
    val episodeNumber: Int?,
    val status: String, // "aired", "airing_now", "upcoming"
    val date: String,
    val wday: String,
    val dayTimestamp: Long,
    val url: String,
    val slug: String,
    val image: String?
)

data class AnimeTooltipDetails(
    val tipId: String,
    val title: String,
    val japaneseTitle: String?,
    val synopsis: String,
    val rating: String?,
    val quality: String?,
    val subDubStatus: List<String>,
    val otherNames: String?,
    val scores: String?,
    val year: String?,
    val duration: String?,
    val status: String?,
    val genres: List<String>,
    val watchUrl: String
)

object AnikotoScheduleRepository {

    private const val BASE_URL = "https://anikoto.cz"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private const val CACHE_TTL_MS = 3 * 60 * 1000L // 3 minutes cache

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private class CacheEntry<T>(val data: T, val timestamp: Long = System.currentTimeMillis())
    private val cache = mutableMapOf<String, CacheEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCached(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            cache.remove(key)
            return null
        }
        return entry.data as? T
    }

    private fun <T> setCache(key: String, data: T) {
        cache[key] = CacheEntry(data)
    }

    /**
     * Fetches the home page to build a poster image lookup map based on titles
     */
    suspend fun fetchHomeImageMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val cacheKey = "home_image_map"
        getCached<Map<String, String>>(cacheKey)?.let { return@withContext it }

        val imageMap = mutableMapOf<String, String>()
        try {
            val req = Request.Builder()
                .url("$BASE_URL/home")
                .header("User-Agent", USER_AGENT)
                .header("Referer", BASE_URL)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val html = resp.body?.string() ?: ""
            val doc = Jsoup.parse(html)

            val imgElements = doc.select("img")
            for (el in imgElements) {
                var src = el.attr("src")
                if (src.isEmpty()) src = el.attr("data-src")
                val alt = el.attr("alt").trim()

                val fullSrc = when {
                    src.startsWith("//") -> "https:$src"
                    src.startsWith("/") -> "$BASE_URL$src"
                    else -> src
                }

                if (fullSrc.isNotEmpty() && alt.isNotEmpty() && fullSrc.startsWith("http")) {
                    val altLower = alt.lowercase()
                    val cleanAlt = altLower.replace(Regex("(?i)season \\d+"), "").trim()
                    imageMap[altLower] = fullSrc
                    if (cleanAlt.isNotEmpty()) {
                        imageMap[cleanAlt] = fullSrc
                    }
                }
            }
            setCache(cacheKey, imageMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext imageMap
    }

    /**
     * Parses schedule item HTML fragments into structured AnimeScheduleItem array
     */
    fun parseScheduleItems(
        html: String,
        dayInfo: ScheduleDayInfo,
        imageMap: Map<String, String> = emptyMap()
    ): List<AnikotoScheduleItem> {
        val doc = Jsoup.parse(html)
        val items = mutableListOf<AnikotoScheduleItem>()

        val itemElements = doc.select("a.item")
        for (el in itemElements) {
            val href = el.attr("href")
            val fullUrl = if (href.startsWith("http")) href else "$BASE_URL$href"

            // Extract slug and ID
            val slugMatch = Regex("/watch/([^/]+)").find(href)
            val slug = slugMatch?.groupValues?.get(1) ?: ""
            val slugParts = slug.split("-")
            val animeId = if (slugParts.size > 1) slugParts.last() else slug

            val time = el.select(".time").text().trim()
            val tipId = el.select(".time").attr("data-tip").ifEmpty { null }
            val episodeStr = el.select(".ep span").text().trim()
            val epMatch = Regex("\\d+").find(episodeStr)
            val episodeNumber = epMatch?.value?.toIntOrNull()

            val title = el.select(".title").text().trim()
            val japaneseTitle = el.select(".title").attr("data-jp").ifEmpty { null }

            val status = when {
                el.hasClass("old") -> "aired"
                el.hasClass("active") -> "airing_now"
                else -> "upcoming"
            }

            val titleLower = title.lowercase()
            val jpLower = (japaneseTitle ?: "").lowercase()

            var directImg = el.select("img").attr("data-src").ifEmpty { el.select("img").attr("src") }
            if (directImg.startsWith("//")) directImg = "https:$directImg"
            else if (directImg.startsWith("/")) directImg = "$BASE_URL$directImg"

            var image: String? = directImg.ifEmpty { null }
            if (image == null) {
                image = imageMap[titleLower] ?: imageMap[jpLower]
            }

            if (image == null) {
                val mapKey = imageMap.keys.find { k ->
                    k.contains(titleLower) || titleLower.contains(k)
                }
                if (mapKey != null) {
                    image = imageMap[mapKey]
                }
            }

            items.add(
                AnikotoScheduleItem(
                    id = if (animeId.isNotEmpty()) animeId else slug,
                    tipId = tipId,
                    title = title,
                    japaneseTitle = japaneseTitle,
                    time = time,
                    episode = episodeStr,
                    episodeNumber = episodeNumber,
                    status = status,
                    date = dayInfo.date,
                    wday = dayInfo.wday,
                    dayTimestamp = dayInfo.timestamp,
                    url = fullUrl,
                    slug = slug,
                    image = image
                )
            )
        }
        return items
    }

    /**
     * Main function to fetch schedule from Anikoto for a day name
     */
    suspend fun getScheduleForDayName(dayName: String): List<ScheduleAnimeItem> = withContext(Dispatchers.IO) {
        val cacheKey = "schedule_day_name_${dayName.lowercase()}"
        getCached<List<ScheduleAnimeItem>>(cacheKey)?.let { return@withContext it }

        try {
            val imageMap = fetchHomeImageMap()

            val req = Request.Builder()
                .url("$BASE_URL/ajax/schedule?tz=0")
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$BASE_URL/home")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val jsonStr = resp.body?.string() ?: ""
            val jsonObj = JSONObject(jsonStr)

            if (jsonObj.optInt("status") == 200) {
                val htmlResult = jsonObj.optString("result", "")
                val doc = Jsoup.parse(htmlResult)

                // Parse day tabs
                val dayElements = doc.select(".day .inner")
                val days = mutableListOf<ScheduleDayInfo>()
                for (el in dayElements) {
                    val timeStr = el.attr("data-time").ifEmpty { "0" }
                    val date = el.select(".date").text().trim()
                    val wday = el.select(".wday").text().trim()
                    val isActive = el.parent()?.hasClass("active") == true

                    days.add(
                        ScheduleDayInfo(
                            timestamp = timeStr.toLongOrNull() ?: 0L,
                            date = date,
                            wday = wday,
                            dayName = "$date ($wday)",
                            isActive = isActive
                        )
                    )
                }

                // Find matching day for dayName (e.g. "Monday", "Mon", "Tuesday", etc.)
                val targetDay = days.find { day ->
                    day.wday.equals(dayName, ignoreCase = true) ||
                    dayName.startsWith(day.wday, ignoreCase = true) ||
                    day.wday.startsWith(dayName.take(3), ignoreCase = true)
                } ?: days.find { it.isActive } ?: days.firstOrNull()

                if (targetDay != null) {
                    val items = if (targetDay.isActive) {
                        parseScheduleItems(htmlResult, targetDay, imageMap)
                    } else {
                        fetchScheduleItemsForTimestamp(targetDay.timestamp, targetDay, imageMap)
                    }

                    val resultList = coroutineScope {
                        items.map { item ->
                            async {
                                var imgUrl = item.image ?: ""
                                if (imgUrl.isEmpty()) {
                                    imgUrl = fetchCoverImageFromAniList(item.title) ?: ""
                                }
                                ScheduleAnimeItem(
                                    id = item.id.ifEmpty { item.title.hashCode().toString() },
                                    title = item.title,
                                    rating = "8.5",
                                    airTime = if (item.time.isNotEmpty()) "${item.time} UTC" else "18:00 UTC",
                                    episode = if (item.episode.isNotEmpty()) item.episode else "Ep 1",
                                    isSub = true,
                                    isDub = item.status == "aired",
                                    imageUrl = imgUrl,
                                    genres = "Anime"
                                )
                            }
                        }.awaitAll()
                    }

                    if (resultList.isNotEmpty()) {
                        setCache(cacheKey, resultList)
                        return@withContext resultList
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    private suspend fun fetchCoverImageFromAniList(title: String): String? {
        val cacheKey = "cover_anilist_${title.lowercase().trim()}"
        getCached<String>(cacheKey)?.let { return it }

        try {
            val cleanTitle = title
                .replace(Regex("(?i)\\b(sub|dub|uncensored|raw|tv|movie|ova|ona)\\b"), "")
                .replace(Regex("(?i)\\((sub|dub|uncensored|raw|tv|movie|ova|ona)\\)"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val json = JSONObject()
            json.put("query", """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    coverImage {
                      extraLarge
                      large
                    }
                  }
                }
            """.trimIndent())
            val variables = JSONObject()
            variables.put("search", cleanTitle)
            json.put("variables", variables)

            val mediaType = "application/json".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            val respObj = JSONObject(respStr)
            val media = respObj.optJSONObject("data")?.optJSONObject("Media")
            val cover = media?.optJSONObject("coverImage")
            val coverUrl = cover?.optString("extraLarge")?.ifEmpty { null }
                ?: cover?.optString("large")?.ifEmpty { null }

            if (coverUrl != null) {
                setCache(cacheKey, coverUrl)
                return coverUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun fetchScheduleItemsForTimestamp(
        timestamp: Long,
        dayInfo: ScheduleDayInfo,
        imageMap: Map<String, String>
    ): List<AnikotoScheduleItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/ajax/schedule/date?tz=0&time=$timestamp")
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$BASE_URL/home")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val jsonStr = resp.body?.string() ?: ""
            val jsonObj = JSONObject(jsonStr)
            val htmlResult = jsonObj.optString("result", "")

            if (htmlResult.isNotEmpty()) {
                return@withContext parseScheduleItems(htmlResult, dayInfo, imageMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * Fetch rich anime details from tooltip endpoint
     */
    suspend fun getAnimeTooltip(tipId: String): AnimeTooltipDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "tooltip_$tipId"
        getCached<AnimeTooltipDetails>(cacheKey)?.let { return@withContext it }

        try {
            val req = Request.Builder()
                .url("$BASE_URL/ajax/anime/tooltip/$tipId")
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$BASE_URL/home")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val html = resp.body?.string() ?: ""
            val doc = Jsoup.parse(html)

            val title = doc.select(".title").text().trim()
            val japaneseTitle = doc.select(".title").attr("data-jp").ifEmpty { null }
            val rating = doc.select(".rating").text().trim().ifEmpty { null }
            val quality = doc.select(".quality").text().trim().ifEmpty { null }
            val synopsis = doc.select(".synopsis").text().trim()

            val subDubStatus = doc.select(".ep-status").map { it.text().trim() }.filter { it.isNotEmpty() }

            var otherNames: String? = null
            var scores: String? = null
            var year: String? = null
            var duration: String? = null
            var status: String? = null
            val genres = mutableListOf<String>()

            for (el in doc.select(".meta-bl > div")) {
                val text = el.text().replace(Regex("\\s+"), " ").trim()
                when {
                    text.startsWith("Other names:") -> otherNames = text.removePrefix("Other names:").trim()
                    text.startsWith("Scores:") -> scores = text.removePrefix("Scores:").trim()
                    text.contains(Regex("Year\\s*:")) -> year = text.replace(Regex(".*Year\\s*:"), "").trim()
                    text.startsWith("Duration:") -> duration = text.removePrefix("Duration:").trim()
                    text.startsWith("Status:") -> status = text.removePrefix("Status:").trim()
                    text.startsWith("Genre:") -> {
                        el.select("a").forEach { aEl ->
                            genres.add(aEl.text().trim())
                        }
                    }
                }
            }

            val watchAttr = doc.select(".actions a.watch").attr("href")
            val watchUrl = if (watchAttr.startsWith("http")) watchAttr else "$BASE_URL$watchAttr"

            val details = AnimeTooltipDetails(
                tipId = tipId,
                title = title,
                japaneseTitle = japaneseTitle,
                synopsis = synopsis,
                rating = rating,
                quality = quality,
                subDubStatus = subDubStatus,
                otherNames = otherNames,
                scores = scores,
                year = year,
                duration = duration,
                status = status,
                genres = genres,
                watchUrl = watchUrl
            )
            setCache(cacheKey, details)
            return@withContext details
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
