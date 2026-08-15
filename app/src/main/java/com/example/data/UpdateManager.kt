package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isNewerVersion: Boolean
)

object UpdateManager {

    // Default GitHub repository owner & repo name
    private const val GITHUB_OWNER = "jhshamim5-dev"
    private const val GITHUB_REPO = "Zniwatch-App-"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_DISMISSED_VERSION = "last_dismissed_version"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Zniwatch-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (apkUrl.isEmpty()) {
                // Fallback to release zip or direct release asset URL construction if assets list is empty
                apkUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tagName/app-debug.apk"
            }

            val currentVersionName = BuildConfig.VERSION_NAME.ifEmpty { "1.0" }
            val isNewer = isVersionNewer(currentVersionName, tagName)

            AppReleaseInfo(
                tagName = tagName,
                releaseName = if (releaseName.isBlank()) "Version $tagName" else releaseName,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                isNewerVersion = isNewer
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isHomePopupDismissed(context: Context, tagName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dismissed = prefs.getString(KEY_DISMISSED_VERSION, null)
        return dismissed == tagName
    }

    fun dismissHomePopup(context: Context, tagName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION, tagName).apply()
    }

    fun getCachedUpdateFile(context: Context, tagName: String? = null): File? {
        val updateDir = File(context.getExternalFilesDir(null), "updates")
        if (!updateDir.exists()) return null
        val cleanTag = tagName?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: ""
        val targetedFile = if (cleanTag.isNotEmpty()) File(updateDir, "update_$cleanTag.apk") else null
        val defaultFile = File(updateDir, "update.apk")

        if (targetedFile != null && targetedFile.exists() && targetedFile.length() > 1024 * 50) {
            return targetedFile
        }
        if (defaultFile.exists() && defaultFile.length() > 1024 * 50) {
            return defaultFile
        }
        return null
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        tagName: String? = null,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.getExternalFilesDir(null), "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }

            val cleanTag = tagName?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: ""
            val apkFile = if (cleanTag.isNotEmpty()) File(updateDir, "update_$cleanTag.apk") else File(updateDir, "update.apk")

            // If already downloaded and non-empty, avoid re-downloading
            if (apkFile.exists() && apkFile.length() > 1024 * 50) {
                withContext(Dispatchers.Main) {
                    onProgress(100)
                }
                return@withContext apkFile
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Zniwatch-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()

            val tempFile = File(updateDir, "${apkFile.name}.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress(progress.coerceIn(0, 100))
                        }
                    }
                }
                outputStream.flush()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            if (tempFile.exists() && tempFile.length() > 0L) {
                if (apkFile.exists()) {
                    apkFile.delete()
                }
                tempFile.renameTo(apkFile)
                return@withContext apkFile
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Explicitly grant read permission to all handling package installers
            val resInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isVersionNewer(currentVer: String, releaseTag: String): Boolean {
        val cleanCurrent = currentVer.lowercase().replace("v", "").trim()
        val cleanRelease = releaseTag.lowercase().replace("v", "").trim()

        if (cleanRelease.isBlank()) return false
        if (cleanCurrent == cleanRelease) return false

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val releaseParts = cleanRelease.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, releaseParts.size)
        for (i in 0 until length) {
            val curr = currentParts.getOrElse(i) { 0 }
            val rel = releaseParts.getOrElse(i) { 0 }
            if (rel > curr) return true
            if (rel < curr) return false
        }

        return false
    }
}
