package com.example.smartattendanceapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room database for AttendX AI.
 *
 * Entities:
 *  - [StudentEntity]          — enrolled students + face embeddings
 *  - [AttendanceRecordEntity] — individual attendance events
 *  - [UserEntity]             — app users (teachers & students)
 *
 * Version history:
 *  v1 → v2: added UserEntity
 *  v2 → v3: added className to AttendanceRecordEntity
 *  v3 → v4: added createdAt to UserEntity
 *
 * fallbackToDestructiveMigration() is safe for development; replace with
 * proper Migration objects before shipping to production.
 */
@Database(
    entities = [
        StudentEntity::class,
        AttendanceRecordEntity::class,
        UserEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance, creating it if needed.
         * Thread-safe via double-checked locking.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}