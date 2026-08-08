package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.AniListRepository
import com.example.data.AnimeCardItem
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.data.WatchHistoryItem
import com.example.data.WatchHistoryManager
import kotlinx.coroutines.launch

data class EditableListItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val isHistoryItem: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onAnimeClick: (AnimeCardItem) -> Unit = {},
    onPlayEpisodeClick: (AnimeCardItem) -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE) }

    var isConnected by remember { mutableStateOf(prefs.getBoolean("anilist_connected", false)) }
    var username by remember { mutableStateOf(prefs.getString("anilist_username", "") ?: "") }
    var avatarUrl by remember { mutableStateOf(prefs.getString("anilist_avatar_url", "") ?: "") }

    var isLoadingFavorites by remember { mutableStateOf(false) }
    var favoritesList by remember { mutableStateOf(com.example.data.LocalMyListManager.getAllMyListItems(context)) }
    var watchHistoryList by remember { mutableStateOf<List<WatchHistoryItem>>(emptyList()) }

    // State for Editor Popup Sheet
    var editingItem by remember { mutableStateOf<EditableListItem?>(null) }
    var selectedStatus by remember { mutableStateOf("CURRENT") }
    var isDeletingOrSaving by remember { mutableStateOf(false) }

    // Re-check connection status and read real local watch history
    LaunchedEffect(Unit) {
        isConnected = prefs.getBoolean("anilist_connected", false)
        username = prefs.getString("anilist_username", "") ?: ""
        avatarUrl = prefs.getString("anilist_avatar_url", "") ?: ""

        val localHistory = WatchHistoryManager.getWatchHistory(context)
        favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)

        // Fetch connected user's AniList favorites & watchlist and AniList watch history
        if (isConnected && username.isNotBlank()) {
            isLoadingFavorites = true
            try {
                val favs = AniListRepository.getUserFavorites(username)
                val mediaList = AniListRepository.getUserMediaList(username)
                val combinedAniList = (favs + mediaList).distinctBy { if (it.id.isNotEmpty()) it.id else it.title.lowercase() }
                com.example.data.LocalMyListManager.saveAniListItems(context, combinedAniList)
                favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)

                val aniHistory = AniListRepository.getAniListWatchHistory(username)
                WatchHistoryManager.saveAniListWatchHistory(context, aniHistory)
                watchHistoryList = WatchHistoryManager.getWatchHistory(context)
            } catch (e: Exception) {
                e.printStackTrace()
                watchHistoryList = localHistory
                favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)
            } finally {
                isLoadingFavorites = false
            }
        } else {
            watchHistoryList = localHistory
            favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Connected Status Badge (Top Right area if connected)
            if (isConnected && username.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF161622))
                                .border(1.dp, Color(0xFF2B2B3D), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = username,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = username,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = PremiumBodyFont,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Section 1: Watch History (Horizontal Row Style - REAL DATA ONLY)
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Watch History",
                        color = Color.White,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (watchHistoryList.isEmpty()) {
                        // Empty real watch history view
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF12121A),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "There isn't any watch history. Please browse to watch.",
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 13.sp,
                                    fontFamily = PremiumBodyFont,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // White and Black Theme Browse Button
                                Button(
                                    onClick = onBrowseClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Browse",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Browse Anime",
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchHistoryList) { historyItem ->
                                RealWatchHistoryCard(
                                    item = historyItem,
                                    onClick = {
                                        val targetEpId = if (historyItem.episodeNumber.isNotEmpty() && historyItem.episodeNumber != "0") {
                                            historyItem.episodeNumber
                                        } else {
                                            historyItem.episodeId
                                        }
                                        onPlayEpisodeClick(
                                            AnimeCardItem(
                                                id = historyItem.animeId.split("|")[0].split("$")[0],
                                                title = historyItem.animeTitle,
                                                imageUrl = historyItem.imageUrl,
                                                startEpisodeId = targetEpId,
                                                startPosition = historyItem.playbackPosition,
                                                startCategory = historyItem.category
                                            )
                                        )
                                    },
                                    onLongClick = {
                                        selectedStatus = "CURRENT"
                                        editingItem = EditableListItem(
                                            id = historyItem.animeId.split("|")[0].split("$")[0],
                                            title = historyItem.animeTitle,
                                            imageUrl = historyItem.imageUrl,
                                            isHistoryItem = true
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Favorites & Watchlist Section
            item {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = "Favorites & Watchlist",
                        color = Color.White,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (!isConnected) {
                        // When NO AniList is connected: Show middle card with White and Black button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF12121A),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Sync your AniList Favorites",
                                    color = Color.White,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Connect your account to display your AniList watchlist and saved favorites.",
                                    color = Color(0xFFAAAAEE),
                                    fontSize = 12.sp,
                                    fontFamily = PremiumBodyFont,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Connect AniList Button (White & Black theme)
                                Button(
                                    onClick = {
                                        try {
                                            val clientId = BuildConfig.ANILIST_CLIENT_ID.ifEmpty { "47902" }
                                            val authUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=$clientId&redirect_uri=jhshamim.zniwatch://anilist-auth&response_type=code"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(46.dp)
                                ) {
                                    AniListLogoBadge(
                                        modifier = Modifier.size(20.dp),
                                        backgroundColor = Color(0xFF02A9FF),
                                        iconColor = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Connect AniList",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // WHEN ANILIST IS CONNECTED: Display 3 cards in a row grid design
                        if (isLoadingFavorites) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else if (favoritesList.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF12121A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Your AniList favorites and watchlist will appear here once saved on AniList.co",
                                    color = Color(0xFF888899),
                                    fontSize = 13.sp,
                                    fontFamily = PremiumBodyFont,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        } else {
                            // Render 3 cards per row
                            val chunkedItems = favoritesList.chunked(3)
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                chunkedItems.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (anime in rowItems) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                GridAnimeCard(
                                                    anime = anime,
                                                    onClick = { onAnimeClick(anime) },
                                                    onLongClick = {
                                                        selectedStatus = "CURRENT"
                                                        editingItem = EditableListItem(
                                                            id = anime.id,
                                                            title = anime.title,
                                                            imageUrl = anime.imageUrl,
                                                            isHistoryItem = false
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        // Fill remaining slots if row has less than 3
                                        for (i in 0 until (3 - rowItems.size)) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Editor Bottom Sheet for Edit Status / Delete Item
        if (editingItem != null) {
            val item = editingItem!!
            ModalBottomSheet(
                onDismissRequest = { if (!isDeletingOrSaving) editingItem = null },
                containerColor = Color(0xFF12121A),
                scrimColor = Color.Black.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Header Row with Anime Poster & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 54.dp, height = 76.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF222232), RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isHistoryItem) Color(0xFF2C1E3C) else Color(0xFF12283A)
                            ) {
                                Text(
                                    text = if (item.isHistoryItem) "Watch History Item" else "Saved List Item",
                                    color = if (item.isHistoryItem) Color(0xFFD8B4FE) else Color(0xFF7DD3FC),
                                    fontSize = 10.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "UPDATE STATUS ON ANILIST",
                        color = Color(0xFF888899),
                        fontSize = 11.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Selector Options
                    val statusOptions = listOf(
                        "CURRENT" to "Watching",
                        "PLANNING" to "Planning",
                        "COMPLETED" to "Completed",
                        "DROPPED" to "Dropped",
                        "PAUSED" to "Paused"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        statusOptions.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for ((key, label) in rowOptions) {
                                    val isSelected = selectedStatus == key
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color.White else Color(0xFF1B1B26),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color.White else Color(0xFF282838)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedStatus = key }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                                                fontSize = 12.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                if (rowOptions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons Row: Delete and Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Delete Button
                        Button(
                            onClick = {
                                if (isDeletingOrSaving) return@Button
                                isDeletingOrSaving = true
                                coroutineScope.launch {
                                    try {
                                        val token = prefs.getString("anilist_access_token", "") ?: ""
                                        if (item.isHistoryItem) {
                                            WatchHistoryManager.deleteWatchHistoryItem(context, item.id, item.title)
                                            if (isConnected && token.isNotBlank()) {
                                                AniListRepository.deleteMediaListEntry(token, item.id, item.title)
                                            }
                                            Toast.makeText(context, "Deleted from Watch History & AniList", Toast.LENGTH_SHORT).show()
                                        } else {
                                            com.example.data.LocalMyListManager.removeMyListItem(context, item.id, item.title)
                                            if (isConnected && token.isNotBlank()) {
                                                AniListRepository.deleteMediaListEntry(token, item.id, item.title)
                                            }
                                            Toast.makeText(context, "Deleted from My List & AniList", Toast.LENGTH_SHORT).show()
                                        }
                                        watchHistoryList = WatchHistoryManager.getWatchHistory(context)
                                        favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isDeletingOrSaving = false
                                        editingItem = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1418)),
                            border = BorderStroke(1.dp, Color(0xFFFF4D4D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF4D4D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete",
                                color = Color(0xFFFF4D4D),
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Save Status Button
                        Button(
                            onClick = {
                                if (isDeletingOrSaving) return@Button
                                isDeletingOrSaving = true
                                coroutineScope.launch {
                                    try {
                                        val token = prefs.getString("anilist_access_token", "") ?: ""
                                        if (isConnected && token.isNotBlank()) {
                                            val updated = AniListRepository.saveMediaListEntry(
                                                accessToken = token,
                                                animeId = item.id,
                                                animeTitle = item.title,
                                                status = selectedStatus
                                            )
                                            if (updated) {
                                                Toast.makeText(context, "Status updated on AniList!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Saved status locally", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Saved status locally", Toast.LENGTH_SHORT).show()
                                        }
                                        watchHistoryList = WatchHistoryManager.getWatchHistory(context)
                                        favoritesList = com.example.data.LocalMyListManager.getAllMyListItems(context)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isDeletingOrSaving = false
                                        editingItem = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            if (isDeletingOrSaving) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save Status",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealWatchHistoryCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(125.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.animeTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xAA000000),
                            Color(0xEE000000)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = item.animeTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0x99000000)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (item.episodeNumber.isNotEmpty()) "EP ${item.episodeNumber}" else "Watched",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridAnimeCard(
    anime: AnimeCardItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF12121C),
        border = BorderStroke(1.dp, Color(0xFF222234)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                AsyncImage(
                    model = anime.imageUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (anime.rating.isNotEmpty() && anime.rating != "N/A") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = anime.rating,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = anime.title,
                color = Color.White,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

