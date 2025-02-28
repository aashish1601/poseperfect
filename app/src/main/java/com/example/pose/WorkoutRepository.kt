package com.example.pose

import androidx.lifecycle.LiveData

class WorkoutRepository(private val workoutDao: WorkoutSessionDao) {
    val allSessions: LiveData<List<WorkoutSession>> = workoutDao.getAllSessions()

    suspend fun insertWorkoutSession(session: WorkoutSession) {
        workoutDao.insert(session)
    }
    suspend fun refreshSessions() {
        // Trigger a refresh of the sessions
        workoutDao.getAllSessions()
    }

    fun getSessionsByExercise(exerciseType: String): LiveData<List<WorkoutSession>> {
        return workoutDao.getSessionsByExercise(exerciseType)
    }
}