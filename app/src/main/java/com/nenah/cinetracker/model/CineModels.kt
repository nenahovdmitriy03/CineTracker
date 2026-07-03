package com.nenah.cinetracker.model

enum class MediaKind(val routeValue: String, val label: String) {
    Movie("movie", "Фильм"),
    Tv("tv", "Сериал");

    companion object {
        fun fromRoute(value: String): MediaKind = entries.firstOrNull { it.routeValue == value } ?: Movie
    }
}

data class MediaItem(
    val id: Int,
    val kind: MediaKind,
    val title: String,
    val subtitle: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double,
    val year: String,
    val runtimeMinutes: Int? = null,
    val ratings: MediaRatings = MediaRatings()
)

data class MediaRatings(
    val kp: Double? = null,
    val imdb: Double? = null,
    val tmdb: Double? = null
) {
    val hasAny: Boolean
        get() = kp != null || imdb != null || tmdb != null

    val primaryScore: Double
        get() = kp ?: imdb ?: tmdb ?: 0.0

    val primarySource: String?
        get() = when {
            kp != null -> "КП"
            imdb != null -> "IMDb"
            tmdb != null -> "TMDb"
            else -> null
        }
}

data class ApiQuota(
    val limit: Int,
    val used: Int,
    val remaining: Int
) {
    val usedFraction: Float
        get() = if (limit > 0) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class TrackStatus(val routeValue: String, val title: String) {
    Watching("watching", "Смотрю"),
    Planned("planned", "В плане"),
    Watched("watched", "Просмотрено");

    companion object {
        fun fromRoute(value: String): TrackStatus = entries.firstOrNull { it.routeValue == value } ?: Planned
    }
}

enum class CineAppTheme(val routeValue: String, val title: String) {
    Cinema("cinema", "Кинотеатр"),
    Light("light", "Светлая"),
    Emerald("emerald", "Изумруд");

    companion object {
        fun fromRoute(value: String?): CineAppTheme = entries.firstOrNull { it.routeValue == value } ?: Cinema
    }
}

data class TrackedTitle(
    val item: MediaItem,
    val status: TrackStatus,
    val progress: Float,
    val updatedAt: Long,
    val personalRating: Int? = null
)

data class TrackerStats(
    val watching: Int = 0,
    val planned: Int = 0,
    val watched: Int = 0,
    val ratedTitles: Int = 0,
    val ratedEpisodes: Int = 0,
    val collections: Int = 0,
    val watchedMinutes: Int = 0
) {
    val total: Int = watching + planned + watched
}

data class MediaDetail(
    val item: MediaItem,
    val genres: List<String>,
    val runtimeText: String,
    val statusText: String,
    val seasons: List<SeasonInfo> = emptyList(),
    val prequels: List<MediaItem> = emptyList(),
    val sequels: List<MediaItem> = emptyList(),
    val related: List<MediaItem> = emptyList()
)

data class SeasonInfo(
    val number: Int,
    val title: String,
    val episodeCount: Int,
    val episodes: List<EpisodeInfo>
)

data class EpisodeInfo(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String = "",
    val posterUrl: String? = null,
    val serviceRating: Double? = null,
    val airDate: String = "",
    val runtimeMinutes: Int? = null
) {
    val key: String
        get() = "$seasonNumber:$episodeNumber"
}

data class EpisodeRating(
    val mediaId: Int,
    val kind: MediaKind,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val rating: Int,
    val runtimeMinutes: Int? = null
) {
    val key: String
        get() = "$seasonNumber:$episodeNumber"
}

data class MediaCollection(
    val id: Long,
    val name: String,
    val itemCount: Int
)

data class TrackerEvent(
    val id: Long,
    val message: String,
    val createdAt: Long
)

data class ContinueWatchingItem(
    val item: MediaItem,
    val episodeText: String,
    val progress: Float,
    val timeLeft: String
)

data class HomeFeed(
    val hero: MediaItem?,
    val trending: List<MediaItem>,
    val popularMovies: List<MediaItem>,
    val popularShows: List<MediaItem>,
    val anime: List<MediaItem> = emptyList(),
    val newReleases: List<MediaItem> = emptyList(),
    val message: String? = null
) {
    val hasContent: Boolean
        get() = hero != null ||
            trending.isNotEmpty() ||
            popularMovies.isNotEmpty() ||
            popularShows.isNotEmpty() ||
            anime.isNotEmpty() ||
            newReleases.isNotEmpty()
}
