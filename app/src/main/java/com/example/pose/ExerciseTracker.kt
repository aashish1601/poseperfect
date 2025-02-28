package com.example.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs



class ExerciseTracker {
    private var repCount = 0
    private var isUpPosition = false
    private var isDownPosition = false
    private var lastGoodForm = false
    var currentExerciseType: ExerciseType = ExerciseType.NONE

    var repStatus = "Start exercise"
        private set
    var formFeedback = ""
        private set

    private var rightJoint = JointAngle(0.0, "", false)
    private var leftJoint = JointAngle(0.0, "", false)

    data class ExerciseAngles(
        val upRange: ClosedRange<Double>,
        val downRange: ClosedRange<Double>,
        val maxAsymmetry: Double,
        val jointName: String
    )

    private val exerciseConfigs = mapOf(
        ExerciseType.SHOULDER_PRESS to ExerciseAngles(
            upRange = 150.0..180.0,
            downRange = 70.0..100.0,
            maxAsymmetry = 20.0,
            jointName = "Shoulder"
        ),
        ExerciseType.SQUAT to ExerciseAngles(
            upRange = 165.0..180.0,
            downRange = 80.0..100.0,
            maxAsymmetry = 10.0,
            jointName = "Hip"
        ),
        ExerciseType.BICEP_CURL to ExerciseAngles(
            upRange = 150.0..170.0,
            downRange = 30.0..45.0,
            maxAsymmetry = 12.0,
            jointName = "Elbow"
        ),
        ExerciseType.PUSHUP to ExerciseAngles(
            upRange = 160.0..180.0,
            downRange = 85.0..100.0,
            maxAsymmetry = 10.0,
            jointName = "Elbow"
        )
    )

    fun getRepCount() = repCount

    // Add this method to reset rep count
    fun resetRepCount() {
        repCount = 0
        repStatus = "Start exercise"
        formFeedback = ""
    }

    // Add this method to set exercise type
    fun setExerciseType(type: ExerciseType) {
        currentExerciseType = type
        resetExercise()
    }

    fun resetExercise() {
        repCount = 0
        isUpPosition = false
        isDownPosition = false
        lastGoodForm = false
        repStatus = "Start exercise"
        formFeedback = ""
        rightJoint = JointAngle(0.0, "", false)
        leftJoint = JointAngle(0.0, "", false)
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

    // ... [rest of the existing methods remain the same]

    private fun processShoulderPress(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.SHOULDER_PRESS]!!
        updateJointAngles(
            landmarks,
            config,
            { PoseAngleCalculator.calculateShoulderAngle(landmarks, true) },
            { PoseAngleCalculator.calculateShoulderAngle(landmarks, false) }
        )
        processExerciseMovement(landmarks, ExerciseType.SHOULDER_PRESS)
    }

    private fun processSquat(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.SQUAT]!!
        updateJointAngles(
            landmarks,
            config,
            { PoseAngleCalculator.calculateHipAngle(landmarks, true) },
            { PoseAngleCalculator.calculateHipAngle(landmarks, false) }
        )
        processExerciseMovement(landmarks, ExerciseType.SQUAT)
    }

    private fun processBicepCurl(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.BICEP_CURL]!!
        updateJointAngles(
            landmarks,
            config,
            { PoseAngleCalculator.calculateElbowAngle(landmarks, true) },
            { PoseAngleCalculator.calculateElbowAngle(landmarks, false) }
        )
        processExerciseMovement(landmarks, ExerciseType.BICEP_CURL)
    }

    private fun processPushup(landmarks: List<NormalizedLandmark>) {
        val config = exerciseConfigs[ExerciseType.PUSHUP]!!
        updateJointAngles(
            landmarks,
            config,
            { PoseAngleCalculator.calculateElbowAngle(landmarks, true) },
            { PoseAngleCalculator.calculateElbowAngle(landmarks, false) }
        )
        processExerciseMovement(landmarks, ExerciseType.PUSHUP)
    }

    private fun updateJointAngles(
        landmarks: List<NormalizedLandmark>,
        config: ExerciseAngles,
        rightAngleCalc: () -> Double,
        leftAngleCalc: () -> Double
    ) {
        val rightAngle = rightAngleCalc()
        val leftAngle = leftAngleCalc()
        val asymmetry = abs(rightAngle - leftAngle)

        rightJoint = JointAngle(
            angle = rightAngle,
            jointName = "${config.jointName} (Right)",
            isCorrect = asymmetry <= config.maxAsymmetry
        )

        leftJoint = JointAngle(
            angle = leftAngle,
            jointName = "${config.jointName} (Left)",
            isCorrect = asymmetry <= config.maxAsymmetry
        )
    }

    private fun processExerciseMovement(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType) {
        val config = exerciseConfigs[exerciseType]!!
        val isGoodForm = isGoodForm(landmarks, exerciseType)
        updateFormFeedback(landmarks, exerciseType)
        trackRepProgress(rightJoint, leftJoint, config, isGoodForm)
    }

    private fun isGoodForm(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType): Boolean {
        return when (exerciseType) {
            ExerciseType.SHOULDER_PRESS -> {
                val isVertical = PoseAngleCalculator.isBodyVertical(landmarks)
                val isSymmetrical = rightJoint.isCorrect && leftJoint.isCorrect
                val elbowsNotFlared = PoseAngleCalculator.areElbowsInline(landmarks)
                isVertical && isSymmetrical && elbowsNotFlared
            }
            ExerciseType.SQUAT ->
                PoseAngleCalculator.isBodyVertical(landmarks) && rightJoint.isCorrect && leftJoint.isCorrect
            ExerciseType.BICEP_CURL ->
                PoseAngleCalculator.isBodyVertical(landmarks) && rightJoint.isCorrect && leftJoint.isCorrect
            ExerciseType.PUSHUP ->
                PoseAngleCalculator.isBodyHorizontal(landmarks) && rightJoint.isCorrect && leftJoint.isCorrect
            ExerciseType.NONE -> false
        }
    }

    private fun trackRepProgress(
        rightJoint: JointAngle,
        leftJoint: JointAngle,
        config: ExerciseAngles,
        isGoodForm: Boolean
    ) {
        when {
            !isDownPosition && isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange) -> {
                isDownPosition = true
                isUpPosition = false
                repStatus = if (currentExerciseType == ExerciseType.SHOULDER_PRESS) {
                    "Press up with control!"
                } else {
                    "Move up!"
                }
                lastGoodForm = isGoodForm
            }
            !isUpPosition && isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange) -> {
                if (isDownPosition && lastGoodForm) {
                    isUpPosition = true
                    isDownPosition = false
                    repCount++
                    repStatus = if (currentExerciseType == ExerciseType.SHOULDER_PRESS) {
                        "Good! Lower slowly to shoulders"
                    } else {
                        "Good! Lower down slowly"
                    }
                } else {
                    repStatus = if (currentExerciseType == ExerciseType.SHOULDER_PRESS) {
                        "Start at shoulder level"
                    } else {
                        "Start from bottom position"
                    }
                }
            }
            isUpPosition && isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange) -> {
                isUpPosition = false
                isDownPosition = true
                repStatus = if (currentExerciseType == ExerciseType.SHOULDER_PRESS) {
                    "Ready for next press"
                } else {
                    "Ready for next rep"
                }
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
        formFeedback = when {
            !rightJoint.isCorrect || !leftJoint.isCorrect ->
                "Keep movements even on both sides (${rightJoint.jointName}: ${rightJoint.angle.toInt()}°, ${leftJoint.jointName}: ${leftJoint.angle.toInt()}°)"
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