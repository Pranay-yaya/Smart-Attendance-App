package com.example.smartattendanceapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [StudentEntity::class, AttendanceRecordEntity::class, UserEntity::class],
    version   = 5,          // bumped from 4 → 5 for the new createdBy columns
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ── Migration 4 → 5: add createdBy columns ────────────────────────────
        // Using ALTER TABLE instead of destructive migration so that existing
        // users/records are preserved. The default value "" means old rows are
        // treated as "unowned" — they won't appear in any user's filtered view,
        // which is the safest behaviour (no data leaks).
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE students ADD COLUMN createdBy TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE attendance_records ADD COLUMN createdBy TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_db"
                )
                    .addMigrations(MIGRATION_4_5)   // safe migration, no data loss
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}