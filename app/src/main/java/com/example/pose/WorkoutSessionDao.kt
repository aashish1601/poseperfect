package com.example.pose

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): LiveData<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseType = :exerciseType ORDER BY date DESC")
    fun getSessionsByExercise(exerciseType: String): LiveData<List<WorkoutSession>>
}