package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnimeCardItem
import com.example.data.AnimeHeroSlide
import com.example.data.AnikotoRepository
import com.example.data.CharacterItem
import com.example.data.GenreItem
import com.example.data.NavigationNavItem
import com.example.data.PremiumBodyFont
import com.example.data.StaffItem
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AllGenresGridScreen
import com.example.ui.screens.AnimeDetailsScreen
import com.example.ui.screens.AppVersionScreen
import com.example.ui.screens.CharacterDetailsScreen
import com.example.ui.screens.ClearCacheScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.EpisodeListScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StaffDetailsScreen
import com.example.ui.screens.SubtitleScreen
import com.example.ui.screens.ViewMoreGridScreen
import com.example.ui.theme.MyApplicationTheme

import kotlinx.coroutines.launch

import com.example.ui.SplashScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleAniListAuthIntent(intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }
        if (showSplash) {
          SplashScreen(
            onSplashComplete = { showSplash = false }
          )
        } else {
          BlackPageScreen()
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    handleAniListAuthIntent(intent)
  }

  private fun handleAniListAuthIntent(intent: android.content.Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme == "jhshamim.zniwatch" && uri.host == "anilist-auth") {
      val code = uri.getQueryParameter("code")
      val fragment = uri.fragment
      var accessToken: String? = null
      if (fragment != null && fragment.contains("access_token=")) {
        accessToken = fragment.split("&")
          .find { it.startsWith("access_token=") }
          ?.substringAfter("access_token=")
      }

      val prefs = getSharedPreferences("anilist_prefs", MODE_PRIVATE)

      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        var tokenToUse = accessToken
        if (tokenToUse.isNullOrEmpty() && !code.isNullOrEmpty()) {
          val clientId = BuildConfig.ANILIST_CLIENT_ID.ifEmpty { "47902" }
          val clientSecret = BuildConfig.ANILIST_CLIENT_SECRET.ifEmpty { "C2pdS6LzOeUNjZbQVlud0uQnucxkAiuPDIv4mBq8" }
          tokenToUse = com.example.data.AniListRepository.exchangeCodeForToken(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = "jhshamim.zniwatch://anilist-auth"
          )
        }

        if (!tokenToUse.isNullOrEmpty()) {
          val userProfile = com.example.data.AniListRepository.getAuthenticatedUser(tokenToUse)
          if (userProfile != null) {
            prefs.edit()
              .putBoolean("anilist_connected", true)
              .putString("anilist_access_token", tokenToUse)
              .putString("anilist_username", userProfile.name)
              .putString("anilist_avatar_url", userProfile.avatarUrl)
              .putString("anilist_banner_url", userProfile.bannerUrl)
              .putInt("anilist_anime_count", userProfile.animeCount)
              .putInt("anilist_episodes_watched", userProfile.episodesWatched)
              .putInt("anilist_user_id", userProfile.id)
              .putLong("anilist_minutes_watched", userProfile.minutesWatched)
              .apply()
          }
        }
      }
    }
  }
}

