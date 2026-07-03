package com.nenah.cinetracker.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nenah.cinetracker.BuildConfig
import com.nenah.cinetracker.model.ApiQuota
import com.nenah.cinetracker.model.EpisodeInfo
import com.nenah.cinetracker.model.HomeFeed
import com.nenah.cinetracker.model.MediaDetail
import com.nenah.cinetracker.model.MediaItem
import com.nenah.cinetracker.model.MediaKind
import com.nenah.cinetracker.model.MediaRatings
import com.nenah.cinetracker.model.SeasonInfo
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class CineRepository(
    context: Context,
    private val token: String = BuildConfig.TMDB_READ_ACCESS_TOKEN,
    private val apiBaseUrl: String = BuildConfig.TMDB_API_BASE_URL,
    private val poiskKinoApiKey: String = BuildConfig.POISKKINO_API_KEY,
    private val poiskKinoApiBaseUrl: String = BuildConfig.POISKKINO_API_BASE_URL
) {
    private val usesProxy: Boolean = !apiBaseUrl.startsWith("https://api.themoviedb.org")
    private val service: TmdbService? = if (token.isNotBlank() || usesProxy) {
        TmdbNetwork.create(token, apiBaseUrl)
    } else {
        null
    }
    private val poiskKinoUsesProxy: Boolean = !poiskKinoApiBaseUrl.startsWith("https://api.poiskkino.dev")
    private val poiskKinoService: PoiskKinoService? = if (poiskKinoApiKey.isNotBlank() || poiskKinoUsesProxy) {
        PoiskKinoNetwork.create(poiskKinoApiKey, context, poiskKinoApiBaseUrl)
    } else {
        null
    }
    private val manualClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val detailCache = mutableMapOf<String, MediaDetail>()
    private val seasonCache = mutableMapOf<String, SeasonInfo>()

    val isConfigured: Boolean = poiskKinoService != null || service != null

    suspend fun loadHome(): HomeFeed {
        service?.let {
            val tmdbFeed = loadTmdbHome()
            if (tmdbFeed.hasContent) return tmdbFeed

            // TMDb недоступен (блокировка, сеть) — пробуем PoiskKino как запасной источник.
            poiskKinoService?.let { api ->
                runCatching { loadPoiskKinoHome(api) }
                    .onFailure { error -> Log.w("CineRepository", "PoiskKino fallback failed", error) }
                    .getOrNull()
                    ?.takeIf { it.hasContent }
                    ?.let { return it }
            }
            return tmdbFeed
        }

        poiskKinoService?.let { api ->
            return runCatching { loadPoiskKinoHome(api) }
                .getOrElse { error ->
                    Log.w("CineRepository", "PoiskKino load failed", error)
                    emptyHomeFeed("Не удалось загрузить PoiskKino.")
                }
        }

        return emptyHomeFeed("Каталог пока не подключен.")
    }

    suspend fun loadApiQuota(): ApiQuota? {
        val api = poiskKinoService ?: return null
        return runCatching {
            api.tokenInfo().toApiQuota()
        }.getOrElse { error ->
            Log.w("CineRepository", "PoiskKino quota failed", error)
            null
        }
    }

    suspend fun rollRoulette(): MediaItem? {
        val api = poiskKinoService ?: return null
        return runCatching {
            val randomPage = (1..100).random()
            val result = api.movies(
                limit = 1,
                page = randomPage,
                ratingKp = "7.5-10",
                votesKp = "50000-99999999",
                sortField = listOf("votes.kp"),
                sortType = listOf("-1")
            )
            result.docs.firstOrNull()?.toMediaItem()
        }.getOrElse { error ->
            Log.w("CineRepository", "Roulette failed", error)
            null
        }
    }

    private suspend fun loadPoiskKinoHome(api: PoiskKinoService): HomeFeed = coroutineScope {
        val currentYear = LocalDate.now().year
        val popular = async {
            fetchPoiskKinoItems("popular", api) {
                movies(limit = 12, ratingKp = "6-10", votesKp = "20000-99999999")
            }
        }
        val movieList = async {
            fetchPoiskKinoItems("movies", api) {
                movies(limit = 12, type = listOf("movie"), ratingKp = "6-10", votesKp = "10000-99999999")
            }
        }
        val shows = async {
            fetchPoiskKinoItems("shows", api) {
                movies(limit = 12, type = listOf("tv-series"), ratingKp = "6-10", votesKp = "10000-99999999")
            }
        }
        val anime = async {
            fetchPoiskKinoItems("anime", api) {
                movies(limit = 12, type = listOf("anime", "animated-series"), ratingKp = "6-10", votesKp = "3000-99999999")
            }
        }
        val newReleases = async {
            fetchPoiskKinoItems("new releases", api) {
                movies(
                    limit = 12,
                    year = "${currentYear - 1}-$currentYear",
                    ratingKp = "5-10",
                    sortField = listOf("year", "votes.kp"),
                    sortType = listOf("-1", "-1")
                )
            }
        }

        val trendingItems = popular.await()
        val movieItems = movieList.await()
        val showItems = shows.await()
        val animeItems = anime.await()
        val newReleaseItems = newReleases.await()

        HomeFeed(
            hero = newReleaseItems.firstOrNull()
                ?: trendingItems.firstOrNull()
                ?: movieItems.firstOrNull()
                ?: showItems.firstOrNull()
                ?: animeItems.firstOrNull(),
            trending = trendingItems,
            popularMovies = movieItems,
            popularShows = showItems,
            anime = animeItems,
            newReleases = newReleaseItems
        )
    }

    private suspend fun loadTmdbHome(): HomeFeed {
        val api = service ?: return emptyHomeFeed("TMDb пока не подключен.")

        return runCatching {
            coroutineScope {
                val trending = async { fetchMediaItems("trending", null) { api.trending() } }
                val movies = async { fetchMediaItems("popular movies", MediaKind.Movie) { api.popularMovies() } }
                val shows = async { fetchMediaItems("popular shows", MediaKind.Tv) { api.popularShows() } }
                val animeShows = async { fetchMediaItems("anime shows", MediaKind.Tv) { api.animeShows() } }
                val animeMovies = async { fetchMediaItems("anime movies", MediaKind.Movie) { api.animeMovies() } }
                val newMovies = async { fetchMediaItems("new movies", MediaKind.Movie) { api.nowPlayingMovies() } }
                val newShows = async { fetchMediaItems("new shows", MediaKind.Tv) { api.onTheAirShows() } }

                val trendingItems = trending.await()
                val movieItems = movies.await()
                val showItems = shows.await()
                val animeItems = (animeShows.await() + animeMovies.await()).distinctByMedia()
                val newReleaseItems = (newMovies.await() + newShows.await()).distinctByMedia()
                val hero = trendingItems.firstOrNull()
                    ?: newReleaseItems.firstOrNull()
                    ?: movieItems.firstOrNull()
                    ?: showItems.firstOrNull()
                    ?: animeItems.firstOrNull()

                HomeFeed(
                    hero = hero,
                    trending = trendingItems,
                    popularMovies = movieItems,
                    popularShows = showItems,
                    anime = animeItems,
                    newReleases = newReleaseItems
                )
            }
        }.getOrElse { error ->
            Log.w("CineRepository", "TMDb load failed", error)
            emptyHomeFeed(tmdbFailureMessage(error))
        }
    }

    suspend fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        service?.let { api ->
            val result = runCatching {
                api.search(query).results.toMediaItems(defaultKind = null)
            }
            result.getOrNull()?.let { return it }
            Log.w("CineRepository", "TMDb search failed", result.exceptionOrNull())
        }

        poiskKinoService?.let { api ->
            val result = runCatching {
                api.search(query = query, limit = 20).docs.mapNotNull(PoiskKinoMovieDto::toMediaItem).distinctByMedia()
            }
            result.getOrNull()?.let { return it }
            Log.w("CineRepository", "PoiskKino search failed", result.exceptionOrNull())
        }

        return emptyList()
    }

    suspend fun details(kind: MediaKind, id: Int): MediaDetail {
        val cacheKey = detailCacheKey(kind, id)
        detailCache[cacheKey]?.let { return it }

        service?.let { api ->
            val result = runCatching {
                when (kind) {
                    MediaKind.Movie -> {
                        val detail = api.movieDetails(id)
                        val (prequels, sequels) = loadMovieCollectionRelations(api, detail)
                        val related = if (prequels.isEmpty() && sequels.isEmpty()) {
                            fetchMediaItems("movie recommendations", MediaKind.Movie) {
                                api.movieRecommendations(id)
                            }.filterNot { it.id == id }.take(12)
                        } else {
                            emptyList()
                        }

                        detail.toDetail(MediaKind.Movie).copy(
                            prequels = prequels,
                            sequels = sequels,
                            related = related
                        )
                    }
                    MediaKind.Tv -> {
                        val detail = api.tvDetails(id)
                        detail.toDetail(MediaKind.Tv).withCachedSeasons(id)
                    }
                }
            }
            result.getOrNull()?.let { loaded ->
                detailCache[cacheKey] = loaded
                return loaded
            }
            Log.w("CineRepository", "TMDb detail failed", result.exceptionOrNull())
        }

        poiskKinoService?.let { api ->
            runCatching {
                return api.details(id).toDetail()
            }.onFailure { error ->
                Log.w("CineRepository", "PoiskKino detail failed", error)
            }
        }

        return missingDetail(kind, id, "Не удалось загрузить данные каталога.")
    }

    suspend fun seasonDetails(showId: Int, season: SeasonInfo): SeasonInfo? {
        val cacheKey = seasonCacheKey(showId, season.number)
        seasonCache[cacheKey]?.let { return it }

        val api = service ?: return null
        return runCatching {
            api.tvSeasonDetails(showId, season.number).toSeasonInfo(season)
        }.getOrElse { error ->
            Log.w("CineRepository", "TMDb season failed: $showId/${season.number}", error)
            null
        }?.also { loaded ->
            seasonCache[cacheKey] = loaded
            val detailKey = detailCacheKey(MediaKind.Tv, showId)
            detailCache[detailKey] = detailCache[detailKey]?.copy(
                seasons = detailCache[detailKey].orEmptySeasons().map { current ->
                    if (current.number == loaded.number) loaded else current
                }
            ) ?: return@also
        }
    }

    private fun detailCacheKey(kind: MediaKind, id: Int): String = "${kind.routeValue}:$id"

    private fun seasonCacheKey(showId: Int, seasonNumber: Int): String = "tv:$showId:$seasonNumber"

    private fun MediaDetail.withCachedSeasons(showId: Int): MediaDetail {
        if (item.kind != MediaKind.Tv || seasons.isEmpty()) return this
        return copy(
            seasons = seasons.map { season ->
                seasonCache[seasonCacheKey(showId, season.number)] ?: season
            }
        )
    }

    private fun MediaDetail?.orEmptySeasons(): List<SeasonInfo> = this?.seasons.orEmpty()

    suspend fun resolveManualLink(rawLink: String): MediaItem? {
        val link = rawLink.trim()
        if (link.isBlank()) return null

        val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        val segments = uri.pathSegments

        if ("themoviedb.org" in host) {
            val movieIndex = segments.indexOf("movie")
            val tvIndex = segments.indexOf("tv")
            val tmdbMovieId = segments.getOrNull(movieIndex + 1)?.toIntOrNull().takeIf { movieIndex >= 0 }
            val tmdbTvId = segments.getOrNull(tvIndex + 1)?.toIntOrNull().takeIf { tvIndex >= 0 }

            tmdbMovieId?.let { return details(MediaKind.Movie, it).item }
            tmdbTvId?.let { return details(MediaKind.Tv, it).item }
        }

        if ("kinopoisk.ru" in host) {
            val type = segments.firstOrNull()
            val sourceId = segments.getOrNull(1)?.toIntOrNull() ?: return null
            val kind = when (type) {
                "series" -> MediaKind.Tv
                "film", "movie" -> MediaKind.Movie
                else -> return null
            }

            poiskKinoService?.let { api ->
                val result = runCatching {
                    api.details(sourceId).toDetail().item
                }
                result.getOrNull()?.let { item ->
                    return item.copy(
                        id = manualId("kinopoisk:${kind.routeValue}:$sourceId"),
                        kind = kind,
                        subtitle = "${kind.label} • ${item.year} • Кинопоиск"
                    )
                }
                Log.w("CineRepository", "PoiskKino manual lookup failed", result.exceptionOrNull())
            }

            return loadKinopoiskLinkItem(link, kind, sourceId, manualClient)
        }

        return null
    }
}

