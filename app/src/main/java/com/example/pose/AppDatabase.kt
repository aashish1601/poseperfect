package com.example.pose

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutSession::class],
    version = 3,  // Increment version to handle any schema changes
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database"
                )
                    .fallbackToDestructiveMigration()  // This will destroy and rebuild if migration fails
                    // If you want to preserve data, add proper migrations instead
                    // .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Define migration if needed
        /*
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration SQL statements if you want to preserve data
                // Example: database.execSQL("ALTER TABLE workout_sessions ADD COLUMN new_column TEXT")
            }
        }
        */
    }
}