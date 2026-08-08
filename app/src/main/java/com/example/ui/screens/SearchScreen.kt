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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
  onAnimeClick: (AnimeCardItem) -> Unit = {}
) {
  var searchQuery by remember { mutableStateOf("") }
  var isFilterOpen by remember { mutableStateOf(false) }
  var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "TV", "Movie"
  var isSearching by remember { mutableStateOf(false) }

  var searchResults by remember { mutableStateOf<List<AnimeCardItem>>(emptyList()) }

  // Perform backend search when searchQuery changes
  LaunchedEffect(searchQuery) {
    isSearching = true
    try {
      if (searchQuery.isBlank()) {
        // Real popular anime from backend when query is empty
        val popular = AnikotoRepository.getPopular()
        searchResults = popular
      } else {
        delay(350) // Debounce typing
        val results = AnikotoRepository.searchAnime(searchQuery)
        searchResults = results
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isSearching = false
    }
  }

  val filteredList = remember(searchResults, selectedTypeFilter) {
    searchResults.filter { item ->
      selectedTypeFilter == "All" || item.type.equals(selectedTypeFilter, ignoreCase = true)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .background(Color.Black)
  ) {
    // Search Bar & Filter Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Input Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            text = "Search anime...",
            color = Color(0xFF777777),
            fontSize = 14.sp,
            fontFamily = PremiumBodyFont
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = Color.White
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear",
                tint = Color(0xFFAAAAAA)
              )
            }
          }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF161618),
          unfocusedContainerColor = Color(0xFF161618),
          focusedIndicatorColor = Color.White,
          unfocusedIndicatorColor = Color(0xFF333333),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.weight(1f)
      )

      // Filter Button
      Surface(
        modifier = Modifier
          .size(48.dp)
          .clickable { isFilterOpen = !isFilterOpen },
        shape = RoundedCornerShape(14.dp),
        color = if (isFilterOpen || selectedTypeFilter != "All") Color.White else Color(0xFF1E1E24),
        border = BorderStroke(1.dp, if (isFilterOpen) Color.White else Color(0xFF333333))
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize()
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Filter",
            tint = if (isFilterOpen || selectedTypeFilter != "All") Color.Black else Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // Filter Chips Expandable Panel
    if (isFilterOpen) {
      val options = listOf("All", "TV", "Movie")
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        items(options) { option ->
          val isSelected = selectedTypeFilter == option
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) Color.White else Color(0xFF222226),
            modifier = Modifier.clickable { selectedTypeFilter = option }
          ) {
            Text(
              text = option,
              color = if (isSelected) Color.Black else Color.White,
              fontSize = 12.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    if (isSearching) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
      ) {
        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
      }
    } else {
      // Adaptive Cards Grid Results
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(filteredList) { anime ->
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
