package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AnimeCardImage
import com.example.data.ActiveDownloadStatus
import com.example.data.DownloadedEpisodeItem
import com.example.data.EpisodeDownloader
import com.example.data.EpisodeItem
import com.example.data.EpisodeStreamResult
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.data.SubtitleTrack
import java.io.File

@Composable
fun DownloadScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeDownloadsMap by EpisodeDownloader.activeDownloads.collectAsStateWithLifecycle()
    val activeDownloadsList = remember(activeDownloadsMap) { activeDownloadsMap.values.toList() }
    
    // Refresh list on active downloads changes or local state updates
    var refreshTrigger by remember { mutableStateOf(0) }
    val downloadedItems = remember(activeDownloadsMap, refreshTrigger) { EpisodeDownloader.getDownloadedEpisodes(context) }

    var selectedFolderAnimeTitle by remember { mutableStateOf<String?>(null) }
    var playingOfflineEpisode by remember { mutableStateOf<DownloadedEpisodeItem?>(null) }

    // Group completed episodes by anime title
    val groupedAnimeFolders = remember(downloadedItems) {
        downloadedItems.groupBy { it.animeTitle }
    }

    if (playingOfflineEpisode != null) {
        val playingEp = playingOfflineEpisode!!
        val folderEpisodes = groupedAnimeFolders[playingEp.animeTitle] ?: listOf(playingEp)
        
        val playerEpisodes = folderEpisodes.map { ep ->
            EpisodeItem(
                id = "${ep.animeTitle}_${ep.episodeNumber}",
                episodeNumber = ep.episodeNumber,
                title = ep.episodeTitle.ifEmpty { "Episode ${ep.episodeNumber}" },
                thumbnail = ""
            )
        }
        val currentEpItem = EpisodeItem(
            id = "${playingEp.animeTitle}_${playingEp.episodeNumber}",
            episodeNumber = playingEp.episodeNumber,
            title = playingEp.episodeTitle.ifEmpty { "Episode ${playingEp.episodeNumber}" },
            thumbnail = ""
        )

        VideoPlayerScreen(
            animeTitle = playingEp.animeTitle,
            initialEpisode = currentEpItem,
            initialStreamResult = EpisodeStreamResult(
                url = playingEp.filePath,
                isM3u8 = false,
                subtitles = run {
                    if (playingEp.subFilePath.isNotEmpty() && File(playingEp.subFilePath).exists()) {
                        listOf(SubtitleTrack(label = "English Subtitle", url = playingEp.subFilePath, isDefault = true))
                    } else if (playingEp.filePath.isNotEmpty()) {
                        val videoFile = File(playingEp.filePath)
                        val parentDir = videoFile.parentFile
                        if (parentDir != null && parentDir.exists()) {
                            val baseName = videoFile.nameWithoutExtension
                            val srtFile = File(parentDir, "$baseName.srt")
                            val vttFile = File(parentDir, "$baseName.vtt")
                            if (srtFile.exists()) {
                                listOf(SubtitleTrack(label = "English Subtitle", url = srtFile.absolutePath, isDefault = true))
                            } else if (vttFile.exists()) {
                                listOf(SubtitleTrack(label = "English Subtitle", url = vttFile.absolutePath, isDefault = true))
                            } else emptyList()
                        } else emptyList()
                    } else emptyList()
                }
            ),
            initialCategory = playingEp.categoryType,
            episodes = playerEpisodes,
            onClose = { playingOfflineEpisode = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (selectedFolderAnimeTitle != null) {
                            selectedFolderAnimeTitle = null
                        } else {
                            onBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFolderAnimeTitle ?: "Downloads",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedFolderAnimeTitle != null) "Downloaded Episodes • Saved on Phone" else "Saved in Phone Movies/Zniwatch/",
                        color = Color(0xFF888899),
                        fontSize = 11.5.sp,
                        fontFamily = PremiumBodyFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area
            if (selectedFolderAnimeTitle == null) {
                // FOLDERS VIEW (ROOT PAGE)
                if (groupedAnimeFolders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF141418), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = "Downloads",
                                tint = Color(0xFF666677),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Downloaded Anime Folders",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Episodes you download will automatically be saved on your phone and organized here into anime folders.",
                                color = Color(0xFF888899),
                                fontSize = 12.5.sp,
                                fontFamily = PremiumBodyFont
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Anime Folders Section
                        if (groupedAnimeFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ANIME DOWNLOAD FOLDERS (${groupedAnimeFolders.size})",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            items(groupedAnimeFolders.keys.toList()) { animeTitle ->
                                val episodesList = groupedAnimeFolders[animeTitle] ?: emptyList()
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF141418),
                                    border = BorderStroke(1.dp, Color(0xFF2B2B38)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFolderAnimeTitle = animeTitle }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = "Folder",
                                                tint = Color.Black,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = animeTitle,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "${episodesList.size} Downloaded Episode${if (episodesList.size > 1) "s" else ""} • Offline Ready",
                                                color = Color(0xFF9999AA),
                                                fontSize = 12.sp,
                                                fontFamily = PremiumBodyFont
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "Open",
                                            tint = Color(0xFF888899),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ANIME FOLDER DETAIL VIEW (EPISODES LIST)
                val folderEpisodes = groupedAnimeFolders[selectedFolderAnimeTitle] ?: emptyList()

                if (folderEpisodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF141418), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No episodes found in this folder.",
                            color = Color(0xFF888899),
                            fontSize = 14.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folderEpisodes) { epItem ->
                            val fileExists = epItem.filePath.isNotEmpty() && File(epItem.filePath).exists()
                            val fileSizeText = remember(epItem.filePath) {
                                if (fileExists) {
                                    val bytes = File(epItem.filePath).length()
                                    val mb = bytes / (1024L * 1024L)
                                    if (mb > 0L) "$mb MB" else "${bytes / 1024L} KB"
                                } else "File on device"
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF141418),
                                border = BorderStroke(1.dp, Color(0xFF282835)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFF22222C), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "EP ${epItem.episodeNumber}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = PremiumTitleFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = epItem.episodeTitle.ifEmpty { "Episode ${epItem.episodeNumber}" },
                                            color = Color.White,
                                            fontSize = 14.5.sp,
                                            fontFamily = PremiumTitleFont,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${epItem.categoryType.uppercase()} • ${epItem.serverName.ifEmpty { "Default Server" }} • $fileSizeText",
                                            color = Color(0xFF888899),
                                            fontSize = 12.sp,
                                            fontFamily = PremiumBodyFont
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { playingOfflineEpisode = epItem },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PLAY",
                                                fontSize = 12.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = {
                                                EpisodeDownloader.deleteDownloadedEpisode(context, epItem)
                                                refreshTrigger++
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
