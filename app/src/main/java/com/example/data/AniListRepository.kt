package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AniListRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun cleanTitle(rawTitle: String): String {
        var clean = rawTitle
            .replace(Regex("(?i)\\b(sub|dub|uncensored|raw|tv|movie|ova|ona)\\b"), "")
            .replace(Regex("(?i)\\((sub|dub|uncensored|raw|tv|movie|ova|ona)\\)"), "")
            .replace(Regex("(?i)\\[(sub|dub|uncensored|raw|tv|movie|ova|ona)\\]"), "")
            .replace(Regex("(?i)-?\\s*episode\\s*\\d+.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return clean.ifEmpty { rawTitle }
    }

    suspend fun getAnimeDetails(
        title: String,
        defaultId: String,
        defaultImg: String,
        defaultRating: String
    ): AnimeDetailResult = withContext(Dispatchers.IO) {
        val cleanedTitle = cleanTitle(title)
        val numericId = defaultId.toIntOrNull()

        val graphqlQuery = """
            query (${'$'}search: String, ${'$'}id: Int) {
              Media (id: ${'$'}id, search: ${'$'}search, type: ANIME) {
                id
                status
                averageScore
                genres
                bannerImage
                coverImage { extraLarge }
                title { romaji english native }
                description(asHtml: false)
                episodes
                duration
                format
                season
                seasonYear
                startDate { year month day }
                endDate { year month day }
                nextAiringEpisode {
                  airingAt
                  timeUntilAiring
                  episode
                }
                studios {
                  nodes {
                    name
                    isAnimationStudio
                  }
                }
                trailer {
                  id
                  site
                }
                characters(perPage: 12, sort: [ROLE, RELEVANCE]) {
                  edges {
                    role
                    node {
                      id
                      name { full }
                      image { large }
                    }
                  }
                }
                staff(perPage: 12, sort: [RELEVANCE]) {
                  edges {
                    role
                    node {
                      id
                      name { full }
                      image { large }
                    }
                  }
                }
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title { romaji english }
                      averageScore
                      coverImage { extraLarge }
                      format
                    }
                  }
                }
                recommendations(perPage: 12, sort: [RATING_DESC]) {
                  nodes {
                    mediaRecommendation {
                      id
                      title { romaji english }
                      averageScore
                      coverImage { extraLarge }
                      format
                    }
                  }
                }
              }
            }
        """.trimIndent()

        // First attempt with numericId if available, or cleaned search title
        var result = fetchGraphQL(graphqlQuery, cleanedTitle, numericId)
        if (result == null && numericId != null) {
            // Retry with search title alone if ID lookup failed
            result = fetchGraphQL(graphqlQuery, cleanedTitle, null)
        }
        if (result == null && cleanedTitle != title) {
            // Retry with raw title if cleaned title didn't match
            result = fetchGraphQL(graphqlQuery, title, null)
        }

        if (result != null) {
            return@withContext parseMediaJson(result, defaultId, title, defaultImg, defaultRating)
        }

        return@withContext AnimeDetailResult(
            id = defaultId,
            title = title,
            rating = defaultRating,
            imageUrl = defaultImg,
            bannerUrl = defaultImg,
            genres = "Anime",
            status = "Finished Airing"
        )
    }

    private fun fetchGraphQL(query: String, search: String?, id: Int?): JSONObject? {
        try {
            val queryJson = JSONObject()
            queryJson.put("query", query)
            val variables = JSONObject()
            if (id != null) {
                variables.put("id", id)
            } else if (!search.isNullOrBlank()) {
                variables.put("search", search)
            } else {
                return null
            }
            queryJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = queryJson.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (respStr.isNotEmpty()) {
                val json = JSONObject(respStr)
                val media = json.optJSONObject("data")?.optJSONObject("Media")
                if (media != null) {
                    return media
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseMediaJson(
        media: JSONObject,
        defaultId: String,
        fallbackTitle: String,
        defaultImg: String,
        defaultRating: String
    ): AnimeDetailResult {
        val titleObj = media.optJSONObject("title")
        val englishTitle = titleObj?.optString("english", "")?.ifEmpty { null }
        val romajiTitle = titleObj?.optString("romaji", "")?.ifEmpty { null }
        val displayTitle = englishTitle ?: romajiTitle ?: fallbackTitle

        val statusRaw = media.optString("status", "FINISHED")
        val statusFormatted = when (statusRaw.uppercase()) {
            "FINISHED" -> "Finished Airing"
            "RELEASING" -> "Currently Airing"
            "NOT_YET_RELEASED" -> "Not Yet Aired"
            "CANCELLED" -> "Cancelled"
            "HIATUS" -> "On Hiatus"
            else -> statusRaw.replace("_", " ")
        }

        val scoreVal = media.optInt("averageScore", 85)
        val ratingStr = String.format("%.1f", if (scoreVal > 0) scoreVal / 10.0 else defaultRating.toDoubleOrNull() ?: 8.5)
        val banner = media.optString("bannerImage", "")
        val cover = media.optJSONObject("coverImage")?.optString("extraLarge", "") ?: ""

        val genresArr = media.optJSONArray("genres")
        val genresList = mutableListOf<String>()
        if (genresArr != null) {
            for (i in 0 until genresArr.length()) {
                genresList.add(genresArr.getString(i))
            }
        }
        val genresStr = if (genresList.isNotEmpty()) genresList.take(3).joinToString(" • ") else "Action • Fantasy"

        val rawDesc = media.optString("description", "")
        val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "").trim()

        val epCountInt = media.optInt("episodes", 0)
        val epCountStr = if (epCountInt > 0) "$epCountInt Episodes" else "Ongoing"

        val durInt = media.optInt("duration", 0)
        val durationStr = if (durInt > 0) "$durInt min per ep" else "N/A"

        val fmtRaw = media.optString("format", "TV").replace("_", " ")

        val seasonName = media.optString("season", "")
        val seasonYr = media.optInt("seasonYear", 0)
        val seasonStr = if (seasonName.isNotEmpty()) {
            "${seasonName.lowercase().replaceFirstChar { it.uppercase() }} ${if (seasonYr > 0) seasonYr else ""}".trim()
        } else "N/A"

        fun parseDateObj(obj: JSONObject?): String {
            if (obj == null) return ""
            val y = obj.optInt("year", 0)
            val m = obj.optInt("month", 0)
            val d = obj.optInt("day", 0)
            if (y == 0) return ""
            val mNames = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val mStr = if (m in 1..12) mNames[m] else ""
            return if (mStr.isNotEmpty()) "$mStr ${if (d > 0) "$d, " else ""}$y" else "$y"
        }

        val startStr = parseDateObj(media.optJSONObject("startDate"))
        val endStr = parseDateObj(media.optJSONObject("endDate"))
        val airingPeriodStr = when {
            startStr.isNotEmpty() && endStr.isNotEmpty() -> "$startStr - $endStr"
            startStr.isNotEmpty() -> "Aired from $startStr"
            else -> "N/A"
        }

        val studioNodes = media.optJSONObject("studios")?.optJSONArray("nodes")
        var studioName = "N/A"
        val producerList = mutableListOf<String>()
        if (studioNodes != null) {
            for (i in 0 until studioNodes.length()) {
                val stObj = studioNodes.getJSONObject(i)
                val name = stObj.optString("name", "")
                val isAnim = stObj.optBoolean("isAnimationStudio", false)
                if (isAnim && studioName == "N/A") {
                    studioName = name
                } else if (name.isNotEmpty()) {
                    producerList.add(name)
                }
            }
        }
        val producersStr = if (producerList.isNotEmpty()) producerList.take(3).joinToString(", ") else "N/A"

        val nextEpObj = media.optJSONObject("nextAiringEpisode")
        var nextEpNum: Int? = null
        var nextEpTimestamp: Long? = null
        if (nextEpObj != null) {
            val ep = nextEpObj.optInt("episode", 0)
            val airingAt = nextEpObj.optLong("airingAt", 0L)
            if (ep > 0 && airingAt > 0) {
                nextEpNum = ep
                nextEpTimestamp = airingAt
            }
        }

        // Trailer
        val trailerObj = media.optJSONObject("trailer")
        var youtubeId: String? = null
        if (trailerObj != null) {
            val site = trailerObj.optString("site", "")
            val tid = trailerObj.optString("id", "")
            if (site.contains("youtube", ignoreCase = true) && tid.isNotEmpty()) {
                youtubeId = tid
            }
        }

        // Characters
        val charList = mutableListOf<CharacterItem>()
        val charEdges = media.optJSONObject("characters")?.optJSONArray("edges")
        if (charEdges != null) {
            for (i in 0 until charEdges.length()) {
                val edge = charEdges.getJSONObject(i)
                val roleRaw = edge.optString("role", "MAIN")
                val roleStr = if (roleRaw.contains("MAIN", ignoreCase = true)) "Main" else "Supporting"
                val node = edge.optJSONObject("node")
                if (node != null) {
                    val cId = node.optString("id", "")
                    val cName = node.optJSONObject("name")?.optString("full", "") ?: "Character"
                    val cImg = node.optJSONObject("image")?.optString("large", "") ?: ""
                    if (cName.isNotEmpty()) {
                        charList.add(CharacterItem(id = cId, name = cName, role = roleStr, imageUrl = cImg))
                    }
                }
            }
        }

        // Staff
        val staffList = mutableListOf<StaffItem>()
        val staffEdges = media.optJSONObject("staff")?.optJSONArray("edges")
        if (staffEdges != null) {
            for (i in 0 until staffEdges.length()) {
                val edge = staffEdges.getJSONObject(i)
                val roleStr = edge.optString("role", "Staff")
                val node = edge.optJSONObject("node")
                if (node != null) {
                    val sId = node.optString("id", "")
                    val sName = node.optJSONObject("name")?.optString("full", "") ?: "Staff"
                    val sImg = node.optJSONObject("image")?.optString("large", "") ?: ""
                    if (sName.isNotEmpty()) {
                        staffList.add(StaffItem(id = sId, name = sName, role = roleStr, imageUrl = sImg))
                    }
                }
            }
        }

        // Relations
        val relationList = mutableListOf<AnimeCardItem>()
        val relEdges = media.optJSONObject("relations")?.optJSONArray("edges")
        if (relEdges != null) {
            for (i in 0 until relEdges.length()) {
                val edge = relEdges.getJSONObject(i)
                val node = edge.optJSONObject("node")
                if (node != null) {
                    val rId = node.optString("id", "")
                    val relTitleObj = node.optJSONObject("title")
                    val rTitle = relTitleObj?.optString("english", "")?.ifEmpty { relTitleObj.optString("romaji", "") } ?: ""
                    val rScore = node.optInt("averageScore", 85)
                    val rRating = String.format("%.1f", if (rScore > 0) rScore / 10.0 else 8.5)
                    val rCover = node.optJSONObject("coverImage")?.optString("extraLarge", "") ?: ""
                    val rFmt = node.optString("format", "TV")
                    if (rTitle.isNotEmpty()) {
                        relationList.add(AnimeCardItem(id = rId, title = rTitle, rating = rRating, imageResId = 0, imageUrl = rCover, type = rFmt))
                    }
                }
            }
        }

        // Recommendations
        val recList = mutableListOf<AnimeCardItem>()
        val recNodes = media.optJSONObject("recommendations")?.optJSONArray("nodes")
        if (recNodes != null) {
            for (i in 0 until recNodes.length()) {
                val node = recNodes.getJSONObject(i).optJSONObject("mediaRecommendation")
                if (node != null) {
                    val rcId = node.optString("id", "")
                    val recTitleObj = node.optJSONObject("title")
                    val rcTitle = recTitleObj?.optString("english", "")?.ifEmpty { recTitleObj.optString("romaji", "") } ?: ""
                    val rcScore = node.optInt("averageScore", 85)
                    val rcRating = String.format("%.1f", if (rcScore > 0) rcScore / 10.0 else 8.5)
                    val rcCover = node.optJSONObject("coverImage")?.optString("extraLarge", "") ?: ""
                    val rcFmt = node.optString("format", "TV")
                    if (rcTitle.isNotEmpty()) {
                        recList.add(AnimeCardItem(id = rcId, title = rcTitle, rating = rcRating, imageResId = 0, imageUrl = rcCover, type = rcFmt))
                    }
                }
            }
        }

        val anilistId = media.optString("id", defaultId)

        return AnimeDetailResult(
            id = if (anilistId.isNotEmpty()) anilistId else defaultId,
            title = displayTitle,
            rating = ratingStr,
            imageUrl = cover.ifEmpty { defaultImg },
            bannerUrl = banner.ifEmpty { cover.ifEmpty { defaultImg } },
            genres = genresStr,
            status = statusFormatted,
            description = cleanDesc,
            studio = studioName,
            producers = producersStr,
            episodes = epCountStr,
            format = fmtRaw,
            duration = durationStr,
            airingPeriod = airingPeriodStr,
            season = seasonStr,
            nextAiringEpisode = nextEpNum,
            nextAiringAtTimestamp = nextEpTimestamp,
            trailerYoutubeId = youtubeId,
            characters = charList,
            staff = staffList,
            relations = relationList,
            recommendations = recList
        )
    }

    suspend fun getRelatedAnime(animeId: String, title: String): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        try {
            val details = getAnimeDetails(title, animeId, "", "8.5")
            if (details.relations.isNotEmpty()) return@withContext details.relations
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    suspend fun getRecommendedAnime(animeId: String, title: String): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        try {
            val details = getAnimeDetails(title, animeId, "", "8.5")
            if (details.recommendations.isNotEmpty()) return@withContext details.recommendations
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    suspend fun getCharacterDetails(
        id: String,
        name: String,
        defaultImg: String
    ): CharacterDetailResult = withContext(Dispatchers.IO) {
        try {
            val queryJson = JSONObject()
            queryJson.put(
                "query",
                """
                query (${'$'}id: Int, ${'$'}search: String) {
                  Character (id: ${'$'}id, search: ${'$'}search) {
                    id
                    name { full native }
                    image { large }
                    description(asHtml: false)
                    gender
                    age
                    dateOfBirth { year month day }
                  }
                }
                """.trimIndent()
            )
            val variables = JSONObject()
            val numericId = id.toIntOrNull()
            if (numericId != null) {
                variables.put("id", numericId)
            } else {
                variables.put("search", name)
            }
            queryJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = queryJson.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (respStr.isNotEmpty()) {
                val json = JSONObject(respStr)
                val charObj = json.optJSONObject("data")?.optJSONObject("Character")
                if (charObj != null) {
                    val full = charObj.optJSONObject("name")?.optString("full", name) ?: name
                    val nativeName = charObj.optJSONObject("name")?.optString("native", "") ?: ""
                    val img = charObj.optJSONObject("image")?.optString("large", defaultImg) ?: defaultImg
                    val rawDesc = charObj.optString("description", "")
                    val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "").replace(Regex("~!|!~"), "").trim()
                    val gender = charObj.optString("gender", "")
                    val age = charObj.optString("age", "")

                    val dobObj = charObj.optJSONObject("dateOfBirth")
                    var dobStr = ""
                    if (dobObj != null) {
                        val y = dobObj.optInt("year", 0)
                        val m = dobObj.optInt("month", 0)
                        val d = dobObj.optInt("day", 0)
                        val mNames = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        val mStr = if (m in 1..12) mNames[m] else ""
                        dobStr = if (mStr.isNotEmpty()) "$mStr ${if (d > 0) "$d" else ""}${if (y > 0) ", $y" else ""}".trim() else if (y > 0) "$y" else ""
                    }

                    return@withContext CharacterDetailResult(
                        id = charObj.optInt("id", 0).toString().ifEmpty { id },
                        name = full,
                        nativeName = nativeName,
                        imageUrl = img,
                        description = cleanDesc,
                        gender = gender,
                        age = age,
                        dateOfBirth = dobStr
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext CharacterDetailResult(
            id = id,
            name = name,
            imageUrl = defaultImg,
            description = "No detailed description available."
        )
    }

    suspend fun getStaffDetails(
        id: String,
        name: String,
        defaultImg: String
    ): StaffDetailResult = withContext(Dispatchers.IO) {
        try {
            val queryJson = JSONObject()
            queryJson.put(
                "query",
                """
                query (${'$'}id: Int, ${'$'}search: String) {
                  Staff (id: ${'$'}id, search: ${'$'}search) {
                    id
                    name { full native }
                    image { large }
                    description(asHtml: false)
                    gender
                    age
                    primaryOccupations
                  }
                }
                """.trimIndent()
            )
            val variables = JSONObject()
            val numericId = id.toIntOrNull()
            if (numericId != null) {
                variables.put("id", numericId)
            } else {
                variables.put("search", name)
            }
            queryJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = queryJson.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (respStr.isNotEmpty()) {
                val json = JSONObject(respStr)
                val staffObj = json.optJSONObject("data")?.optJSONObject("Staff")
                if (staffObj != null) {
                    val full = staffObj.optJSONObject("name")?.optString("full", name) ?: name
                    val nativeName = staffObj.optJSONObject("name")?.optString("native", "") ?: ""
                    val img = staffObj.optJSONObject("image")?.optString("large", defaultImg) ?: defaultImg
                    val rawDesc = staffObj.optString("description", "")
                    val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "").replace(Regex("~!|!~"), "").trim()
                    val gender = staffObj.optString("gender", "")
                    val age = staffObj.optString("age", "")

                    val occArr = staffObj.optJSONArray("primaryOccupations")
                    val occList = mutableListOf<String>()
                    if (occArr != null) {
                        for (i in 0 until occArr.length()) {
                            occList.add(occArr.getString(i))
                        }
                    }
                    val occStr = occList.joinToString(", ")

                    return@withContext StaffDetailResult(
                        id = staffObj.optInt("id", 0).toString().ifEmpty { id },
                        name = full,
                        nativeName = nativeName,
                        imageUrl = img,
                        description = cleanDesc,
                        gender = gender,
                        age = age,
                        occupations = occStr
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext StaffDetailResult(
            id = id,
            name = name,
            imageUrl = defaultImg,
            description = "No detailed description available."
        )
    }

    suspend fun getAnimeGalleryImages(
        title: String,
        defaultBanner: String,
        defaultCover: String
    ): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val queryJson = JSONObject()
            queryJson.put(
                "query",
                """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    bannerImage
                    coverImage { extraLarge }
                    streamingEpisodes {
                      thumbnail
                    }
                  }
                }
                """.trimIndent()
            )
            val variables = JSONObject()
            variables.put("search", title)
            queryJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = queryJson.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (respStr.isNotEmpty()) {
                val json = JSONObject(respStr)
                val media = json.optJSONObject("data")?.optJSONObject("Media")
                if (media != null) {
                    val banner = media.optString("bannerImage", "")
                    val cover = media.optJSONObject("coverImage")?.optString("extraLarge", "") ?: ""

                    if (banner.isNotEmpty()) list.add(banner)

                    val episodesArr = media.optJSONArray("streamingEpisodes")
                    if (episodesArr != null) {
                        for (i in 0 until episodesArr.length()) {
                            val epObj = episodesArr.getJSONObject(i)
                            val thumb = epObj.optString("thumbnail", "")
                            if (thumb.isNotEmpty() && !list.contains(thumb)) {
                                list.add(thumb)
                            }
                        }
                    }
                    if (cover.isNotEmpty() && !list.contains(cover)) {
                        list.add(cover)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            if (defaultBanner.isNotEmpty()) list.add(defaultBanner)
            if (defaultCover.isNotEmpty() && !list.contains(defaultCover)) list.add(defaultCover)
        }
        return@withContext list
    }

    suspend fun getEpisodeThumbnailsMap(title: String): Map<Int, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Int, String>()
        val cleanedTitle = cleanTitle(title)

        // 1. AniList GraphQL streamingEpisodes
        try {
            val queryJson = JSONObject()
            queryJson.put(
                "query",
                """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    streamingEpisodes {
                      title
                      thumbnail
                    }
                  }
                }
                """.trimIndent()
            )
            val variables = JSONObject()
            variables.put("search", cleanedTitle)
            queryJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = queryJson.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (respStr.isNotEmpty()) {
                val json = JSONObject(respStr)
                val media = json.optJSONObject("data")?.optJSONObject("Media")
                val episodesArr = media?.optJSONArray("streamingEpisodes")
                if (episodesArr != null) {
                    for (i in 0 until episodesArr.length()) {
                        val epObj = episodesArr.getJSONObject(i)
                        val epTitle = epObj.optString("title", "")
                        val thumb = epObj.optString("thumbnail", "")
                        if (thumb.isNotEmpty()) {
                            val match = Regex("""(?:Episode|Ep|\b)\s*(\d+)""", RegexOption.IGNORE_CASE).find(epTitle)
                            val epNum = match?.groupValues?.get(1)?.toIntOrNull()
                            if (epNum != null && !map.containsKey(epNum)) {
                                map[epNum] = thumb
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Kitsu API fallback if map is empty or sparse
        try {
            val encodedTitle = java.net.URLEncoder.encode(cleanedTitle, "UTF-8")
            val kitsuSearchReq = Request.Builder()
                .url("https://kitsu.io/api/edge/anime?filter[text]=$encodedTitle")
                .build()
            val kitsuResp = okHttpClient.newCall(kitsuSearchReq).execute()
            val kitsuJson = JSONObject(kitsuResp.body?.string() ?: "{}")
            val kitsuData = kitsuJson.optJSONArray("data")
            if (kitsuData != null && kitsuData.length() > 0) {
                val kitsuId = kitsuData.getJSONObject(0).optString("id")
                if (kitsuId.isNotEmpty()) {
                    val kitsuEpReq = Request.Builder()
                        .url("https://kitsu.io/api/edge/anime/$kitsuId/episodes?page[limit]=100")
                        .build()
                    val kitsuEpResp = okHttpClient.newCall(kitsuEpReq).execute()
                    val kitsuEpJson = JSONObject(kitsuEpResp.body?.string() ?: "{}")
                    val epDataArr = kitsuEpJson.optJSONArray("data")
                    if (epDataArr != null) {
                        for (i in 0 until epDataArr.length()) {
                            val epObj = epDataArr.getJSONObject(i)
                            val attr = epObj.optJSONObject("attributes")
                            val num = attr?.optInt("number", 0) ?: 0
                            val thumbObj = attr?.optJSONObject("thumbnail")
                            val thumbUrl = thumbObj?.optString("original", "")?.ifEmpty { thumbObj.optString("medium", "") } ?: ""
                            if (num > 0 && thumbUrl.isNotEmpty() && !map.containsKey(num)) {
                                map[num] = thumbUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext map
    }

    suspend fun getUserProfile(username: String): AniListUserProfile? = withContext(Dispatchers.IO) {
        try {
            val graphqlQuery = """
                query (${'$'}name: String) {
                  User (name: ${'$'}name) {
                    id
                    name
                    avatar {
                      large
                      medium
                    }
                    bannerImage
                    statistics {
                      anime {
                        count
                        episodesWatched
                        minutesWatched
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("name", username.trim())
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val userObj = json.optJSONObject("data")?.optJSONObject("User")
                if (userObj != null) {
                    val id = userObj.optInt("id", 0)
                    val name = userObj.optString("name", username)
                    val avatarObj = userObj.optJSONObject("avatar")
                    val avatarUrl = avatarObj?.optString("large", "")?.ifEmpty { avatarObj.optString("medium", "") } ?: ""
                    val bannerUrl = userObj.optString("bannerImage", "")
                    
                    val animeStats = userObj.optJSONObject("statistics")?.optJSONObject("anime")
                    val animeCount = animeStats?.optInt("count", 0) ?: 0
                    val episodesWatched = animeStats?.optInt("episodesWatched", 0) ?: 0
                    val minutesWatched = animeStats?.optLong("minutesWatched", 0L) ?: 0L

                    return@withContext AniListUserProfile(
                        id = id,
                        name = name,
                        avatarUrl = avatarUrl,
                        bannerUrl = bannerUrl,
                        animeCount = animeCount,
                        episodesWatched = episodesWatched,
                        minutesWatched = minutesWatched
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun exchangeCodeForToken(code: String, clientId: String, clientSecret: String, redirectUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("grant_type", "authorization_code")
                put("client_id", clientId)
                put("client_secret", clientSecret)
                put("redirect_uri", redirectUri)
                put("code", code)
            }

            val request = Request.Builder()
                .url("https://anilist.co/api/v2/oauth/token")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                return@withContext if (json.has("access_token")) json.getString("access_token") else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun getAuthenticatedUser(accessToken: String): AniListUserProfile? = withContext(Dispatchers.IO) {
        try {
            val graphqlQuery = """
                query {
                  Viewer {
                    id
                    name
                    avatar {
                      large
                      medium
                    }
                    bannerImage
                    statistics {
                      anime {
                        count
                        episodesWatched
                        minutesWatched
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val userObj = json.optJSONObject("data")?.optJSONObject("Viewer")
                if (userObj != null) {
                    val id = userObj.optInt("id", 0)
                    val name = userObj.optString("name", "")
                    val avatarObj = userObj.optJSONObject("avatar")
                    val avatarUrl = avatarObj?.optString("large", "")?.ifEmpty { avatarObj.optString("medium", "") } ?: ""
                    val bannerUrl = userObj.optString("bannerImage", "")
                    
                    val animeStats = userObj.optJSONObject("statistics")?.optJSONObject("anime")
                    val animeCount = animeStats?.optInt("count", 0) ?: 0
                    val episodesWatched = animeStats?.optInt("episodesWatched", 0) ?: 0
                    val minutesWatched = animeStats?.optLong("minutesWatched", 0L) ?: 0L

                    return@withContext AniListUserProfile(
                        id = id,
                        name = name,
                        avatarUrl = avatarUrl,
                        bannerUrl = bannerUrl,
                        animeCount = animeCount,
                        episodesWatched = episodesWatched,
                        minutesWatched = minutesWatched
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun getUserFavorites(username: String): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AnimeCardItem>()
        if (username.isBlank()) return@withContext list
        try {
            val graphqlQuery = """
                query (${'$'}name: String) {
                  User (name: ${'$'}name) {
                    favourites {
                      anime {
                        nodes {
                          id
                          title {
                            userPreferred
                            english
                            romaji
                          }
                          coverImage {
                            extraLarge
                            large
                          }
                          averageScore
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("name", username.trim())
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val nodes = json.optJSONObject("data")
                    ?.optJSONObject("User")
                    ?.optJSONObject("favourites")
                    ?.optJSONObject("anime")
                    ?.optJSONArray("nodes")

                if (nodes != null) {
                    for (i in 0 until nodes.length()) {
                        val media = nodes.optJSONObject(i) ?: continue
                        val id = media.optInt("id", 0)
                        val titleObj = media.optJSONObject("title")
                        val title = titleObj?.optString("userPreferred")?.ifEmpty {
                            titleObj.optString("english")?.ifEmpty { titleObj.optString("romaji", "Anime") }
                        } ?: "Anime"

                        val coverObj = media.optJSONObject("coverImage")
                        val imageUrl = coverObj?.optString("extraLarge")?.ifEmpty {
                            coverObj.optString("large", "")
                        } ?: ""

                        val score = media.optInt("averageScore", 0)
                        val rating = if (score > 0) String.format("%.1f", score / 10.0) else "N/A"

                        list.add(
                            AnimeCardItem(
                                id = id.toString(),
                                title = title,
                                rating = rating,
                                imageUrl = imageUrl
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext list
    }

    suspend fun getUserMediaList(username: String): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AnimeCardItem>()
        if (username.isBlank()) return@withContext list
        try {
            val graphqlQuery = """
                query (${'$'}userName: String) {
                  MediaListCollection (userName: ${'$'}userName, type: ANIME) {
                    lists {
                      name
                      status
                      entries {
                        id
                        media {
                          id
                          title {
                            userPreferred
                            english
                            romaji
                          }
                          coverImage {
                            extraLarge
                            large
                          }
                          averageScore
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("userName", username.trim())
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val lists = json.optJSONObject("data")
                    ?.optJSONObject("MediaListCollection")
                    ?.optJSONArray("lists")

                if (lists != null) {
                    val seenIds = mutableSetOf<Int>()
                    for (i in 0 until lists.length()) {
                        val listObj = lists.optJSONObject(i) ?: continue
                        val entries = listObj.optJSONArray("entries") ?: continue
                        for (j in 0 until entries.length()) {
                            val entry = entries.optJSONObject(j) ?: continue
                            val media = entry.optJSONObject("media") ?: continue
                            val id = media.optInt("id", 0)
                            if (id == 0 || seenIds.contains(id)) continue
                            seenIds.add(id)

                            val titleObj = media.optJSONObject("title")
                            val title = titleObj?.optString("userPreferred")?.ifEmpty {
                                titleObj.optString("english")?.ifEmpty { titleObj.optString("romaji", "Anime") }
                            } ?: "Anime"

                            val coverObj = media.optJSONObject("coverImage")
                            val imageUrl = coverObj?.optString("extraLarge")?.ifEmpty {
                                coverObj.optString("large", "")
                            } ?: ""

                            val score = media.optInt("averageScore", 0)
                            val rating = if (score > 0) String.format("%.1f", score / 10.0) else "N/A"

                            list.add(
                                AnimeCardItem(
                                    id = id.toString(),
                                    title = title,
                                    rating = rating,
                                    imageUrl = imageUrl
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext list
    }

    suspend fun getAniListWatchHistory(username: String): List<WatchHistoryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchHistoryItem>()
        if (username.isBlank()) return@withContext list
        try {
            val graphqlQuery = """
                query (${'$'}userName: String) {
                  MediaListCollection (userName: ${'$'}userName, type: ANIME, status_in: [CURRENT, COMPLETED, PAUSED]) {
                    lists {
                      name
                      status
                      entries {
                        id
                        progress
                        updatedAt
                        media {
                          id
                          title {
                            userPreferred
                            english
                            romaji
                          }
                          coverImage {
                            extraLarge
                            large
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("userName", username.trim())
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val lists = json.optJSONObject("data")
                    ?.optJSONObject("MediaListCollection")
                    ?.optJSONArray("lists")

                if (lists != null) {
                    val seenIds = mutableSetOf<Int>()
                    for (i in 0 until lists.length()) {
                        val listObj = lists.optJSONObject(i) ?: continue
                        val entries = listObj.optJSONArray("entries") ?: continue
                        for (j in 0 until entries.length()) {
                            val entry = entries.optJSONObject(j) ?: continue
                            val media = entry.optJSONObject("media") ?: continue
                            val id = media.optInt("id", 0)
                            if (id == 0 || seenIds.contains(id)) continue
                            seenIds.add(id)

                            val progress = entry.optInt("progress", 0)
                            val titleObj = media.optJSONObject("title")
                            val title = titleObj?.optString("userPreferred")?.ifEmpty {
                                titleObj.optString("english")?.ifEmpty { titleObj.optString("romaji", "Anime") }
                            } ?: "Anime"

                            val coverObj = media.optJSONObject("coverImage")
                            val imageUrl = coverObj?.optString("extraLarge")?.ifEmpty {
                                coverObj.optString("large", "")
                            } ?: ""

                            val updatedAt = entry.optLong("updatedAt", System.currentTimeMillis() / 1000) * 1000

                            list.add(
                                WatchHistoryItem(
                                    animeId = id.toString(),
                                    animeTitle = title,
                                    episodeId = "$id",
                                    episodeTitle = if (progress > 0) "Episode $progress" else "Watching",
                                    episodeNumber = if (progress > 0) "$progress" else "1",
                                    imageUrl = imageUrl,
                                    playbackPosition = 0L,
                                    category = "SUB",
                                    timestamp = updatedAt
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext list
    }

    suspend fun resolveAniListMediaId(animeId: String, animeTitle: String): Int? = withContext(Dispatchers.IO) {
        val cleanIdStr = animeId.split("|")[0].split("$")[0].trim()
        val numericId = cleanIdStr.toIntOrNull()
        if (numericId != null && numericId > 0) return@withContext numericId
        
        val cleaned = cleanTitle(animeTitle)
        if (cleaned.isBlank()) return@withContext null

        try {
            val graphqlQuery = """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    id
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("search", cleaned)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val mediaId = json.optJSONObject("data")?.optJSONObject("Media")?.optInt("id", 0)
                if (mediaId != null && mediaId > 0) {
                    return@withContext mediaId
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun saveMediaListEntry(
        accessToken: String,
        animeId: String,
        animeTitle: String,
        status: String = "PLANNING",
        progress: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val mediaId = resolveAniListMediaId(animeId, animeTitle) ?: return@withContext false
        if (accessToken.isBlank()) return@withContext false

        try {
            val graphqlQuery = """
                mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int) {
                  SaveMediaListEntry (mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress) {
                    id
                    status
                    progress
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("mediaId", mediaId)
                    put("status", status)
                    if (progress > 0) {
                        put("progress", progress)
                    }
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                if (json.has("data") && !json.isNull("data")) {
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun toggleFavourite(
        accessToken: String,
        animeId: String,
        animeTitle: String
    ): Boolean = withContext(Dispatchers.IO) {
        val mediaId = resolveAniListMediaId(animeId, animeTitle) ?: return@withContext false
        if (accessToken.isBlank()) return@withContext false

        try {
            val graphqlQuery = """
                mutation (${'$'}animeId: Int) {
                  ToggleFavourite (animeId: ${'$'}animeId) {
                    anime {
                      nodes {
                        id
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphqlQuery)
                put("variables", JSONObject().apply {
                    put("animeId", mediaId)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                if (json.has("data") && !json.isNull("data")) {
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun deleteMediaListEntry(
        accessToken: String,
        animeId: String,
        animeTitle: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext false
        val mediaId = resolveAniListMediaId(animeId, animeTitle) ?: return@withContext false

        try {
            val query = """
                query (${'$'}mediaId: Int) {
                  Media (id: ${'$'}mediaId) {
                    id
                    isFavourite
                    mediaListEntry {
                      id
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply {
                    put("mediaId", mediaId)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            var success = false

            if (responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val mediaObj = json.optJSONObject("data")?.optJSONObject("Media")
                val entryId = mediaObj?.optJSONObject("mediaListEntry")?.optInt("id", 0) ?: 0
                val isFav = mediaObj?.optBoolean("isFavourite", false) ?: false

                if (entryId > 0) {
                    val deleteMutation = """
                        mutation (${'$'}id: Int) {
                          DeleteMediaListEntry (id: ${'$'}id) {
                            deleted
                          }
                        }
                    """.trimIndent()

                    val delBody = JSONObject().apply {
                        put("query", deleteMutation)
                        put("variables", JSONObject().apply {
                            put("id", entryId)
                        })
                    }

                    val delRequest = Request.Builder()
                        .url("https://graphql.anilist.co")
                        .post(delBody.toString().toRequestBody("application/json".toMediaType()))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()

                    val delResponse = okHttpClient.newCall(delRequest).execute()
                    val delString = delResponse.body?.string() ?: ""
                    if (delString.contains("deleted") || delString.contains("data")) {
                        success = true
                    }
                }

                if (isFav) {
                    val toggleMutation = """
                        mutation (${'$'}animeId: Int) {
                          ToggleFavourite (animeId: ${'$'}animeId) {
                            anime {
                              nodes {
                                id
                              }
                            }
                          }
                        }
                    """.trimIndent()

                    val favBody = JSONObject().apply {
                        put("query", toggleMutation)
                        put("variables", JSONObject().apply {
                            put("animeId", mediaId)
                        })
                    }

                    val favRequest = Request.Builder()
                        .url("https://graphql.anilist.co")
                        .post(favBody.toString().toRequestBody("application/json".toMediaType()))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()

                    val favResponse = okHttpClient.newCall(favRequest).execute()
                    val favString = favResponse.body?.string() ?: ""
                    if (favString.contains("data")) {
                        success = true
                    }
                }

                if (entryId == 0 && !isFav) {
                    success = true
                }
            }
            return@withContext success
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}

