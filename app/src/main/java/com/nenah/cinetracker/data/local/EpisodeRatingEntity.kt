package com.nenah.cinetracker.data.local

import androidx.room.Entity

@Entity(
    tableName = "episode_ratings",
    primaryKeys = ["mediaId", "kind", "seasonNumber", "episodeNumber"]
)
data class EpisodeRatingEntity(
    val mediaId: Int,
    val kind: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val rating: Int,
    val runtimeMinutes: Int? = null,
    val updatedAt: Long
)
