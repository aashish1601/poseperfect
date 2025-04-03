package com.example.pose

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    // Define rep state to make the sequence clear and unambiguous
    private enum class RepState {
        NEUTRAL,      // Starting neutral position
        DOWN_POSITION,  // In down position (start of rep)
        UP_POSITION,    // In up position (rep completion)
        RETURNING
    }

    // Current state of the rep cycle
    private var repState = RepState.NEUTRAL

    var repStatus = "Start exercise"
        private set
    var formFeedback = ""
        private set

    // Add these variables for feedback delay
    private var lastFeedbackTime = 0L
    private val feedbackDelayMs = 6000L  // 6 seconds delay

    // Add these variables for TTS
    private var lastSpokenFeedback = ""
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    // Add a queue for TTS commands to prevent overlap
    private val ttsQueue = mutableListOf<String>()
    private var isSpeaking = false
    private val ttsDelayBetweenUtterances = 1000L // 1 second delay between utterances

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
            upRange = 160.0..180.0,
            downRange = 70.0..100.0,
            maxAsymmetry = 25.0,
            jointName = "Shoulder"
        ),
        ExerciseType.SQUAT to ExerciseAngles(
            upRange = 140.0..180.0,
            downRange = 80.0..110.0,
            maxAsymmetry = 20.0,
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

    // Initialize Text-to-Speech with utterance listener
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("ExerciseTracker", "Language not supported for TTS")
                } else {
                    isTtsReady = true

                    // Add utterance progress listener to handle queue
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                            Log.d("ExerciseTracker", "TTS started: $utteranceId")
                        }

                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            Log.d("ExerciseTracker", "TTS completed: $utteranceId")

                            // Process next item in queue after a delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                processNextInTtsQueue()
                            }, ttsDelayBetweenUtterances)
                        }

                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            Log.e("ExerciseTracker", "TTS error: $utteranceId")

                            // Process next item in queue
                            processNextInTtsQueue()
                        }
                    })

                    Log.d("ExerciseTracker", "TTS initialized successfully")
                }
            } else {
                Log.e("ExerciseTracker", "TTS initialization failed with status: $status")
            }
        }
    }

    // Add to TTS queue instead of speaking immediately
    private fun speakFeedback(text: String) {
        if (isTtsReady && text.isNotEmpty()) {
            // Add to queue
            synchronized(ttsQueue) {
                // Check if this exact message is already in queue to avoid duplicates
                if (!ttsQueue.contains(text)) {
                    ttsQueue.add(text)
                    Log.d("ExerciseTracker", "Added to TTS queue: $text, Queue size: ${ttsQueue.size}")

                    // Start processing queue if not already speaking
                    if (!isSpeaking) {
                        processNextInTtsQueue()
                    }
                }
            }
        }
    }

    // Process next item in TTS queue
    private fun processNextInTtsQueue() {
        synchronized(ttsQueue) {
            if (ttsQueue.isNotEmpty() && !isSpeaking) {
                val textToSpeak = ttsQueue.removeAt(0)
                Log.d("ExerciseTracker", "Speaking from queue: $textToSpeak, Remaining: ${ttsQueue.size}")

                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "feedback_${System.currentTimeMillis()}"

                textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params)
                lastSpokenFeedback = textToSpeak
            }
        }
    }

    // Clear TTS queue when resetting
    private fun clearTtsQueue() {
        synchronized(ttsQueue) {
            ttsQueue.clear()
            Log.d("ExerciseTracker", "TTS queue cleared")
        }
    }

    // Add this method to reset rep count
    fun resetRepCount() {
        repCount = 0
        repStatus = "Start exercise"
        formFeedback = ""
        lastFeedbackTime = 0L
        clearTtsQueue()
        speakFeedback("Rep count reset to zero")
    }

    @Synchronized
    fun resetExercise() {
        Log.d("ExerciseTracker", "Resetting exercise tracker. Current exercise type: ${currentExerciseType.name}")
        repCount = 0
        isUpPosition = false
        isDownPosition = false
        repState = RepState.NEUTRAL
        lastGoodForm = false
        repStatus = "Start exercise"
        formFeedback = ""
        rightJoint = JointAngle(0.0, "", false)
        leftJoint = JointAngle(0.0, "", false)
        lastFeedbackTime = 0L
        clearTtsQueue()
        speakFeedback("Exercise reset")
    }

    @Synchronized
    fun setExerciseType(type: ExerciseType) {
        Log.d("ExerciseTracker", "Setting exercise type: ${type.name}")
        currentExerciseType = type
        resetExercise()
        val exerciseName = type.name.replace("_", " ").toLowerCase(Locale.ROOT)
        speakFeedback("Exercise set to $exerciseName")
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
        trackRepProgressImproved(rightJoint, leftJoint, config, isGoodForm)
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

    // Improved rep tracking logic using state machine approach
    private fun trackRepProgressImproved(
        rightJoint: JointAngle,
        leftJoint: JointAngle,
        config: ExerciseAngles,
        isGoodForm: Boolean
    ) {
        // Log current state and angles
        Log.d("ExerciseTracker", "Current state: $repState")
        Log.d("ExerciseTracker", "Right angle: ${rightJoint.angle}, Left angle: ${leftJoint.angle}")
        Log.d("ExerciseTracker", "Form quality: $isGoodForm")

        // Special handling for squats to be more tolerant of asymmetry
        val effectiveGoodForm = if (currentExerciseType == ExerciseType.SQUAT) {
            val asymmetry = abs(rightJoint.angle - leftJoint.angle)
            val adjustedFormCheck = asymmetry <= 40.0 || isGoodForm
            Log.d("ExerciseTracker", "Adjusted squat form check: $adjustedFormCheck (asymmetry: $asymmetry)")
            adjustedFormCheck
        } else {
            isGoodForm
        }

        val prevRepStatus = repStatus
        val prevRepState = repState
        val prevRepCount = repCount  // Store previous rep count to check if it changed

        // State machine for rep counting
        when (repState) {
            RepState.NEUTRAL -> {
                // Check if we've entered the down position (starting a rep)
                if (isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                    repState = RepState.DOWN_POSITION
                    repStatus = getExerciseStartPrompt(currentExerciseType)
                    lastGoodForm = effectiveGoodForm
                    Log.d("ExerciseTracker", "State change: NEUTRAL -> DOWN_POSITION")

                    // Speak the exercise start prompt if status changed
                    if (repStatus != prevRepStatus) {
                        speakFeedback(repStatus)
                    }
                }
            }

            RepState.DOWN_POSITION -> {
                // Check if we've moved to the up position (completing rep)
                if (isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange)) {
                    repState = RepState.UP_POSITION
                    repCount++

                    // Update status based on form
                    if (effectiveGoodForm) {
                        repStatus = "Good! (${repCount})"
                    } else {
                        repStatus = "(${repCount})"
                    }

                    Log.d("ExerciseTracker", "State change: DOWN_POSITION -> UP_POSITION, Rep count: $repCount")

                    // Speak the rep count with appropriate message
                    if (repCount != prevRepCount) {
                        val repMessage = if (effectiveGoodForm) {
                            "Good job! Rep ${repCount} completed."
                        } else {
                            " ${repCount}"
                        }
                        speakFeedback(repMessage)
                    }
                }
            }

            RepState.UP_POSITION -> {
                // Check if we're returning to a neutral position
                if (!isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange) &&
                    !isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                    repState = RepState.RETURNING
                    repStatus = ""
                    Log.d("ExerciseTracker", "State change: UP_POSITION -> RETURNING")
                }
            }

            RepState.RETURNING -> {
                // Check if we've returned to the down position to start next rep
                if (isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange)) {
                    repState = RepState.DOWN_POSITION
                    repStatus = getExerciseStartPrompt(currentExerciseType)
                    lastGoodForm = effectiveGoodForm
                    Log.d("ExerciseTracker", "State change: RETURNING -> DOWN_POSITION")

                    // Speak the exercise start prompt if status changed
                    if (repStatus != prevRepStatus) {
                        speakFeedback(repStatus)
                    }
                }
                // We can also go back to neutral if the person stands up completely
                else if (!isInDownPosition(rightJoint.angle, leftJoint.angle, config.downRange) &&
                    isInUpPosition(rightJoint.angle, leftJoint.angle, config.upRange)) {
                    repState = RepState.NEUTRAL
                    repStatus = "Ready for next rep"
                    Log.d("ExerciseTracker", "State change: RETURNING -> NEUTRAL")

                    // Speak the ready instruction if status changed
                    if (repStatus != prevRepStatus) {
                        speakFeedback("Ready for next rep")
                    }
                }
            }
        }

        // Keep these traditional flags in sync with new state for backward compatibility
        isUpPosition = (repState == RepState.UP_POSITION)
        isDownPosition = (repState == RepState.DOWN_POSITION)

        // Log state change
        if (prevRepState != repState) {
            Log.d("ExerciseTracker", "Rep state changed from $prevRepState to $repState")
        }
    }

    private fun getExerciseStartPrompt(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.SHOULDER_PRESS -> "Press"
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
            // Consider any angle above 145 degrees as "up enough" for squats
            // Also take the better angle for sideways orientation
            val betterAngle = maxOf(rightAngle, leftAngle)
            val result = betterAngle > 145.0
            Log.d("ExerciseTracker", "Squat up position check: $betterAngle > 145.0 = $result")
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
        if (newFeedback != formFeedback && newFeedback.isNotEmpty()) {
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
                ""
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

    // Update this method to provide spoken milestone feedback
    fun provideRepMilestoneFeedback() {
        // Provide milestone feedback for every 5 reps
        if (repCount > 0 && repCount % 5 == 0) {
            val milestoneMessage = "Great job! You've completed $repCount reps."
            speakFeedback(milestoneMessage)
        }
    }

    // Clean up resources
    fun shutdown() {
        if (textToSpeech != null) {
            clearTtsQueue()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
        }
    }
}