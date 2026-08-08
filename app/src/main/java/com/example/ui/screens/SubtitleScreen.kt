package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SubtitlePreferencesManager
import com.example.data.SubtitleSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefManager = remember { SubtitlePreferencesManager(context) }
    val initialSettings = remember { prefManager.getSettings() }

    var fontSizeSp by remember { mutableFloatStateOf(initialSettings.fontSizeSp) }
    var bottomOffsetDp by remember { mutableIntStateOf(initialSettings.bottomOffsetDp) }
    var bgColorHex by remember { mutableLongStateOf(initialSettings.backgroundColorHex) }
    var fontColorHex by remember { mutableLongStateOf(initialSettings.fontColorHex) }

    fun saveCurrentState() {
        prefManager.saveSettings(
            SubtitleSettings(
                fontSizeSp = fontSizeSp,
                bottomOffsetDp = bottomOffsetDp,
                backgroundColorHex = bgColorHex,
                fontColorHex = fontColorHex
            )
        )
    }

    val backgroundColors = listOf(
        Pair("None", 0x00000000L),
        Pair("50% Black", 0x80000000L),
        Pair("80% Black", 0xCC000000L),
        Pair("Solid Black", 0xFF000000L),
        Pair("Dark Blue", 0xCC0A1128L)
    )

    val fontColors = listOf(
        Pair("White", 0xFFFFFFFFL),
        Pair("Yellow", 0xFFFFE500L),
        Pair("Cyan", 0xFF00E5FFL),
        Pair("Green", 0xFF00FF66L),
        Pair("Orange", 0xFFFF9100L),
        Pair("Black", 0xFF000000L)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Subtitle Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Default Reset Button
                OutlinedButton(
                    onClick = {
                        val defaultSet = prefManager.resetToDefault()
                        fontSizeSp = defaultSet.fontSizeSp
                        bottomOffsetDp = defaultSet.bottomOffsetDp
                        bgColorHex = defaultSet.backgroundColorHex
                        fontColorHex = defaultSet.fontColorHex
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = "Reset to Default",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Default",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Live Preview Card
                Text(
                    text = "LIVE PREVIEW",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181824))
                        .border(1.dp, Color(0xFF2E2E3E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = bottomOffsetDp.dp)
                            .background(
                                color = Color(bgColorHex),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sample Subtitle Text - Preview",
                            color = Color(fontColorHex),
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 1. Font Size Setting
                SettingSectionCard(title = "1. Font Size (${fontSizeSp.toInt()} sp)") {
                    Slider(
                        value = fontSizeSp,
                        onValueChange = {
                            fontSizeSp = it
                            saveCurrentState()
                        },
                        valueRange = 12f..32f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF333344)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Vertical Position Setting
                SettingSectionCard(title = "2. Subtitle Height Offset (${bottomOffsetDp} dp)") {
                    Slider(
                        value = bottomOffsetDp.toFloat(),
                        onValueChange = {
                            bottomOffsetDp = it.toInt()
                            saveCurrentState()
                        },
                        valueRange = 12f..100f,
                        steps = 88,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF333344)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Subtitle Background Color
                SettingSectionCard(title = "3. Subtitle Background Color") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        backgroundColors.forEach { (name, hex) ->
                            val isSelected = bgColorHex == hex
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color.White else Color(0xFF1E1E2C),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF3A3A4C)),
                                modifier = Modifier.clickable {
                                    bgColorHex = hex
                                    saveCurrentState()
                                }
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Font Color Setting
                SettingSectionCard(title = "4. Font Color") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        fontColors.forEach { (name, hex) ->
                            val isSelected = fontColorHex == hex
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color.White else Color(0xFF1E1E2C),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF3A3A4C)),
                                modifier = Modifier.clickable {
                                    fontColorHex = hex
                                    saveCurrentState()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Color(hex), CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF12121A),
        border = BorderStroke(1.dp, Color(0xFF222232)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}