private suspend fun loadMovieCollectionRelations(
    api: TmdbService,
    detail: TmdbDetailDto
): Pair<List<MediaItem>, List<MediaItem>> {
    val collectionId = detail.belongsToCollection?.id?.takeIf { it > 0 } ?: return emptyList<MediaItem>() to emptyList()
    val currentDate = detail.releaseDate.normalizedDate()

    return runCatching {
        val collection = api.collectionDetails(collectionId)
        val parts = collection.parts
            .filter { it.id != detail.id }
            .mapNotNull { dto -> dto.toMediaItem(MediaKind.Movie)?.let { dto to it } }
            .sortedWith(compareBy<Pair<TmdbMediaDto, MediaItem>> { it.first.releaseDate.normalizedDate() }.thenBy { it.second.title })

        val (prequels, sequels) = parts.partition { (dto, _) ->
            val date = dto.releaseDate.normalizedDate()
            currentDate.isNotBlank() && date.isNotBlank() && date < currentDate
        }

        prequels.map { it.second } to sequels.map { it.second }
    }.getOrElse { error ->
        Log.w("CineRepository", "TMDb collection failed: $collectionId", error)
        emptyList<MediaItem>() to emptyList()
    }
}

private fun List<TmdbMediaDto>.toMediaItems(defaultKind: MediaKind?): List<MediaItem> {
    return mapNotNull { it.toMediaItem(defaultKind) }
}

