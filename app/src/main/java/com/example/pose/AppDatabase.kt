package com.example.pose

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WorkoutSession::class],
    version = 2,  // Incremented version
    exportSchema = false
)
@TypeConverters(DateConverter::class)  // Moved converter here
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
                    .fallbackToDestructiveMigration()  // Added for version change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}