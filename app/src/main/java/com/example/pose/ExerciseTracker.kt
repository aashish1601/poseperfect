package com.example.pose

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.Locale
import kotlin.math.abs

class ExerciseTracker(private val context: Context) {
    private var repCount = 0
    private var isUpPosition = false
    private var isDownPosition = false
    private var lastGoodForm = false
    var currentExerciseType: ExerciseType = ExerciseType.NONE

    var repStatus = "Start exercise"
        private set
    var formFeedback = ""
        private set

    // Add these variables for feedback delay
    private var lastFeedbackTime = 0L
    private val feedbackDelayMs = 4000L  // 4 seconds delay

    // Add these variables for TTS
    private var lastSpokenFeedback = ""
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var rightJoint = JointAngle(0.0, "", false)
    private var leftJoint = JointAngle(0.0, "", false)

    // Initialize Text-to-Speech
    init {
        initializeTextToSpeech()
    }

    // Add this data class to make the code compile
    data class JointAngle(
        val angle: Double,
        val jointName: String,
        val isCorrect: Boolean
    )

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
            maxAsymmetry = 30.0,
            jointName = "Shoulder"
        ),
        ExerciseType.SQUAT to ExerciseAngles(
            upRange = 140.0..180.0,  // More lenient up range
            downRange = 80.0..110.0,  // Slightly wider down range
            maxAsymmetry = 20.0,     // More tolerant of asymmetry
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

    // Initialize Text-to-Speech
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("ExerciseTracker", "Language not supported for TTS")
                } else {
                    isTtsReady = true
                    Log.d("ExerciseTracker", "TTS initialized successfully")
                }
            } else {
                Log.e("ExerciseTracker", "TTS initialization failed with status: $status")
            }
        }
    }

    // Speak text using TTS
    private fun speakFeedback(text: String) {
        if (isTtsReady && text.isNotEmpty() && text != lastSpokenFeedback) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "feedback_id")
            lastSpokenFeedback = text
            Log.d("ExerciseTracker", "Speaking feedback: $text")
        }
    }

    // Add this method to reset rep count
    fun resetRepCount() {
        repCount = 0
        repStatus = "Start exercise"
        formFeedback = ""
        lastFeedbackTime = 0L
        speakFeedback("Rep count reset")
    }

    @Synchronized
    fun resetExercise() {
        Log.d("ExerciseTracker", "Resetting exercise tracker. Current exercise type: ${currentExerciseType.name}")
        repCount = 0
        isUpPosition = false
        isDownPosition = false
        lastGoodForm = false
        repStatus = "Start exercise"
        formFeedback = ""
        rightJoint = JointAngle(0.0, "", false)
        leftJoint = JointAngle(0.0, "", false)
        lastFeedbackTime = 0L
        speakFeedback("Exercise reset")
    }

    @Synchronized
    fun setExerciseType(type: ExerciseType) {
        Log.d("ExerciseTracker", "Setting exercise type: ${type.name}")
        currentExerciseType = type
        resetExercise()
        speakFeedback("Exercise set to ${type.name.replace("_", " ").toLowerCase()}")
    }

    @Synchronized
    fun processExercise(landmarks: List<NormalizedLandmark>) {
        if (landmarks.size < 33) {
            Log.e("ExerciseTracker", "Not enough landmarks detected: ${landmarks.size}")
            return
        }

        Log.d("ExerciseTracker", "Processing exercise: ${currentExerciseType.name}")

        when (currentExerciseType) {
            ExerciseType.SHOULDER_PRESS -> processShoulderPress(landmarks)
            ExerciseType.SQUAT -> processSquat(landmarks)
            ExerciseType.BICEP_CURL -> processBicepCurl(landmarks)
            ExerciseType.PUSHUP -> processPushup(landmarks)
            ExerciseType.NONE -> {
                Log.d("ExerciseTracker", "No exercise type set. Resetting.")
                resetExercise()
            }
        }
    }

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

        // Log the angles for debugging
        Log.d("ExerciseTracker", "Right angle: $rightAngle, Left angle: $leftAngle, Asymmetry: $asymmetry")
    }

    private fun processExerciseMovement(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType) {
        val config = exerciseConfigs[exerciseType]!!
        val isGoodForm = isGoodForm(landmarks, exerciseType)
        updateFormFeedback(landmarks, exerciseType)
        trackRepProgress(rightJoint, leftJoint, config, isGoodForm)
    }

    private fun isGoodForm(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType): Boolean {
        // First check if the form is good according to the PoseAngleCalculator
        val baseFormGood = PoseAngleCalculator.isGoodForm(landmarks, exerciseType)

        // Then also check our joint angles are good (symmetry)
        val jointsGood = rightJoint.isCorrect && leftJoint.isCorrect

        // Log the form status for debugging
        Log.d("ExerciseTracker", "Form check - Base: $baseFormGood, Joints: $jointsGood")

        return baseFormGood && jointsGood
    }

    private fun trackRepProgress(
        rightJoint: JointAngle,
        leftJoint: JointAngle,
        config: ExerciseAngles,
        isGoodForm: Boolean
    ) {
        // Add more debug logging
        Log.d("ExerciseTracker", "Current positions - Down: $isDownPosition, Up: $isUpPosition")
        Log.d("ExerciseTracker", "Right angle: ${rightJoint.angle}, Left angle: ${leftJoint.angle}")
        Log.d("ExerciseTracker", "Down range: ${config.downRange}, Up range: ${config.upRange}")
        Log.d("ExerciseTracker", "Form quality: $isGoodForm")

        // Calculate the average angle for position checks
        val avgAngle = (rightJoint.angle + leftJoint.angle) / 2
        Log.d("ExerciseTracker", "Average angle: $avgAngle")

        // For squats, be much more tolerant of asymmetry for sideways orientation
        val effectiveGoodForm = if (currentExerciseType == ExerciseType.SQUAT) {
            // Allow up to 40° asymmetry for squats to accommodate sideways orientation
            val asymmetry = abs(rightJoint.angle - leftJoint.angle)
            val adjustedFormCheck = asymmetry <= 40.0 || isGoodForm
            Log.d("ExerciseTracker", "Adjusted squat form check: $adjustedFormCheck (asymmetry: $asymmetry)")
            adjustedFormCheck
        } else {
            isGoodForm
        }

        // Fix the issue where both isUpPosition and isDownPosition are true
        if (isUpPosition && isDownPosition) {
            // Reset the state if both are true to avoid getting stuck
            if (!isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange)) {
                isUpPosition = false
                Log.d("ExerciseTracker", "Resetting conflicting UP position state")
            }
            if (!isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                isDownPosition = false
                Log.d("ExerciseTracker", "Resetting conflicting DOWN position state")
            }
        }

        val prevRepStatus = repStatus

        if (!isUpPosition && !isDownPosition) {
            // Check if we're in the down position to start a rep
            if (isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                isDownPosition = true
                repStatus = getExerciseStartPrompt(currentExerciseType)
                lastGoodForm = effectiveGoodForm
                Log.d("ExerciseTracker", "DOWN position detected with form quality: $lastGoodForm")

                // Speak the exercise start prompt
                if (repStatus != prevRepStatus) {
                    speakFeedback(repStatus)
                }
            }
        }
        else if (isDownPosition && !isUpPosition) {
            // Check if we've moved to the up position to complete a rep
            if (isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange)) {
                isUpPosition = true

                // Count the rep regardless of form quality, but provide feedback
                repCount++
                if (effectiveGoodForm) {
                    repStatus = "Good! (${repCount})"
                } else {
                    repStatus = "Rep counted, but check form (${repCount})"
                }
                Log.d("ExerciseTracker", "UP position detected, counting rep: $repCount with form: $effectiveGoodForm")

                // Speak the rep status
                if (repStatus != prevRepStatus) {
                    speakFeedback(repStatus)
                }
            }
        }
        else if (isUpPosition && isDownPosition) {
            // Check if we're out of both positions to reset for next rep
            if (!isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange) &&
                !isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                isUpPosition = false
                isDownPosition = false
                repStatus = "Return to starting position"
                Log.d("ExerciseTracker", "Returning to neutral position")

                // Speak the reset instruction
                if (repStatus != prevRepStatus) {
                    speakFeedback(repStatus)
                }
            }
        }
        else if (isUpPosition && !isDownPosition) {
            // Check if we're going back to down position
            if (isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                isDownPosition = true
                isUpPosition = false
                repStatus = getExerciseStartPrompt(currentExerciseType)
                lastGoodForm = effectiveGoodForm
                Log.d("ExerciseTracker", "Back to DOWN position, preparing for next rep")

                // Speak the exercise start prompt
                if (repStatus != prevRepStatus) {
                    speakFeedback(repStatus)
                }
            }
        }
    }

    private fun getExerciseStartPrompt(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.SHOULDER_PRESS -> "Press up with control!"
            ExerciseType.SQUAT -> "Stand up!"
            ExerciseType.BICEP_CURL -> "Curl up!"
            ExerciseType.PUSHUP -> "Push up!"
            ExerciseType.NONE -> "Start exercise"
        }
    }

    private fun isInDownPosition(rightAngle: Double, leftAngle: Double, range: ClosedRange<Double>): Boolean {
        // Add special handling for squats from sideways orientation
        if (currentExerciseType == ExerciseType.SQUAT) {
            // When sideways, one leg might appear more bent than the other
            // Take the better angle (more bent) for detection
            val betterAngle = minOf(rightAngle, leftAngle)
            val avgAngle = (rightAngle + leftAngle) / 2

            // Check if either the better angle or average is in range
            val result = betterAngle in range || avgAngle in range
            Log.d("ExerciseTracker", "Squat down position check: better=$betterAngle, avg=$avgAngle in $range = $result")
            return result
        } else {
            // Original code for other exercises
            val avgAngle = (rightAngle + leftAngle) / 2
            val result = avgAngle in range
            Log.d("ExerciseTracker", "Down position check: $avgAngle in $range = $result")
            return result
        }
    }

    private fun isInUpPosition(rightAngle: Double, leftAngle: Double, range: ClosedRange<Double>): Boolean {
        // For squats specifically, be even more lenient with the up position
        if (currentExerciseType == ExerciseType.SQUAT) {
            // Consider any angle above 130 degrees as "up enough" for squats
            // Also take the better angle for sideways orientation
            val betterAngle = maxOf(rightAngle, leftAngle)
            val result = betterAngle > 145.0
            Log.d("ExerciseTracker", "Squat up position check: $betterAngle > 130.0 = $result")
            return result
        } else {
            // Original code for other exercises
            val betterAngle = maxOf(rightAngle, leftAngle)
            val result = betterAngle in range
            Log.d("ExerciseTracker", "Up position check: $betterAngle in $range = $result")
            return result
        }
    }

    private fun updateFormFeedback(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType) {
        // Get current time
        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since last feedback
        if (currentTime - lastFeedbackTime < feedbackDelayMs) {
            // Not enough time has passed, keep the current feedback
            Log.d("ExerciseTracker", "Skipping feedback update, not enough time passed: ${currentTime - lastFeedbackTime}ms")
            return
        }

        // Time to update feedback
        val newFeedback = generateFormFeedback(landmarks, exerciseType)

        // Only update if feedback actually changed
        if (newFeedback != formFeedback) {
            formFeedback = newFeedback
            lastFeedbackTime = currentTime
            Log.d("ExerciseTracker", "Updated feedback: $formFeedback")

            // Speak the new feedback
            speakFeedback(newFeedback)
        }
    }

    private fun generateFormFeedback(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType): String {
        // More detailed feedback
        return when {
            !rightJoint.isCorrect || !leftJoint.isCorrect -> {
                "Asymmetry detected: Fix your form"
            }
            !isGoodForm(landmarks, exerciseType) -> {
                when (exerciseType) {
                    ExerciseType.SHOULDER_PRESS -> "Keep your back straight and elbows aligned"
                    ExerciseType.SQUAT -> "Keep knees aligned with toes, back straight"
                    ExerciseType.BICEP_CURL -> "Keep elbows stationary close to body"
                    ExerciseType.PUSHUP -> "Maintain straight body alignment"
                    ExerciseType.NONE -> "Position yourself to start"
                }
            }
            else -> {
                "Good form!"
            }
        }
    }

    // Clean up resources
    fun shutdown() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
        }
    }
}