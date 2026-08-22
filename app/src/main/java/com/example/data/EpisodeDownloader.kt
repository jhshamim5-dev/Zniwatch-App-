package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit

enum class ActiveDownloadStatus {
    IDLE,
    FETCHING_PLAYLIST,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class ActiveDownloadTask(
    val id: String, // animeTitle_epNumber
    val animeTitle: String,
    val animeImageUrl: String = "",
    val episodeNumber: Int,
    val episodeTitle: String,
    val serverName: String,
    val categoryType: String,
    val progress: Int = 0,
    val downloadedSegments: Int = 0,
    val totalSegments: Int = 0,
    val status: ActiveDownloadStatus = ActiveDownloadStatus.IDLE,
    val errorMessage: String? = null,
    val savedFilePath: String = ""
)

data class DownloadedEpisodeItem(
    val animeTitle: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val fileName: String,
    val serverName: String,
    val categoryType: String,
    val timestamp: Long,
    val filePath: String = "",
    val subFilePath: String = ""
)

object EpisodeDownloader {

    const val FOREGROUND_NOTIF_ID = 9999
    private const val CHANNEL_ID = "zniwatch_downloads_channel"
    private const val NOTIF_BASE_ID = 8800

    private const val PREFS_NAME = "zniwatch_downloader_prefs"
    private const val KEY_ACTIVE_TASKS = "active_tasks_json"
    private const val KEY_LAST_COMPLETED = "last_completed_map_json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val downloaderScope = CoroutineScope(Dispatchers.IO)

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownloadTask>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownloadTask>> = _activeDownloads.asStateFlow()

    private val _lastCompletedEpisodeMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lastCompletedEpisodeMap: StateFlow<Map<String, Int>> = _lastCompletedEpisodeMap.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        loadPersistedState(appContext)
    }

