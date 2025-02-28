package com.example.pose

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date
@Entity(tableName = "workout_sessions")
@TypeConverters(DateConverter::class)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val exerciseType: String,
    val date: Date,
    val totalReps: Int,
    val totalSets: Int,
    val targetReps: Int,
    val targetSets: Int,
    val restTimeSeconds: Int
)