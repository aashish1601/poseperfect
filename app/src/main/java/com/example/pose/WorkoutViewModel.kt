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
        val workoutDao = WorkoutDatabase.getDatabase(application).workoutDao()
        repository = WorkoutRepository(workoutDao)
        allSessions = repository.allSessions
    }
    fun refreshSessions() = viewModelScope.launch {
        repository.refreshSessions()
    }

    fun insert(session: WorkoutSession) = viewModelScope.launch {
        repository.insertWorkoutSession(session)
    }
}