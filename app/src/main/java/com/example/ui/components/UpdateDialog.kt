package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppReleaseInfo
import com.example.data.PremiumBodyFont
import com.example.data.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    releaseInfo: AppReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var downloadedFile by remember {
        mutableStateOf<File?>(UpdateManager.getCachedUpdateFile(context, releaseInfo.tagName))
    }
    var hasInstallPermission by remember {
        mutableStateOf(UpdateManager.canInstallUnknownApps(context))
    }

    // Automatically refresh permission & cached file status on resume from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasInstallPermission = UpdateManager.canInstallUnknownApps(context)
                val cached = UpdateManager.getCachedUpdateFile(context, releaseInfo.tagName)
                if (cached != null) {
                    downloadedFile = cached
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161622),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A3D)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("update_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF252538), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Update Available",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PremiumBodyFont
                        )
                    }

                    if (!isDownloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("update_dialog_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Release Info Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F1F2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = releaseInfo.releaseName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PremiumBodyFont
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = releaseInfo.releaseNotes,
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress or Action States
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color.White,
                            trackColor = Color(0xFF333348)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = downloadStatusText.ifEmpty { "Downloading... ${(downloadProgress * 100).toInt()}%" },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                } else {
                    val isCachedReady = downloadedFile != null && downloadedFile!!.exists() && downloadedFile!!.length() > 0

                    if (!hasInstallPermission) {
                        // Helpful banner explaining the install permission upfront before download
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E2214),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A3D1E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCachedReady)
                                        "Update downloaded! Tap below to allow Install Unknown Apps permission and complete installation."
                                    else
                                        "Android requires 'Install Unknown Apps' permission. Enable it first so updates install seamlessly without re-downloading.",
                                    color = Color(0xFFFFE0B2),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = PremiumBodyFont
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    } else if (isCachedReady) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF142B1A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E5A2E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF66BB6A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Update downloaded and ready to install.",
                                    color = Color(0xFFC8E6C9),
                                    fontSize = 12.sp,
                                    fontFamily = PremiumBodyFont
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            if (!hasInstallPermission) {
                                // Request permission first so user doesn't download and get stuck
                                UpdateManager.openUnknownSourcesSettings(context)
                            } else if (isCachedReady) {
                                // Already downloaded: install immediately without downloading again!
                                UpdateManager.installApk(context, downloadedFile!!)
                            } else {
                                // Permission is ready, start download
                                isDownloading = true
                                downloadStatusText = "Connecting..."
                                coroutineScope.launch {
                                    val file = UpdateManager.downloadApk(
                                        context = context,
                                        downloadUrl = releaseInfo.apkDownloadUrl,
                                        tagName = releaseInfo.tagName,
                                        onProgress = { progress ->
                                            downloadProgress = progress / 100f
                                            downloadStatusText = "Downloading... $progress%"
                                        }
                                    )
                                    isDownloading = false
                                    if (file != null) {
                                        downloadedFile = file
                                        if (UpdateManager.canInstallUnknownApps(context)) {
                                            UpdateManager.installApk(context, file)
                                        } else {
                                            downloadStatusText = "Grant permission to install"
                                            UpdateManager.openUnknownSourcesSettings(context)
                                        }
                                    } else {
                                        downloadStatusText = "Download failed. Please try again or open in browser."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_dialog_white_update_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (!hasInstallPermission) Icons.Filled.Security else if (isCachedReady) Icons.Filled.SystemUpdate else Icons.Filled.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !hasInstallPermission -> "Grant Install Permission"
                                    isCachedReady -> "Install Now"
                                    else -> "Download & Update"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = PremiumBodyFont,
                                color = Color.Black
                            )
                        }
                    }

                    // Secondary options if permission is not granted yet
                    if (!hasInstallPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Direct download anyway without pre-permission
                            OutlinedButton(
                                onClick = {
                                    isDownloading = true
                                    downloadStatusText = "Connecting..."
                                    coroutineScope.launch {
                                        val file = UpdateManager.downloadApk(
                                            context = context,
                                            downloadUrl = releaseInfo.apkDownloadUrl,
                                            tagName = releaseInfo.tagName,
                                            onProgress = { progress ->
                                                downloadProgress = progress / 100f
                                                downloadStatusText = "Downloading... $progress%"
                                            }
                                        )
                                        isDownloading = false
                                        if (file != null) {
                                            downloadedFile = file
                                            downloadStatusText = "Download complete"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4E))
                            ) {
                                Text(
                                    text = if (isCachedReady) "Downloaded" else "Download Anyway",
                                    fontSize = 12.sp,
                                    color = Color(0xFFDDDDDD),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = PremiumBodyFont
                                )
                            }

                            // Open in Browser fallback
                            OutlinedButton(
                                onClick = {
                                    UpdateManager.openInBrowser(context, releaseInfo.apkDownloadUrl)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4E))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = null,
                                    tint = Color(0xFFDDDDDD),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Browser",
                                    fontSize = 12.sp,
                                    color = Color(0xFFDDDDDD),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = PremiumBodyFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