private suspend fun fetchMediaItems(
    label: String,
    defaultKind: MediaKind?,
    request: suspend () -> TmdbPageDto
): List<MediaItem> {
    return runCatching {
        request().results.toMediaItems(defaultKind).distinctByMedia()
    }.getOrElse { error ->
        Log.w("CineRepository", "TMDb section failed: $label", error)
        emptyList()
    }
}

private suspend fun fetchPoiskKinoItems(
    label: String,
    api: PoiskKinoService,
    request: suspend PoiskKinoService.() -> PoiskKinoPageDto
): List<MediaItem> {
    return runCatching {
        api.request().docs.mapNotNull(PoiskKinoMovieDto::toMediaItem).distinctByMedia()
    }.getOrElse { error ->
        Log.w("CineRepository", "PoiskKino section failed: $label", error)
        emptyList()
    }
}

private fun TmdbMediaDto.toMediaItem(defaultKind: MediaKind?): MediaItem? {
    val kind = when (mediaType ?: defaultKind?.routeValue) {
        "movie" -> MediaKind.Movie
        "tv" -> MediaKind.Tv
        else -> return null
    }
    val mediaTitle = title ?: name ?: return null
    val date = releaseDate ?: firstAirDate.orEmpty()
    val year = date.take(4).ifBlank { "----" }
    val score = voteAverage.validRating() ?: 0.0

    return MediaItem(
        id = id,
        kind = kind,
        title = mediaTitle,
        subtitle = "${kind.label} • $year",
        overview = overview.orEmpty().ifBlank { "Описание пока не добавлено." },
        posterUrl = posterPath?.let(::posterUrl),
        backdropUrl = backdropPath?.let(::backdropUrl),
        rating = score,
        year = year,
        ratings = MediaRatings(tmdb = score.takeIf { it > 0.0 })
    )
}

