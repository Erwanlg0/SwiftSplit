package com.elg.swiftsplit.infrastructure.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elg.swiftsplit.infrastructure.persistence.dao.RunDao
import com.elg.swiftsplit.infrastructure.persistence.entity.*

@Database(
    entities = [
        RunEntity::class,
        SegmentEntity::class,
        SplitTimeEntity::class,
        AttemptEntity::class,
        SegmentHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpeedrunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao

    companion object {
        private const val DB_NAME = "speedrun_companion.db"

        fun create(context: Context): SpeedrunDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SpeedrunDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }
}
