package com.example.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs

class ExerciseTracker {
    private var repCount = 0
    private var isUpPosition = false
    private var isDownPosition = false
    private var lastGoodForm = false
    private var currentExerciseType = ExerciseType.NONE

    var repStatus = "Start exercise"
        private set
    var formFeedback = ""
        private set

    private var rightJointAngle = 0.0
    private var leftJointAngle = 0.0

    data class ExerciseAngles(
        val upRange: ClosedRange<Double>,
        val downRange: ClosedRange<Double>,
        val maxAsymmetry: Double
    )

    private val exerciseConfigs = mapOf(
        ExerciseType.SHOULDER_PRESS to ExerciseAngles(
            upRange = 165.0..180.0,
            downRange = 80.0..100.0,
            maxAsymmetry = 15.0
        ),
        ExerciseType.SQUAT to ExerciseAngles(
            upRange = 165.0..180.0,
            downRange = 80.0..100.0,
            maxAsymmetry = 10.0
        ),
        ExerciseType.BICEP_CURL to ExerciseAngles(
            upRange = 150.0..170.0,
            downRange = 30.0..45.0,
            maxAsymmetry = 12.0
        ),
        ExerciseType.PUSHUP to ExerciseAngles(
            upRange = 160.0..180.0,
            downRange = 85.0..100.0,
            maxAsymmetry = 10.0
        )
    )

    fun getRepCount() = repCount

    fun resetExercise() {
        repCount = 0
        isUpPosition = false
        isDownPosition = false
        lastGoodForm = false
        repStatus = "Start exercise"
        formFeedback = ""
    }

    fun detectExerciseType(landmarks: List<NormalizedLandmark>): ExerciseType {
        return PoseAngleCalculator.detectExerciseType(landmarks)
    }

    fun processExercise(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType) {
        currentExerciseType = exerciseType

        when (exerciseType) {
            ExerciseType.SHOULDER_PRESS -> processShoulderPress(landmarks)
            ExerciseType.SQUAT -> processSquat(landmarks)
            ExerciseType.BICEP_CURL -> processBicepCurl(landmarks)
            ExerciseType.PUSHUP -> processPushup(landmarks)
            ExerciseType.NONE -> resetExercise()
        }
    }

    private fun processShoulderPress(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.SHOULDER_PRESS]!!
        rightJointAngle = PoseAngleCalculator.calculateShoulderAngle(landmarks, true)
        leftJointAngle = PoseAngleCalculator.calculateShoulderAngle(landmarks, false)

        val isGoodForm = isGoodForm(landmarks, ExerciseType.SHOULDER_PRESS)
        updateFormFeedback(landmarks, ExerciseType.SHOULDER_PRESS)
        trackRepProgress(rightJointAngle, leftJointAngle, config, isGoodForm)
    }

    private fun processSquat(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.SQUAT]!!
        rightJointAngle = PoseAngleCalculator.calculateHipAngle(landmarks, true)
        leftJointAngle = PoseAngleCalculator.calculateHipAngle(landmarks, false)

        val isGoodForm = isGoodForm(landmarks, ExerciseType.SQUAT)
        updateFormFeedback(landmarks, ExerciseType.SQUAT)
        trackRepProgress(rightJointAngle, leftJointAngle, config, isGoodForm)
    }

    private fun processBicepCurl(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.BICEP_CURL]!!
        rightJointAngle = PoseAngleCalculator.calculateElbowAngle(landmarks, true)
        leftJointAngle = PoseAngleCalculator.calculateElbowAngle(landmarks, false)

        val isGoodForm = isGoodForm(landmarks, ExerciseType.BICEP_CURL)
        updateFormFeedback(landmarks, ExerciseType.BICEP_CURL)
        trackRepProgress(rightJointAngle, leftJointAngle, config, isGoodForm)
    }

    private fun processPushup(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.PUSHUP]!!
        rightJointAngle = PoseAngleCalculator.calculateElbowAngle(landmarks, true)
        leftJointAngle = PoseAngleCalculator.calculateElbowAngle(landmarks, false)

        val isGoodForm = isGoodForm(landmarks, ExerciseType.PUSHUP)
        updateFormFeedback(landmarks, ExerciseType.PUSHUP)
        trackRepProgress(rightJointAngle, leftJointAngle, config, isGoodForm)
    }

    private fun isGoodForm(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType): Boolean {
        val config = exerciseConfigs[exerciseType] ?: return false
        val asymmetry = abs(rightJointAngle - leftJointAngle)

        return when (exerciseType) {
            ExerciseType.SHOULDER_PRESS ->
                PoseAngleCalculator.isBodyVertical(landmarks) && asymmetry <= config.maxAsymmetry
            ExerciseType.SQUAT ->
                PoseAngleCalculator.isBodyVertical(landmarks) && asymmetry <= config.maxAsymmetry
            ExerciseType.BICEP_CURL ->
                PoseAngleCalculator.isBodyVertical(landmarks) && asymmetry <= config.maxAsymmetry
            ExerciseType.PUSHUP ->
                PoseAngleCalculator.isBodyHorizontal(landmarks) && asymmetry <= config.maxAsymmetry
            ExerciseType.NONE -> false
        }
    }

    private fun trackRepProgress(
        rightAngle: Double,
        leftAngle: Double,
        config: ExerciseAngles,
        isGoodForm: Boolean
    ) {
        when {
            !isDownPosition && isInDownPosition(rightAngle, leftAngle, config.downRange) -> {
                isDownPosition = true
                isUpPosition = false
                repStatus = "Move up!"
                lastGoodForm = isGoodForm
            }
            !isUpPosition && isInUpPosition(rightAngle, leftAngle, config.upRange) -> {
                if (isDownPosition && lastGoodForm) {
                    isUpPosition = true
                    isDownPosition = false
                    repCount++
                    repStatus = "Good! Lower down slowly"
                } else {
                    repStatus = "Start from bottom position"
                }
            }
            isUpPosition && isInDownPosition(rightAngle, leftAngle, config.downRange) -> {
                isUpPosition = false
                isDownPosition = true
                repStatus = "Ready for next rep"
            }
        }
    }

    private fun isInDownPosition(rightAngle: Double, leftAngle: Double, range: ClosedRange<Double>): Boolean {
        return rightAngle in range && leftAngle in range
    }

    private fun isInUpPosition(rightAngle: Double, leftAngle: Double, range: ClosedRange<Double>): Boolean {
        return rightAngle in range && leftAngle in range
    }

    private fun updateFormFeedback(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType) {
        val config = exerciseConfigs[exerciseType] ?: return
        val asymmetry = abs(rightJointAngle - leftJointAngle)

        formFeedback = when {
            asymmetry > config.maxAsymmetry -> "Keep movements even on both sides"
            !isGoodForm(landmarks, exerciseType) -> when (exerciseType) {
                ExerciseType.SHOULDER_PRESS -> "Keep your body straight"
                ExerciseType.SQUAT -> "Maintain proper form"
                ExerciseType.BICEP_CURL -> "Keep elbows close to body"
                ExerciseType.PUSHUP -> "Keep body aligned"
                ExerciseType.NONE -> "Position yourself to start"
            }
            else -> "Good form!"
        }
    }
}