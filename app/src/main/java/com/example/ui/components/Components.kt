package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.data.AniListRepository
import com.example.data.LocalMyListManager
import coil.compose.AsyncImage
import com.example.data.AnimeCardItem
import com.example.data.AnimeHeroSlide
import com.example.data.AnikotoRepository
import com.example.data.GenreItem
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun AnimeCardImage(
  imageUrl: String,
  imageResId: Int,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop
) {
  if (imageUrl.isNotEmpty()) {
    AsyncImage(
      model = imageUrl,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = modifier
    )
  } else if (imageResId != 0) {
    Image(
      painter = painterResource(id = imageResId),
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = modifier
    )
  } else {
    Box(
      modifier = modifier.background(Color(0xFF222226))
    )
  }
}

@Composable
fun GenreCardButton(
  genre: GenreItem,
  onGenreSelect: (String) -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("genre_images_cache", Context.MODE_PRIVATE) }
  var bgUrl by remember(genre.name) {
    mutableStateOf(genre.imageUrl.ifEmpty { prefs.getString(genre.name, "") ?: "" })
  }

  LaunchedEffect(genre.name) {
    if (bgUrl.isEmpty()) {
      try {
        val fetchedUrl = AnikotoRepository.getGenreImage(genre.name)
        if (fetchedUrl.isNotEmpty()) {
          bgUrl = fetchedUrl
          prefs.edit().putString(genre.name, fetchedUrl).apply()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(88.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable { onGenreSelect(genre.name) },
    contentAlignment = Alignment.Center
  ) {
    AnimeCardImage(
      imageUrl = bgUrl,
      imageResId = genre.imageResId,
      contentDescription = genre.name,
      modifier = Modifier.fillMaxSize()
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x33000000),
              Color(0xBB000000)
            )
          )
        )
        .border(
          BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x55FFFFFF), Color(0x11FFFFFF)))),
          RoundedCornerShape(16.dp)
        )
    )

    Text(
      text = genre.name,
      color = Color.White,
      fontSize = 15.sp,
      fontFamily = PremiumTitleFont,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 8.dp)
    )
  }
}

@Composable
fun AnimeHorizontalSection(
  title: String,
  items: List<AnimeCardItem>,
  onViewMore: (() -> Unit)? = null,
  showViewMore: Boolean = true,
  onAnimeClick: (AnimeCardItem) -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 20.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )

      if (showViewMore && onViewMore != null) {
        Text(
          text = "View More",
          color = Color.White,
          fontSize = 13.sp,
          fontFamily = PremiumTitleFont,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onViewMore() }
        )
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(items.size) { index ->
        val anime = items[index]
        Column(
          modifier = Modifier
            .width(112.dp)
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
                  .padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xAA000000)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
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
            fontSize = 13.sp,
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

@Composable
fun GenreLiquidGlassSection(
  title: String,
  genres: List<GenreItem>,
  onGenreSelect: (String) -> Unit,
  onViewMoreGenres: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 20.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )

      Text(
        text = "View More",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { onViewMoreGenres() }
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(genres) { genre ->
        Box(
          modifier = Modifier
            .width(135.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onGenreSelect(genre.name) },
          contentAlignment = Alignment.Center
        ) {
          AnimeCardImage(
            imageUrl = genre.imageUrl,
            imageResId = genre.imageResId,
            contentDescription = genre.name,
            modifier = Modifier.fillMaxSize()
          )

          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color(0x44000000),
                    Color(0x99000000)
                  )
                )
              )
              .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF)))),
                RoundedCornerShape(16.dp)
              )
          )

          Text(
            text = genre.name,
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
      }
    }
  }
}

