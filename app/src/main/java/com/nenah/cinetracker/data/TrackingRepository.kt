package com.nenah.cinetracker.data

import android.content.Context
import com.nenah.cinetracker.data.local.CineDatabase
import com.nenah.cinetracker.data.local.CollectionEntity
import com.nenah.cinetracker.data.local.CollectionItemEntity
import com.nenah.cinetracker.data.local.CollectionSummaryRow
import com.nenah.cinetracker.data.local.EpisodeRatingEntity
import com.nenah.cinetracker.data.local.TrackedTitleEntity
import com.nenah.cinetracker.data.local.TrackerEventEntity
import com.nenah.cinetracker.model.EpisodeRating
import com.nenah.cinetracker.model.MediaCollection
import com.nenah.cinetracker.model.MediaItem
import com.nenah.cinetracker.model.MediaKind
import com.nenah.cinetracker.model.TrackStatus
import com.nenah.cinetracker.model.TrackedTitle
import com.nenah.cinetracker.model.TrackerEvent
import com.nenah.cinetracker.model.TrackerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackingRepository(context: Context) {
    private val dao = CineDatabase.getInstance(context).trackedTitleDao()

    fun observeAll(): Flow<List<TrackedTitle>> = dao.observeAll().map { entities ->
        entities.map(TrackedTitleEntity::toTrackedTitle)
    }

    fun observeOne(item: MediaItem): Flow<TrackedTitle?> {
        return dao.observeOne(item.id, item.kind.routeValue).map { it?.toTrackedTitle() }
    }

    fun observeStats(): Flow<TrackerStats> = observeAll().map { titles ->
        TrackerStats(
            watching = titles.count { it.status == TrackStatus.Watching },
            planned = titles.count { it.status == TrackStatus.Planned },
            watched = titles.count { it.status == TrackStatus.Watched },
            ratedTitles = titles.count { it.personalRating != null },
            watchedMinutes = titles
                .filter { it.status == TrackStatus.Watched && it.item.kind == MediaKind.Movie }
                .sumOf { it.item.runtimeMinutes ?: 100 }
        )
    }

    fun observeEpisodeRatings(item: MediaItem): Flow<List<EpisodeRating>> {
        return dao.observeEpisodeRatings(item.id, item.kind.routeValue).map { ratings ->
            ratings.map(EpisodeRatingEntity::toEpisodeRating)
        }
    }

    fun observeEpisodeRatingCount(): Flow<Int> = dao.observeEpisodeRatingCount()

    fun observeEpisodeWatchedMinutes(): Flow<Int> = dao.observeEpisodeWatchedMinutes()

    fun observeRecentEvents(): Flow<List<TrackerEvent>> {
        return dao.observeRecentEvents().map { events ->
            events.map { TrackerEvent(it.id, it.message, it.createdAt) }
        }
    }

    fun observeCollections(): Flow<List<MediaCollection>> {
        return dao.observeCollections().map { rows ->
            rows.map(CollectionSummaryRow::toMediaCollection)
        }
    }

    fun observeCollectionIdsForItem(item: MediaItem): Flow<Set<Long>> {
        return dao.observeCollectionIdsForItem(item.id, item.kind.routeValue).map { it.toSet() }
    }

    fun observeCollectionItems(collectionId: Long): Flow<List<MediaItem>> {
        return dao.observeCollectionItems(collectionId).map { items ->
            items.map { it.toMediaItem() }
        }
    }

    suspend fun setRating(item: MediaItem, rating: Int) {
        val existing = dao.getOne(item.id, item.kind.routeValue) ?: return
        dao.upsert(existing.copy(personalRating = rating, updatedAt = System.currentTimeMillis()))
        logEvent("Оценка $rating: ${item.title}")
    }

    suspend fun setEpisodeRating(
        item: MediaItem,
        seasonNumber: Int,
        episodeNumber: Int,
        rating: Int,
        runtimeMinutes: Int?
    ) {
        dao.upsertEpisodeRating(
            EpisodeRatingEntity(
                mediaId = item.id,
                kind = item.kind.routeValue,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                rating = rating,
                runtimeMinutes = runtimeMinutes,
                updatedAt = System.currentTimeMillis()
            )
        )
        logEvent("Серия S$seasonNumber E$episodeNumber оценена на $rating: ${item.title}")
    }

    suspend fun createCollection(name: String): Long {
        val cleanName = name.trim().take(36)
        if (cleanName.isBlank()) return 0
        val id = dao.insertCollection(
            CollectionEntity(
                name = cleanName,
                createdAt = System.currentTimeMillis()
            )
        )
        logEvent("Создана коллекция: $cleanName")
        return id
    }

    suspend fun addToCollection(collectionId: Long, item: MediaItem) {
        if (collectionId <= 0) return
        dao.upsertCollectionItem(
            CollectionItemEntity(
                collectionId = collectionId,
                mediaId = item.id,
                kind = item.kind.routeValue,
                title = item.title,
                subtitle = item.subtitle,
                overview = item.overview,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                rating = item.rating,
                year = item.year,
                runtimeMinutes = item.runtimeMinutes,
                addedAt = System.currentTimeMillis()
            )
        )
        logEvent("Добавлено в коллекцию: ${item.title}")
    }

    suspend fun removeFromCollection(collectionId: Long, item: MediaItem) {
        dao.deleteCollectionItem(collectionId, item.id, item.kind.routeValue)
        logEvent("Удалено из коллекции: ${item.title}")
    }

    suspend fun setStatus(item: MediaItem, status: TrackStatus) {
        val now = System.currentTimeMillis()
        val existing = dao.getOne(item.id, item.kind.routeValue)
        val progress = when {
            status == TrackStatus.Watched -> 1f
            existing != null -> existing.progress
            status == TrackStatus.Watching -> 0.08f
            else -> 0f
        }

        dao.upsert(
            TrackedTitleEntity(
                mediaId = item.id,
                kind = item.kind.routeValue,
                title = item.title,
                subtitle = item.subtitle,
                overview = item.overview,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                rating = item.rating,
                year = item.year,
                runtimeMinutes = item.runtimeMinutes,
                status = status.routeValue,
                progress = progress.coerceIn(0f, 1f),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                personalRating = existing?.personalRating
            )
        )
        logEvent("${status.title}: ${item.title}")
    }

    suspend fun remove(item: MediaItem) {
        dao.delete(item.id, item.kind.routeValue)
        logEvent("Удалено из трекера: ${item.title}")
    }

    private suspend fun logEvent(message: String) {
        dao.insertEvent(
            TrackerEventEntity(
                message = message,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

private fun CollectionItemEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = mediaId,
        kind = MediaKind.fromRoute(kind),
        title = title,
        subtitle = subtitle,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        rating = rating,
        year = year,
        runtimeMinutes = runtimeMinutes
    )
}

private fun EpisodeRatingEntity.toEpisodeRating(): EpisodeRating {
    return EpisodeRating(
        mediaId = mediaId,
        kind = MediaKind.fromRoute(kind),
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        rating = rating,
        runtimeMinutes = runtimeMinutes
    )
}

private fun CollectionSummaryRow.toMediaCollection(): MediaCollection {
    return MediaCollection(
        id = id,
        name = name,
        itemCount = itemCount
    )
}

private fun TrackedTitleEntity.toTrackedTitle(): TrackedTitle {
    val kind = MediaKind.fromRoute(kind)
    return TrackedTitle(
        item = MediaItem(
            id = mediaId,
            kind = kind,
            title = title,
            subtitle = subtitle,
            overview = overview,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = rating,
            year = year,
            runtimeMinutes = runtimeMinutes
        ),
        status = TrackStatus.fromRoute(status),
        progress = progress.coerceIn(0f, 1f),
        updatedAt = updatedAt,
        personalRating = personalRating
    )
}