private fun PoiskKinoMovieDto.toMediaItem(): MediaItem? {
    val mediaTitle = name ?: alternativeName ?: enName ?: return null
    val kind = poiskKind(type = type, isSeries = isSeries)
    val mediaYear = year?.toString() ?: "----"
    val ratings = rating.toRatings()
    val score = ratings.primaryScore

    return MediaItem(
        id = id,
        kind = kind,
        title = mediaTitle,
        subtitle = "${kind.label} • $mediaYear",
        overview = description ?: shortDescription ?: "Описание пока не добавлено.",
        posterUrl = poster?.previewUrl ?: poster?.url,
        backdropUrl = backdrop?.previewUrl ?: backdrop?.url,
        rating = score,
        year = mediaYear,
        ratings = ratings
    )
}

private fun PoiskKinoLinkedMovieDto.toMediaItem(fallbackKind: MediaKind): MediaItem? {
    val mediaTitle = name ?: alternativeName ?: enName ?: return null
    val kind = poiskKind(type = type, isSeries = fallbackKind == MediaKind.Tv)
    val mediaYear = year?.toString() ?: "----"
    val ratings = rating.toRatings()

    return MediaItem(
        id = id,
        kind = kind,
        title = mediaTitle,
        subtitle = "${kind.label} • $mediaYear",
        overview = "Связанное название из PoiskKino.",
        posterUrl = poster?.previewUrl ?: poster?.url,
        backdropUrl = null,
        rating = ratings.primaryScore,
        year = mediaYear,
        ratings = ratings
    )
}

