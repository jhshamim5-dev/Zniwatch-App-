package com.example.data

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily

// Premium font styling constants
val PremiumTitleFont = FontFamily.Serif
val PremiumBodyFont = FontFamily.SansSerif

data class NavigationNavItem(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String
)

data class AnimeHeroSlide(
  val id: String,
  val title: String,
  val rating: String,
  val tags: String,
  val description: String,
  val imageResId: Int = 0,
  val imageUrl: String = ""
)

data class AnimeCardItem(
  val id: String,
  val title: String,
  val rating: String = "N/A",
  val imageResId: Int = 0,
  val type: String = "TV", // "TV" or "Movie"
  val imageUrl: String = "",
  val startEpisodeId: String? = null,
  val startPosition: Long = 0L,
  val startCategory: String? = null
)

data class GenreItem(
  val name: String,
  val imageResId: Int = 0,
  val imageUrl: String = ""
)

data class ScheduleAnimeItem(
  val id: String,
  val title: String,
  val rating: String = "8.5",
  val airTime: String,
  val episode: String,
  val isSub: Boolean = true,
  val isDub: Boolean = false,
  val imageUrl: String = "",
  val genres: String = "Anime"
)

data class CharacterItem(
  val id: String,
  val name: String,
  val role: String,
  val imageUrl: String
)

data class CharacterDetailResult(
  val id: String,
  val name: String,
  val nativeName: String = "",
  val imageUrl: String = "",
  val description: String = "",
  val gender: String = "",
  val age: String = "",
  val dateOfBirth: String = ""
)

data class StaffItem(
  val id: String,
  val name: String,
  val role: String,
  val imageUrl: String
)

data class StaffDetailResult(
  val id: String,
  val name: String,
  val nativeName: String = "",
  val imageUrl: String = "",
  val description: String = "",
  val gender: String = "",
  val age: String = "",
  val occupations: String = ""
)

data class AnimeDetailResult(
  val id: String,
  val title: String,
  val rating: String = "8.5",
  val imageUrl: String = "",
  val bannerUrl: String = "",
  val genres: String = "Action • Fantasy",
  val status: String = "Finished Airing",
  val description: String = "",
  val studio: String = "N/A",
  val producers: String = "N/A",
  val episodes: String = "N/A",
  val format: String = "TV",
  val duration: String = "N/A",
  val airingPeriod: String = "N/A",
  val season: String = "N/A",
  val nextAiringEpisode: Int? = null,
  val nextAiringAtTimestamp: Long? = null,
  val trailerYoutubeId: String? = null,
  val characters: List<CharacterItem> = emptyList(),
  val staff: List<StaffItem> = emptyList(),
  val relations: List<AnimeCardItem> = emptyList(),
  val recommendations: List<AnimeCardItem> = emptyList()
)

data class EpisodeItem(
  val id: String,
  val episodeNumber: Int,
  val title: String,
  val thumbnail: String = "",
  val url: String = "",
  val isFiller: Boolean = false,
  val hasSub: Boolean = true,
  val hasDub: Boolean = false
)

data class SubtitleTrack(
  val url: String,
  val label: String,
  val isDefault: Boolean = false
)

data class EpisodeStreamResult(
  val url: String,
  val isM3u8: Boolean = true,
  val isIframe: Boolean = false,
  val quality: String = "Auto",
  val headers: Map<String, String> = emptyMap(),
  val subtitles: List<SubtitleTrack> = emptyList(),
  val introStartSec: Long? = null,
  val introEndSec: Long? = null,
  val outroStartSec: Long? = null,
  val outroEndSec: Long? = null
)

data class AnikotoServer(
  val name: String,
  val linkId: String,
  val type: String, // "sub", "dub", "hsub"
  val embedUrl: String = ""
)

data class AniListUserProfile(
  val id: Int = 0,
  val name: String = "",
  val avatarUrl: String = "",
  val bannerUrl: String = "",
  val animeCount: Int = 0,
  val episodesWatched: Int = 0,
  val minutesWatched: Long = 0L
)


