package com.nenah.cinetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nenah.cinetracker.data.AppSettingsRepository
import com.nenah.cinetracker.data.CineRepository
import com.nenah.cinetracker.data.TrackingRepository
import com.nenah.cinetracker.model.ApiQuota
import com.nenah.cinetracker.model.CineAppTheme
import com.nenah.cinetracker.model.MediaCollection
import com.nenah.cinetracker.model.HomeFeed
import com.nenah.cinetracker.model.MediaDetail
import com.nenah.cinetracker.model.MediaItem
import com.nenah.cinetracker.model.MediaKind
import com.nenah.cinetracker.model.SeasonInfo
import com.nenah.cinetracker.model.TrackStatus
import com.nenah.cinetracker.model.TrackedTitle
import com.nenah.cinetracker.model.TrackerEvent
import com.nenah.cinetracker.model.TrackerStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val AiChatModelName = "gemini-2.5-flash"

data class AiChatMessage(
    val text: String,
    val isUser: Boolean,
    val recommendations: List<MediaItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class CineUiState(
    val homeFeed: HomeFeed? = null,
    val isHomeLoading: Boolean = true,
    val isTmdbConfigured: Boolean = false,
    val selectedDetail: MediaDetail? = null,
    val isDetailLoading: Boolean = false,
    val selectedTrackedTitle: TrackedTitle? = null,
    val trackedTitles: List<TrackedTitle> = emptyList(),
    val trackerStats: TrackerStats = TrackerStats(),
    val apiQuota: ApiQuota? = null,
    val isApiQuotaLoading: Boolean = false,
    val appTheme: CineAppTheme = CineAppTheme.Cinema,
    val episodeRatings: Map<String, Int> = emptyMap(),
    val collections: List<MediaCollection> = emptyList(),
    val selectedCollectionIds: Set<Long> = emptySet(),
    val recentEvents: List<TrackerEvent> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<MediaItem> = emptyList(),
    val isSearchLoading: Boolean = false,
    val aiChatMessages: List<AiChatMessage> = emptyList(),
    val aiChatInput: String = "",
    val isAiChatLoading: Boolean = false,
    val aiChatError: String? = null,
    val manualLink: String = "",
    val isManualAddLoading: Boolean = false,
    val manualAddMessage: String? = null,
    val rouletteResult: MediaItem? = null,
    val isRouletteLoading: Boolean = false,
    val currentCollectionItems: List<MediaItem> = emptyList(),
    val libraryStatusFilter: TrackStatus? = null
)

class CineViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = CineRepository(application)
    private val trackingRepository = TrackingRepository(application)
    private val settingsRepository = AppSettingsRepository(application)
    private val _uiState = MutableStateFlow(
        CineUiState(
            isTmdbConfigured = repository.isConfigured,
            appTheme = settingsRepository.currentTheme
        )
    )
    val uiState: StateFlow<CineUiState> = _uiState.asStateFlow()

    val filteredLibraryTitles = combine(
        _uiState.map { it.trackedTitles },
        _uiState.map { it.libraryStatusFilter }
    ) { titles, status ->
        titles.filter { status == null || it.status == status }
              .sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null
    private var detailTrackingJob: Job? = null
    private var episodeRatingsJob: Job? = null
    private var detailCollectionsJob: Job? = null
    private var collectionItemsJob: Job? = null
    private var pendingDetailItem: MediaItem? = null
    private var titleWatchedMinutes: Int = 0
    private var episodeWatchedMinutes: Int = 0
    private val seasonLoadsInFlight = mutableSetOf<String>()

    init {
        observeSettings()
        observeTracker()
        refreshHome()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.theme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    private fun observeTracker() {
        viewModelScope.launch {
            trackingRepository.observeAll().collect { titles ->
                _uiState.update { it.copy(trackedTitles = titles) }
            }
        }
        viewModelScope.launch {
            trackingRepository.observeStats().collect { stats ->
                titleWatchedMinutes = stats.watchedMinutes
                _uiState.update {
                    it.copy(
                        trackerStats = stats.copy(
                            ratedEpisodes = it.trackerStats.ratedEpisodes,
                            collections = it.trackerStats.collections,
                            watchedMinutes = titleWatchedMinutes + episodeWatchedMinutes
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            trackingRepository.observeEpisodeRatingCount().collect { count ->
                _uiState.update { it.copy(trackerStats = it.trackerStats.copy(ratedEpisodes = count)) }
            }
        }
        viewModelScope.launch {
            trackingRepository.observeEpisodeWatchedMinutes().collect { minutes ->
                episodeWatchedMinutes = minutes
                _uiState.update {
                    it.copy(
                        trackerStats = it.trackerStats.copy(
                            watchedMinutes = titleWatchedMinutes + episodeWatchedMinutes
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            trackingRepository.observeCollections().collect { collections ->
                _uiState.update {
                    it.copy(
                        collections = collections,
                        trackerStats = it.trackerStats.copy(collections = collections.size)
                    )
                }
            }
        }
        viewModelScope.launch {
            trackingRepository.observeRecentEvents().collect { events ->
                _uiState.update { it.copy(recentEvents = events) }
            }
        }
    }

    fun refreshHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHomeLoading = true) }
            val feed = repository.loadHome()
            _uiState.update { it.copy(homeFeed = feed, isHomeLoading = false) }
        }
    }

    fun refreshApiQuota() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApiQuotaLoading = true) }
            val quota = repository.loadApiQuota()
            _uiState.update { it.copy(apiQuota = quota, isApiQuotaLoading = false) }
        }
    }

    fun openDetail(kind: MediaKind, id: Int) {
        viewModelScope.launch {
            val fallbackItem = pendingDetailItem?.takeIf { it.kind == kind && it.id == id }
            val fallbackDetail = fallbackItem?.toStoredDetail()
            seasonLoadsInFlight.clear()
            _uiState.update {
                it.copy(
                    isDetailLoading = fallbackDetail == null,
                    selectedDetail = fallbackDetail,
                    selectedTrackedTitle = null,
                    episodeRatings = emptyMap(),
                    selectedCollectionIds = emptySet()
                )
            }
            fallbackItem?.let(::observeSelectedTitle)
            val detail = repository.details(kind, id)
            val finalDetail = if (detail.item.isMissingCatalogItem() && fallbackDetail != null) {
                fallbackDetail
            } else {
                detail
            }
            _uiState.update { it.copy(selectedDetail = finalDetail, isDetailLoading = false) }
            observeSelectedTitle(finalDetail.item)
            finalDetail.seasons.firstOrNull()?.number?.let(::loadSeasonDetails)
        }
    }

    fun loadSeasonDetails(seasonNumber: Int) {
        val detail = _uiState.value.selectedDetail ?: return
        if (detail.item.kind != MediaKind.Tv) return
        val season = detail.seasons.firstOrNull { it.number == seasonNumber } ?: return
        if (season.hasLoadedEpisodeDetails()) return

        val loadKey = "${detail.item.id}:$seasonNumber"
        if (!seasonLoadsInFlight.add(loadKey)) return

        viewModelScope.launch {
            val loadedSeason = repository.seasonDetails(detail.item.id, season)
            if (loadedSeason != null) {
                _uiState.update { state ->
                    val currentDetail = state.selectedDetail ?: return@update state
                    if (currentDetail.item.id != detail.item.id || currentDetail.item.kind != detail.item.kind) {
                        state
                    } else {
                        state.copy(
                            selectedDetail = currentDetail.copy(
                                seasons = currentDetail.seasons.map { currentSeason ->
                                    if (currentSeason.number == loadedSeason.number) loadedSeason else currentSeason
                                }
                            )
                        )
                    }
                }
            }
            seasonLoadsInFlight.remove(loadKey)
        }
    }

    fun rememberDetailItem(item: MediaItem) {
        pendingDetailItem = item
    }

    private fun observeSelectedTitle(item: MediaItem) {
        detailTrackingJob?.cancel()
        detailTrackingJob = viewModelScope.launch {
            trackingRepository.observeOne(item).collect { trackedTitle ->
                _uiState.update { it.copy(selectedTrackedTitle = trackedTitle) }
            }
        }
        episodeRatingsJob?.cancel()
        episodeRatingsJob = viewModelScope.launch {
            trackingRepository.observeEpisodeRatings(item).collect { ratings ->
                _uiState.update { state ->
                    state.copy(episodeRatings = ratings.associate { it.key to it.rating })
                }
            }
        }
        detailCollectionsJob?.cancel()
        detailCollectionsJob = viewModelScope.launch {
            trackingRepository.observeCollectionIdsForItem(item).collect { ids ->
                _uiState.update { it.copy(selectedCollectionIds = ids) }
            }
        }
    }

    fun setSelectedStatus(status: TrackStatus) {
        val item = _uiState.value.selectedDetail?.item ?: return
        viewModelScope.launch {
            trackingRepository.setStatus(item, status)
        }
    }

    fun setSelectedRating(rating: Int) {
        val item = _uiState.value.selectedDetail?.item ?: return
        viewModelScope.launch {
            trackingRepository.setRating(item, rating)
        }
    }

    fun setEpisodeRating(seasonNumber: Int, episodeNumber: Int, rating: Int, runtimeMinutes: Int?) {
        val item = _uiState.value.selectedDetail?.item ?: return
        viewModelScope.launch {
            trackingRepository.setEpisodeRating(item, seasonNumber, episodeNumber, rating, runtimeMinutes)
        }
    }

    fun removeSelectedFromTracker() {
        val item = _uiState.value.selectedDetail?.item ?: return
        viewModelScope.launch {
            trackingRepository.remove(item)
        }
    }

    fun setTheme(theme: CineAppTheme) {
        settingsRepository.setTheme(theme)
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            trackingRepository.createCollection(name)
        }
    }

    fun createCollectionForSelected(name: String) {
        val item = _uiState.value.selectedDetail?.item ?: return
        viewModelScope.launch {
            val id = trackingRepository.createCollection(name)
            if (id > 0) {
                trackingRepository.addToCollection(id, item)
            }
        }
    }

    fun toggleSelectedCollection(collectionId: Long) {
        val item = _uiState.value.selectedDetail?.item ?: return
        val isSelected = collectionId in _uiState.value.selectedCollectionIds
        viewModelScope.launch {
            if (isSelected) {
                trackingRepository.removeFromCollection(collectionId, item)
            } else {
                trackingRepository.addToCollection(collectionId, item)
            }
        }
    }

    fun loadCollectionItems(collectionId: Long) {
        collectionItemsJob?.cancel()
        collectionItemsJob = viewModelScope.launch {
            trackingRepository.observeCollectionItems(collectionId).collect { items ->
                _uiState.update { it.copy(currentCollectionItems = items) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            if (query.isBlank()) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearchLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(isSearchLoading = true) }
            val results = repository.search(query)
            _uiState.update { it.copy(searchResults = results, isSearchLoading = false) }
        }
    }

    fun updateManualLink(link: String) {
        _uiState.update { it.copy(manualLink = link, manualAddMessage = null) }
    }

    fun addManualLinkToPlan() {
        val link = _uiState.value.manualLink.trim()
        if (link.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isManualAddLoading = true, manualAddMessage = null) }
            val item = repository.resolveManualLink(link)
            if (item == null) {
                _uiState.update {
                    it.copy(
                        isManualAddLoading = false,
                        manualAddMessage = "Не получилось распознать ссылку."
                    )
                }
                return@launch
            }

            trackingRepository.setStatus(item, TrackStatus.Planned)
            _uiState.update {
                it.copy(
                    manualLink = "",
                    isManualAddLoading = false,
                    manualAddMessage = "Добавлено в план: ${item.title}"
                )
            }
        }
    }

    fun rollRoulette() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRouletteLoading = true, rouletteResult = null) }
            val result = repository.rollRoulette()
            _uiState.update { it.copy(rouletteResult = result, isRouletteLoading = false) }
        }
    }

    fun updateAiChatInput(input: String) {
        _uiState.update { it.copy(aiChatInput = input, aiChatError = null) }
    }

    fun sendAiChatMessage() {
        val userText = _uiState.value.aiChatInput.trim()
        if (userText.isBlank() || _uiState.value.isAiChatLoading) return

        val userMessage = AiChatMessage(text = userText, isUser = true)
        _uiState.update {
            it.copy(
                aiChatMessages = it.aiChatMessages + userMessage,
                aiChatInput = "",
                aiChatError = null,
                isAiChatLoading = true
            )
        }

        viewModelScope.launch {
            val apiKey = com.nenah.cinetracker.BuildConfig.GEMINI_API_KEY.trim()
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        aiChatMessages = it.aiChatMessages + AiChatMessage(
                            text = "AI-чат не настроен. Добавь gemini.api.key в local.properties или переменную окружения GEMINI_API_KEY.",
                            isUser = false
                        ),
                        isAiChatLoading = false,
                        aiChatError = "Не настроен ключ Gemini."
                    )
                }
                return@launch
            }

            try {
                val model = com.google.ai.client.generativeai.GenerativeModel(
                    modelName = AiChatModelName,
                    apiKey = apiKey
                )
                val response = model.generateContent(buildAiChatPrompt(userText))
                val answer = response.text?.trim()
                val recommendations = resolveAiRecommendations(answer.orEmpty())
                _uiState.update {
                    it.copy(
                        aiChatMessages = it.aiChatMessages + AiChatMessage(
                            text = if (answer.isNullOrBlank()) {
                                "Не смог собрать ответ. Попробуй спросить чуть иначе."
                            } else {
                                answer
                            },
                            isUser = false,
                            recommendations = recommendations
                        ),
                        isAiChatLoading = false,
                        aiChatError = null
                    )
                }
            } catch (e: Exception) {
                val errorMessage = aiChatErrorMessage(e)
                _uiState.update {
                    it.copy(
                        aiChatMessages = it.aiChatMessages + AiChatMessage(
                            text = errorMessage,
                            isUser = false
                        ),
                        isAiChatLoading = false,
                        aiChatError = errorMessage
                    )
                }
            }
        }
    }

    private fun buildAiChatPrompt(userText: String): String {
        val state = _uiState.value
        val trackedContext = state.trackedTitles
            .sortedByDescending { it.updatedAt }
            .take(80)
            .joinToString("\n") { tracked ->
                val rating = tracked.personalRating?.let { ", оценка $it/10" }.orEmpty()
                "- ${tracked.item.title} (${tracked.item.year.ifBlank { "год неизвестен" }}), ${tracked.item.kind.label}, статус: ${tracked.status.title}$rating"
            }
            .ifBlank { "- Пользователь пока почти не заполнил список." }

        val catalogueContext = state.homeFeed?.let { feed ->
            (feed.trending + feed.newReleases + feed.popularMovies + feed.popularShows + feed.anime)
                .distinctBy { it.kind.routeValue + ":" + it.id }
                .take(50)
                .joinToString("\n") { item ->
                    val score = item.ratings.primaryScore.takeIf { rating -> rating > 0.0 } ?: item.rating
                    "- ${item.title} (${item.year.ifBlank { "год неизвестен" }}), ${item.kind.label}, рейтинг $score"
                }
        }.orEmpty().ifBlank { "- Каталог еще не загружен." }

        val recentDialog = state.aiChatMessages
            .takeLast(10)
            .joinToString("\n") { message ->
                "${if (message.isUser) "Пользователь" else "AI"}: ${message.text}"
            }

        return """
            Формат ответа:
            - Пиши коротко, без длинного вступления.
            - Не используй Markdown-символы **, *, #.
            - Давай 3-5 рекомендаций. Каждый пункт начинай строго с номера: 1. Название (год) — фильм/сериал
            - После каждого названия обязательно пиши строку "Почему:".
            - Если знаешь рейтинг, можно добавить "Рейтинг:", но не выдумывай его.
            - Для рекомендаций используй формат:
              1. Название (год) — фильм/сериал
              Почему: одна короткая причина.
              Кинопоиск: https://www.kinopoisk.ru/index.php?kp_query=название+год
              TMDb: https://www.themoviedb.org/search?query=название+год
            - Не придумывай прямые страницы фильмов, давай только поисковые ссылки.

            Ты кино-ассистент внутри приложения CineTracker. Отвечай на русском, дружелюбно и по делу.
            Подбирай фильмы и сериалы под вкус пользователя, учитывай оценки, статусы и уже просмотренное.
            Если советуешь тайтлы, дай 3-7 вариантов с короткой причиной, годом и пометкой фильм/сериал.
            Не советуй то, что явно уже просмотрено, если пользователь сам не просит обсудить просмотренное.

            Список пользователя:
            $trackedContext

            Текущий каталог/тренды:
            $catalogueContext

            Последний диалог:
            $recentDialog

            Новый вопрос пользователя:
            $userText
        """.trimIndent()
    }

    private data class AiRecommendationQuery(
        val title: String,
        val year: String?
    )

    private suspend fun resolveAiRecommendations(answer: String): List<MediaItem> {
        val queries = extractAiRecommendationQueries(answer)
        if (queries.isEmpty()) return emptyList()

        val resolved = mutableListOf<MediaItem>()
        val seenKeys = mutableSetOf<String>()

        for (query in queries) {
            val searchQuery = listOfNotNull(query.title, query.year).joinToString(" ")
            val item = bestAiRecommendationMatch(repository.search(searchQuery), query) ?: continue
            val key = "${item.kind.routeValue}:${item.id}"
            if (seenKeys.add(key)) {
                resolved += item
            }
            if (resolved.size >= 8) break
        }

        return resolved
    }

    private fun extractAiRecommendationQueries(answer: String): List<AiRecommendationQuery> {
        val numberedLine = Regex("""^\s*\d+\.\s+(.+?)(?:\s+\((\d{4})\))?(?:\s+[—-].*)?$""")
        return answer.lines()
            .mapNotNull { rawLine ->
                val line = rawLine.replace("**", "").trim()
                val match = numberedLine.find(line) ?: return@mapNotNull null
                val title = match.groupValues[1]
                    .substringBefore(" — ")
                    .substringBefore(" - ")
                    .trim()
                    .trim('"', '«', '»')
                if (title.length < 2) return@mapNotNull null
                AiRecommendationQuery(
                    title = title,
                    year = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                )
            }
            .distinctBy { "${it.title.lowercase()}-${it.year.orEmpty()}" }
            .take(8)
    }

    private fun bestAiRecommendationMatch(
        results: List<MediaItem>,
        query: AiRecommendationQuery
    ): MediaItem? {
        if (results.isEmpty()) return null
        val normalizedQueryTitle = query.title.normalizedAiTitle()
        return results.firstOrNull { item ->
            query.year != null &&
                item.year == query.year &&
                item.title.normalizedAiTitle().let { title ->
                    title.contains(normalizedQueryTitle) || normalizedQueryTitle.contains(title)
                }
        } ?: results.firstOrNull { item ->
            item.title.normalizedAiTitle().let { title ->
                title.contains(normalizedQueryTitle) || normalizedQueryTitle.contains(title)
            }
        } ?: results.firstOrNull { item ->
            query.year != null && item.year == query.year
        } ?: results.firstOrNull()
    }

    private fun String.normalizedAiTitle(): String {
        return lowercase()
            .replace(Regex("""[^\p{L}\p{Nd}]+"""), " ")
            .trim()
    }

    private fun aiChatErrorMessage(error: Throwable): String {
        val details = error.message.orEmpty()
        return when {
            details.contains("404") || details.contains("not found", ignoreCase = true) ->
                "AI-модель была недоступна. Я переключил чат на актуальную модель, попробуй отправить сообщение ещё раз."
            details.contains("API key", ignoreCase = true) ||
                details.contains("permission", ignoreCase = true) ||
                details.contains("unauthorized", ignoreCase = true) ->
                "AI-ключ не принят. Проверь ключ Gemini в local.properties или переменную GEMINI_API_KEY."
            details.contains("quota", ignoreCase = true) ||
                details.contains("429") ->
                "Лимит AI временно закончился. Попробуй ещё раз чуть позже."
            else ->
                "AI сейчас не смог ответить. Попробуй ещё раз через минуту."
        }
    }

    fun getAiRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true) }
            val model = com.google.ai.client.generativeai.GenerativeModel(
                modelName = AiChatModelName,
                apiKey = com.nenah.cinetracker.BuildConfig.GEMINI_API_KEY
            )
            
            val ratedTitles = _uiState.value.trackedTitles.filter { it.personalRating != null }
            val history = ratedTitles.joinToString("\n") { 
                "- ${it.item.title} (${it.item.year}), моя оценка: ${it.personalRating}/10" 
            }
            
            val prompt = "Я посмотрел следующие фильмы/сериалы: \n$history\n. " +
                         "На основе этих предпочтений посоветуй 5 фильмов, которые мне стоит посмотреть. " +
                         "Ответь списком, где указано название, год и короткая причина."

            try {
                val response = model.generateContent(prompt)
                _uiState.update { it.copy(manualAddMessage = response.text, isSearchLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(manualAddMessage = "Ошибка ИИ: ${e.message}", isSearchLoading = false) }
            }
        }
    }

    fun clearRoulette() {
        _uiState.update { it.copy(rouletteResult = null) }
    }

    fun setLibraryStatus(status: TrackStatus?) {
        _uiState.update { it.copy(libraryStatusFilter = status) }
    }
}

private fun MediaItem.toStoredDetail(): MediaDetail {
    return MediaDetail(
        item = this,
        genres = listOf("Кино"),
        runtimeText = "Добавлено вручную",
        statusText = "Сохранено в трекере"
    )
}

private fun SeasonInfo.hasLoadedEpisodeDetails(): Boolean {
    return episodes.any { episode ->
        episode.posterUrl != null ||
            episode.overview.isNotBlank() ||
            episode.serviceRating != null ||
            episode.airDate.isNotBlank() ||
            episode.runtimeMinutes != null
    }
}

private fun MediaItem.isMissingCatalogItem(): Boolean {
    return title == "Не удалось загрузить"
}
