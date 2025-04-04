package com.example.pose

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutSessionDao) {
    val allSessions: LiveData<List<WorkoutSession>> = workoutDao.getAllWorkoutSessions().asLiveData()

    suspend fun insertWorkoutSession(session: WorkoutSession) {
        workoutDao.insert(session)
    }

    suspend fun refreshSessions() {
        // Implementation depends on your app's requirements
    }

    fun getSessionsByExercise(exerciseType: String): LiveData<List<WorkoutSession>> {
        return workoutDao.getWorkoutSessionsByExerciseType(exerciseType).asLiveData()
    }

    fun getSessionsByExerciseChronological(exerciseType: String): LiveData<List<WorkoutSession>> {
        return workoutDao.getSessionsByExerciseChronological(exerciseType).asLiveData()
    }

    fun getMaxWeightForExercise(exerciseType: String): LiveData<Float> {
        return workoutDao.getMaxWeightForExercise(exerciseType).asLiveData()
    }
}