package com.example.pose

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutSession: WorkoutSession): Long

    @Update
    suspend fun update(workoutSession: WorkoutSession)

    @Delete
    suspend fun delete(workoutSession: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getWorkoutSessionById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE exerciseType = :exerciseType ORDER BY date DESC")
    fun getWorkoutSessionsByExerciseType(exerciseType: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseType = :exerciseType ORDER BY date ASC")
    fun getSessionsByExerciseChronological(exerciseType: String): Flow<List<WorkoutSession>>

    @Query("SELECT COALESCE(MAX(weight), 0.0) FROM workout_sessions WHERE exerciseType = :exerciseType")
    fun getMaxWeightForExercise(exerciseType: String): Flow<Float>
}