@Composable
fun BlackPageScreen() {
  var selectedIndex by remember { mutableIntStateOf(0) }
  var activeViewMoreCategory by remember { mutableStateOf<String?>(null) }
  var activeProfileSubScreen by remember { mutableStateOf<String?>(null) }
  var selectedAnimeForDetails by remember { mutableStateOf<AnimeCardItem?>(null) }
  var selectedAnimeForEpisodes by remember { mutableStateOf<AnimeCardItem?>(null) }
  var selectedCharacter by remember { mutableStateOf<CharacterItem?>(null) }
  var selectedStaff by remember { mutableStateOf<StaffItem?>(null) }

  val navItems = listOf(
    NavigationNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavigationNavItem("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    NavigationNavItem("Schedule", Icons.Filled.DateRange, Icons.Outlined.DateRange, "nav_schedule"),
    NavigationNavItem("My List", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_mylist"),
    NavigationNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
  )

  // Dynamic real data state variables initialized empty (NO mock data)
  var section1List by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var section2List by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var section3List by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var upcomingList by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var completedList by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var heroSlides by remember { mutableStateOf<List<AnimeHeroSlide>>(emptyList()) }
  var isHomeLoading by remember { mutableStateOf(true) }

  var genreList by remember {
    mutableStateOf(
      listOf(
        GenreItem("Action"), GenreItem("Adventure"),
        GenreItem("Boys Love"), GenreItem("Comedy"), GenreItem("Demons"),
        GenreItem("Drama"), GenreItem("Ecchi"), GenreItem("Fantasy"), GenreItem("Girls Love"),
        GenreItem("Gourmet"), GenreItem("Harem"), GenreItem("Historical"), GenreItem("Horror"),
        GenreItem("Isekai"), GenreItem("Magic"), GenreItem("Martial Arts"),
        GenreItem("Mecha"), GenreItem("Military"), GenreItem("Music"), GenreItem("Mystery"),
        GenreItem("Parody"), GenreItem("Psychological"), GenreItem("Romance"), GenreItem("Samurai"),
        GenreItem("School"), GenreItem("Sci-Fi"), GenreItem("Slice of Life"), GenreItem("Space"),
        GenreItem("Sports"), GenreItem("Super Power"), GenreItem("Supernatural"), GenreItem("Suspense"),
        GenreItem("Thriller")
      )
    )
  }

  // Fetch real data for Home Page on initialization
  LaunchedEffect(Unit) {
    isHomeLoading = true
    try {
      // 1. Hero slides from https://anikoto.cz/status/currently-airing (first 5)
      val currentlyAiring = AnikotoRepository.getCurrentlyAiring()
      if (currentlyAiring.isNotEmpty()) {
        val slides = currentlyAiring.take(5).map { item ->
          AnimeHeroSlide(
            id = item.id,
            title = item.title,
            rating = item.rating,
            tags = "${item.type} • Airing",
            description = item.title,
            imageUrl = item.imageUrl
          )
        }
        if (slides.isNotEmpty()) {
          heroSlides = slides
        }
      }

      // 2. Latest Episodes
      val latest = AnikotoRepository.getLatestEpisodes()
      if (latest.isNotEmpty()) {
        section1List = latest
      }

      // 3. Popular Anime
      val popular = AnikotoRepository.getPopular()
      if (popular.isNotEmpty()) {
        section2List = popular
      }

      // 4. Top Rated
      val topRated = AnikotoRepository.getTopRated()
      if (topRated.isNotEmpty()) {
        section3List = topRated
      }

      // 5. Upcoming Anime (https://anikoto.cz/status/not-yet-aired)
      val upcoming = AnikotoRepository.getUpcoming()
      if (upcoming.isNotEmpty()) {
        upcomingList = upcoming
      }

      // 6. Completed (https://anikoto.cz/status/finished-airing)
      val completed = AnikotoRepository.getCompleted()
      if (completed.isNotEmpty()) {
        completedList = completed
      }

      // Fetch dynamic real backend image URLs for initial home genre buttons
      val initialHomeGenres = genreList.take(8).map { genre ->
        val bgUrl = AnikotoRepository.getGenreImage(genre.name)
        genre.copy(imageUrl = bgUrl)
      }
      genreList = initialHomeGenres + genreList.drop(8)

    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isHomeLoading = false
    }
  }

  if (activeProfileSubScreen != null) {
    BackHandler { activeProfileSubScreen = null }
    when (activeProfileSubScreen) {
      "account" -> AccountScreen(onBack = { activeProfileSubScreen = null })
      "download" -> DownloadScreen(onBack = { activeProfileSubScreen = null })
      "community" -> CommunityScreen(onBack = { activeProfileSubScreen = null })
      "subtitle" -> SubtitleScreen(onBack = { activeProfileSubScreen = null })
      "clear_cache" -> ClearCacheScreen(onBack = { activeProfileSubScreen = null })
      "app_version" -> AppVersionScreen(onBack = { activeProfileSubScreen = null })
    }
  } else if (selectedAnimeForEpisodes != null) {
    BackHandler { selectedAnimeForEpisodes = null }
    key(selectedAnimeForEpisodes!!.id, selectedAnimeForEpisodes!!.title) {
      EpisodeListScreen(
        anime = selectedAnimeForEpisodes!!,
        onBack = { selectedAnimeForEpisodes = null }
      )
    }
  } else if (selectedCharacter != null) {
    BackHandler { selectedCharacter = null }
    key(selectedCharacter!!.id, selectedCharacter!!.name) {
      CharacterDetailsScreen(
        character = selectedCharacter!!,
        onBack = { selectedCharacter = null }
      )
    }
  } else if (selectedStaff != null) {
    BackHandler { selectedStaff = null }
    key(selectedStaff!!.id, selectedStaff!!.name) {
      StaffDetailsScreen(
        staff = selectedStaff!!,
        onBack = { selectedStaff = null }
      )
    }
  } else if (selectedAnimeForDetails != null) {
    BackHandler { selectedAnimeForDetails = null }
    key(selectedAnimeForDetails!!.id, selectedAnimeForDetails!!.title, selectedAnimeForDetails!!.imageUrl) {
      AnimeDetailsScreen(
        anime = selectedAnimeForDetails!!,
        onBack = { selectedAnimeForDetails = null },
        onAnimeClick = { selectedAnimeForDetails = it },
        onCharacterClick = { selectedCharacter = it },
        onStaffClick = { selectedStaff = it },
        onWatchNowClick = { selectedAnimeForEpisodes = it }
      )
    }
  } else {
    if (activeViewMoreCategory != null) {
      BackHandler { activeViewMoreCategory = null }
    } else if (selectedIndex != 0) {
      BackHandler { selectedIndex = 0 }
    }
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Black,
      bottomBar = {
        NavigationBar(
          containerColor = Color(0xFF101010),
          contentColor = Color.White
        ) {
          navItems.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index && activeViewMoreCategory == null
            NavigationBarItem(
              selected = isSelected,
              onClick = {
                selectedIndex = index
                activeViewMoreCategory = null
              },
              icon = {
                Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.title
                )
              },
              label = {
                Text(
                  text = item.title,
                  fontFamily = PremiumBodyFont,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 10.sp,
                  color = if (isSelected) Color.White else Color(0xFF888888)
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color(0xFF888888),
                indicatorColor = Color.White,
                selectedTextColor = Color.White,
                unselectedTextColor = Color(0xFF888888)
              ),
              modifier = Modifier.testTag(item.tag)
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black)
          .padding(bottom = innerPadding.calculateBottomPadding())
      ) {
        if (activeViewMoreCategory != null) {
          if (activeViewMoreCategory == "Genres") {
            AllGenresGridScreen(
              genres = genreList,
              onGenreSelect = { genreName -> activeViewMoreCategory = genreName },
              onBack = { activeViewMoreCategory = null }
            )
          } else {
            ViewMoreGridScreen(
              categoryTitle = activeViewMoreCategory!!,
              onBack = { activeViewMoreCategory = null },
              onAnimeClick = { anime -> selectedAnimeForDetails = anime }
            )
          }
        } else {
          when (selectedIndex) {
            0 -> {
              HomeScreen(
                isHomeLoading = isHomeLoading,
                heroSlides = heroSlides,
                section1List = section1List,
                section2List = section2List,
                section3List = section3List,
                genreList = genreList,
                upcomingList = upcomingList,
                completedList = completedList,
                onViewMoreCategory = { category -> activeViewMoreCategory = category },
                onAnimeClick = { anime -> selectedAnimeForDetails = anime },
                onHeroClick = { slide ->
                  selectedAnimeForDetails = AnimeCardItem(
                    id = slide.id,
                    title = slide.title,
                    rating = slide.rating,
                    imageUrl = slide.imageUrl
                  )
                }
              )
            }
            1 -> {
              SearchScreen(
                onAnimeClick = { anime -> selectedAnimeForDetails = anime }
              )
            }
            2 -> {
              ScheduleScreen(
                onAnimeClick = { anime -> selectedAnimeForDetails = anime }
              )
            }
            3 -> {
              MyListScreen(
                onAnimeClick = { anime -> selectedAnimeForDetails = anime },
                onPlayEpisodeClick = { anime -> selectedAnimeForEpisodes = anime },
                onBrowseClick = { selectedIndex = 1 }
              )
            }
            4 -> {
              ProfileScreen(
                onAccountClick = { activeProfileSubScreen = "account" },
                onDownloadClick = { activeProfileSubScreen = "download" },
                onCommunityClick = { activeProfileSubScreen = "community" },
                onSubtitleClick = { activeProfileSubScreen = "subtitle" },
                onClearCacheClick = { activeProfileSubScreen = "clear_cache" },
                onAppVersionClick = { activeProfileSubScreen = "app_version" }
              )
            }
          }
        }
      }
    }
  }
}
