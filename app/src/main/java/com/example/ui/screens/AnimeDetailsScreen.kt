package com.example.ui.screens

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AniListRepository
import com.example.data.AnimeCardItem
import com.example.data.AnimeDetailResult
import com.example.data.AnikotoRepository
import com.example.data.CharacterItem
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.data.StaffItem
import com.example.ui.components.AnimeCardImage
import com.example.ui.components.AnimeHorizontalSection
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.LocalMyListManager

@Composable
fun AnimeDetailsScreen(
  anime: AnimeCardItem,
  onBack: () -> Unit,
  onAnimeClick: (AnimeCardItem) -> Unit = {},
  onCharacterClick: (CharacterItem) -> Unit = {},
  onStaffClick: (StaffItem) -> Unit = {},
  onWatchNowClick: (AnimeCardItem) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val prefs = remember { context.getSharedPreferences("anilist_prefs", android.content.Context.MODE_PRIVATE) }

  var isSavedInList by remember(anime.id, anime.title) {
    mutableStateOf(LocalMyListManager.isSaved(context, anime.id, anime.title))
  }
  var detailResult by remember(anime.id, anime.title, anime.imageUrl) {
    mutableStateOf(
      AnimeDetailResult(
        id = anime.id,
        title = anime.title,
        rating = anime.rating,
        imageUrl = anime.imageUrl,
        bannerUrl = anime.imageUrl,
        genres = "Anime",
        status = "Finished Airing"
      )
    )
  }
  var isLoading by remember(anime.id, anime.title, anime.imageUrl) { mutableStateOf(true) }

  var relatedList by remember(anime.id) { mutableStateOf<List<AnimeCardItem>>(emptyList()) }
  var recommendedList by remember(anime.id) { mutableStateOf<List<AnimeCardItem>>(emptyList()) }

  LaunchedEffect(anime.id, anime.title, anime.imageUrl) {
    try {
      val result = AniListRepository.getAnimeDetails(
        title = anime.title,
        defaultId = anime.id,
        defaultImg = anime.imageUrl,
        defaultRating = anime.rating
      )
      detailResult = result

      // Load related and recommended from custom AniList repository
      val rels = if (result.relations.isNotEmpty()) result.relations else AniListRepository.getRelatedAnime(anime.id, anime.title)
      val recs = if (result.recommendations.isNotEmpty()) result.recommendations else AniListRepository.getRecommendedAnime(anime.id, anime.title)
      relatedList = rels
      recommendedList = recs
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isLoading = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // Scrollable Main Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 100.dp)
    ) {
      // Big Banner Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp)
      ) {
        // Banner Background Image
        AnimeCardImage(
          imageUrl = detailResult.bannerUrl.ifEmpty { detailResult.imageUrl },
          imageResId = anime.imageResId,
          contentDescription = detailResult.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )

        // Dark Gradient Overlay inside Banner
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(0x99000000),
                  Color(0x33000000),
                  Color(0xFF000000)
                )
              )
            )
        )

        // Small Image Card, Rating, and Genre inside Banner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomStart)
            .padding(horizontal = 16.dp, vertical = 16.dp),
          verticalAlignment = Alignment.Bottom
        ) {
          // Small Poster Image Card
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E22),
            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
            shadowElevation = 8.dp,
            modifier = Modifier
              .width(105.dp)
              .height(150.dp)
          ) {
            AnimeCardImage(
              imageUrl = detailResult.imageUrl,
              imageResId = anime.imageResId,
              contentDescription = detailResult.title,
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          // Details inside Banner (Title, Rating, Genre, Status)
          Column(
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = detailResult.title,
              color = Color.White,
              fontSize = 20.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating inside Banner
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Rating",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = detailResult.rating,
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

            Spacer(modifier = Modifier.height(6.dp))

            // Genre inside Banner
            Text(
              text = detailResult.genres,
              color = Color(0xFFDDDDDD),
              fontSize = 13.sp,
              fontFamily = PremiumBodyFont,
              fontWeight = FontWeight.Medium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            if (detailResult.status.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x44000000),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
              ) {
                Text(
                  text = "Status: ${detailResult.status}",
                  color = Color(0xFFBBBBBB),
                  fontSize = 11.sp,
                  fontFamily = PremiumBodyFont,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

      // 1. Add to My List Button
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Button(
          onClick = {
            val itemToSave = AnimeCardItem(
              id = anime.id,
              title = anime.title,
              rating = detailResult.rating,
              imageUrl = detailResult.imageUrl.ifEmpty { anime.imageUrl }
            )
            isSavedInList = LocalMyListManager.toggleSaved(context, itemToSave)
            
            val isConnected = prefs.getBoolean("anilist_connected", false)
            val token = prefs.getString("anilist_access_token", "") ?: ""
            if (isConnected && token.isNotEmpty()) {
              scope.launch {
                try {
                  if (isSavedInList) {
                    AniListRepository.saveMediaListEntry(token, anime.id, anime.title, "PLANNING")
                    AniListRepository.toggleFavourite(token, anime.id, anime.title)
                    android.widget.Toast.makeText(context, "Added to My List & AniList", android.widget.Toast.LENGTH_SHORT).show()
                  } else {
                    AniListRepository.deleteMediaListEntry(token, anime.id, anime.title)
                    android.widget.Toast.makeText(context, "Removed from My List & AniList", android.widget.Toast.LENGTH_SHORT).show()
                  }
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              }
            } else {
              if (isSavedInList) {
                android.widget.Toast.makeText(context, "Added to My List", android.widget.Toast.LENGTH_SHORT).show()
              } else {
                android.widget.Toast.makeText(context, "Removed from My List", android.widget.Toast.LENGTH_SHORT).show()
              }
            }
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isSavedInList) Color(0xFF222228) else Color.White,
            contentColor = if (isSavedInList) Color.White else Color.Black
          ),
          border = if (isSavedInList) BorderStroke(1.dp, Color(0xFF444450)) else null,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("add_to_my_list_button")
        ) {
          Icon(
            imageVector = if (isSavedInList) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = "My List",
            tint = if (isSavedInList) Color.White else Color.Black,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isSavedInList) "IN LIST" else "ADD TO MY LIST",
            color = if (isSavedInList) Color.White else Color.Black,
            fontSize = 14.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }

        // 2. Next Episode Countdown Box (If airing and nextAiringAtTimestamp is valid)
        val timestamp = detailResult.nextAiringAtTimestamp
        if (timestamp != null) {
          var remainingSec by remember(timestamp) {
            mutableLongStateOf(maxOf(0L, timestamp - System.currentTimeMillis() / 1000))
          }

          LaunchedEffect(timestamp) {
            while (true) {
              val now = System.currentTimeMillis() / 1000
              val diff = timestamp - now
              if (diff <= 0) {
                remainingSec = 0L
                break
              } else {
                remainingSec = diff
              }
              delay(1000)
            }
          }

          if (remainingSec > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFF141418),
              border = BorderStroke(1.dp, Color(0xFF282832)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "NEXT EPISODE COUNTDOWN",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontFamily = PremiumTitleFont,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    )
                  }

                  if (detailResult.nextAiringEpisode != null) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color.White
                    ) {
                      Text(
                        text = "EPISODE ${detailResult.nextAiringEpisode}",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val days = remainingSec / 86400
                val hours = (remainingSec % 86400) / 3600
                val minutes = (remainingSec % 3600) / 60
                val seconds = remainingSec % 60

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CountdownTile(value = String.format("%02d", days), label = "DAYS")
                  Text(":", color = Color(0xFF666666), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                  CountdownTile(value = String.format("%02d", hours), label = "HOURS")
                  Text(":", color = Color(0xFF666666), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                  CountdownTile(value = String.format("%02d", minutes), label = "MINS")
                  Text(":", color = Color(0xFF666666), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                  CountdownTile(value = String.format("%02d", seconds), label = "SECS")
                }
              }
            }
          }
        }

        // 3. Description Section with More / Less toggle
        val descText = detailResult.description.ifEmpty { "No synopsis available for this title." }
        var isExpanded by remember { mutableStateOf(false) }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "Synopsis",
          color = Color.White,
          fontSize = 18.sp,
          fontFamily = PremiumTitleFont,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = descText,
          color = Color(0xFFCCCCCC),
          fontSize = 13.sp,
          fontFamily = PremiumBodyFont,
          lineHeight = 19.sp,
          maxLines = if (isExpanded) Int.MAX_VALUE else 3,
          overflow = TextOverflow.Ellipsis
        )

        if (descText.length > 120) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { isExpanded = !isExpanded }
              .padding(vertical = 4.dp)
          ) {
            Text(
              text = if (isExpanded) "Read Less" else "Read More",
              color = Color.White,
              fontSize = 12.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // 4. Detail Info Boxes Grid
        Spacer(modifier = Modifier.height(24.dp))
        Text(
          text = "Information",
          color = Color.White,
          fontSize = 18.sp,
          fontFamily = PremiumTitleFont,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DetailInfoCard(title = "Studio", value = detailResult.studio, modifier = Modifier.weight(1f))
            DetailInfoCard(title = "Producers", value = detailResult.producers, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DetailInfoCard(title = "Episodes", value = "${detailResult.episodes} (${detailResult.format})", modifier = Modifier.weight(1f))
            DetailInfoCard(title = "Duration", value = detailResult.duration, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DetailInfoCard(title = "Airing Period", value = detailResult.airingPeriod, modifier = Modifier.weight(1f))
            DetailInfoCard(title = "Season", value = detailResult.season, modifier = Modifier.weight(1f))
          }
        }

        // 5. YouTube Trailer Player Section
        val ytId = detailResult.trailerYoutubeId
        if (!ytId.isNullOrEmpty()) {
          Spacer(modifier = Modifier.height(24.dp))
          Text(
            text = "Trailer",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(10.dp))
          YouTubeTrailerPlayer(youtubeId = ytId)
        }

        // 6. Characters Section (Circle cards)
        if (detailResult.characters.isNotEmpty()) {
          CharactersSection(
            characters = detailResult.characters,
            onCharacterClick = onCharacterClick
          )
        }

        // 7. Staff Section (Small rounded cards)
        if (detailResult.staff.isNotEmpty()) {
          StaffSection(
            staff = detailResult.staff,
            onStaffClick = onStaffClick
          )
        }

        // 8. Related Anime Section (Homepage card format, custom backend API data)
        if (relatedList.isNotEmpty()) {
          AnimeHorizontalSection(
            title = "Related Anime",
            items = relatedList,
            showViewMore = false,
            onAnimeClick = { onAnimeClick(it) }
          )
        }

        // 9. Recommended Anime Section (Homepage card format, custom backend API data)
        if (recommendedList.isNotEmpty()) {
          AnimeHorizontalSection(
            title = "Recommended Anime",
            items = recommendedList,
            showViewMore = false,
            onAnimeClick = { onAnimeClick(it) }
          )
        }
      }
    }

    // Top-Left Back Button on Top
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .statusBarsPadding()
        .padding(start = 12.dp, top = 8.dp)
        .size(42.dp)
        .clip(CircleShape)
        .background(Color(0x66000000))
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = Color.White
      )
    }

    // Bottom Floating Navbar with Watch Now Button (White Button, Black Text)
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF141418),
        border = BorderStroke(1.dp, Color(0xFF282830)),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { onWatchNowClick(anime) },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("watch_now_button")
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = "Watch Now",
              tint = Color.Black,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "WATCH NOW",
              color = Color.Black,
              fontSize = 15.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CountdownTile(value: String, label: String) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1E1E24),
    border = BorderStroke(1.dp, Color(0xFF333340))
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Text(
        text = value,
        color = Color.White,
        fontSize = 18.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = label,
        color = Color(0xFF888888),
        fontSize = 9.sp,
        fontFamily = PremiumBodyFont,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun DetailInfoCard(title: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF121215),
    border = BorderStroke(1.dp, Color(0xFF24242C)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title.uppercase(),
        color = Color(0xFF777788),
        fontSize = 10.sp,
        fontFamily = PremiumBodyFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun YouTubeTrailerPlayer(youtubeId: String) {
  val embedHtml = remember(youtubeId) {
    """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body, html { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
        .iframe-container { position: relative; width: 100%; height: 100%; }
        iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
      </style>
    </head>
    <body>
      <div class="iframe-container">
        <iframe src="https://www.youtube-nocookie.com/embed/$youtubeId?enablejsapi=1&autoplay=0&controls=1&modestbranding=1&rel=0&playsinline=1" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
        </iframe>
      </div>
    </body>
    </html>
    """.trimIndent()
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.Black,
    border = BorderStroke(1.dp, Color(0xFF282832)),
    modifier = Modifier
      .fillMaxWidth()
      .height(210.dp)
  ) {
    AndroidView(
      factory = { context ->
        WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          webChromeClient = WebChromeClient()
          webViewClient = WebViewClient()
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
          }
          setBackgroundColor(android.graphics.Color.BLACK)
          loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
        }
      },
      update = { webView ->
        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "utf-8", null)
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
private fun CharactersSection(
  characters: List<CharacterItem>,
  onCharacterClick: (CharacterItem) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
    Text(
      text = "Characters",
      color = Color.White,
      fontSize = 18.sp,
      fontFamily = PremiumTitleFont,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      items(characters.size) { index ->
        val item = characters[index]
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(76.dp)
            .clickable { onCharacterClick(item) }
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0xFF1E1E22),
            border = BorderStroke(1.dp, Color(0xFF333340)),
            modifier = Modifier.size(72.dp)
          ) {
            AnimeCardImage(
              imageUrl = item.imageUrl,
              imageResId = 0,
              contentDescription = item.name,
              modifier = Modifier.fillMaxSize()
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = item.name,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = item.role,
            color = Color(0xFF888899),
            fontSize = 10.sp,
            fontFamily = PremiumBodyFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun StaffSection(
  staff: List<StaffItem>,
  onStaffClick: (StaffItem) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
    Text(
      text = "Staff",
      color = Color.White,
      fontSize = 18.sp,
      fontFamily = PremiumTitleFont,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(staff.size) { index ->
        val item = staff[index]
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF121215),
          border = BorderStroke(1.dp, Color(0xFF24242C)),
          modifier = Modifier
            .width(110.dp)
            .clickable { onStaffClick(item) }
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
              AnimeCardImage(
                imageUrl = item.imageUrl,
                imageResId = 0,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize()
              )
            }
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = item.name,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = item.role,
                color = Color(0xFF888899),
                fontSize = 9.sp,
                fontFamily = PremiumBodyFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}
