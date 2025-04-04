// MainViewModel.kt
package com.example.pose

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class MainViewModel(private val appContext: Context) : ViewModel() {
    // Existing ML model configuration
    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE

    // Exercise tracker state
    val exerciseTracker = ExerciseTracker(appContext)

    // Target mode workout state
    var isTargetMode = false
    var targetReps = 0
    var targetSets = 0
    var currentSet = 1
    var restTimeSeconds = 0
    var currentWeight: Float = 0f
    var weightUnit: String = "kg"
    var currentReps: Int
        get() = exerciseTracker.getRepCount()
        set(value) {
            exerciseTracker.resetRepCount()
        }

    // Getters for ML configuration
    val currentDelegate: Int get() = _delegate
    val currentModel: Int get() = _model
    val currentMinPoseDetectionConfidence: Float get() = _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float get() = _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float get() = _minPosePresenceConfidence

    // Setters for ML configuration
    fun setDelegate(delegate: Int) { _delegate = delegate }
    fun setMinPoseDetectionConfidence(confidence: Float) { _minPoseDetectionConfidence = confidence }
    fun setMinPoseTrackingConfidence(confidence: Float) { _minPoseTrackingConfidence = confidence }
    fun setMinPosePresenceConfidence(confidence: Float) { _minPosePresenceConfidence = confidence }
    fun setModel(model: Int) { _model = model }

    fun setExerciseType(type: ExerciseType) {
        // Reset the rep counter whenever the exercise type changes
        exerciseTracker.resetExercise()
        // Then set the new type
        exerciseTracker.setExerciseType(type)
        Log.d("MainViewModel", "Exercise type changed to: ${type.name}")
    }
    // Workout state management
    fun resetWorkoutState() {
        isTargetMode = false
        targetReps = 0
        targetSets = 0
        currentSet = 1
        restTimeSeconds = 0
        exerciseTracker.resetExercise()
        // Log to verify reset
        Log.d("MainViewModel", "Workout state reset")
    }

    fun startWorkoutSession(reps: Int, sets: Int, rest: Int) {
        targetReps = reps.coerceAtLeast(1)
        targetSets = sets.coerceAtLeast(1)
        restTimeSeconds = rest.coerceAtLeast(0)
        currentSet = 1
        exerciseTracker.resetRepCount()
        isTargetMode = true
    }
}

class MainViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}