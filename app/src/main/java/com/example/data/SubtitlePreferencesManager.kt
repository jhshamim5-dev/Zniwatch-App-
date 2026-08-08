package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class SubtitleSettings(
    val fontSizeSp: Float = 18f,
    val bottomOffsetDp: Int = 32,
    val backgroundColorHex: Long = 0x80000000, // 50% Translucent Black
    val fontColorHex: Long = 0xFFFFFFFF // White
)

class SubtitlePreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_BOTTOM_OFFSET = "bottom_offset"
        const val KEY_BG_COLOR = "bg_color"
        const val KEY_FONT_COLOR = "font_color"

        const val DEFAULT_FONT_SIZE = 18f
        const val DEFAULT_BOTTOM_OFFSET = 32
        const val DEFAULT_BG_COLOR = 0x80000000L
        const val DEFAULT_FONT_COLOR = 0xFFFFFFFFL
    }

    fun getSettings(): SubtitleSettings {
        return SubtitleSettings(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE),
            bottomOffsetDp = prefs.getInt(KEY_BOTTOM_OFFSET, DEFAULT_BOTTOM_OFFSET),
            backgroundColorHex = prefs.getLong(KEY_BG_COLOR, DEFAULT_BG_COLOR),
            fontColorHex = prefs.getLong(KEY_FONT_COLOR, DEFAULT_FONT_COLOR)
        )
    }

    fun saveSettings(settings: SubtitleSettings) {
        prefs.edit()
            .putFloat(KEY_FONT_SIZE, settings.fontSizeSp)
            .putInt(KEY_BOTTOM_OFFSET, settings.bottomOffsetDp)
            .putLong(KEY_BG_COLOR, settings.backgroundColorHex)
            .putLong(KEY_FONT_COLOR, settings.fontColorHex)
            .apply()
    }

    fun resetToDefault(): SubtitleSettings {
        val defaultSettings = SubtitleSettings()
        saveSettings(defaultSettings)
        return defaultSettings
    }
}
