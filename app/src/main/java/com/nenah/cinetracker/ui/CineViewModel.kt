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

    fun getAiRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true) }
            val model = com.google.ai.client.generativeai.GenerativeModel(
                modelName = "gemini-1.5-flash",
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
