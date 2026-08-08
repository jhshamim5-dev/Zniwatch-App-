package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalMyListManager {
    private const val PREFS_NAME = "zniwatch_my_list_prefs"
    private const val KEY_MY_LIST = "my_list_items_json"
    private const val KEY_ANILIST_ITEMS = "anilist_items_json"

    private fun cleanId(raw: String): String {
        return raw.split("|")[0].split("$")[0].trim()
    }

    private fun cleanTitle(raw: String): String {
        return raw.lowercase()
            .replace(Regex("(?i)\\b(sub|dub|uncensored|raw|tv|movie|ova|ona)\\b"), "")
            .replace(Regex("(?i)\\((sub|dub|uncensored|raw|tv|movie|ova|ona)\\)"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    fun toggleSaved(context: Context, anime: AnimeCardItem): Boolean {
        val currentlySaved = isSaved(context, anime.id, anime.title)
        if (currentlySaved) {
            removeMyListItem(context, anime.id, anime.title)
            return false
        } else {
            val current = getSavedList(context).toMutableList()
            current.add(0, anime)
            saveList(context, current)
            return true
        }
    }

    fun isSaved(context: Context, animeId: String, animeTitle: String): Boolean {
        val all = getAllMyListItems(context)
        val cTitle = cleanTitle(animeTitle)
        val aid = cleanId(animeId)
        return all.any { item ->
            val itemId = cleanId(item.id)
            (aid.isNotEmpty() && itemId.isNotEmpty() && aid == itemId) ||
            item.title.equals(animeTitle, ignoreCase = true) ||
            (cTitle.isNotEmpty() && cleanTitle(item.title) == cTitle)
        }
    }

    fun getAllMyListItems(context: Context): List<AnimeCardItem> {
        val local = getSavedList(context)
        val aniList = getAniListItems(context)
        val combined = local + aniList
        return combined.distinctBy { item ->
            val c = cleanTitle(item.title)
            if (c.isNotEmpty()) c else item.id
        }
    }

    fun getSavedList(context: Context): List<AnimeCardItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_MY_LIST, null) ?: return emptyList()
        return parseJsonList(jsonString)
    }

    fun saveAniListItems(context: Context, list: List<AnimeCardItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("rating", item.rating)
                put("imageUrl", item.imageUrl)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ANILIST_ITEMS, array.toString()).apply()
    }

    fun getAniListItems(context: Context): List<AnimeCardItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_ANILIST_ITEMS, null) ?: return emptyList()
        return parseJsonList(jsonString)
    }

    private fun parseJsonList(jsonString: String): List<AnimeCardItem> {
        val list = mutableListOf<AnimeCardItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AnimeCardItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        rating = obj.optString("rating", "N/A"),
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun removeMyListItem(context: Context, animeId: String, animeTitle: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cTitle = cleanTitle(animeTitle)
        val aid = cleanId(animeId)
        
        val localList = getSavedList(context).toMutableList()
        localList.removeAll { item ->
            val itemId = cleanId(item.id)
            (aid.isNotEmpty() && itemId.isNotEmpty() && aid == itemId) ||
            item.title.equals(animeTitle, ignoreCase = true) ||
            (cTitle.isNotEmpty() && cleanTitle(item.title) == cTitle)
        }
        saveList(context, localList)

        val aniList = getAniListItems(context).toMutableList()
        aniList.removeAll { item ->
            val itemId = cleanId(item.id)
            (aid.isNotEmpty() && itemId.isNotEmpty() && aid == itemId) ||
            item.title.equals(animeTitle, ignoreCase = true) ||
            (cTitle.isNotEmpty() && cleanTitle(item.title) == cTitle)
        }
        saveAniListItems(context, aniList)
    }

    private fun saveList(context: Context, list: List<AnimeCardItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("rating", item.rating)
                put("imageUrl", item.imageUrl)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_MY_LIST, array.toString()).apply()
    }
}
