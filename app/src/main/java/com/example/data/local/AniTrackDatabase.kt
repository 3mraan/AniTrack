package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AnimeEntity
import com.example.data.model.EpisodeHistoryEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserAnimeEntity
import com.example.data.model.UserPreferencesEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistAnimeEntity
import com.example.data.model.WatchlistEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class AniTrackConverters {
    @TypeConverter
    fun fromStatus(status: WatchStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): WatchStatus = runCatching { WatchStatus.valueOf(value) }.getOrDefault(WatchStatus.PLANNED)
}

@Database(
    entities = [
        AnimeEntity::class,
        UserAnimeEntity::class,
        EpisodeHistoryEntity::class,
        ReviewEntity::class,
        WatchlistEntity::class,
        WatchlistAnimeEntity::class,
        UserPreferencesEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AniTrackConverters::class)
abstract class AniTrackDatabase : RoomDatabase() {
    abstract fun aniTrackDao(): AniTrackDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE anime_metadata ADD COLUMN coverLargeUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE anime_metadata ADD COLUMN coverMediumUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AniTrackDatabase? = null

        fun getInstance(context: Context): AniTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AniTrackDatabase::class.java,
                    "anitrack_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
