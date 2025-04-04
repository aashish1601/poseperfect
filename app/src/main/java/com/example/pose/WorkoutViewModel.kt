package com.example.pose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WorkoutRepository
    val allSessions: LiveData<List<WorkoutSession>>

    init {
        val workoutDao = AppDatabase.getDatabase(application).workoutDao()
        repository = WorkoutRepository(workoutDao)
        allSessions = repository.allSessions
    }

    fun refreshSessions() = viewModelScope.launch {
        repository.refreshSessions()
    }

    fun insert(session: WorkoutSession) = viewModelScope.launch {
        repository.insertWorkoutSession(session)
    }

    fun getSessionsByExercise(exerciseType: String): LiveData<List<WorkoutSession>> {
        return repository.getSessionsByExercise(exerciseType)
    }

    fun getSessionsByExerciseChronological(exerciseType: String): LiveData<List<WorkoutSession>> {
        return repository.getSessionsByExerciseChronological(exerciseType)
    }

    fun getMaxWeightForExercise(exerciseType: String): LiveData<Float> {
        return repository.getMaxWeightForExercise(exerciseType)
    }
}