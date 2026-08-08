package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        text = "Genres",
        color = Color.White,
        fontSize = 22.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    }

    // Grid of Liquid Glass Genre Buttons (Adaptive for all phone widths)
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 140.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(genres) { genre ->
        GenreCardButton(
          genre = genre,
          onGenreSelect = onGenreSelect
        )
      }
    }
  }
}
