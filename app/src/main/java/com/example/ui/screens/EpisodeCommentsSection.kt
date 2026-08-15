package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.CommentItem
import com.example.data.LocalCommentsManager
import com.example.data.RemoteCommentsManager
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import kotlinx.coroutines.launch

@Composable
fun EpisodeCommentsSection(
    animeTitle: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE) }

    val isConnected = prefs.getBoolean("anilist_connected", false)
    val customDisplayName = prefs.getString("custom_display_name", null)
    val anilistUsername = prefs.getString("anilist_username", "") ?: ""
    val finalUsername = when {
        !customDisplayName.isNullOrEmpty() -> customDisplayName
        anilistUsername.isNotEmpty() -> anilistUsername
        else -> "Guest User"
    }

    val useCustomAvatar = prefs.getBoolean("use_custom_avatar", false)
    val customAvatarUrl = prefs.getString("custom_avatar_url", null)
    val anilistAvatarUrl = prefs.getString("anilist_avatar_url", null)
    val finalAvatarUrl = if (useCustomAvatar) customAvatarUrl else anilistAvatarUrl

    var newCommentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    val comments = remember(animeTitle) {
        mutableStateListOf<CommentItem>()
    }

    // Load comments initially from local cache, then sync online
    LaunchedEffect(animeTitle) {
        comments.clear()
        comments.addAll(LocalCommentsManager.getCommentsForAnime(context, animeTitle))
        coroutineScope.launch {
            val remoteList = RemoteCommentsManager.fetchRemoteComments(context, animeTitle)
            comments.clear()
            comments.addAll(remoteList)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Comments",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${comments.size} Comments",
                color = Color(0xFF888899),
                fontSize = 12.sp,
                fontFamily = PremiumBodyFont
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Comment Input Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141418),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                // Current user's avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E2C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!finalAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = finalAvatarUrl,
                            contentDescription = "My Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val firstLetter = finalUsername.getOrNull(0)?.uppercase() ?: "G"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE50914)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PremiumTitleFont
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = {
                        Text(
                            text = "Add a comment...",
                            color = Color(0xFF666677),
                            fontSize = 13.sp,
                            fontFamily = PremiumBodyFont
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank() && !isPosting) {
                            val textToPost = newCommentText.trim()
                            newCommentText = ""
                            val newComment = CommentItem(
                                id = "c_${System.currentTimeMillis()}",
                                username = finalUsername,
                                avatarLetter = finalUsername.getOrNull(0)?.uppercase()?.toString() ?: "U",
                                avatarBgColorHex = "#8B5CF6",
                                timeAgo = "Just now",
                                commentText = textToPost,
                                likesCount = 0,
                                isLiked = false,
                                avatarUrl = finalAvatarUrl
                            )
                            comments.add(0, newComment)
                            isPosting = true
                            coroutineScope.launch {
                                val updatedList = RemoteCommentsManager.postRemoteComment(context, animeTitle, newComment)
                                comments.clear()
                                comments.addAll(updatedList)
                                isPosting = false
                            }
                        }
                    },
                    enabled = newCommentText.isNotBlank() && !isPosting
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post Comment",
                        tint = if (newCommentText.isNotBlank() && !isPosting) Color.White else Color(0xFF444455),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Comment List
        if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF141418), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No comments yet. Be the first to comment!",
                    color = Color(0xFF777788),
                    fontSize = 13.5.sp,
                    fontFamily = PremiumBodyFont
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                comments.forEach { comment ->
                    key(comment.id) {
                        CommentCardItem(
                            animeTitle = animeTitle,
                            comment = comment
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCardItem(
    animeTitle: String,
    comment: CommentItem
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var likes by remember { mutableIntStateOf(comment.likesCount) }
    var isLiked by remember { mutableStateOf(comment.isLiked) }

    val bgColor = remember(comment.avatarBgColorHex) {
        try {
            Color(android.graphics.Color.parseColor(comment.avatarBgColorHex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141418),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!comment.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = comment.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = comment.avatarLetter,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PremiumTitleFont
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = comment.username,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = comment.timeAgo,
                        color = Color(0xFF777788),
                        fontSize = 11.sp,
                        fontFamily = PremiumBodyFont
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val newIsLiked = !isLiked
                            val newLikes = if (newIsLiked) likes + 1 else (likes - 1).coerceAtLeast(0)
                            isLiked = newIsLiked
                            likes = newLikes
                            coroutineScope.launch {
                                RemoteCommentsManager.updateLikeRemote(context, animeTitle, comment.id, newIsLiked, newLikes)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFE50914) else Color(0xFF888899),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$likes",
                        color = if (isLiked) Color(0xFFE50914) else Color(0xFF888899),
                        fontSize = 12.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = comment.commentText,
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp,
                fontFamily = PremiumBodyFont,
                lineHeight = 18.sp
            )
        }
    }
}