@Composable
fun UpcomingLandscapeSection(
  title: String,
  items: List<AnimeCardItem>,
  onViewMore: () -> Unit,
  onAnimeClick: (AnimeCardItem) -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 20.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )

      Text(
        text = "View More",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { onViewMore() }
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(items) { anime ->
        Box(
          modifier = Modifier
            .width(220.dp)
            .height(125.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onAnimeClick(anime) }
        ) {
          AnimeCardImage(
            imageUrl = anime.imageUrl,
            imageResId = anime.imageResId,
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize()
          )

          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
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
              text = anime.title,
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
                  imageVector = Icons.Filled.Star,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
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
      }
    }
  }
}

@Composable
fun HeroBannerSlider(
  slides: List<AnimeHeroSlide>,
  onSlideClick: (AnimeHeroSlide) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val prefs = remember { context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE) }
  val pagerState = rememberPagerState(pageCount = { slides.size })

  LaunchedEffect(pagerState, slides.size) {
    if (slides.isNotEmpty()) {
      while (true) {
        delay(5000)
        val nextPage = (pagerState.currentPage + 1) % slides.size
        pagerState.animateScrollToPage(nextPage)
      }
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "banner_motion")
  val motionScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(8000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "motion_scale"
  )
  val motionTranslateX by infiniteTransition.animateFloat(
    initialValue = -12f,
    targetValue = 12f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "motion_translate_x"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(480.dp)
      .clipToBounds()
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize()
    ) { page ->
      val slide = slides[page]
      var isSaved by remember(slide.id, slide.title) {
        mutableStateOf(LocalMyListManager.isSaved(context, slide.id, slide.title))
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .clipToBounds()
          .clickable { onSlideClick(slide) }
      ) {
        AnimeCardImage(
          imageUrl = slide.imageUrl,
          imageResId = slide.imageResId,
          contentDescription = slide.title,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = motionScale
              scaleY = motionScale
              translationX = motionTranslateX
            }
        )

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0x44000000),
                  Color(0xFF000000)
                )
              )
            )
        )

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
          verticalArrangement = Arrangement.Bottom
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0x99000000)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Rating",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = slide.rating,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = PremiumBodyFont,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = slide.title,
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { onSlideClick(slide) },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
              ),
              modifier = Modifier
                .height(42.dp)
                .weight(1f)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = "Play",
                  tint = Color.Black,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Play",
                  color = Color.Black,
                  fontSize = 14.sp,
                  fontFamily = PremiumTitleFont,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Button(
              onClick = {
                val itemToSave = AnimeCardItem(
                  id = slide.id,
                  title = slide.title,
                  rating = slide.rating,
                  imageUrl = slide.imageUrl
                )
                val saved = LocalMyListManager.toggleSaved(context, itemToSave)
                isSaved = saved
                val connectedNow = prefs.getBoolean("anilist_connected", false)
                val token = prefs.getString("anilist_access_token", "") ?: ""
                if (connectedNow && token.isNotEmpty()) {
                  scope.launch {
                    try {
                      if (saved) {
                        AniListRepository.saveMediaListEntry(token, slide.id, slide.title, "PLANNING")
                        AniListRepository.toggleFavourite(token, slide.id, slide.title)
                        Toast.makeText(context, "Added to My List & AniList", Toast.LENGTH_SHORT).show()
                      } else {
                        AniListRepository.deleteMediaListEntry(token, slide.id, slide.title)
                        Toast.makeText(context, "Removed from My List & AniList", Toast.LENGTH_SHORT).show()
                      }
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                  }
                } else {
                  if (saved) {
                    Toast.makeText(context, "Added to My List", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Removed from My List", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isSaved) Color(0xFF222228) else Color(0x33FFFFFF),
                contentColor = Color.White
              ),
              border = BorderStroke(1.dp, if (isSaved) Color(0xFF444450) else Color(0x66FFFFFF)),
              modifier = Modifier
                .height(42.dp)
                .weight(1f)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isSaved) Icons.Filled.Check else Icons.Filled.Add,
                  contentDescription = "My List",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isSaved) "In List" else "My List",
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
    }
  }
}
