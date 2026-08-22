package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GenreItem
import com.example.data.PremiumTitleFont
import com.example.ui.components.GenreCardButton

@Composable
fun AllGenresGridScreen(
  genres: List<GenreItem>,
  onGenreSelect: (String) -> Unit,
  onBack: () -> Unit
) {
  val years = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015", "2010", "2005", "2000")

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
        .padding(horizontal = 12.dp, vertical = 12.dp),
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
        text = "Genres & Years",
        color = Color.White,
        fontSize = 22.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    }

    // Grid of Liquid Glass Genre Buttons & Year Filter Row
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 140.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // Release Years Section Header & Row
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
          Text(
            text = "Browse by Release Year",
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(years) { yr ->
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF222226),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                modifier = Modifier.clickable { onGenreSelect(yr) }
              ) {
                Text(
                  text = yr,
                  color = Color.White,
                  fontSize = 13.sp,
                  fontFamily = PremiumTitleFont,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Anime Genres",
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold
          )
        }
      }

      items(genres) { genre ->
        GenreCardButton(
          genre = genre,
          onGenreSelect = onGenreSelect
        )
      }
    }
  }
}

