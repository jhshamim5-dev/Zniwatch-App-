package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.AnimeCardItem
import com.example.data.AnimeHeroSlide
import com.example.data.AppReleaseInfo
import com.example.data.GenreItem
import com.example.data.UpdateManager
import com.example.ui.components.AnimeHorizontalSection
import com.example.ui.components.AppPullToRefreshLayout
import com.example.ui.components.GenreLiquidGlassSection
import com.example.ui.components.HeroBannerSlider
import com.example.ui.components.UpcomingLandscapeSection
import com.example.ui.components.UpdateDialog

@Composable
fun HomeScreen(
  isHomeLoading: Boolean,
  heroSlides: List<AnimeHeroSlide>,
  section1List: List<AnimeCardItem>,
  section2List: List<AnimeCardItem>,
  section3List: List<AnimeCardItem>,
  genreList: List<GenreItem>,
  upcomingList: List<AnimeCardItem>,
  completedList: List<AnimeCardItem> = emptyList(),
  isRefreshing: Boolean = false,
  onRefresh: () -> Unit = {},
  onViewMoreCategory: (String) -> Unit,
  onAnimeClick: (AnimeCardItem) -> Unit,
  onHeroClick: (AnimeHeroSlide) -> Unit = {}
) {
  val context = LocalContext.current
  var availableUpdate by remember { mutableStateOf<AppReleaseInfo?>(null) }

  LaunchedEffect(Unit) {
    try {
      val release = UpdateManager.checkForUpdate(context)
      if (release != null && release.isNewerVersion) {
        if (!UpdateManager.isHomePopupDismissed(context, release.tagName)) {
          availableUpdate = release
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  if (availableUpdate != null) {
    UpdateDialog(
      releaseInfo = availableUpdate!!,
      onDismiss = {
        UpdateManager.dismissHomePopup(context, availableUpdate!!.tagName)
        availableUpdate = null
      }
    )
  }

  if (isHomeLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
    }
  } else {
    AppPullToRefreshLayout(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.fillMaxSize()
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
      if (heroSlides.isNotEmpty()) {
        item {
          HeroBannerSlider(
            slides = heroSlides,
            onSlideClick = onHeroClick
          )
        }
      }

      if (section1List.isNotEmpty()) {
        item {
          AnimeHorizontalSection(
            title = "Latest Episodes",
            items = section1List,
            onViewMore = { onViewMoreCategory("Latest Episodes") },
            onAnimeClick = onAnimeClick
          )
        }
      }

      if (section2List.isNotEmpty()) {
        item {
          AnimeHorizontalSection(
            title = "Popular Anime",
            items = section2List,
            onViewMore = { onViewMoreCategory("Popular Anime") },
            onAnimeClick = onAnimeClick
          )
        }
      }

      if (section3List.isNotEmpty()) {
        item {
          AnimeHorizontalSection(
            title = "Top Rated",
            items = section3List,
            onViewMore = { onViewMoreCategory("Top Rated") },
            onAnimeClick = onAnimeClick
          )
        }
      }

      item {
        GenreLiquidGlassSection(
          title = "Genres",
          genres = genreList,
          onGenreSelect = { onViewMoreCategory(it) },
          onViewMoreGenres = { onViewMoreCategory("Genres") }
        )
      }

      if (upcomingList.isNotEmpty()) {
        item {
          UpcomingLandscapeSection(
            title = "Upcoming Anime",
            items = upcomingList,
            onViewMore = { onViewMoreCategory("Upcoming Anime") },
            onAnimeClick = onAnimeClick
          )
        }
      }

      if (completedList.isNotEmpty()) {
        item {
          AnimeHorizontalSection(
            title = "Completed",
            items = completedList,
            onViewMore = { onViewMoreCategory("Completed") },
            onAnimeClick = onAnimeClick
          )
        }
      }
      }
    }
  }
}
