package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun ClearCacheScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCalculating by remember { mutableStateOf(true) }
    var isClearing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableLongStateOf(0L) }
    var tempCacheSize by remember { mutableLongStateOf(0L) }
    var externalCacheSize by remember { mutableLongStateOf(0L) }
    var totalCacheSize by remember { mutableLongStateOf(0L) }

    fun calculateCacheSizes() {
        coroutineScope.launch {
            isCalculating = true
            withContext(Dispatchers.IO) {
                val imageCacheDir = context.cacheDir.resolve("image_cache")
                val imgSize = getFolderSize(imageCacheDir)

                val fullCacheSize = getFolderSize(context.cacheDir)
                // Temp files size is total cache minus the image cache
                val tempSize = (fullCacheSize - imgSize).coerceAtLeast(0L)

                val extSize = getFolderSize(context.externalCacheDir)

                imageCacheSize = imgSize
                tempCacheSize = tempSize
                externalCacheSize = extSize
                totalCacheSize = imgSize + tempSize + extSize
            }
            isCalculating = false
        }
    }

    LaunchedEffect(Unit) {
        calculateCacheSizes()
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF161622),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Cache?",
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear the application cache? This will delete all cached images and temporary buffers, freeing up space. Your Watch History and My List remain safe.",
                    fontFamily = PremiumBodyFont,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        isClearing = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                // Delete files
                                deleteFolderContents(context.cacheDir)
                                context.externalCacheDir?.let { deleteFolderContents(it) }
                                delay(1000) // Small delay for premium feel
                            }
                            isClearing = false
                            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                            calculateCacheSizes()
                        }
                    }
                ) {
                    Text(
                        text = "Clear Cache",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontFamily = PremiumTitleFont
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = PremiumTitleFont
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Clear Cache",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero section with Total Size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF161622), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Sweep Icon",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Total Cache Storage",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontFamily = PremiumBodyFont
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isCalculating) "Calculating..." else formatSize(totalCacheSize),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Breakdown section
                Text(
                    text = "Cache Breakdown",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF12121A),
                    border = BorderStroke(1.dp, Color(0xFF222232)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        BreakdownRow(
                            icon = Icons.Filled.Image,
                            title = "Image Cover Cache",
                            sizeStr = if (isCalculating) "..." else formatSize(imageCacheSize)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BreakdownRow(
                            icon = Icons.Filled.FolderOpen,
                            title = "App Buffers & Logs",
                            sizeStr = if (isCalculating) "..." else formatSize(tempCacheSize)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BreakdownRow(
                            icon = Icons.Filled.FolderOpen,
                            title = "External Cache",
                            sizeStr = if (isCalculating) "..." else formatSize(externalCacheSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Clear button
                Button(
                    onClick = {
                        if (!isCalculating && !isClearing && totalCacheSize > 0) {
                            showConfirmDialog = true
                        } else if (totalCacheSize == 0L) {
                            Toast.makeText(context, "Cache is already empty!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (totalCacheSize == 0L) Color(0xFF1E1E2C) else Color.White,
                        contentColor = if (totalCacheSize == 0L) Color.White.copy(alpha = 0.4f) else Color.Black
                    ),
                    enabled = !isCalculating && !isClearing
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Clearing Cache...",
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = if (totalCacheSize == 0L) "Cache Cleaned" else "Clear App Cache",
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(
    icon: ImageVector,
    title: String,
    sizeStr: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontFamily = PremiumBodyFont
            )
        }
        Text(
            text = sizeStr,
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getFolderSize(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    if (!file.isDirectory) return file.length()
    var size = 0L
    val files = file.listFiles()
    if (files != null) {
        for (f in files) {
            size += getFolderSize(f)
        }
    }
    return size
}

private fun deleteFolderContents(file: File?): Boolean {
    if (file == null || !file.exists()) return false
    var deletedAll = true
    if (file.isDirectory) {
        val files = file.listFiles()
        if (files != null) {
            for (f in files) {
                val success = deleteFolderContents(f)
                if (success) {
                    f.delete()
                } else {
                    deletedAll = false
                }
            }
        }
    } else {
        deletedAll = file.delete()
    }
    return deletedAll
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
