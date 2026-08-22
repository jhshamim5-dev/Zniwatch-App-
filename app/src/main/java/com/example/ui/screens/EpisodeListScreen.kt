package com.example.ui.screens

import kotlinx.coroutines.async
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ActiveDownloadStatus
import com.example.data.ActiveDownloadTask
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AniListRepository
import com.example.data.AnikotoRepository
import com.example.data.AnimeCardItem
import com.example.data.EpisodeDownloader
import com.example.data.EpisodeItem
import com.example.data.EpisodeStreamResult
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.ui.components.AnimeCardImage
import com.example.ui.components.AppPullToRefreshLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ActivePlayerState(
    val episode: EpisodeItem,
    val category: String,
    val streamResult: EpisodeStreamResult,
    val startPosition: Long = 0L
)

private data class EpisodeNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val index: Int
)

@Composable
fun EpisodeListScreen(
    anime: AnimeCardItem,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var historyItem by remember(anime.id) { mutableStateOf<com.example.data.WatchHistoryItem?>(null) }
    
    var images by remember(anime.id, anime.title) {
        mutableStateOf<List<String>>(emptyList())
    }
    var isLoading by remember(anime.id, anime.title) {
        mutableStateOf(true)
    }
    var episodes by remember(anime.id, anime.title) {
        mutableStateOf<List<EpisodeItem>>(emptyList())
    }
    var isEpisodesLoading by remember(anime.id, anime.title) {
        mutableStateOf(true)
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(anime.id, anime.title, refreshTrigger) {
        val allHistory = com.example.data.WatchHistoryManager.getWatchHistory(context)
        val animeId = anime.id.split("|")[0].ifEmpty { anime.title.lowercase().replace(" ", "-") }
        historyItem = allHistory.find { it.animeId == animeId || it.animeTitle.equals(anime.title, ignoreCase = true) }

        isLoading = true
        isEpisodesLoading = true

        kotlinx.coroutines.coroutineScope {
            val imagesDeferred = async {
                try {
                    AniListRepository.getAnimeGalleryImages(
                        title = anime.title,
                        defaultBanner = anime.imageUrl,
                        defaultCover = anime.imageUrl
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            val episodesDeferred = async {
                try {
                    AnikotoRepository.getEpisodes(anime.title, anime.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            val fetchedImages = imagesDeferred.await()
            images = if (fetchedImages.isNotEmpty()) fetchedImages else listOf(anime.imageUrl)
            isLoading = false

            episodes = episodesDeferred.await()
            isEpisodesLoading = false
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRangeIndex by remember(episodes.size) { mutableIntStateOf(0) }
    var isRangeDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDownloadRangeIndex by remember(episodes.size) { mutableIntStateOf(0) }
    var isDownloadRangeDropdownExpanded by remember { mutableStateOf(false) }
    var selectedEpisodeForAudio by remember { mutableStateOf<EpisodeItem?>(null) }
    var selectedEpisodeForDownload by remember { mutableStateOf<EpisodeItem?>(null) }
    var activePlayingStream by remember { mutableStateOf<ActivePlayerState?>(null) }
    var isFetchingStream by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var hasAutoPlayed by remember(anime.id) { mutableStateOf(false) }

    val activeDownloadsState by EpisodeDownloader.activeDownloads.collectAsStateWithLifecycle()
    val lastCompletedMap by EpisodeDownloader.lastCompletedEpisodeMap.collectAsStateWithLifecycle()

    val currentActiveTask = activeDownloadsState.values.find {
        it.animeTitle.equals(anime.title, ignoreCase = true) &&
        (it.status == ActiveDownloadStatus.DOWNLOADING || it.status == ActiveDownloadStatus.FETCHING_PLAYLIST)
    }

    val lastCompletedEpNum = lastCompletedMap[anime.title]
        ?: remember(anime.title, activeDownloadsState) {
            val downloadedList = EpisodeDownloader.getDownloadedEpisodes(context)
            downloadedList.filter { it.animeTitle.equals(anime.title, ignoreCase = true) }
                .maxOfOrNull { it.episodeNumber }
        }

    val playEpisodeStream: (EpisodeItem, String, Long) -> Unit = remember(anime.title) {
        { ep, category, startPos ->
            coroutineScope.launch {
                isFetchingStream = true
                try {
                    val streamResult = AnikotoRepository.getEpisodeStreamUrl(
                        animeTitle = anime.title,
                        episodeId = ep.id,
                        episodeNumber = ep.episodeNumber,
                        category = category
                    )
                    activePlayingStream = ActivePlayerState(
                        episode = ep,
                        category = category.uppercase(),
                        streamResult = streamResult,
                        startPosition = startPos
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isFetchingStream = false
                }
            }
        }
    }

    LaunchedEffect(episodes, hasAutoPlayed) {
        if (!hasAutoPlayed && episodes.isNotEmpty() && anime.startEpisodeId != null) {
            val targetEp = episodes.find { 
                it.id == anime.startEpisodeId || 
                it.episodeNumber.toString() == anime.startEpisodeId ||
                it.id.endsWith(anime.startEpisodeId!!)
            } ?: run {
                val num = anime.startEpisodeId!!.toIntOrNull()
                if (num != null) episodes.find { it.episodeNumber == num } else null
            }
            if (targetEp != null) {
                hasAutoPlayed = true
                playEpisodeStream(targetEp, anime.startCategory ?: "SUB", anime.startPosition)
            }
        }
    }

    if (activePlayingStream != null) {
        val playerState = activePlayingStream!!
        VideoPlayerScreen(
            animeTitle = anime.title,
            animeImageUrl = anime.imageUrl,
            initialEpisode = playerState.episode,
            initialStreamResult = playerState.streamResult,
            initialCategory = playerState.category,
            episodes = episodes,
            startPosition = playerState.startPosition,
            onClose = { activePlayingStream = null }
        )
        return
    }

    if (selectedEpisodeForAudio != null) {
        val ep = selectedEpisodeForAudio!!
        AudioLanguageSelectionBottomSheet(
            episode = ep,
            onDismiss = { selectedEpisodeForAudio = null },
            onSelectLanguage = { category ->
                selectedEpisodeForAudio = null
                playEpisodeStream(ep, category, 0L)
            }
        )
    }

    if (selectedEpisodeForDownload != null) {
        val ep = selectedEpisodeForDownload!!
        DownloadServerSelectionBottomSheet(
            episode = ep,
            animeTitle = anime.title,
            onDismiss = { selectedEpisodeForDownload = null },
            onSelectServer = { server ->
                selectedEpisodeForDownload = null
                coroutineScope.launch {
                    isFetchingStream = true
                    try {
                        val streamResult = AnikotoRepository.fetchStreamFromLinkId(
                            linkId = server.linkId,
                            animeTitle = anime.title,
                            category = server.type,
                            episodeId = ep.id,
                            episodeNumber = ep.episodeNumber
                        )
                        EpisodeDownloader.startDownload(
                            context = context,
                            animeTitle = anime.title,
                            episode = ep,
                            server = server,
                            streamResult = streamResult,
                            animeImageUrl = anime.imageUrl
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(
                            context,
                            "Download error: ${e.localizedMessage}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isFetchingStream = false
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            AnimatedEpisodeBottomBar(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Top Bar
            val topBarTitle = when (selectedTab) {
                0 -> "Episodes"
                1 -> "Comments"
                2 -> "Download"
                else -> "Episodes"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = topBarTitle,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AppPullToRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        refreshTrigger++
                        delay(800)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                // Hero Auto Slider (Smaller height as requested)
                val imageList = if (images.isNotEmpty()) images else listOf(anime.imageUrl)
                val pagerState = rememberPagerState(pageCount = { imageList.size })

                // Auto-slider effect
                LaunchedEffect(pagerState, imageList.size) {
                    if (imageList.size > 1) {
                        while (true) {
                            delay(3500)
                            val nextPage = (pagerState.currentPage + 1) % imageList.size
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                }

                val infiniteTransition = rememberInfiniteTransition(label = "banner_motion")
                val motionScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(7000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "motion_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clipToBounds()
                ) {
                    if (isLoading && imageList.size == 1 && imageList[0].isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(32.dp))
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val imgUrl = imageList[page]
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimeCardImage(
                                    imageUrl = imgUrl,
                                    imageResId = anime.imageResId,
                                    contentDescription = anime.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = motionScale
                                            scaleY = motionScale
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x66000000),
                                                    Color.Transparent,
                                                    Color(0xFF000000)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Slider Page Indicator Dots
                        if (imageList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(imageList.size.coerceAtMost(8)) { index ->
                                    val color = if (pagerState.currentPage % 8 == index) Color.White else Color(0x66FFFFFF)
                                    val width = if (pagerState.currentPage % 8 == index) 14.dp else 5.dp
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .height(5.dp)
                                            .width(width)
                                            .background(color = color, shape = CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title and Rating under the Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = anime.rating,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / 10",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF222228), thickness = 1.dp)

                // Animated Tab Content Section
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "tab_content_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> {
                            val rangeChunkSize = 50
                            val totalRanges = (episodes.size + rangeChunkSize - 1) / rangeChunkSize
                            val rangeLabels = (0 until totalRanges).map { idx ->
                                val start = idx * rangeChunkSize + 1
                                val end = minOf((idx + 1) * rangeChunkSize, episodes.size)
                                "$start - $end"
                            }
                            val displayedEpisodes = if (episodes.size > 50) {
                                val startIndex = (selectedRangeIndex * rangeChunkSize).coerceAtMost(episodes.size)
                                val endIndex = ((selectedRangeIndex + 1) * rangeChunkSize).coerceAtMost(episodes.size)
                                episodes.subList(startIndex, endIndex)
                            } else {
                                episodes
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            ) {
                                if (historyItem != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(78.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                val targetEpNumber = historyItem!!.episodeNumber.ifEmpty { historyItem!!.episodeId }
                                                val targetEp = episodes.find { ep ->
                                                    ep.id == historyItem!!.episodeId ||
                                                    ep.episodeNumber.toString() == targetEpNumber ||
                                                    ep.id.endsWith(targetEpNumber)
                                                } ?: run {
                                                    val num = targetEpNumber.toIntOrNull()
                                                    if (num != null) episodes.find { it.episodeNumber == num } else null
                                                }
                                                if (targetEp != null) {
                                                    playEpisodeStream(targetEp, historyItem!!.category, historyItem!!.playbackPosition)
                                                } else if (episodes.isNotEmpty()) {
                                                    playEpisodeStream(episodes.first(), historyItem!!.category, historyItem!!.playbackPosition)
                                                }
                                            }
                                    ) {
                                        AnimeCardImage(
                                            imageUrl = historyItem!!.imageUrl.ifEmpty { anime.imageUrl },
                                            imageResId = 0,
                                            contentDescription = "Continue Watching",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xEE000000),
                                                            Color(0xBB000000),
                                                            Color(0x44000000)
                                                        )
                                                    )
                                                )
                                                .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(16.dp))
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 18.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Continue : Episode - ${historyItem!!.episodeNumber}",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontFamily = PremiumTitleFont,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Resume from where you left",
                                                    color = Color(0xFFDDDDDD),
                                                    fontSize = 13.sp,
                                                    fontFamily = PremiumBodyFont
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Select Episode",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (episodes.isNotEmpty()) {
                                        Text(
                                            text = "${episodes.size} Episodes",
                                            color = Color(0xFF888899),
                                            fontSize = 12.sp,
                                            fontFamily = PremiumBodyFont
                                        )
                                    }
                                }

                                // Range dropdown for >50 episodes
                                if (episodes.size > 50) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box {
                                        Surface(
                                            onClick = { isRangeDropdownExpanded = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1B1B22),
                                            border = BorderStroke(1.dp, Color(0xFF2D2D38)),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = rangeLabels.getOrElse(selectedRangeIndex) { "1 - 50" },
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontFamily = PremiumTitleFont,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                    contentDescription = "Select Range",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isRangeDropdownExpanded,
                                            onDismissRequest = { isRangeDropdownExpanded = false },
                                            modifier = Modifier
                                                .background(Color(0xFF1B1B22))
                                                .width(130.dp)
                                        ) {
                                            rangeLabels.forEachIndexed { idx, label ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            color = if (idx == selectedRangeIndex) Color.White else Color(0xFF888899),
                                                            fontFamily = PremiumTitleFont,
                                                            fontWeight = if (idx == selectedRangeIndex) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedRangeIndex = idx
                                                        isRangeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isEpisodesLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(Color(0xFF141418), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else if (episodes.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(Color(0xFF141418), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No episodes available",
                                            color = Color(0xFF777788),
                                            fontSize = 14.sp,
                                            fontFamily = PremiumBodyFont
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        displayedEpisodes.forEach { ep ->
                                            EpisodeCardItem(
                                                episode = ep,
                                                fallbackImage = anime.imageUrl,
                                                onClick = {
                                                    if (ep.hasDub) {
                                                        selectedEpisodeForAudio = ep
                                                    } else {
                                                        playEpisodeStream(ep, "sub", 0L)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            EpisodeCommentsSection(animeTitle = anime.title)
                        }
                        2 -> {
                            val rangeChunkSize = 50
                            val totalRanges = (episodes.size + rangeChunkSize - 1) / rangeChunkSize
                            val rangeLabels = (0 until totalRanges).map { idx ->
                                val start = idx * rangeChunkSize + 1
                                val end = minOf((idx + 1) * rangeChunkSize, episodes.size)
                                "$start - $end"
                            }
                            val displayedEpisodes = if (episodes.size > 50) {
                                val startIndex = (selectedDownloadRangeIndex * rangeChunkSize).coerceAtMost(episodes.size)
                                val endIndex = ((selectedDownloadRangeIndex + 1) * rangeChunkSize).coerceAtMost(episodes.size)
                                episodes.subList(startIndex, endIndex)
                            } else {
                                episodes
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            ) {
                                if (currentActiveTask != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(16.dp))
                                    ) {
                                        AnimeCardImage(
                                            imageUrl = currentActiveTask.animeImageUrl.ifEmpty { anime.imageUrl },
                                            imageResId = 0,
                                            contentDescription = "Background Image",
                                            modifier = Modifier.matchParentSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xF2000000),
                                                            Color(0xD9000000),
                                                            Color(0x99000000)
                                                        )
                                                    )
                                                )
                                        )
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = { (currentActiveTask.progress / 100f).coerceIn(0f, 1f) },
                                                        color = Color.Black,
                                                        strokeWidth = 3.dp,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = "Downloading",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Downloading Episode ${currentActiveTask.episodeNumber}",
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                        fontFamily = PremiumTitleFont,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${currentActiveTask.progress}% • ${if (currentActiveTask.status == ActiveDownloadStatus.FETCHING_PLAYLIST) "Fetching playlist..." else "Downloading..."}",
                                                        color = Color(0xFFDDDDDD),
                                                        fontSize = 12.5.sp,
                                                        fontFamily = PremiumBodyFont
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(
                                                progress = { (currentActiveTask.progress / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = Color.White,
                                                trackColor = Color(0x66FFFFFF)
                                            )
                                        }
                                    }
                                } else if (lastCompletedEpNum != null) {
                                    val nextEpToDownload = episodes.find { it.episodeNumber == lastCompletedEpNum + 1 }
                                    if (nextEpToDownload != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                                .height(78.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedEpisodeForDownload = nextEpToDownload
                                                }
                                        ) {
                                            AnimeCardImage(
                                                imageUrl = anime.imageUrl,
                                                imageResId = 0,
                                                contentDescription = "Download Next Episode",
                                                modifier = Modifier.matchParentSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color(0xF2000000),
                                                                Color(0xCC000000),
                                                                Color(0x66000000)
                                                            )
                                                        )
                                                    )
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = "Download Next",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column {
                                                    Text(
                                                        text = "Download Next Ep - ${nextEpToDownload.episodeNumber}",
                                                        color = Color.White,
                                                        fontSize = 16.sp,
                                                        fontFamily = PremiumTitleFont,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Episode $lastCompletedEpNum downloaded • Tap to download Ep ${nextEpToDownload.episodeNumber}",
                                                        color = Color(0xFFDDDDDD),
                                                        fontSize = 12.5.sp,
                                                        fontFamily = PremiumBodyFont
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Download Episode",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (episodes.isNotEmpty()) {
                                        Text(
                                            text = "${episodes.size} Episodes",
                                            color = Color(0xFF888899),
                                            fontSize = 12.sp,
                                            fontFamily = PremiumBodyFont
                                        )
                                    }
                                }

                                // Range dropdown for >50 episodes
                                if (episodes.size > 50) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box {
                                        Surface(
                                            onClick = { isDownloadRangeDropdownExpanded = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1B1B22),
                                            border = BorderStroke(1.dp, Color(0xFF2D2D38)),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = rangeLabels.getOrElse(selectedDownloadRangeIndex) { "1 - 50" },
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontFamily = PremiumTitleFont,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                    contentDescription = "Select Range",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isDownloadRangeDropdownExpanded,
                                            onDismissRequest = { isDownloadRangeDropdownExpanded = false },
                                            modifier = Modifier
                                                .background(Color(0xFF1B1B22))
                                                .width(130.dp)
                                        ) {
                                            rangeLabels.forEachIndexed { idx, label ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            color = if (idx == selectedDownloadRangeIndex) Color.White else Color(0xFF888899),
                                                            fontFamily = PremiumTitleFont,
                                                            fontWeight = if (idx == selectedDownloadRangeIndex) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedDownloadRangeIndex = idx
                                                        isDownloadRangeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isEpisodesLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(Color(0xFF141418), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else if (episodes.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(Color(0xFF141418), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No episodes available for download",
                                            color = Color(0xFF777788),
                                            fontSize = 14.sp,
                                            fontFamily = PremiumBodyFont
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        displayedEpisodes.forEach { ep ->
                                            EpisodeCardItem(
                                                episode = ep,
                                                fallbackImage = anime.imageUrl,
                                                isDownloadMode = true,
                                                onClick = {
                                                    selectedEpisodeForDownload = ep
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (isFetchingStream) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Color(0xFF1B1B22), RoundedCornerShape(16.dp))
                    .padding(28.dp)
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Preparing Stream...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
}

@Composable
private fun EpisodeCardItem(
    episode: EpisodeItem,
    fallbackImage: String,
    isDownloadMode: Boolean = false,
    onClick: () -> Unit
) {
    val actionIcon = if (isDownloadMode) Icons.Filled.Download else Icons.Filled.PlayArrow
    val actionDesc = if (isDownloadMode) "Download" else "Play"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141418),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AnimeCardImage(
                    imageUrl = episode.thumbnail.ifEmpty { fallbackImage },
                    imageResId = 0,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionDesc,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Episode ${episode.episodeNumber}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.title,
                    color = Color(0xFF9999AA),
                    fontSize = 11.5.sp,
                    fontFamily = PremiumBodyFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (episode.isFiller) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFFFB800).copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, Color(0xFFFFB800))
                        ) {
                            Text(
                                text = "FILLER",
                                color = Color(0xFFFFC107),
                                fontSize = 8.5.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (episode.hasSub) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF262632)
                        ) {
                            Text(
                                text = "SUB",
                                color = Color(0xFFDDDDDD),
                                fontSize = 8.5.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (episode.hasDub) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF1B3822)
                        ) {
                            Text(
                                text = "DUB",
                                color = Color(0xFF4CAF50),
                                fontSize = 8.5.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22222A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDesc,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedEpisodeBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val navItems = remember {
        listOf(
            EpisodeNavItem("Episodes", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow, 0),
            EpisodeNavItem("Comments", Icons.Filled.ModeComment, Icons.Outlined.ModeComment, 1),
            EpisodeNavItem("Download", Icons.Filled.Download, Icons.Outlined.Download, 2)
        )
    }

    Surface(
        color = Color(0xFF101010),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = selectedIndex == item.index

                val animScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "tab_scale"
                )

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = tween(300),
                    label = "tab_bg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF888899),
                    animationSpec = tween(300),
                    label = "tab_color"
                )


                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(item.index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = animScale
                            scaleY = animScale
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(),
                            exit = fadeOut(animationSpec = tween(200)) + shrinkHorizontally()
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    color = contentColor,
                                    fontSize = 13.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioLanguageSelectionBottomSheet(
    episode: EpisodeItem,
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141A),
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFF444455), RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Audio Language",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Episode ${episode.episodeNumber}: ${episode.title}",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.5.sp,
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

            Spacer(modifier = Modifier.height(18.dp))

            // Option 1: SUB (Japanese)
            Surface(
                onClick = { onSelectLanguage("sub") },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E26),
                border = BorderStroke(1.dp, Color(0xFF333344)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ClosedCaption,
                            contentDescription = "Subtitled",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Japanese Audio (SUB)",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SUB",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Original Japanese audio with English subtitles",
                            color = Color(0xFF888899),
                            fontSize = 12.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: DUB (English)
            Surface(
                onClick = { onSelectLanguage("dub") },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E26),
                border = BorderStroke(1.dp, Color(0xFF333344)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Dubbed",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "English Audio (DUB)",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "DUB",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "English voiceover audio stream",
                            color = Color(0xFF888899),
                            fontSize = 12.sp,
                            fontFamily = PremiumBodyFont
                        )
                    }
                }
            }
        }
    }
}
