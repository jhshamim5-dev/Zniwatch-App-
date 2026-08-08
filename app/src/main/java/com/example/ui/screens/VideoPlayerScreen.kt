package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.AnikotoRepository
import com.example.data.AnikotoServer
import com.example.data.EpisodeItem
import com.example.data.EpisodeStreamResult
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.data.SubtitleTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerScreen(
    animeTitle: String,
    animeImageUrl: String = "",
    initialEpisode: EpisodeItem,
    initialStreamResult: EpisodeStreamResult,
    initialCategory: String,
    episodes: List<EpisodeItem> = emptyList(),
    startPosition: Long = 0L,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    // Enforce Sensor Landscape Orientation and Fullscreen Immersive
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            val windowDispose = activity?.window
            if (windowDispose != null) {
                val controllerDispose = WindowCompat.getInsetsController(windowDispose, windowDispose.decorView)
                controllerDispose.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        onClose()
    }

    ProfessionalAnimePlayer(
        animeTitle = animeTitle,
        animeImageUrl = animeImageUrl,
        initialEpisode = initialEpisode,
        initialStreamResult = initialStreamResult,
        initialCategory = initialCategory,
        episodes = episodes,
        startPosition = startPosition,
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalAnimePlayer(
    animeTitle: String,
    animeImageUrl: String = "",
    initialEpisode: EpisodeItem,
    initialStreamResult: EpisodeStreamResult,
    initialCategory: String,
    episodes: List<EpisodeItem>,
    startPosition: Long = 0L,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentEpisode by remember { mutableStateOf(initialEpisode) }
    var currentStreamResult by remember { mutableStateOf(initialStreamResult) }
    var currentCategory by remember { mutableStateOf(initialCategory) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(currentEpisode) {
        val animeId = currentEpisode.id.split("|")[0].ifEmpty { animeTitle.lowercase().replace(" ", "-") }
        com.example.data.WatchHistoryManager.saveWatchHistory(
            context = context,
            animeId = animeId,
            animeTitle = animeTitle,
            episodeId = currentEpisode.id,
            episodeTitle = currentEpisode.title,
            episodeNumber = currentEpisode.episodeNumber.toString(),
            imageUrl = currentEpisode.thumbnail.ifEmpty { animeImageUrl },
            playbackPosition = currentPositionMs,
            category = currentCategory
        )
    }

    var availableServers by remember { mutableStateOf<List<AnikotoServer>>(emptyList()) }
    var selectedServer by remember { mutableStateOf<AnikotoServer?>(null) }
    var activeServerCategoryTab by remember { mutableStateOf(currentCategory.lowercase().ifEmpty { "sub" }) }

    var isFetchingServers by remember { mutableStateOf(false) }
    var isFetchingStream by remember { mutableStateOf(false) }
    var isExtractingM3u8 by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }

    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var autoNextEpisode by remember { mutableStateOf(true) }

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentQuality by remember { mutableStateOf("Auto") }
    var availableQualities by remember { mutableStateOf<List<String>>(listOf("Auto")) }
    var selectedSubtitleTrack by remember { mutableStateOf<SubtitleTrack?>(null) }
    var isSubtitlesEnabled by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Double Tap Seek Ripple State
    var showSeekFeedbackLeft by remember { mutableStateOf(false) }
    var showSeekFeedbackRight by remember { mutableStateOf(false) }

    // Modal Bottom Sheets
    var showServerSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, 64 * 1024))
            .setBufferDurationsMs(
                60000,   // Min buffer size: 60s
                240000,  // Max buffer size: 240s (Allows deep pre-buffering to completely prevent stuttering)
                1500,    // Buffer for playback start: 1.5s (Starts playback extremely fast)
                3000     // Buffer after rebuffer: 3s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30000, true) // Enable 30s back buffer so seeking backwards is instant
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    // Function to load stream into ExoPlayer with Subtitles using custom HTTP headers
    val prepareExoPlayer: (EpisodeStreamResult) -> Unit = remember(exoPlayer) {
        { targetResult ->
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                val userAgent = targetResult.headers["User-Agent"]
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                setUserAgent(userAgent)
                setAllowCrossProtocolRedirects(true)
                setConnectTimeoutMs(20000) // Highly resilient timeouts
                setReadTimeoutMs(20000)

                val reqProps = HashMap<String, String>()
                targetResult.headers.forEach { (k, v) -> reqProps[k] = v }
                if (!reqProps.containsKey("Referer")) {
                    reqProps["Referer"] = "https://anikoto.cz/"
                }
                setDefaultRequestProperties(reqProps)
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            val mediaUri = Uri.parse(targetResult.url)
            val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)

            // Attach Subtitle Tracks if present
            if (targetResult.subtitles.isNotEmpty()) {
                val subtitleConfigs = targetResult.subtitles.map { sub ->
                    val mimeType = when {
                        sub.url.endsWith(".vtt") || sub.url.contains(".vtt") -> MimeTypes.TEXT_VTT
                        sub.url.endsWith(".srt") || sub.url.contains(".srt") -> MimeTypes.APPLICATION_SUBRIP
                        sub.url.endsWith(".ass") || sub.url.contains(".ass") -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.TEXT_VTT
                    }
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                        .setMimeType(mimeType)
                        .setLanguage(sub.label)
                        .setLabel(sub.label)
                        .setSelectionFlags(if (sub.isDefault) androidx.media3.common.C.SELECTION_FLAG_DEFAULT else 0)
                        .build()
                }
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
                if (selectedSubtitleTrack == null) {
                    selectedSubtitleTrack = targetResult.subtitles.firstOrNull { it.isDefault } ?: targetResult.subtitles.firstOrNull()
                }
            }

            val mediaItem = mediaItemBuilder.build()
            
            // Check if the source is HLS and use the optimized HlsMediaSource with chunkless preparation for fast & smooth segment downloading
            val isHls = targetResult.isM3u8 || targetResult.url.contains(".m3u8")
            val mediaSource = if (isHls) {
                androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                mediaSourceFactory.createMediaSource(mediaItem)
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.setPlaybackSpeed(currentSpeed)

            val trackParamsBuilder = exoPlayer.trackSelectionParameters.buildUpon()
            if (isSubtitlesEnabled) {
                trackParamsBuilder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                if (selectedSubtitleTrack != null) {
                    trackParamsBuilder.setPreferredTextLanguage(selectedSubtitleTrack!!.label)
                }
            } else {
                trackParamsBuilder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            }
            exoPlayer.trackSelectionParameters = trackParamsBuilder.build()

            exoPlayer.prepare()
        }
    }

    // Load available servers when episode changes
    LaunchedEffect(currentEpisode.id) {
        isFetchingServers = true
        try {
            val servers = AnikotoRepository.fetchEpisodeServers(
                animeTitle = animeTitle,
                episodeId = currentEpisode.id,
                episodeNumber = currentEpisode.episodeNumber
            )
            availableServers = servers
            if (servers.isNotEmpty()) {
                val matchCat = servers.firstOrNull { it.type.equals(currentCategory, ignoreCase = true) }
                selectedServer = matchCat ?: servers.first()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFetchingServers = false
        }
    }

    // Process stream URL when currentStreamResult changes
    var hasSeekedToStart by remember { mutableStateOf(false) }

    LaunchedEffect(currentStreamResult.url) {
        hasPlaybackError = false
        isBuffering = true

        var targetResult = currentStreamResult
        if (!targetResult.isM3u8 && !targetResult.url.contains(".m3u8") && !targetResult.url.endsWith(".mp4")) {
            isExtractingM3u8 = true
            try {
                val extracted = AnikotoRepository.extractM3u8FromEmbedUrl(targetResult.url)
                if (extracted.url.isNotEmpty()) {
                    targetResult = extracted
                    currentStreamResult = extracted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isExtractingM3u8 = false
            }
        }

        prepareExoPlayer(targetResult)
        if (!hasSeekedToStart && startPosition > 0L) {
            exoPlayer.seekTo(startPosition)
            hasSeekedToStart = true
        }
    }

    // Listener for ExoPlayer events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val heights = mutableSetOf<Int>()
                for (group in tracks.groups) {
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.height > 0) {
                                heights.add(format.height)
                            }
                        }
                    }
                }
                val sortedHeights = heights.sortedDescending()
                val options = mutableListOf<String>()
                options.add("Auto")
                sortedHeights.forEach { h ->
                    options.add("${h}p")
                }
                availableQualities = options
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    hasPlaybackError = false
                } else if (state == Player.STATE_ENDED) {
                    // Auto play next episode if enabled
                    if (autoNextEpisode && episodes.isNotEmpty()) {
                        val currIdx = episodes.indexOfFirst { it.episodeNumber == currentEpisode.episodeNumber }
                        if (currIdx != -1 && currIdx < episodes.size - 1) {
                            val nextEp = episodes[currIdx + 1]
                            coroutineScope.launch {
                                isFetchingStream = true
                                try {
                                    val newStream = AnikotoRepository.getEpisodeStreamUrl(
                                        animeTitle = animeTitle,
                                        episodeId = nextEp.id,
                                        episodeNumber = nextEp.episodeNumber,
                                        category = currentCategory
                                    )
                                    currentEpisode = nextEp
                                    currentStreamResult = newStream
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isFetchingStream = false
                                }
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                hasPlaybackError = true
                errorMessage = error.localizedMessage ?: "Playback error"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    var lastSavedPosition by remember { mutableLongStateOf(0L) }

    // Position updater loop
    LaunchedEffect(isPlaying, currentEpisode) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            
            if (kotlin.math.abs(currentPositionMs - lastSavedPosition) > 10000) {
                lastSavedPosition = currentPositionMs
                val animeId = currentEpisode.id.split("|")[0].ifEmpty { animeTitle.lowercase().replace(" ", "-") }
                com.example.data.WatchHistoryManager.saveWatchHistory(
                    context = context,
                    animeId = animeId,
                    animeTitle = animeTitle,
                    episodeId = currentEpisode.id,
                    episodeTitle = currentEpisode.title,
                    episodeNumber = currentEpisode.episodeNumber.toString(),
                    imageUrl = currentEpisode.thumbnail.ifEmpty { animeImageUrl },
                    playbackPosition = currentPositionMs,
                    category = currentCategory
                )

                // Sync watched episode progress to AniList if connected
                val prefs = context.getSharedPreferences("anilist_prefs", android.content.Context.MODE_PRIVATE)
                val isConnected = prefs.getBoolean("anilist_connected", false)
                val accessToken = prefs.getString("anilist_access_token", "") ?: ""
                val epNumInt = currentEpisode.episodeNumber
                if (isConnected && accessToken.isNotEmpty() && epNumInt > 0) {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            com.example.data.AniListRepository.saveMediaListEntry(
                                accessToken = accessToken,
                                animeId = animeId,
                                animeTitle = animeTitle,
                                status = "CURRENT",
                                progress = epNumInt
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            delay(500)
        }
    }

    // Auto hide controls after 5 seconds
    LaunchedEffect(showControls, isScreenLocked) {
        if (showControls && !isScreenLocked) {
            delay(5000)
            showControls = false
        }
    }

    // Function to change speed
    val changeSpeed: (Float) -> Unit = { speed ->
        currentSpeed = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    // Function to change quality
    val changeQuality: (String) -> Unit = { qualityLabel ->
        currentQuality = qualityLabel
        val trackParamsBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        if (qualityLabel == "Auto") {
            trackParamsBuilder.clearVideoSizeConstraints()
        } else {
            val h = qualityLabel.replace(Regex("[^0-9]"), "").toIntOrNull()
            if (h != null) {
                trackParamsBuilder
                    .setMinVideoSize(0, h)
                    .setMaxVideoSize(Int.MAX_VALUE, h)
            } else {
                trackParamsBuilder.clearVideoSizeConstraints()
            }
        }
        exoPlayer.trackSelectionParameters = trackParamsBuilder.build()
    }

    // Function to switch server
    val selectServer: (AnikotoServer) -> Unit = { server ->
        selectedServer = server
        currentCategory = server.type.uppercase()
        showServerSheet = false
        coroutineScope.launch {
            isFetchingStream = true
            try {
                val newStream = AnikotoRepository.fetchStreamFromLinkId(
                    linkId = server.linkId,
                    animeTitle = animeTitle,
                    category = server.type,
                    episodeId = currentEpisode.id,
                    episodeNumber = currentEpisode.episodeNumber
                )
                currentStreamResult = newStream
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingStream = false
            }
        }
    }

    // Function to switch episode
    val selectEpisode: (EpisodeItem) -> Unit = { episode ->
        currentEpisode = episode
        showEpisodeSheet = false
        coroutineScope.launch {
            isFetchingStream = true
            try {
                val newStream = AnikotoRepository.getEpisodeStreamUrl(
                    animeTitle = animeTitle,
                    episodeId = episode.id,
                    episodeNumber = episode.episodeNumber,
                    category = currentCategory
                )
                currentStreamResult = newStream
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingStream = false
            }
        }
    }

    val currentPositionSec = currentPositionMs / 1000L
    val isInIntro = currentStreamResult.introStartSec != null &&
            currentStreamResult.introEndSec != null &&
            currentPositionSec in (currentStreamResult.introStartSec!! - 2)..(currentStreamResult.introEndSec!!)

    val isInOutro = currentStreamResult.outroStartSec != null &&
            currentStreamResult.outroEndSec != null &&
            currentPositionSec in (currentStreamResult.outroStartSec!! - 2)..(currentStreamResult.outroEndSec!!)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isScreenLocked) {
                            val halfWidth = size.width / 2f
                            if (offset.x < halfWidth) {
                                // Double tap left -> seek -10s
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                coroutineScope.launch {
                                    showSeekFeedbackLeft = true
                                    delay(700)
                                    showSeekFeedbackLeft = false
                                }
                            } else {
                                // Double tap right -> seek +10s
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                coroutineScope.launch {
                                    showSeekFeedbackRight = true
                                    delay(700)
                                    showSeekFeedbackRight = false
                                }
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        val subtitlePrefManager = remember { com.example.data.SubtitlePreferencesManager(context) }
        val subSettings = remember { subtitlePrefManager.getSettings() }

        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
                playerView.subtitleView?.let { subView ->
                    subView.setApplyEmbeddedStyles(false)
                    subView.setApplyEmbeddedFontSizes(false)
                    subView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subSettings.fontSizeSp)
                    subView.setStyle(
                        androidx.media3.ui.CaptionStyleCompat(
                            subSettings.fontColorHex.toInt(),
                            subSettings.backgroundColorHex.toInt(),
                            android.graphics.Color.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE,
                            android.graphics.Color.TRANSPARENT,
                            null
                        )
                    )
                    val density = playerView.context.resources.displayMetrics.density
                    val bottomPx = (subSettings.bottomOffsetDp * density).toInt()
                    subView.translationY = -bottomPx.toFloat()
                    subView.setBottomPaddingFraction((subSettings.bottomOffsetDp / 200f).coerceIn(0.02f, 0.8f))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Double Tap Visual Ripples
        AnimatedVisibility(
            visible = showSeekFeedbackLeft,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "◄◄", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "-10s", color = Color.White, fontSize = 14.sp, fontFamily = PremiumTitleFont, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = showSeekFeedbackRight,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "►►", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "+10s", color = Color.White, fontSize = 14.sp, fontFamily = PremiumTitleFont, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Floating Action Skip Intro / Skip Outro Pills
        if (!isScreenLocked && (isInIntro || isInOutro)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 125.dp, end = 28.dp)
            ) {
                Surface(
                    onClick = {
                        if (isInIntro && currentStreamResult.introEndSec != null) {
                            val targetMs = currentStreamResult.introEndSec!! * 1000L
                            exoPlayer.seekTo(targetMs)
                            currentPositionMs = targetMs
                        } else if (isInOutro && currentStreamResult.outroEndSec != null) {
                            val targetMs = currentStreamResult.outroEndSec!! * 1000L
                            exoPlayer.seekTo(targetMs)
                            currentPositionMs = targetMs
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "Skip",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInIntro) "SKIP INTRO" else "SKIP OUTRO",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Loading / Buffering Overlay
        if ((isBuffering || isExtractingM3u8 || isFetchingStream) && !hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = when {
                            isFetchingStream -> "Fetching episode stream..."
                            isExtractingM3u8 -> "Resolving player link..."
                            else -> "Loading video..."
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Playback Error Screen
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF1E1E24), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF383842), RoundedCornerShape(16.dp))
                        .padding(28.dp)
                        .widthIn(max = 420.dp)
                ) {
                    Text(
                        text = "Unable to Play Video",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (errorMessage.isNotEmpty()) errorMessage else "Video server is currently unresponsive or blocked.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        fontFamily = PremiumBodyFont,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = {
                                if (availableServers.isNotEmpty()) {
                                    showServerSheet = true
                                } else {
                                    hasPlaybackError = false
                                    prepareExoPlayer(currentStreamResult)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White
                        ) {
                            Text(
                                text = "Switch Server",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                        Surface(
                            onClick = onClose,
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFF555566))
                        ) {
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Screen Lock Touch Overlay
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        onClick = { isScreenLocked = false },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Controls Locked • Tap to Unlock",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = PremiumBodyFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Full Interactive Overlays (Top, Center, Bottom)
        if (!isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Gradient Overlay & Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Back Button
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Title & Ep Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = animeTitle,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color.White)
                                    ) {
                                        Text(
                                            text = currentCategory.uppercase(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontFamily = PremiumTitleFont,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Episode ${currentEpisode.episodeNumber}: ${currentEpisode.title.ifEmpty { "Episode ${currentEpisode.episodeNumber}" }}",
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    fontFamily = PremiumBodyFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Buttons (Server, Episode, Subtitle, Speed, Quality, Aspect, Lock)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Server Selector Button
                                IconButton(onClick = { showServerSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Dns,
                                        contentDescription = "Server List",
                                        tint = if (showServerSheet) Color.White else Color(0xFFCCCCCC),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Episode List Button
                                if (episodes.isNotEmpty()) {
                                    IconButton(onClick = { showEpisodeSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.List,
                                            contentDescription = "Episode List",
                                            tint = if (showEpisodeSheet) Color.White else Color(0xFFCCCCCC),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Subtitles Button
                                IconButton(onClick = { showSubtitleSheet = true }) {
                                    Icon(
                                        imageVector = if (isSubtitlesEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                                        contentDescription = "Subtitles",
                                        tint = if (isSubtitlesEnabled) Color.White else Color(0xFF888888),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Speed Button
                                IconButton(onClick = { showSpeedSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = "Video Speed",
                                        tint = if (currentSpeed != 1.0f) Color.White else Color(0xFFCCCCCC),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Quality Button
                                IconButton(onClick = { showQualitySheet = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.HighQuality,
                                        contentDescription = "Video Quality",
                                        tint = if (currentQuality != "Auto") Color.White else Color(0xFFCCCCCC),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Aspect Ratio Button
                                IconButton(
                                    onClick = {
                                        resizeMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                        val label = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit Screen"
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom / Crop"
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill Screen"
                                            else -> "Fit Screen"
                                        }
                                        Toast.makeText(context, "Aspect Ratio: $label", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Center Hero Controls (Play/Pause + Previous/Next Episode)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    ) {
                        // Previous Episode
                        if (episodes.isNotEmpty()) {
                            val currIdx = episodes.indexOfFirst { it.episodeNumber == currentEpisode.episodeNumber }
                            IconButton(
                                enabled = currIdx > 0,
                                onClick = {
                                    if (currIdx > 0) {
                                        selectEpisode(episodes[currIdx - 1])
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous Episode",
                                    tint = if (currIdx > 0) Color.White else Color(0xFF555555),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(36.dp))
                        }

                        // Play/Pause Hero Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        // Next Episode
                        if (episodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(36.dp))
                            val currIdx = episodes.indexOfFirst { it.episodeNumber == currentEpisode.episodeNumber }
                            IconButton(
                                enabled = currIdx != -1 && currIdx < episodes.size - 1,
                                onClick = {
                                    if (currIdx != -1 && currIdx < episodes.size - 1) {
                                        selectEpisode(episodes[currIdx + 1])
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = if (currIdx != -1 && currIdx < episodes.size - 1) Color.White else Color(0xFF555555),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Bottom Control Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Column {
                            // Progress Bar & Time Text
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatTimeMs(currentPositionMs),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                CrunchyrollTimelineBar(
                                    currentPositionMs = currentPositionMs,
                                    durationMs = durationMs,
                                    bufferedPositionMs = bufferedPositionMs,
                                    onSeekTo = { targetMs ->
                                        exoPlayer.seekTo(targetMs)
                                        currentPositionMs = targetMs
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                val rightTimeDisplay = if (showRemainingTime && durationMs > currentPositionMs) {
                                    "-${formatTimeMs(durationMs - currentPositionMs)}"
                                } else {
                                    formatTimeMs(durationMs)
                                }

                                Text(
                                    text = rightTimeDisplay,
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bottom Controls: Screen Lock (Left) + Auto Next Toggle (Right)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Screen Lock Button
                                IconButton(onClick = { isScreenLocked = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.LockOpen,
                                        contentDescription = "Lock Screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Auto Next Episode Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Auto Next Ep",
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 11.sp,
                                        fontFamily = PremiumBodyFont
                                    )
                                    Switch(
                                        checked = autoNextEpisode,
                                        onCheckedChange = { autoNextEpisode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF333344)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= BOTTOM SHEETS =================

        // 1. SERVER SELECTION SHEET
        if (showServerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showServerSheet = false },
                containerColor = Color(0xFF16161E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Select Anime Server",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a different server if video is lagging or failing to load.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        fontFamily = PremiumBodyFont
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Server Category Filter Tabs (SUB / DUB / HSUB)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("sub", "dub", "hsub").forEach { catTab ->
                            val isSelected = activeServerCategoryTab.equals(catTab, ignoreCase = true)
                            Surface(
                                onClick = { activeServerCategoryTab = catTab },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White else Color(0xFF222230),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                            ) {
                                Text(
                                    text = catTab.uppercase(),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = PremiumTitleFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isFetchingServers) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        }
                    } else {
                        val filteredServers = availableServers.filter {
                            it.type.equals(activeServerCategoryTab, ignoreCase = true)
                        }.ifEmpty { availableServers }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(filteredServers) { server ->
                                val isSelected = selectedServer?.linkId == server.linkId
                                Surface(
                                    onClick = { selectServer(server) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = server.name,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Server Type: ${server.type.uppercase()}",
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 11.sp,
                                                fontFamily = PremiumBodyFont
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
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

        // 2. EPISODE SELECTION SHEET
        if (showEpisodeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEpisodeSheet = false },
                containerColor = Color(0xFF16161E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Select Episode",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        items(episodes) { ep ->
                            val isSelected = ep.episodeNumber == currentEpisode.episodeNumber
                            Surface(
                                onClick = { selectEpisode(ep) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color(0xFF333344)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${ep.episodeNumber}",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = PremiumTitleFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title.ifEmpty { "Episode ${ep.episodeNumber}" },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontFamily = PremiumBodyFont,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Playing",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. SUBTITLE SELECTION SHEET
        if (showSubtitleSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubtitleSheet = false },
                containerColor = Color(0xFF16161E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Subtitles",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Turn off subtitles option
                    Surface(
                        onClick = {
                            isSubtitlesEnabled = false
                            selectedSubtitleTrack = null
                            showSubtitleSheet = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isSubtitlesEnabled) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                        border = BorderStroke(1.dp, if (!isSubtitlesEnabled) Color.White else Color(0xFF333344))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Subtitles Off",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = PremiumBodyFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isSubtitlesEnabled) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Off", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val tracks = currentStreamResult.subtitles
                    if (tracks.isEmpty()) {
                        Surface(
                            onClick = {
                                isSubtitlesEnabled = true
                                showSubtitleSheet = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSubtitlesEnabled) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                            border = BorderStroke(1.dp, if (isSubtitlesEnabled) Color.White else Color(0xFF333344))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Embedded Hardsub / Default Track",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = PremiumBodyFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSubtitlesEnabled) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(180.dp)
                        ) {
                            items(tracks) { track ->
                                val isSelected = isSubtitlesEnabled && selectedSubtitleTrack?.url == track.url
                                Surface(
                                    onClick = {
                                        isSubtitlesEnabled = true
                                        selectedSubtitleTrack = track
                                        showSubtitleSheet = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = track.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontFamily = PremiumBodyFont,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. SPEED SELECTION SHEET
        if (showSpeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                containerColor = Color(0xFF16161E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Playback Speed",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(speeds) { speed ->
                            val isSelected = currentSpeed == speed
                            Surface(
                                onClick = {
                                    changeSpeed(speed)
                                    showSpeedSheet = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = PremiumBodyFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. QUALITY SELECTION SHEET
        if (showQualitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showQualitySheet = false },
                containerColor = Color(0xFF16161E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Video Quality",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val qualities = if (availableQualities.isNotEmpty()) availableQualities else listOf("Auto")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(qualities) { quality ->
                            val isSelected = currentQuality == quality
                            Surface(
                                onClick = {
                                    changeQuality(quality)
                                    showQualitySheet = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF22222E),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333344))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = quality,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = PremiumBodyFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
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

@Composable
private fun CrunchyrollTimelineBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUserSeeking by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isUserSeeking) dragProgress else {
        if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
    }.coerceIn(0f, 1f)

    val bufferedProgress = if (durationMs > 0) {
        (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val orangeColor = Color(0xFFFF6400) // Crunchyroll signature orange
    val bufferedColor = Color(0x88FFFFFF) // Translucent white for buffered segment
    val trackBgColor = Color(0x33FFFFFF) // Track background

    Box(
        modifier = modifier
            .height(30.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onPress = { offset ->
                        if (durationMs <= 0) return@detectTapGestures
                        val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        isUserSeeking = true
                        dragProgress = newFraction
                        val seekTarget = (newFraction * durationMs).toLong()
                        onSeekTo(seekTarget)
                        tryAwaitRelease()
                        isUserSeeking = false
                    }
                )
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (durationMs <= 0) return@detectHorizontalDragGestures
                        isUserSeeking = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (durationMs <= 0) return@detectHorizontalDragGestures
                        val seekTarget = (dragProgress * durationMs).toLong()
                        onSeekTo(seekTarget)
                        isUserSeeking = false
                    },
                    onDragCancel = {
                        isUserSeeking = false
                    },
                    onHorizontalDrag = { change, _ ->
                        if (durationMs <= 0) return@detectHorizontalDragGestures
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val trackHeight = if (isUserSeeking) 5.dp.toPx() else 3.5.dp.toPx()
            val yOffset = size.height / 2f
            val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // 1. Inactive Track Background Line
            drawRoundRect(
                color = trackBgColor,
                topLeft = Offset(0f, yOffset - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = cornerRadius
            )

            // 2. Buffered Progress Track Line
            if (bufferedProgress > 0f) {
                drawRoundRect(
                    color = bufferedColor,
                    topLeft = Offset(0f, yOffset - trackHeight / 2f),
                    size = Size(size.width * bufferedProgress, trackHeight),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Active Crunchyroll Orange Progress Fill Line
            if (currentProgress > 0f) {
                drawRoundRect(
                    color = orangeColor,
                    topLeft = Offset(0f, yOffset - trackHeight / 2f),
                    size = Size(size.width * currentProgress, trackHeight),
                    cornerRadius = cornerRadius
                )
            }

            // 4. Thumb Knob / Indicator Dot
            val thumbX = size.width * currentProgress
            val thumbRadius = if (isUserSeeking) 7.5.dp.toPx() else 5.5.dp.toPx()

            // Outer subtle halo when dragging
            if (isUserSeeking) {
                drawCircle(
                    color = orangeColor.copy(alpha = 0.25f),
                    radius = thumbRadius + 4.dp.toPx(),
                    center = Offset(thumbX, yOffset)
                )
            }

            // Main Crunchyroll Orange Thumb Circle
            drawCircle(
                color = orangeColor,
                radius = thumbRadius,
                center = Offset(thumbX, yOffset)
            )

            // Inner white center dot
            drawCircle(
                color = Color.White,
                radius = thumbRadius * 0.45f,
                center = Offset(thumbX, yOffset)
            )
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
