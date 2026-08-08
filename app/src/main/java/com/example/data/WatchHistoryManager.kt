package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryItem(
    val animeId: String,
    val animeTitle: String,
    val episodeId: String,
    val episodeTitle: String,
    val episodeNumber: String,
    val imageUrl: String,
    val playbackPosition: Long = 0L,
    val category: String = "SUB",
    val timestamp: Long = System.currentTimeMillis()
)

object WatchHistoryManager {
    private const val PREFS_NAME = "zniwatch_history_prefs"
    private const val KEY_HISTORY = "watch_history_json"

    fun saveWatchHistory(
        context: Context,
        animeId: String,
        animeTitle: String,
        episodeId: String,
        episodeTitle: String,
        episodeNumber: String,
        imageUrl: String,
        playbackPosition: Long = 0L,
        category: String = "SUB"
    ) {
        if (animeId.isBlank() || animeTitle.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentList = getWatchHistory(context).toMutableList()

        // Remove if existing entry for same anime to update to latest episode
        currentList.removeAll { it.animeId == animeId || it.animeTitle.equals(animeTitle, ignoreCase = true) }

        // Add latest at the beginning
        currentList.add(
            0,
            WatchHistoryItem(
                animeId = animeId,
                animeTitle = animeTitle,
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                episodeNumber = episodeNumber,
                imageUrl = imageUrl,
                playbackPosition = playbackPosition,
                category = category,
                timestamp = System.currentTimeMillis()
            )
        )

        val trimmed = currentList.take(30)
        val jsonArray = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("animeId", item.animeId)
                put("animeTitle", item.animeTitle)
                put("episodeId", item.episodeId)
                put("episodeTitle", item.episodeTitle)
                put("episodeNumber", item.episodeNumber)
                put("imageUrl", item.imageUrl)
                put("playbackPosition", item.playbackPosition)
                put("category", item.category)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun saveAniListWatchHistory(context: Context, aniListItems: List<WatchHistoryItem>) {
        if (aniListItems.isEmpty()) return
        val currentList = getWatchHistory(context).toMutableList()
        var updated = false
        for (item in aniListItems) {
            val exists = currentList.any { it.animeId == item.animeId || it.animeTitle.equals(item.animeTitle, ignoreCase = true) }
            if (!exists) {
                currentList.add(item)
                updated = true
            } else {
                // If existing item has no episode number or 0, update it with AniList episode info
                val existingIndex = currentList.indexOfFirst { it.animeId == item.animeId || it.animeTitle.equals(item.animeTitle, ignoreCase = true) }
                if (existingIndex != -1) {
                    val existing = currentList[existingIndex]
                    if (existing.episodeNumber.isEmpty() || existing.episodeNumber == "0" || existing.episodeNumber == "1") {
                        if (item.episodeNumber.isNotEmpty() && item.episodeNumber != "0") {
                            currentList[existingIndex] = existing.copy(
                                episodeNumber = item.episodeNumber,
                                episodeTitle = item.episodeTitle,
                                timestamp = item.timestamp
                            )
                            updated = true
                        }
                    }
                }
            }
        }
        if (updated) {
            val sorted = currentList.sortedByDescending { it.timestamp }.take(40)
            val jsonArray = JSONArray()
            for (item in sorted) {
                val obj = JSONObject().apply {
                    put("animeId", item.animeId)
                    put("animeTitle", item.animeTitle)
                    put("episodeId", item.episodeId)
                    put("episodeTitle", item.episodeTitle)
                    put("episodeNumber", item.episodeNumber)
                    put("imageUrl", item.imageUrl)
                    put("playbackPosition", item.playbackPosition)
                    put("category", item.category)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        }
    }

    fun getWatchHistory(context: Context): List<WatchHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<WatchHistoryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WatchHistoryItem(
                        animeId = obj.optString("animeId", ""),
                        animeTitle = obj.optString("animeTitle", ""),
                        episodeId = obj.optString("episodeId", ""),
                        episodeTitle = obj.optString("episodeTitle", ""),
                        episodeNumber = obj.optString("episodeNumber", ""),
                        imageUrl = obj.optString("imageUrl", ""),
                        playbackPosition = obj.optLong("playbackPosition", 0L),
                        category = obj.optString("category", "SUB"),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteWatchHistoryItem(context: Context, animeId: String, animeTitle: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentList = getWatchHistory(context).toMutableList()
        currentList.removeAll {
            (animeId.isNotEmpty() && it.animeId == animeId) ||
            it.animeTitle.equals(animeTitle, ignoreCase = true)
        }
        val jsonArray = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("animeId", item.animeId)
                put("animeTitle", item.animeTitle)
                put("episodeId", item.episodeId)
                put("episodeTitle", item.episodeTitle)
                put("episodeNumber", item.episodeNumber)
                put("imageUrl", item.imageUrl)
                put("playbackPosition", item.playbackPosition)
                put("category", item.category)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
