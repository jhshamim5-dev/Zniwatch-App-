package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnikotoRepository
import com.example.data.AnikotoServer
import com.example.data.EpisodeItem
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadServerSelectionBottomSheet(
    episode: EpisodeItem,
    animeTitle: String,
    onDismiss: () -> Unit,
    onSelectServer: (AnikotoServer) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    val servers = remember { mutableStateListOf<AnikotoServer>() }
    var selectedTypeTab by remember { mutableStateOf("sub") } // "sub", "dub", "hsub"

    LaunchedEffect(episode.id) {
        isLoading = true
        servers.clear()
        try {
            val fetched = AnikotoRepository.fetchEpisodeServers(
                animeTitle = animeTitle,
                episodeId = episode.id,
                episodeNumber = episode.episodeNumber
            )
            servers.addAll(fetched)
            
            val hasSub = fetched.any { it.type.lowercase().contains("sub") && !it.type.lowercase().contains("dub") }
            val hasDub = fetched.any { it.type.lowercase().contains("dub") }
            val hasHsub = fetched.any { it.type.lowercase().contains("hsub") || it.type.lowercase().contains("hard") }

            selectedTypeTab = when {
                hasSub -> "sub"
                hasDub -> "dub"
                hasHsub -> "hsub"
                else -> "sub"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121216),
        scrimColor = Color.Black.copy(alpha = 0.8f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Episode ${episode.episodeNumber}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (episode.title.isNotEmpty()) episode.title else animeTitle,
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        fontFamily = PremiumBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub / Dub / Hard Sub Tabs (Black & White high contrast theme)
            val tabs = listOf(
                "sub" to "SUB",
                "dub" to "DUB",
                "hsub" to "HARD SUB"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C24), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                tabs.forEach { (typeKey, typeLabel) ->
                    val isSelected = selectedTypeTab == typeKey
                    val count = servers.count { s ->
                        when (typeKey) {
                            "sub" -> s.type.lowercase().contains("sub") && !s.type.lowercase().contains("dub") && !s.type.lowercase().contains("hsub")
                            "dub" -> s.type.lowercase().contains("dub")
                            "hsub" -> s.type.lowercase().contains("hsub") || s.type.lowercase().contains("hard")
                            else -> false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedTypeTab = typeKey }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = typeLabel,
                                color = if (isSelected) Color.Black else Color(0xFF9999AA),
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF181820), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Loading available servers...",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.5.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                }
            } else {
                val filteredServers = servers.filter { s ->
                    when (selectedTypeTab) {
                        "sub" -> s.type.lowercase().contains("sub") && !s.type.lowercase().contains("dub") && !s.type.lowercase().contains("hsub")
                        "dub" -> s.type.lowercase().contains("dub")
                        "hsub" -> s.type.lowercase().contains("hsub") || s.type.lowercase().contains("hard")
                        else -> true
                    }
                }

                if (filteredServers.isEmpty()) {
                    // Fallback servers if list empty
                    val fallbackList = when (selectedTypeTab) {
                        "dub" -> listOf(
                            AnikotoServer("HD-1 (Dub)", "fallback_dub1", "dub"),
                            AnikotoServer("Vidstream-2 (Dub)", "fallback_dub2", "dub")
                        )
                        "hsub" -> listOf(
                            AnikotoServer("HD-1 (HardSub)", "fallback_hsub1", "hsub"),
                            AnikotoServer("VidPlay (HardSub)", "fallback_hsub2", "hsub")
                        )
                        else -> listOf(
                            AnikotoServer("HD-1", "fallback_sub1", "sub"),
                            AnikotoServer("Vidstream-2", "fallback_sub2", "sub"),
                            AnikotoServer("VidPlay-1", "fallback_sub3", "sub")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        fallbackList.forEach { server ->
                            ServerItemRow(
                                server = server,
                                onClick = { onSelectServer(server) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(filteredServers) { server ->
                            ServerItemRow(
                                server = server,
                                onClick = { onSelectServer(server) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerItemRow(
    server: AnikotoServer,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A22),
        border = BorderStroke(1.dp, Color(0xFF30303E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = "Server",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Server (${server.type.uppercase()}) • Fast Download",
                    color = Color(0xFF888899),
                    fontSize = 12.sp,
                    fontFamily = PremiumBodyFont
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.clickable { onClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Download",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
