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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnimeCardItem
import com.example.data.AnikotoRepository
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import com.example.data.ScheduleAnimeItem
import com.example.ui.components.AnimeCardImage
import com.example.ui.components.AppPullToRefreshLayout
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.ScheduleReminderManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ScheduleScreen(
  onAnimeClick: (AnimeCardItem) -> Unit = {}
) {
  val context = LocalContext.current
  val daysOfWeek = remember {
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  }

  val todayName = remember {
    val sdf = SimpleDateFormat("EEEE", Locale.US)
    sdf.timeZone = TimeZone.getDefault()
    sdf.format(Date())
  }

  val userTimeZone = remember {
    TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
  }

  var selectedDay by remember {
    mutableStateOf(
      if (daysOfWeek.any { it.equals(todayName, ignoreCase = true) }) {
        daysOfWeek.first { it.equals(todayName, ignoreCase = true) }
      } else "Monday"
    )
  }

  var scheduleList by remember { mutableStateOf<List<ScheduleAnimeItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  val savedReminders = remember { mutableStateListOf<String>() }

  LaunchedEffect(Unit) {
    savedReminders.clear()
    savedReminders.addAll(ScheduleReminderManager.getSavedReminders(context))
  }

  var refreshTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }

  LaunchedEffect(selectedDay, refreshTrigger) {
    isLoading = true
    try {
      val result = AnikotoRepository.getSchedule(selectedDay)
      scheduleList = result
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
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Broadcast Schedule",
          color = Color.White,
          fontSize = 22.sp,
          fontFamily = PremiumTitleFont,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "Simulcasts • Local Time ($userTimeZone)",
          color = Color(0xFFAAAAAA),
          fontSize = 12.sp,
          fontFamily = PremiumBodyFont
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A1E),
        border = BorderStroke(1.dp, Color(0xFF33333D))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(Color(0xFF00E676))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = PremiumBodyFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
        }
      }
    }

    // Scrollable Day Bar
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 12.dp)
    ) {
      items(daysOfWeek) { day ->
        val isSelected = selectedDay.equals(day, ignoreCase = true)
        val isToday = todayName.equals(day, ignoreCase = true)

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = if (isSelected) Color.White else Color(0xFF16161A),
          border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF2A2A30)),
          modifier = Modifier.clickable { selectedDay = day }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text(
              text = day.take(3).uppercase(),
              color = if (isSelected) Color.Black else Color.White,
              fontSize = 12.sp,
              fontFamily = PremiumTitleFont,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )

            if (isToday) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color.Black else Color.White
              ) {
                Text(
                  text = "TODAY",
                  color = if (isSelected) Color.White else Color.Black,
                  fontSize = 8.sp,
                  fontFamily = PremiumBodyFont,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }
    }

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
      if (scheduleList.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No broadcasts scheduled for $selectedDay",
            color = Color(0xFF888888),
            fontSize = 14.sp,
            fontFamily = PremiumBodyFont
          )
        }
      } else {
        AppPullToRefreshLayout(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            refreshTrigger++
            isRefreshing = false
          },
          modifier = Modifier.fillMaxSize()
        ) {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
          ) {
          items(scheduleList) { item ->
            val isReminderSet = savedReminders.contains(item.id)
            val localTimeStr = remember(item.airTime, selectedDay) {
              ScheduleReminderManager.formatLocalTime(item.airTime, selectedDay)
            }

            val hasAired = remember(item.airTime, selectedDay) {
              try {
                val millis = ScheduleReminderManager.getAirTimestampMillis(item.airTime, selectedDay, forAlarm = false)
                System.currentTimeMillis() > millis
              } catch (e: Exception) {
                false
              }
            }

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFF131316),
              border = BorderStroke(1.dp, Color(0xFF282830)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onAnimeClick(
                    AnimeCardItem(
                      id = item.id,
                      title = item.title,
                      rating = item.rating,
                      imageUrl = item.imageUrl,
                      type = "TV"
                    )
                  )
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Time & Airing Badge
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.width(68.dp)
                ) {
                  Text(
                    text = localTimeStr,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                  )

                  Text(
                    text = userTimeZone,
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontFamily = PremiumBodyFont
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  if (selectedDay.equals(todayName, ignoreCase = true)) {
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0x3300E676)
                    ) {
                      Text(
                        text = "TODAY",
                        color = Color(0xFF00E676),
                        fontSize = 8.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Poster
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF222226),
                  modifier = Modifier
                    .width(68.dp)
                    .height(96.dp)
                ) {
                  AnimeCardImage(
                    imageUrl = item.imageUrl,
                    imageResId = 0,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info Column
                Column(
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    // Episode Badge
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color.White
                    ) {
                      Text(
                        text = item.episode,
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                      )
                    }

                    if (item.isSub) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2A2A32)
                      ) {
                        Text(
                          text = "SUB",
                          color = Color.White,
                          fontSize = 9.sp,
                          fontFamily = PremiumBodyFont,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }

                    if (item.isDub) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2A2A32)
                      ) {
                        Text(
                          text = "DUB",
                          color = Color.White,
                          fontSize = 9.sp,
                          fontFamily = PremiumBodyFont,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = item.genres,
                    color = Color(0xFF888888),
                    fontSize = 11.sp,
                    fontFamily = PremiumBodyFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Reminder Toggle Button (only show if the episode has NOT already aired)
                if (!hasAired) {
                  IconButton(
                    onClick = {
                      if (isReminderSet) {
                        ScheduleReminderManager.cancelReminder(context, item.id)
                        savedReminders.remove(item.id)
                        Toast.makeText(context, "Reminder cancelled for ${item.title}", Toast.LENGTH_SHORT).show()
                      } else {
                        ScheduleReminderManager.setReminder(context, item, selectedDay)
                        if (!savedReminders.contains(item.id)) {
                          savedReminders.add(item.id)
                        }
                        Toast.makeText(context, "Reminder set for ${item.title} (${item.episode})", Toast.LENGTH_SHORT).show()
                      }
                    }
                  ) {
                    Icon(
                      imageVector = if (isReminderSet) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                      contentDescription = "Remind Me",
                      tint = if (isReminderSet) Color.White else Color(0xFF666666)
                    )
                  }
                } else {
                  // Show a subtle indicator that it has already aired (e.g., small status tag or spacer to keep spacing aligned)
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .padding(8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0x1AFF5555)
                    ) {
                      Text(
                        text = "AIRED",
                        color = Color(0xFFFF5555),
                        fontSize = 8.sp,
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
  }
}
}
