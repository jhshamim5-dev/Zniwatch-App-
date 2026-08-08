package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnimeCardItem
import com.example.data.AnikotoRepository
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.ui.components.AnimeCardImage

@Composable
fun ViewMoreGridScreen(
  categoryTitle: String,
  onBack: () -> Unit,
  onAnimeClick: (AnimeCardItem) -> Unit = {}
) {
  var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "TV", "Movie"
  var isLoading by remember { mutableStateOf(true) }

  val displayList = remember { mutableStateListOf<AnimeCardItem>() }

  // Real backend call depending on categoryTitle
  LaunchedEffect(categoryTitle) {
    isLoading = true
    try {
      val resultList = when {
        categoryTitle.contains("Latest", ignoreCase = true) || categoryTitle.contains("Trending", ignoreCase = true) -> AnikotoRepository.getLatestEpisodes()
        categoryTitle.contains("Popular", ignoreCase = true) -> AnikotoRepository.getPopular()
        categoryTitle.contains("Top Rated", ignoreCase = true) -> AnikotoRepository.getTopRated()
        categoryTitle.contains("Upcoming", ignoreCase = true) -> AnikotoRepository.getUpcoming()
        categoryTitle.contains("Completed", ignoreCase = true) || categoryTitle.contains("Finished", ignoreCase = true) -> AnikotoRepository.getCompleted()
        categoryTitle.contains("Airing", ignoreCase = true) -> AnikotoRepository.getCurrentlyAiring()
        else -> AnikotoRepository.getByGenre(categoryTitle)
      }
      displayList.clear()
      displayList.addAll(resultList)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isLoading = false
    }
  }

  val filteredDisplayList = remember(displayList.toList(), selectedTypeFilter) {
    if (selectedTypeFilter == "All") displayList
    else displayList.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .background(Color.Black)
  ) {
    // Header Row with Back Button & Title
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
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
        text = categoryTitle,
        color = Color.White,
        fontSize = 22.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Top Filter Buttons (All, TV, Movie)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val filterOptions = listOf("All", "TV", "Movie")
      filterOptions.forEach { option ->
        val isSelected = selectedTypeFilter == option
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isSelected) Color.White else Color(0xFF222226),
          border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333333)),
          modifier = Modifier.clickable { selectedTypeFilter = option }
        ) {
          Text(
            text = option,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
      ) {
        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
      }
    } else {
      // Adaptive Cards Grid for varying screen widths
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(filteredDisplayList) { anime ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onAnimeClick(anime) }
          ) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF181818)
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                AnimeCardImage(
                  imageUrl = anime.imageUrl,
                  imageResId = anime.imageResId,
                  contentDescription = anime.title,
                  modifier = Modifier.fillMaxSize()
                )

                // Rating Tag
                Surface(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xAA000000)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Star,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                      text = anime.rating,
                      color = Color.White,
                      fontSize = 10.sp,
                      fontFamily = PremiumBodyFont,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = anime.title,
              color = Color.White,
              fontSize = 12.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
