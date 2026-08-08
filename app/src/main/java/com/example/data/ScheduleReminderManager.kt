package com.example.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ScheduleReminderManager {
    private const val PREFS_NAME = "schedule_reminders_prefs"
    private const val KEY_SAVED_REMINDERS = "saved_reminder_ids"

    fun getSavedReminders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SAVED_REMINDERS, emptySet()) ?: emptySet()
    }

    fun isReminderSet(context: Context, id: String): Boolean {
        return getSavedReminders(context).contains(id)
    }

    fun setReminder(context: Context, item: ScheduleAnimeItem, dayName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = getSavedReminders(context).toMutableSet()
        set.add(item.id)
        prefs.edit().putStringSet(KEY_SAVED_REMINDERS, set).apply()

        // Also save item details for receiver
        prefs.edit()
            .putString("title_${item.id}", item.title)
            .putString("episode_${item.id}", item.episode)
            .putString("image_${item.id}", item.imageUrl)
            .apply()

        scheduleAlarm(context, item, dayName)
    }

    fun cancelReminder(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = getSavedReminders(context).toMutableSet()
        set.remove(id)
        prefs.edit().putStringSet(KEY_SAVED_REMINDERS, set).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = "com.example.ANIME_REMINDER_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getAirTimestampMillis(airTime: String, dayName: String, forAlarm: Boolean = false): Long {
        val isJst = airTime.contains("JST", ignoreCase = true) || !airTime.contains("UTC", ignoreCase = true)
        val cleanTime = airTime.replace(" JST", "").replace(" UTC", "").replace(" GMT", "").trim()
        val parts = cleanTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 18
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val targetDayOfWeek = when (dayName.lowercase(Locale.US)) {
            "sunday", "sun" -> Calendar.SUNDAY
            "monday", "mon" -> Calendar.MONDAY
            "tuesday", "tue" -> Calendar.TUESDAY
            "wednesday", "wed" -> Calendar.WEDNESDAY
            "thursday", "thu" -> Calendar.THURSDAY
            "friday", "fri" -> Calendar.FRIDAY
            "saturday", "sat" -> Calendar.SATURDAY
            else -> Calendar.MONDAY
        }

        val sourceTimeZone = if (isJst) {
            TimeZone.getTimeZone("Asia/Tokyo")
        } else {
            TimeZone.getTimeZone("UTC")
        }

        val sourceCal = Calendar.getInstance(sourceTimeZone)
        val currentDayOfWeek = sourceCal.get(Calendar.DAY_OF_WEEK)
        val diff = targetDayOfWeek - currentDayOfWeek

        sourceCal.add(Calendar.DAY_OF_YEAR, diff)
        sourceCal.set(Calendar.HOUR_OF_DAY, hour)
        sourceCal.set(Calendar.MINUTE, minute)
        sourceCal.set(Calendar.SECOND, 0)
        sourceCal.set(Calendar.MILLISECOND, 0)

        var timeMillis = sourceCal.timeInMillis
        // Only shift to next week if setting an alarm for an upcoming episode
        if (forAlarm && timeMillis < System.currentTimeMillis()) {
            sourceCal.add(Calendar.DAY_OF_YEAR, 7)
            timeMillis = sourceCal.timeInMillis
        }

        return timeMillis
    }

    fun formatLocalTime(airTime: String, dayName: String): String {
        try {
            val millis = getAirTimestampMillis(airTime, dayName, forAlarm = false)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(Date(millis))
        } catch (e: Exception) {
            return airTime.replace(" JST", "").replace(" UTC", "").replace(" GMT", "")
        }
    }

    private fun scheduleAlarm(context: Context, item: ScheduleAnimeItem, dayName: String) {
        val timeMillis = getAirTimestampMillis(item.airTime, dayName, forAlarm = true)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = "com.example.ANIME_REMINDER_NOTIFICATION"
            putExtra("anime_id", item.id)
            putExtra("anime_title", item.title)
            putExtra("anime_episode", item.episode)
            putExtra("anime_image", item.imageUrl)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ScheduleNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val animeId = intent.getStringExtra("anime_id") ?: ""
        val title = intent.getStringExtra("anime_title") ?: "Anime Episode"
        val episode = intent.getStringExtra("anime_episode") ?: "New Episode"
        val imageUrl = intent.getStringExtra("anime_image") ?: ""

        val channelId = "anime_episode_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Anime Episode Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when a scheduled anime episode airs"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_anime_id", animeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            animeId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            var bitmap: Bitmap? = null
            if (imageUrl.isNotEmpty()) {
                try {
                    val url = URL(imageUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.connect()
                    val input: InputStream = conn.inputStream
                    bitmap = BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$title $episode is Out Now!")
                .setContentText("$episode of $title has arrived. Click to watch now.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)

            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setBigContentTitle("$title $episode is Out Now!")
                        .setSummaryText("$episode of $title has arrived.")
                )
            }

            notificationManager.notify(animeId.hashCode(), notificationBuilder.build())
        }
    }
}
