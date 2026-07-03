package com.nenah.cinetracker.data.local

import androidx.room.Entity

@Entity(
    tableName = "tracked_titles",
    primaryKeys = ["mediaId", "kind"]
)
data class TrackedTitleEntity(
    val mediaId: Int,
    val kind: String,
    val title: String,
    val subtitle: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double,
    val year: String,
    val runtimeMinutes: Int? = null,
    val status: String,
    val progress: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val personalRating: Int? = null
)
