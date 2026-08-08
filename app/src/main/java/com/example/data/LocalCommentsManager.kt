package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CommentItem(
    val id: String,
    val username: String,
    val avatarLetter: String,
    val avatarBgColorHex: String,
    val timeAgo: String,
    val commentText: String,
    var likesCount: Int,
    var isLiked: Boolean = false,
    val avatarUrl: String? = null
)

object LocalCommentsManager {
    private const val PREFS_NAME = "zniwatch_comments_prefs"

    fun getCommentsForAnime(context: Context, animeTitle: String): List<CommentItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "comments_${animeTitle.lowercase().trim()}"
        val jsonString = prefs.getString(key, null)

        val list = mutableListOf<CommentItem>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CommentItem(
                            id = obj.optString("id", ""),
                            username = obj.optString("username", "Anonymous"),
                            avatarLetter = obj.optString("avatarLetter", "A"),
                            avatarBgColorHex = obj.optString("avatarBgColorHex", "#8B5CF6"),
                            timeAgo = obj.optString("timeAgo", "Just now"),
                            commentText = obj.optString("commentText", ""),
                            likesCount = obj.optInt("likesCount", 0),
                            isLiked = obj.optBoolean("isLiked", false),
                            avatarUrl = obj.optString("avatarUrl", "").ifEmpty { null }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Filter out any fake default comments
        list.removeAll { item ->
            item.id.startsWith("def_") ||
            item.username in listOf("AnimeFan99", "OtakuLife", "SubEnjoyer")
        }

        return list
    }

    fun saveCommentsForAnime(context: Context, animeTitle: String, comments: List<CommentItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "comments_${animeTitle.lowercase().trim()}"
        val array = JSONArray()
        try {
            for (comment in comments) {
                val obj = JSONObject().apply {
                    put("id", comment.id)
                    put("username", comment.username)
                    put("avatarLetter", comment.avatarLetter)
                    put("avatarBgColorHex", comment.avatarBgColorHex)
                    put("timeAgo", comment.timeAgo)
                    put("commentText", comment.commentText)
                    put("likesCount", comment.likesCount)
                    put("isLiked", comment.isLiked)
                    put("avatarUrl", comment.avatarUrl ?: "")
                }
                array.put(obj)
            }
            prefs.edit().putString(key, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addComment(context: Context, animeTitle: String, comment: CommentItem) {
        val current = getCommentsForAnime(context, animeTitle).toMutableList()
        current.add(0, comment)
        saveCommentsForAnime(context, animeTitle, current)
    }

    fun updateLike(context: Context, animeTitle: String, commentId: String, isLiked: Boolean, newLikes: Int) {
        val current = getCommentsForAnime(context, animeTitle).toMutableList()
        val index = current.indexOfFirst { it.id == commentId }
        if (index >= 0) {
            val comment = current[index]
            current[index] = comment.copy(isLiked = isLiked, likesCount = newLikes)
            saveCommentsForAnime(context, animeTitle, current)
        }
    }
}
