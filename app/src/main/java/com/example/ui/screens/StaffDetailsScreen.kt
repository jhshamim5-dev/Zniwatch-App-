package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AniListRepository
import com.example.data.PremiumTitleFont
import com.example.data.StaffDetailResult
import com.example.data.StaffItem
import com.example.ui.components.AnimeCardImage

@Composable
fun StaffDetailsScreen(
    staff: StaffItem,
    onBack: () -> Unit
) {
    var details by remember(staff.id, staff.name) {
        mutableStateOf<StaffDetailResult?>(null)
    }
    var isLoading by remember(staff.id, staff.name) {
        mutableStateOf(true)
    }

    LaunchedEffect(staff.id, staff.name) {
        isLoading = true
        try {
            val result = AniListRepository.getStaffDetails(
                id = staff.id,
                name = staff.name,
                defaultImg = staff.imageUrl
            )
            details = result
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
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
                text = "Staff Details",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Round Profile Picture
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E1E22),
                modifier = Modifier.size(150.dp)
            ) {
                AnimeCardImage(
                    imageUrl = details?.imageUrl?.ifEmpty { staff.imageUrl } ?: staff.imageUrl,
                    imageResId = 0,
                    contentDescription = staff.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Name of Staff
            Text(
                text = details?.name?.ifEmpty { staff.name } ?: staff.name,
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!details?.nativeName.isNull_or_empty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details!!.nativeName,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (staff.role.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${staff.role}",
                    color = Color(0xFFE50914),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick Info Tags (Gender, Age, Occupations)
            val infoList = mutableListOf<String>()
            if (!details?.occupations.isNull_or_empty()) infoList.add(details!!.occupations)
            if (!details?.gender.isNull_or_empty()) infoList.add("Gender: ${details!!.gender}")
            if (!details?.age.isNull_or_empty()) infoList.add("Age: ${details!!.age}")

            if (infoList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = infoList.joinToString(" • "),
                    color = Color(0xFF888899),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF222228), thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // About / Description Section
            Text(
                text = "About",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE50914),
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                val desc = details?.description?.ifEmpty { "No detailed description available." }
                    ?: "No detailed description available."
                Text(
                    text = desc,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