private fun PoiskKinoMovieDto.toDetail(): MediaDetail {
    val item = toMediaItem() ?: MediaItem(
        id = id,
        kind = poiskKind(type = type, isSeries = isSeries),
        title = name ?: alternativeName ?: enName ?: "Без названия",
        subtitle = year?.toString() ?: "----",
        overview = description ?: shortDescription ?: "Описание отсутствует.",
        posterUrl = poster?.url ?: poster?.previewUrl,
        backdropUrl = backdrop?.url ?: backdrop?.previewUrl,
        rating = rating.toRatings().primaryScore,
        year = year?.toString() ?: "----",
        ratings = rating.toRatings()
    )

    val linked = sequelsAndPrequels
        .mapNotNull { it.toMediaItem(item.kind) }
        .filterNot { it.id == item.id }
        .distinctByMedia()

    val currentYear = year ?: 0
    val prequels = linked.filter { it.year.toIntOrNull()?.let { linkedYear -> currentYear > 0 && linkedYear < currentYear } == true }
    val sequels = linked.filterNot { it in prequels }
    val related = similarMovies
        .mapNotNull { it.toMediaItem(item.kind) }
        .filterNot { it.id == item.id }
        .distinctByMedia()
        .take(12)

    return MediaDetail(
        item = item,
        genres = genres.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        runtimeText = poiskRuntimeText(item.kind),
        statusText = status?.ifBlank { null } ?: "Данные PoiskKino",
        seasons = item.poiskSeasons(seasonsInfo),
        prequels = prequels,
        sequels = sequels,
        related = related
    )
}

private fun PoiskKinoMovieDto.poiskRuntimeText(kind: MediaKind): String {
    return when (kind) {
        MediaKind.Movie -> movieLength?.takeIf { it > 0 }?.let { "$it мин" } ?: "Хронометраж неизвестен"
        MediaKind.Tv -> {
            val seasons = seasonsInfo
                .mapNotNull { it.number }
                .distinct()
                .count()
                .takeIf { it > 0 }
            val episodes = seasonsInfo.sumOf { it.episodesCount ?: 0 }.takeIf { it > 0 }
            val minutes = seriesLength ?: totalSeriesLength
            listOfNotNull(
                seasons?.let { "$it сез." },
                episodes?.let { "$it эп." },
                minutes?.takeIf { it > 0 }?.let { "$it мин" }
            ).joinToString(" • ").ifBlank { "Данные по сериям уточняются" }
        }
    }
}

private fun MediaItem.poiskSeasons(info: List<PoiskKinoSeasonInfoDto>): List<SeasonInfo> {
    if (kind != MediaKind.Tv) return emptyList()
    return info
        .mapNotNull { season ->
            val number = season.number?.takeIf { it > 0 } ?: return@mapNotNull null
            val count = season.episodesCount?.coerceAtLeast(0) ?: 0
            SeasonInfo(
                number = number,
                title = "Сезон $number",
                episodeCount = count,
                episodes = episodePlaceholders(number, count)
            )
        }
        .sortedBy { it.number }
}

private fun tmdbSeasons(
    totalSeasons: Int?,
    totalEpisodes: Int?,
    seasons: List<TmdbSeasonDto>
): List<SeasonInfo> {
    val mapped = seasons
        .filter { it.seasonNumber > 0 }
        .map { season ->
            val count = season.episodeCount.coerceAtLeast(0)
            SeasonInfo(
                number = season.seasonNumber,
                title = season.name?.takeIf(String::isNotBlank) ?: "Сезон ${season.seasonNumber}",
                episodeCount = count,
                episodes = episodePlaceholders(season.seasonNumber, count)
            )
        }
        .sortedBy { it.number }

    if (mapped.isNotEmpty()) return mapped

    val seasonCount = totalSeasons?.takeIf { it > 0 } ?: return emptyList()
    val perSeason = totalEpisodes
        ?.takeIf { it > 0 }
        ?.let { (it / seasonCount).coerceAtLeast(1) }
        ?: 0

    return (1..seasonCount).map { number ->
        SeasonInfo(
            number = number,
            title = "Сезон $number",
            episodeCount = perSeason,
            episodes = episodePlaceholders(number, perSeason)
        )
    }
}