    private fun loadPersistedState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_ACTIVE_TASKS, null)
            if (!jsonStr.isNullOrEmpty()) {
                val jsonArray = JSONArray(jsonStr)
                val map = mutableMapOf<String, ActiveDownloadTask>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val statusStr = obj.optString("status", ActiveDownloadStatus.IDLE.name)
                    val status = try { ActiveDownloadStatus.valueOf(statusStr) } catch(e: Exception) { ActiveDownloadStatus.IDLE }
                    val task = ActiveDownloadTask(
                        id = obj.getString("id"),
                        animeTitle = obj.getString("animeTitle"),
                        animeImageUrl = obj.optString("animeImageUrl", ""),
                        episodeNumber = obj.getInt("episodeNumber"),
                        episodeTitle = obj.getString("episodeTitle"),
                        serverName = obj.getString("serverName"),
                        categoryType = obj.getString("categoryType"),
                        progress = obj.getInt("progress"),
                        downloadedSegments = obj.optInt("downloadedSegments", 0),
                        totalSegments = obj.optInt("totalSegments", 0),
                        status = status,
                        errorMessage = obj.optString("errorMessage", null),
                        savedFilePath = obj.optString("savedFilePath", "")
                    )
                    map[task.id] = task
                }
                _activeDownloads.value = map
            }

            val lastCompStr = prefs.getString(KEY_LAST_COMPLETED, null)
            if (!lastCompStr.isNullOrEmpty()) {
                val jsonObj = JSONObject(lastCompStr)
                val map = mutableMapOf<String, Int>()
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = jsonObj.getInt(key)
                }
                _lastCompletedEpisodeMap.value = map
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveActiveTasks(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            _activeDownloads.value.values.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("animeTitle", task.animeTitle)
                    put("animeImageUrl", task.animeImageUrl)
                    put("episodeNumber", task.episodeNumber)
                    put("episodeTitle", task.episodeTitle)
                    put("serverName", task.serverName)
                    put("categoryType", task.categoryType)
                    put("progress", task.progress)
                    put("downloadedSegments", task.downloadedSegments)
                    put("totalSegments", task.totalSegments)
                    put("status", task.status.name)
                    put("errorMessage", task.errorMessage ?: "")
                    put("savedFilePath", task.savedFilePath)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_ACTIVE_TASKS, jsonArray.toString()).apply()

            val lastCompObj = JSONObject()
            _lastCompletedEpisodeMap.value.forEach { (k, v) ->
                lastCompObj.put(k, v)
            }
            prefs.edit().putString(KEY_LAST_COMPLETED, lastCompObj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun manageServiceState(context: Context) {
        val hasRunning = _activeDownloads.value.values.any { 
            it.status == ActiveDownloadStatus.DOWNLOADING || it.status == ActiveDownloadStatus.FETCHING_PLAYLIST 
        }
        if (hasRunning) {
            try {
                val intent = Intent(context, DownloadService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getTaskKey(animeTitle: String, episodeNumber: Int): String {
        return "${animeTitle.lowercase().trim()}_ep_${episodeNumber}"
    }

    fun startDownload(
        context: Context,
        animeTitle: String,
        episode: EpisodeItem,
        server: AnikotoServer,
        streamResult: EpisodeStreamResult,
        animeImageUrl: String = ""
    ) {
        val taskKey = getTaskKey(animeTitle, episode.episodeNumber)

        val existingTask = _activeDownloads.value[taskKey]
        if (existingTask?.status == ActiveDownloadStatus.DOWNLOADING || existingTask?.status == ActiveDownloadStatus.FETCHING_PLAYLIST) {
            Toast.makeText(context, "Download for Ep ${episode.episodeNumber} is already in progress", Toast.LENGTH_SHORT).show()
            return
        }

        val newTask = ActiveDownloadTask(
            id = taskKey,
            animeTitle = animeTitle,
            animeImageUrl = animeImageUrl,
            episodeNumber = episode.episodeNumber,
            episodeTitle = episode.title,
            serverName = server.name,
            categoryType = server.type,
            status = ActiveDownloadStatus.FETCHING_PLAYLIST
        )

        updateTask(context, newTask)
        createNotificationChannel(context)

        Toast.makeText(context, "Started downloading Ep ${episode.episodeNumber} (${server.type.uppercase()})", Toast.LENGTH_SHORT).show()

        downloaderScope.launch {
            runM3u8DownloadPipeline(context.applicationContext, newTask, streamResult)
        }
    }

    private fun updateTask(context: Context, task: ActiveDownloadTask) {
        val current = _activeDownloads.value.toMutableMap()
        current[task.id] = task
        _activeDownloads.value = current
        saveActiveTasks(context)
        manageServiceState(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zniwatch Episode Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time Zniwatch episode download progress"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        contentText: String,
        progress: Int,
        isFinished: Boolean
    ) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (isFinished) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(contentText)
                .setOngoing(!isFinished)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (isFinished) {
                builder.setProgress(0, 0, false)
            } else {
                builder.setProgress(100, progress, false)
            }

            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAnimeStorageFolder(animeTitle: String): File {
        val cleanTitle = animeTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val zniwatchDir = File(moviesDir, "Zniwatch")
        if (!zniwatchDir.exists()) {
            zniwatchDir.mkdirs()
        }
        val animeFolder = File(zniwatchDir, cleanTitle)
        if (!animeFolder.exists()) {
            animeFolder.mkdirs()
        }
        return animeFolder
    }

    private suspend fun runM3u8DownloadPipeline(
        context: Context,
        initialTask: ActiveDownloadTask,
        streamResult: EpisodeStreamResult
    ) = withContext(Dispatchers.IO) {
        val notifId = NOTIF_BASE_ID + initialTask.episodeNumber.hashCode() % 1000
        val animeTitle = initialTask.animeTitle
        val episodeNum = initialTask.episodeNumber

        try {
            showNotification(
                context, notifId,
                "Downloading: $animeTitle Ep $episodeNum",
                "Fetching best quality stream...",
                0, false
            )

            val rawStreamUrl = streamResult.url
            val headers = streamResult.headers

            if (rawStreamUrl.isEmpty()) {
                failTask(context, initialTask, notifId, "Stream URL is empty")
                return@withContext
            }

            // 1. Fetch playlist content and resolve best quality
            val (mediaPlaylistUrl, mediaPlaylistContent) = resolveBestQualityPlaylist(rawStreamUrl, headers)

            // 2. Extract TS segment URLs from media playlist
            val segmentUrls = extractSegmentUrls(mediaPlaylistUrl, mediaPlaylistContent)

            if (segmentUrls.isEmpty()) {
                // If direct MP4 video rather than m3u8 segments
                if (rawStreamUrl.contains(".mp4") || !mediaPlaylistContent.contains("#EXTM3U")) {
                    downloadDirectVideoFile(context, initialTask, rawStreamUrl, headers, notifId, streamResult)
                    return@withContext
                } else {
                    failTask(context, initialTask, notifId, "No video segments found in stream playlist")
                    return@withContext
                }
            }

            // 3. Prepare target file on phone
            val cleanTitle = animeTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
            val fileName = "${cleanTitle}_Ep${episodeNum}_${initialTask.categoryType.uppercase()}.mp4"

            val animeFolder = getAnimeStorageFolder(animeTitle)
            val outputFile = File(animeFolder, fileName)
            val outputStream = FileOutputStream(outputFile, false)

            var taskState = initialTask.copy(
                totalSegments = segmentUrls.size,
                status = ActiveDownloadStatus.DOWNLOADING,
                savedFilePath = outputFile.absolutePath
            )
            updateTask(context, taskState)

            // 4. Download and append each segment sequentially
            var downloadedCount = 0
            val totalSegs = segmentUrls.size

            for (i in segmentUrls.indices) {
                val segUrl = segmentUrls[i]
                var downloadSuccess = false

                // Retry segment download up to 3 times
                for (attempt in 1..3) {
                    try {
                        val reqBuilder = Request.Builder().url(segUrl)
                        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

                        val response = httpClient.newCall(reqBuilder.build()).execute()
                        if (response.isSuccessful && response.body != null) {
                            val inputStream: InputStream = response.body!!.byteStream()
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            inputStream.close()
                            response.close()
                            downloadSuccess = true
                            break
                        }
                        response.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!downloadSuccess) {
                    outputStream.close()
                    failTask(context, initialTask, notifId, "Failed to download video segment ${i + 1}/$totalSegs")
                    return@withContext
                }

                downloadedCount++
                val progress = ((downloadedCount.toDouble() / totalSegs) * 100).toInt()

                taskState = taskState.copy(
                    downloadedSegments = downloadedCount,
                    progress = progress
                )
                updateTask(context, taskState)

                // Update notification every 3 segments or when completed
                if (downloadedCount % 3 == 0 || downloadedCount == totalSegs) {
                    showNotification(
                        context, notifId,
                        "Downloading: $animeTitle Ep $episodeNum",
                        "$progress% ($downloadedCount/$totalSegs segments)",
                        progress, false
                    )
                }
            }

            outputStream.flush()
            outputStream.close()

            // 5. Download subtitles if present
            val subFile = downloadSubtitlesIfAvailable(context, animeTitle, cleanTitle, episodeNum, initialTask.categoryType, streamResult)

            // 6. Register in MediaScanner so it appears in phone gallery / video app
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 7. Complete task
            taskState = taskState.copy(
                status = ActiveDownloadStatus.COMPLETED,
                progress = 100
            )
            updateTask(context, taskState)

            // Update last completed episode map
            val map = _lastCompletedEpisodeMap.value.toMutableMap()
            map[animeTitle] = episodeNum
            _lastCompletedEpisodeMap.value = map

            // Save record in shared prefs for Downloads screen
            saveDownloadedRecord(
                context = context,
                animeTitle = animeTitle,
                episodeNumber = episodeNum,
                episodeTitle = taskState.episodeTitle,
                fileName = fileName,
                serverName = taskState.serverName,
                type = taskState.categoryType,
                filePath = outputFile.absolutePath,
                subFilePath = subFile?.absolutePath ?: ""
            )

            showNotification(
                context, notifId,
                "Download Completed!",
                "$animeTitle Ep $episodeNum saved to phone storage",
                100, true
            )

        } catch (e: Exception) {
            e.printStackTrace()
            failTask(context, initialTask, notifId, e.localizedMessage ?: "Download failed")
        }
    }

    private fun downloadDirectVideoFile(
        context: Context,
        task: ActiveDownloadTask,
        videoUrl: String,
        headers: Map<String, String>,
        notifId: Int,
        streamResult: EpisodeStreamResult
    ) {
        try {
            val cleanTitle = task.animeTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
            val fileName = "${cleanTitle}_Ep${task.episodeNumber}_${task.categoryType.uppercase()}.mp4"

            val animeFolder = getAnimeStorageFolder(task.animeTitle)
            val outputFile = File(animeFolder, fileName)

            val reqBuilder = Request.Builder().url(videoUrl)
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful || response.body == null) {
                failTask(context, task, notifId, "HTTP Error ${response.code}")
                return
            }

            val contentLength = response.body!!.contentLength()
            val inputStream = response.body!!.byteStream()
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Long = 0
            var read: Int

            var updatedTask = task.copy(status = ActiveDownloadStatus.DOWNLOADING, savedFilePath = outputFile.absolutePath)
            updateTask(context, updatedTask)

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesRead += read
                if (contentLength > 0) {
                    val progress = ((bytesRead.toDouble() / contentLength) * 100).toInt()
                    updatedTask = updatedTask.copy(progress = progress)
                    updateTask(context, updatedTask)
                    showNotification(context, notifId, "Downloading: ${task.animeTitle} Ep ${task.episodeNumber}", "$progress%", progress, false)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            response.close()

            // Download subtitles if present
            val subFile = downloadSubtitlesIfAvailable(context, task.animeTitle, cleanTitle, task.episodeNumber, task.categoryType, streamResult)

            // Scan file for Gallery
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            updatedTask = updatedTask.copy(status = ActiveDownloadStatus.COMPLETED, progress = 100)
            updateTask(context, updatedTask)

            saveDownloadedRecord(
                context = context,
                animeTitle = task.animeTitle,
                episodeNumber = task.episodeNumber,
                episodeTitle = task.episodeTitle,
                fileName = fileName,
                serverName = task.serverName,
                type = task.categoryType,
                filePath = outputFile.absolutePath,
                subFilePath = subFile?.absolutePath ?: ""
            )

            showNotification(context, notifId, "Download Completed!", "${task.animeTitle} Ep ${task.episodeNumber} saved to phone gallery", 100, true)

        } catch (e: Exception) {
            failTask(context, task, notifId, e.localizedMessage ?: "Failed direct download")
        }
    }

    private fun resolveBestQualityPlaylist(initialUrl: String, headers: Map<String, String>): Pair<String, String> {
        val reqBuilder = Request.Builder().url(initialUrl)
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        val response = httpClient.newCall(reqBuilder.build()).execute()
        val bodyString = response.body?.string() ?: ""
        response.close()

        if (!bodyString.contains("#EXT-X-STREAM-INF:")) {
            // Already single quality media playlist
            return Pair(initialUrl, bodyString)
        }

        // Master playlist containing multiple quality variants
        val lines = bodyString.lines()
        var maxBandwidth = -1L
        var bestVariantUrl: String? = null

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                var bandwidth = 0L
                val bwMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                if (bwMatch != null) {
                    bandwidth = bwMatch.groupValues[1].toLongOrNull() ?: 0L
                }

                // Check resolution if present e.g. RESOLUTION=1920x1080
                val resMatch = Regex("RESOLUTION=(\\d+)x(\\d+)").find(line)
                if (resMatch != null) {
                    val w = resMatch.groupValues[1].toLongOrNull() ?: 1L
                    val h = resMatch.groupValues[2].toLongOrNull() ?: 1L
                    bandwidth = maxOf(bandwidth, w * h)
                }

                // Next line is the URL for this variant
                if (i + 1 < lines.size) {
                    val variantPath = lines[i + 1].trim()
                    if (variantPath.isNotEmpty() && !variantPath.startsWith("#")) {
                        if (bandwidth > maxBandwidth || bestVariantUrl == null) {
                            maxBandwidth = bandwidth
                            bestVariantUrl = resolveRelativeUrl(initialUrl, variantPath)
                        }
                    }
                }
            }
            i++
        }

        if (bestVariantUrl != null) {
            val subReq = Request.Builder().url(bestVariantUrl)
            headers.forEach { (k, v) -> subReq.addHeader(k, v) }
            val subRes = httpClient.newCall(subReq.build()).execute()
            val subBody = subRes.body?.string() ?: ""
            subRes.close()
            return Pair(bestVariantUrl, subBody)
        }

        return Pair(initialUrl, bodyString)
    }

    private fun extractSegmentUrls(playlistUrl: String, playlistContent: String): List<String> {
        val segmentUrls = mutableListOf<String>()
        val lines = playlistContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val fullSegUrl = resolveRelativeUrl(playlistUrl, trimmed)
                segmentUrls.add(fullSegUrl)
            }
        }
        return segmentUrls
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeOrAbsolute: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relativeOrAbsolute).toString()
        } catch (e: Exception) {
            if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
                relativeOrAbsolute
            } else {
                val baseDir = baseUrl.substringBeforeLast("/")
                "$baseDir/$relativeOrAbsolute"
            }
        }
    }

    private fun downloadSubtitlesIfAvailable(
        context: Context,
        animeTitle: String,
        cleanTitle: String,
        episodeNum: Int,
        category: String,
        streamResult: EpisodeStreamResult
    ): File? {
        if (streamResult.subtitles.isEmpty()) return null
        try {
            val subTrack = streamResult.subtitles.find { it.isDefault }
                ?: streamResult.subtitles.find { it.label.contains("English", ignoreCase = true) }
                ?: streamResult.subtitles.firstOrNull() ?: return null

            val animeFolder = getAnimeStorageFolder(animeTitle)
            val baseName = "${cleanTitle}_Ep${episodeNum}_${category.uppercase()}"
            val srtFile = File(animeFolder, "$baseName.srt")
            val vttFile = File(animeFolder, "$baseName.vtt")

            val reqBuilder = Request.Builder().url(subTrack.url)
            streamResult.headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful && response.body != null) {
                val rawText = response.body!!.string()
                response.close()

                val isVtt = subTrack.url.contains(".vtt") || rawText.startsWith("WEBVTT")
                val srtText = if (isVtt) convertVttToSrt(rawText) else rawText
                val vttText = if (isVtt) rawText else convertSrtToVtt(rawText)

                srtFile.writeText(srtText)
                vttFile.writeText(vttText)

                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(srtFile.absolutePath, vttFile.absolutePath),
                        arrayOf("text/plain", "text/vtt"),
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return srtFile
            }
            response.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun convertVttToSrt(vtt: String): String {
        val lines = vtt.lines()
        val sb = StringBuilder()
        var index = 1
        var inHeader = true

        for (line in lines) {
            val trimmed = line.trim()
            if (inHeader) {
                if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("Kind:") || trimmed.startsWith("Language:")) {
                    continue
                }
                if (trimmed.isEmpty()) {
                    inHeader = false
                    continue
                }
            }

            if (trimmed.contains("-->")) {
                val formattedTime = trimmed.replace('.', ',')
                sb.append(index++).append("\n")
                sb.append(formattedTime).append("\n")
            } else if (trimmed.isNotEmpty()) {
                sb.append(trimmed).append("\n")
            } else {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun convertSrtToVtt(srt: String): String {
        val sb = StringBuilder("WEBVTT\n\n")
        val lines = srt.lines()
        for (line in lines) {
            if (line.trim().contains("-->")) {
                sb.append(line.trim().replace(',', '.')).append("\n")
            } else {
                sb.append(line).append("\n")
            }
        }
        return sb.toString()
    }

    private fun failTask(context: Context, task: ActiveDownloadTask, notifId: Int, message: String) {
        val failed = task.copy(status = ActiveDownloadStatus.FAILED, errorMessage = message)
        updateTask(context, failed)
        showNotification(context, notifId, "Download Failed", "$message (${task.animeTitle} Ep ${task.episodeNumber})", 0, true)
    }

    private fun saveDownloadedRecord(
        context: Context,
        animeTitle: String,
        episodeNumber: Int,
        episodeTitle: String,
        fileName: String,
        serverName: String,
        type: String,
        filePath: String = "",
        subFilePath: String = ""
    ) {
        try {
            val prefs = context.getSharedPreferences("zniwatch_downloads", Context.MODE_PRIVATE)
            val currentList = prefs.getStringSet("downloaded_items", emptySet())?.toMutableSet() ?: mutableSetOf()
            val timestamp = System.currentTimeMillis()
            val record = "$animeTitle|$episodeNumber|$episodeTitle|$fileName|$serverName|$type|$timestamp|$filePath|$subFilePath"
            currentList.add(record)
            prefs.edit().putStringSet("downloaded_items", currentList).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDownloadedEpisode(context: Context, item: DownloadedEpisodeItem) {
        try {
            if (item.filePath.isNotEmpty()) {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(file.absolutePath),
                            arrayOf("video/mp4"),
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (item.subFilePath.isNotEmpty()) {
                val subFile = File(item.subFilePath)
                if (subFile.exists()) subFile.delete()
            }

            val prefs = context.getSharedPreferences("zniwatch_downloads", Context.MODE_PRIVATE)
            val rawSet = prefs.getStringSet("downloaded_items", emptySet())?.toMutableSet() ?: mutableSetOf()
            rawSet.removeAll { record ->
                val parts = record.split("|")
                parts.size >= 2 && parts[0] == item.animeTitle && parts[1].toIntOrNull() == item.episodeNumber
            }
            prefs.edit().putStringSet("downloaded_items", rawSet).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDownloadedEpisodes(context: Context): List<DownloadedEpisodeItem> {
        val prefs = context.getSharedPreferences("zniwatch_downloads", Context.MODE_PRIVATE)
        var rawSet = prefs.getStringSet("downloaded_items", emptySet()) ?: emptySet()
        if (rawSet.isEmpty()) {
            val oldPrefs = context.getSharedPreferences("anikoto_downloads", Context.MODE_PRIVATE)
            rawSet = oldPrefs.getStringSet("downloaded_items", emptySet()) ?: emptySet()
        }
        val result = mutableListOf<DownloadedEpisodeItem>()
        for (item in rawSet) {
            val parts = item.split("|")
            if (parts.size >= 7) {
                val animeTitle = parts[0]
                val episodeNumber = parts[1].toIntOrNull() ?: 1
                val episodeTitle = parts[2]
                val fileName = parts[3]
                val serverName = parts[4]
                val categoryType = parts[5]
                val timestamp = parts[6].toLongOrNull() ?: 0L
                var filePath = if (parts.size >= 8) parts[7] else ""
                val subFilePath = if (parts.size >= 9) parts[8] else ""

                if (filePath.isEmpty() || !File(filePath).exists()) {
                    val cleanTitle = animeTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
                    val possiblePaths = listOf(
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Zniwatch/$cleanTitle/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Zniwatch/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Zniwatch/$cleanTitle/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Zniwatch/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Anikoto/$cleanTitle/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Anikoto/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Anikoto/$cleanTitle/$fileName"),
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Anikoto/$fileName")
                    )
                    val foundFile = possiblePaths.firstOrNull { it.exists() }
                    if (foundFile != null) {
                        filePath = foundFile.absolutePath
                    }
                }

                result.add(
                    DownloadedEpisodeItem(
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        fileName = fileName,
                        serverName = serverName,
                        categoryType = categoryType,
                        timestamp = timestamp,
                        filePath = filePath,
                        subFilePath = subFilePath
                    )
                )
            }
        }
        return result.sortedByDescending { it.timestamp }
    }
}
