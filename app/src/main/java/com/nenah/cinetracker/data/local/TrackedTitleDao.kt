package com.nenah.cinetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedTitleDao {
    @Query("SELECT * FROM tracked_titles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TrackedTitleEntity>>

    @Query("SELECT * FROM tracked_titles WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<TrackedTitleEntity>>

    @Query("SELECT * FROM tracked_titles WHERE mediaId = :mediaId AND kind = :kind LIMIT 1")
    fun observeOne(mediaId: Int, kind: String): Flow<TrackedTitleEntity?>

    @Query("SELECT * FROM tracked_titles WHERE mediaId = :mediaId AND kind = :kind LIMIT 1")
    suspend fun getOne(mediaId: Int, kind: String): TrackedTitleEntity?

    @Upsert
    suspend fun upsert(entity: TrackedTitleEntity)

    @Delete
    suspend fun delete(entity: TrackedTitleEntity)

    @Query("DELETE FROM tracked_titles WHERE mediaId = :mediaId AND kind = :kind")
    suspend fun delete(mediaId: Int, kind: String)

    @Query("SELECT * FROM episode_ratings WHERE mediaId = :mediaId AND kind = :kind")
    fun observeEpisodeRatings(mediaId: Int, kind: String): Flow<List<EpisodeRatingEntity>>

    @Query("SELECT COUNT(*) FROM episode_ratings")
    fun observeEpisodeRatingCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(COALESCE(runtimeMinutes, 45)), 0) FROM episode_ratings")
    fun observeEpisodeWatchedMinutes(): Flow<Int>

    @Upsert
    suspend fun upsertEpisodeRating(entity: EpisodeRatingEntity)

    @Query("DELETE FROM episode_ratings WHERE mediaId = :mediaId AND kind = :kind AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun deleteEpisodeRating(mediaId: Int, kind: String, seasonNumber: Int, episodeNumber: Int)

    @Query(
        """
        SELECT collections.id AS id, collections.name AS name, COUNT(collection_items.mediaId) AS itemCount
        FROM collections
        LEFT JOIN collection_items ON collections.id = collection_items.collectionId
        GROUP BY collections.id, collections.name, collections.createdAt
        ORDER BY collections.createdAt DESC
        """
    )
    fun observeCollections(): Flow<List<CollectionSummaryRow>>

    @Insert
    suspend fun insertCollection(entity: CollectionEntity): Long

    @Query("SELECT collectionId FROM collection_items WHERE mediaId = :mediaId AND kind = :kind")
    fun observeCollectionIdsForItem(mediaId: Int, kind: String): Flow<List<Long>>

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun observeCollectionItems(collectionId: Long): Flow<List<CollectionItemEntity>>

    @Upsert
    suspend fun upsertCollectionItem(entity: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND mediaId = :mediaId AND kind = :kind")
    suspend fun deleteCollectionItem(collectionId: Long, mediaId: Int, kind: String)

    @Query("SELECT * FROM tracker_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 20): Flow<List<TrackerEventEntity>>

    @Insert
    suspend fun insertEvent(entity: TrackerEventEntity)
}