private fun TmdbSeasonDto.toSeasonInfo(): SeasonInfo {
    val count = episodeCount.coerceAtLeast(0)
    return SeasonInfo(
        number = seasonNumber,
        title = name?.takeIf(String::isNotBlank) ?: "Сезон $seasonNumber",
        episodeCount = count,
        episodes = episodePlaceholders(seasonNumber, count)
    )
}

private fun TmdbSeasonDetailDto.toSeasonInfo(summary: TmdbSeasonDto): SeasonInfo {
    val resolvedSeasonNumber = seasonNumber.takeIf { it > 0 } ?: summary.seasonNumber
    val episodeList = episodes
        .filter { it.episodeNumber > 0 }
        .map { episode ->
            EpisodeInfo(
                seasonNumber = episode.seasonNumber.takeIf { it > 0 } ?: resolvedSeasonNumber,
                episodeNumber = episode.episodeNumber,
                title = episode.name?.takeIf(String::isNotBlank) ?: "Серия ${episode.episodeNumber}",
                overview = episode.overview.orEmpty().ifBlank { "Описание серии пока не добавлено." },
                posterUrl = episode.stillPath?.let(::stillUrl),
                serviceRating = episode.voteAverage.validRating(),
                airDate = episode.airDate.orEmpty(),
                runtimeMinutes = episode.runtime?.takeIf { it > 0 }
            )
        }
        .sortedBy { it.episodeNumber }

    return SeasonInfo(
        number = resolvedSeasonNumber,
        title = name?.takeIf(String::isNotBlank)
            ?: summary.name?.takeIf(String::isNotBlank)
            ?: "Сезон $resolvedSeasonNumber",
        episodeCount = episodeList.size.takeIf { it > 0 } ?: summary.episodeCount.coerceAtLeast(0),
        episodes = episodeList.ifEmpty { episodePlaceholders(resolvedSeasonNumber, summary.episodeCount) }
    )
}

private fun TmdbSeasonDetailDto.toSeasonInfo(summary: SeasonInfo): SeasonInfo {
    val resolvedSeasonNumber = seasonNumber.takeIf { it > 0 } ?: summary.number
    val episodeList = episodes
        .filter { it.episodeNumber > 0 }
        .map { episode ->
            EpisodeInfo(
                seasonNumber = episode.seasonNumber.takeIf { it > 0 } ?: resolvedSeasonNumber,
                episodeNumber = episode.episodeNumber,
                title = episode.name?.takeIf(String::isNotBlank) ?: "Серия ${episode.episodeNumber}",
                overview = episode.overview.orEmpty().ifBlank { "Описание серии пока не добавлено." },
                posterUrl = episode.stillPath?.let(::stillUrl),
                serviceRating = episode.voteAverage.validRating(),
                airDate = episode.airDate.orEmpty(),
                runtimeMinutes = episode.runtime?.takeIf { it > 0 }
            )
        }
        .sortedBy { it.episodeNumber }

    return SeasonInfo(
        number = resolvedSeasonNumber,
        title = name?.takeIf(String::isNotBlank)
            ?: summary.title.takeIf(String::isNotBlank)
            ?: "Сезон $resolvedSeasonNumber",
        episodeCount = episodeList.size.takeIf { it > 0 } ?: summary.episodeCount,
        episodes = episodeList.ifEmpty { summary.episodes }
    )
}

private fun episodePlaceholders(seasonNumber: Int, count: Int): List<EpisodeInfo> {
    return (1..count.coerceAtLeast(0)).map { episodeNumber ->
        EpisodeInfo(
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = "Серия $episodeNumber"
        )
    }
}

private fun poiskKind(type: String?, isSeries: Boolean?): MediaKind {
    return when {
        isSeries == true -> MediaKind.Tv
        type in setOf("tv-series", "animated-series", "tv-show") -> MediaKind.Tv
        else -> MediaKind.Movie
    }
}

