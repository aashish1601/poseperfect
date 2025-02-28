package com.example.pose

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions")
    fun getAllSessions(): LiveData<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseType = :exerciseType")
    fun getSessionsByExercise(exerciseType: String): LiveData<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession)
}