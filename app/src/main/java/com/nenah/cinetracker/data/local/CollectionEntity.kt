package com.nenah.cinetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "mediaId", "kind"]
)
data class CollectionItemEntity(
    val collectionId: Long,
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
    val addedAt: Long
)

data class CollectionSummaryRow(
    val id: Long,
    val name: String,
    val itemCount: Int
)