private fun PoiskKinoRatingDto?.toRatings(): MediaRatings {
    return MediaRatings(
        kp = this?.kp.validRating(),
        imdb = this?.imdb.validRating(),
        tmdb = this?.tmdb.validRating()
    )
}

private fun PoiskKinoTokenInfoDto.toApiQuota(): ApiQuota {
    return ApiQuota(
        limit = requestsLimit,
        used = requestsUsed,
        remaining = requestsRemaining
    )
}

private suspend fun loadKinopoiskLinkItem(
    link: String,
    kind: MediaKind,
    sourceId: Int,
    client: OkHttpClient
): MediaItem = withContext(Dispatchers.IO) {
    val html = runCatching {
        val request = Request.Builder()
            .url(link)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.7")
            .build()
        client.newCall(request).execute().use { response ->
            response.body?.string().orEmpty()
        }
    }.getOrElse { error ->
        Log.w("CineRepository", "Kinopoisk manual page failed", error)
        ""
    }

    val plainText = html.stripHtml()
    val rawTitle = html.metaContent("og:title")
        ?: html.titleContent()
        ?: plainText.lineSequence().firstOrNull { it.isNotBlank() }
    val title = rawTitle
        ?.cleanKinopoiskTitle()
        ?.takeUnless { it.equals("Похоже, вы используете VPN", ignoreCase = true) }
        ?.takeIf(String::isNotBlank)
        ?: "Кинопоиск #$sourceId"
    val description = html.metaContent("description")
        ?.decodeHtmlEntities()
        ?.takeIf(String::isNotBlank)
        ?: plainText.substringAfter(title, "").trim().take(220).takeIf(String::isNotBlank)
        ?: "Добавлено вручную по ссылке Кинопоиска."
    val year = (rawTitle.orEmpty() + " " + plainText)
        .let { Regex("""\b(?:19|20)\d{2}\b""").find(it)?.value }
        ?: "----"
    val kpRating = Regex("""Рейтинг\s+Кинопоиска\s+([0-9]+[,.][0-9]+)""")
        .find(plainText)
        ?.groupValues
        ?.getOrNull(1)
        ?.ratingFromText()
    val imdbRating = Regex("""IMDb:\s*([0-9]+[,.][0-9]+)""")
        .find(plainText)
        ?.groupValues
        ?.getOrNull(1)
        ?.ratingFromText()
    val ratings = MediaRatings(kp = kpRating, imdb = imdbRating)

    MediaItem(
        id = manualId("kinopoisk:${kind.routeValue}:$sourceId"),
        kind = kind,
        title = title,
        subtitle = "${kind.label} • $year • Кинопоиск",
        overview = description,
        posterUrl = html.metaContent("og:image"),
        backdropUrl = null,
        rating = ratings.primaryScore,
        year = year,
        ratings = ratings
    )
}

private fun TmdbDetailDto.toDetail(kind: MediaKind, loadedSeasons: List<SeasonInfo> = emptyList()): MediaDetail {
    val mediaTitle = title ?: name ?: "Без названия"
    val date = releaseDate ?: firstAirDate.orEmpty()
    val year = date.take(4).ifBlank { "----" }
    val score = voteAverage.validRating() ?: 0.0
    val item = MediaItem(
        id = id,
        kind = kind,
        title = mediaTitle,
        subtitle = "${kind.label} • $year",
        overview = overview.orEmpty().ifBlank { "Описание пока не добавлено." },
        posterUrl = posterPath?.let(::posterUrl),
        backdropUrl = backdropPath?.let(::backdropUrl),
        rating = score,
        year = year,
        runtimeMinutes = when (kind) {
            MediaKind.Movie -> runtime?.takeIf { it > 0 }
            MediaKind.Tv -> episodeRunTime?.firstOrNull { it > 0 }
        },
        ratings = MediaRatings(tmdb = score.takeIf { it > 0.0 })
    )

    val runtimeText = when (kind) {
        MediaKind.Movie -> runtime?.takeIf { it > 0 }?.let { "$it мин" } ?: "Хронометраж неизвестен"
        MediaKind.Tv -> {
            val episodeTime = episodeRunTime?.firstOrNull()?.takeIf { it > 0 }
            val seasons = numberOfSeasons?.let { "$it сез." }
            val episodes = numberOfEpisodes?.let { "$it эп." }
            listOfNotNull(seasons, episodes, episodeTime?.let { "$it мин" }).joinToString(" • ")
                .ifBlank { "Данные по сериям уточняются" }
        }
    }

    return MediaDetail(
        item = item,
        genres = genres.mapNotNull { it.name.takeIf(String::isNotBlank) },
        runtimeText = runtimeText,
        statusText = status ?: "Статус неизвестен",
        seasons = if (kind == MediaKind.Tv) {
            loadedSeasons.ifEmpty { tmdbSeasons(numberOfSeasons, numberOfEpisodes, seasons) }
        } else {
            emptyList()
        }
    )
}

