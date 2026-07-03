package com.nenah.cinetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackedTitleEntity::class,
        EpisodeRatingEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        TrackerEventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CineDatabase : RoomDatabase() {
    abstract fun trackedTitleDao(): TrackedTitleDao

    companion object {
        @Volatile
        private var instance: CineDatabase? = null

        fun getInstance(context: Context): CineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CineDatabase::class.java,
                    "cinetracker.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
