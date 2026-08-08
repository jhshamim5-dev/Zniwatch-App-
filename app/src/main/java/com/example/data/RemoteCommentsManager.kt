package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RemoteCommentsManager {
    private const val MASTER_INDEX_ID = "ff8081819f7e10ae019fdfabacd20fda"
    private const val BASE_URL = "https://api.restful-api.dev/objects"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun cleanKey(title: String): String {
        return title.lowercase()
            .replace(Regex("(?i)\\b(sub|dub|uncensored|raw|tv|movie|ova|ona)\\b"), "")
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifEmpty { "unknown_anime" }
    }

    private suspend fun getObjectIdForAnime(animeKey: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Master Index
            val request = Request.Builder()
                .url("$BASE_URL/$MASTER_INDEX_ID")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val root = JSONObject(bodyString)
            val dataObj = root.optJSONObject("data") ?: JSONObject()
            val animeMap = dataObj.optJSONObject("anime_map") ?: JSONObject()

            if (animeMap.has(animeKey)) {
                return@withContext animeMap.getString(animeKey)
            }

            // If not found in Master Index, create a new object for this anime
            val newObjectJson = JSONObject().apply {
                put("name", "zniwatch_comment_$animeKey")
                put("data", JSONObject().apply {
                    put("comments", JSONArray())
                })
            }

            val createReq = Request.Builder()
                .url(BASE_URL)
                .post(newObjectJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val createResp = client.newCall(createReq).execute()
            if (!createResp.isSuccessful) return@withContext null

            val createBody = createResp.body?.string() ?: return@withContext null
            val createdObj = JSONObject(createBody)
            val newId = createdObj.optString("id", "")

            if (newId.isNotEmpty()) {
                // Update Master Index with new mapping
                animeMap.put(animeKey, newId)
                val updateIndexJson = JSONObject().apply {
                    put("name", "zniwatch_master_index")
                    put("data", JSONObject().apply {
                        put("anime_map", animeMap)
                    })
                }

                val updateReq = Request.Builder()
                    .url("$BASE_URL/$MASTER_INDEX_ID")
                    .put(updateIndexJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(updateReq).execute().close()
            }

            return@withContext if (newId.isNotEmpty()) newId else null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun fetchRemoteComments(context: Context, animeTitle: String): List<CommentItem> = withContext(Dispatchers.IO) {
        val animeKey = cleanKey(animeTitle)
        val objectId = getObjectIdForAnime(animeKey) ?: return@withContext LocalCommentsManager.getCommentsForAnime(context, animeTitle)

        try {
            val req = Request.Builder()
                .url("$BASE_URL/$objectId")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext LocalCommentsManager.getCommentsForAnime(context, animeTitle)

            val body = resp.body?.string() ?: return@withContext LocalCommentsManager.getCommentsForAnime(context, animeTitle)
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: JSONObject()
            val commentsArr = dataObj.optJSONArray("comments") ?: JSONArray()

            val remoteList = mutableListOf<CommentItem>()
            for (i in 0 until commentsArr.length()) {
                val item = commentsArr.getJSONObject(i)
                val commentId = item.optString("id", "")
                val username = item.optString("username", "Anonymous")
                if (commentId.startsWith("def_") || username in listOf("AnimeFan99", "OtakuLife", "SubEnjoyer")) {
                    continue
                }
                remoteList.add(
                    CommentItem(
                        id = commentId,
                        username = username,
                        avatarLetter = item.optString("avatarLetter", "A"),
                        avatarBgColorHex = item.optString("avatarBgColorHex", "#8B5CF6"),
                        timeAgo = item.optString("timeAgo", "Just now"),
                        commentText = item.optString("commentText", ""),
                        likesCount = item.optInt("likesCount", 0),
                        isLiked = false,
                        avatarUrl = item.optString("avatarUrl", "").ifEmpty { null }
                    )
                )
            }

            // Sync to local cache
            LocalCommentsManager.saveCommentsForAnime(context, animeTitle, remoteList)
            return@withContext remoteList
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext LocalCommentsManager.getCommentsForAnime(context, animeTitle)
        }
    }

    suspend fun postRemoteComment(context: Context, animeTitle: String, comment: CommentItem): List<CommentItem> = withContext(Dispatchers.IO) {
        val animeKey = cleanKey(animeTitle)
        val objectId = getObjectIdForAnime(animeKey)

        LocalCommentsManager.addComment(context, animeTitle, comment)
        val currentLocal = LocalCommentsManager.getCommentsForAnime(context, animeTitle)

        if (objectId == null) return@withContext currentLocal

        try {
            val req = Request.Builder().url("$BASE_URL/$objectId").get().build()
            val resp = client.newCall(req).execute()

            val remoteList = mutableListOf<CommentItem>()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                if (body.isNotEmpty()) {
                    val root = JSONObject(body)
                    val dataObj = root.optJSONObject("data") ?: JSONObject()
                    val arr = dataObj.optJSONArray("comments") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val cid = item.optString("id")
                        val uname = item.optString("username")
                        if (cid.startsWith("def_") || uname in listOf("AnimeFan99", "OtakuLife", "SubEnjoyer")) continue
                        remoteList.add(
                            CommentItem(
                                id = cid,
                                username = uname,
                                avatarLetter = item.optString("avatarLetter", "A"),
                                avatarBgColorHex = item.optString("avatarBgColorHex", "#8B5CF6"),
                                timeAgo = item.optString("timeAgo", "Just now"),
                                commentText = item.optString("commentText", ""),
                                likesCount = item.optInt("likesCount", 0),
                                avatarUrl = item.optString("avatarUrl", "").ifEmpty { null }
                            )
                        )
                    }
                }
            }

            if (remoteList.none { it.id == comment.id }) {
                remoteList.add(0, comment)
            }

            val arrJson = JSONArray()
            for (item in remoteList) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("username", item.username)
                    put("avatarLetter", item.avatarLetter)
                    put("avatarBgColorHex", item.avatarBgColorHex)
                    put("timeAgo", item.timeAgo)
                    put("commentText", item.commentText)
                    put("likesCount", item.likesCount)
                    put("avatarUrl", item.avatarUrl ?: "")
                }
                arrJson.put(obj)
            }

            val updatePayload = JSONObject().apply {
                put("name", "zniwatch_comment_$animeKey")
                put("data", JSONObject().apply {
                    put("comments", arrJson)
                })
            }

            val putReq = Request.Builder()
                .url("$BASE_URL/$objectId")
                .put(updatePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(putReq).execute().close()

            LocalCommentsManager.saveCommentsForAnime(context, animeTitle, remoteList)
            return@withContext remoteList
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext currentLocal
        }
    }

    suspend fun updateLikeRemote(context: Context, animeTitle: String, commentId: String, isLiked: Boolean, likesCount: Int) = withContext(Dispatchers.IO) {
        val animeKey = cleanKey(animeTitle)
        LocalCommentsManager.updateLike(context, animeTitle, commentId, isLiked, likesCount)
        val objectId = getObjectIdForAnime(animeKey) ?: return@withContext

        try {
            val req = Request.Builder().url("$BASE_URL/$objectId").get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext

            val body = resp.body?.string() ?: return@withContext
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: JSONObject()
            val arr = dataObj.optJSONArray("comments") ?: JSONArray()

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.optString("id") == commentId) {
                    item.put("likesCount", likesCount)
                    break
                }
            }

            val updatePayload = JSONObject().apply {
                put("name", "zniwatch_comment_$animeKey")
                put("data", JSONObject().apply {
                    put("comments", arr)
                })
            }

            val putReq = Request.Builder()
                .url("$BASE_URL/$objectId")
                .put(updatePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(putReq).execute().close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
