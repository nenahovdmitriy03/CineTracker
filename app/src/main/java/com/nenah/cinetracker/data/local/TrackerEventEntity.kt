package com.nenah.cinetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_events")
data class TrackerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val createdAt: Long
)