private fun posterUrl(path: String): String = "${BuildConfig.TMDB_IMAGE_BASE_URL.trimEnd('/')}/w342$path"

private fun backdropUrl(path: String): String = "${BuildConfig.TMDB_IMAGE_BASE_URL.trimEnd('/')}/w780$path"

private fun stillUrl(path: String): String = "${BuildConfig.TMDB_IMAGE_BASE_URL.trimEnd('/')}/w300$path"

private fun String?.normalizedDate(): String {
    return this?.take(10).orEmpty()
}

private fun Double?.validRating(): Double? {
    return this
        ?.takeIf { !it.isNaN() && it > 0.0 }
        ?.coerceAtMost(10.0)
}

private fun String.ratingFromText(): Double? {
    return replace(',', '.').toDoubleOrNull().validRating()
}

private fun String.stripHtml(): String {
    return replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<[^>]+>"""), " ")
        .decodeHtmlEntities()
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.metaContent(name: String): String? {
    val escaped = Regex.escape(name)
    val propertyFirst = Regex(
        """<meta[^>]+(?:property|name)=["']$escaped["'][^>]+content=["']([^"']*)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    val contentFirst = Regex(
        """<meta[^>]+content=["']([^"']*)["'][^>]+(?:property|name)=["']$escaped["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )

    return (propertyFirst.find(this)?.groupValues?.getOrNull(1)
        ?: contentFirst.find(this)?.groupValues?.getOrNull(1))
        ?.decodeHtmlEntities()
        ?.trim()
        ?.takeIf(String::isNotBlank)
}

private fun String.titleContent(): String? {
    return Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.decodeHtmlEntities()
        ?.trim()
        ?.takeIf(String::isNotBlank)
}

private fun String.cleanKinopoiskTitle(): String {
    return decodeHtmlEntities()
        .substringBefore(" смотреть онлайн")
        .substringBefore(" смотреть фильм")
        .substringBefore(" смотреть сериал")
        .substringBefore(" — Кинопоиск")
        .substringBefore(" | Кинопоиск")
        .trim(' ', '\n', '\t', ',', '.', '-')
}

private fun String.decodeHtmlEntities(): String {
    return replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&#160;", " ")
        .replace("&#39;", "'")
}

private fun manualId(source: String): Int {
    return 1_000_000_000 + (source.hashCode() and 0x3FFFFFFF)
}

private fun List<MediaItem>.distinctByMedia(): List<MediaItem> {
    return distinctBy { "${it.kind.routeValue}:${it.id}" }
}

private fun emptyHomeFeed(message: String? = null): HomeFeed {
    return HomeFeed(
        hero = null,
        trending = emptyList(),
        popularMovies = emptyList(),
        popularShows = emptyList(),
        anime = emptyList(),
        newReleases = emptyList(),
        message = message
    )
}

private fun missingDetail(kind: MediaKind, id: Int, message: String): MediaDetail {
    return MediaDetail(
        item = MediaItem(
            id = id,
            kind = kind,
            title = "Не удалось загрузить",
            subtitle = kind.label,
            overview = message,
            posterUrl = null,
            backdropUrl = null,
            rating = 0.0,
            year = "----"
        ),
        genres = emptyList(),
        runtimeText = "Данные недоступны",
        statusText = "Ошибка загрузки"
    )
}

private fun tmdbFailureMessage(error: Throwable): String {
    val trace = generateSequence(error) { it.cause }
        .joinToString(" ") { it.message.orEmpty() }

    return if ("127.0.0.1" in trace || "localhost" in trace) {
        "TMDb заблокирован DNS/AdGuard на телефоне. Проверь прокси или сеть."
    } else {
        "Не удалось загрузить TMDb. Проверь подключение или прокси."
    }
}
