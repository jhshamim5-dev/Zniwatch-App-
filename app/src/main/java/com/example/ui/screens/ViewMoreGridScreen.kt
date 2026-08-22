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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
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
  var selectedYearFilter by remember {
    mutableStateOf(if (categoryTitle.matches(Regex("\\d{4}"))) categoryTitle else "All")
  }
  var isYearDropdownExpanded by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(true) }
  var isLoadingMore by remember { mutableStateOf(false) }
  var currentPage by remember { mutableStateOf(1) }
  var hasMorePages by remember { mutableStateOf(true) }

  val coroutineScope = rememberCoroutineScope()
  val displayList = remember { mutableStateListOf<AnimeCardItem>() }

  val yearOptions = remember {
    listOf(
      "All Years", "2026", "2025", "2024", "2023", "2022", "2021", "2020",
      "2019", "2018", "2017", "2016", "2015", "2014", "2013", "2012", "2011",
      "2010", "2005", "2000"
    )
  }

  // Helper to load next page
  val loadNextPage: () -> Unit = {
    if (!isLoadingMore && hasMorePages && !isLoading) {
      isLoadingMore = true
      coroutineScope.launch {
        try {
          val nextPage = currentPage + 1
          val resultList = AnikotoRepository.getFilteredAnime(
            categoryTitle = categoryTitle,
            typeFilter = selectedTypeFilter,
            yearFilter = selectedYearFilter,
            page = nextPage
          )
          val existingTitles = displayList.map { it.title.lowercase().trim() }.toSet()
          val uniqueList = resultList.filter { it.title.lowercase().trim() !in existingTitles }

          if (uniqueList.isNotEmpty()) {
            displayList.addAll(uniqueList)
            currentPage = nextPage
          } else {
            hasMorePages = false
          }
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          isLoadingMore = false
        }
      }
    }
  }

  // Fetch initial page whenever categoryTitle, selectedTypeFilter, or selectedYearFilter changes
  LaunchedEffect(categoryTitle, selectedTypeFilter, selectedYearFilter) {
    isLoading = true
    currentPage = 1
    hasMorePages = true
    try {
      val resultList = AnikotoRepository.getFilteredAnime(
        categoryTitle = categoryTitle,
        typeFilter = selectedTypeFilter,
        yearFilter = selectedYearFilter,
        page = 1
      )
      displayList.clear()
      displayList.addAll(resultList)
      if (resultList.isEmpty() || resultList.size < 12) {
        hasMorePages = false
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isLoading = false
    }
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
        text = if (categoryTitle.matches(Regex("\\d{4}"))) "Anime of $categoryTitle" else categoryTitle,
        color = Color.White,
        fontSize = 22.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Top Filter Bar: Type Chips & Year Dropdown Button
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Type Filters (All, TV, Movie)
      val typeOptions = listOf("All", "TV", "Movie")
      items(typeOptions) { option ->
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

      // Year Dropdown Button
      item {
        Box {
          val isYearActive = selectedYearFilter != "All" && selectedYearFilter != "All Years"
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isYearActive) Color.White else Color(0xFF222226),
            border = BorderStroke(1.dp, if (isYearActive) Color.White else Color(0xFF333333)),
            modifier = Modifier.clickable { isYearDropdownExpanded = true }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = if (isYearActive) Color.Black else Color.White,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isYearActive) "Year: $selectedYearFilter" else "Year",
                color = if (isYearActive) Color.Black else Color.White,
                fontSize = 13.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Expand Years",
                tint = if (isYearActive) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          DropdownMenu(
            expanded = isYearDropdownExpanded,
            onDismissRequest = { isYearDropdownExpanded = false },
            modifier = Modifier
              .background(Color(0xFF1E1E22))
              .height(280.dp)
          ) {
            yearOptions.forEach { year ->
              val isSelectedYear = (selectedYearFilter == year) || (year == "All Years" && (selectedYearFilter == "All" || selectedYearFilter == "All Years"))
              DropdownMenuItem(
                text = {
                  Text(
                    text = year,
                    color = Color.White,
                    fontWeight = if (isSelectedYear) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    fontFamily = PremiumBodyFont
                  )
                },
                onClick = {
                  selectedYearFilter = if (year == "All Years") "All" else year
                  isYearDropdownExpanded = false
                },
                modifier = Modifier.background(if (isSelectedYear) Color(0xFF383842) else Color.Transparent)
              )
            }
          }
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
    } else if (displayList.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
      ) {
        Text(
          text = "No anime found for selected filters.",
          color = Color(0xFFAAAAAA),
          fontSize = 14.sp,
          fontFamily = PremiumBodyFont
        )
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
        itemsIndexed(displayList) { index, anime ->
          if (index >= displayList.size - 6 && !isLoadingMore && hasMorePages && !isLoading) {
            LaunchedEffect(index) {
              loadNextPage()
            }
          }
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

        // Bottom Loader / Load More Button
        if (isLoadingMore) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
            }
          }
        } else if (hasMorePages && displayList.isNotEmpty()) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF222226),
                border = BorderStroke(1.dp, Color(0xFF444444)),
                modifier = Modifier.clickable { loadNextPage() }
              ) {
                Text(
                  text = "Load More Anime",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontFamily = PremiumTitleFont,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

