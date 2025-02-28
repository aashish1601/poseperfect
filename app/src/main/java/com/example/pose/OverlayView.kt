package com.example.pose

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    interface WorkoutCompletionListener {
        fun onWorkoutCompleted(session: WorkoutSession)
    }
    private var viewModel: MainViewModel? = null


    private var results: PoseLandmarkerResult? = null
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val repBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val statusBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPath = Path()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private val points = FloatArray(33 * 2)
    private val exerciseTracker = ExerciseTracker()
    private val cornerRadius = 30f




    private var isInRestPeriod = false
    private var currentRestTime = 0
    private var restTimer: CountDownTimer? = null
    fun setViewModel(viewModel: MainViewModel) {
        this.viewModel = viewModel
        invalidate() // Redraw if needed
    }


    private var workoutCompletionListener: WorkoutCompletionListener? = null
    fun setWorkoutCompletionListener(listener: WorkoutCompletionListener) {
        this.workoutCompletionListener = listener
    }


    fun getCurrentExerciseType(): String = exerciseTracker.currentExerciseType.name
    fun getCurrentReps(): Int = viewModel?.currentReps ?: 0
    // In OverlayView.kt
    fun getCurrentSet(): Int = viewModel?.currentSet ?: 1
    fun getTargetReps(): Int = viewModel?.targetReps ?: 0
    fun getTargetSets(): Int = viewModel?.targetSets ?: 0
    fun getRestTimeSeconds(): Int = viewModel?.restTimeSeconds ?: 0




    private val bodySegments = arrayOf(
        intArrayOf(11, 12),    // Shoulders
        intArrayOf(11, 23),    // Left side
        intArrayOf(12, 24),    // Right side
        intArrayOf(23, 24),    // Hips
        intArrayOf(11, 13, 15), // Left arm
        intArrayOf(12, 14, 16), // Right arm
        intArrayOf(23, 25, 27), // Left leg
        intArrayOf(24, 26, 28)  // Right leg
    )

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        initPaints()
    }

    // Modified setTargetModeParams using ViewModel
    fun setTargetModeParams(targetReps: Int, targetSets: Int, restTimeSeconds: Int) {
        viewModel?.let { vm ->
            Log.d("OverlayView", "Setting target params: $targetReps reps, $targetSets sets, $restTimeSeconds rest")
            vm.apply {
                this.targetReps = targetReps.coerceAtLeast(1)
                this.targetSets = targetSets.coerceAtLeast(1)
                this.restTimeSeconds = restTimeSeconds.coerceAtLeast(0)
                isTargetMode = true
                currentSet = 1
                currentReps = 0
            }
            exerciseTracker.resetRepCount()
            post {
                invalidate()
                requestLayout()
            }
        } ?: run {
            Log.e("OverlayView", "ViewModel not initialized!")
        }
    }
    private var onExerciseCompleteListener: (() -> Unit)? = null

    fun setOnExerciseCompleteListener(listener: () -> Unit) {
        onExerciseCompleteListener = listener
    }

    private fun initPaints() {
        linePaint.apply {
            color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        pointPaint.apply {
            color = Color.YELLOW
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }

        textPaint.apply {
            color = Color.WHITE
            textSize = 60f
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        repBoxPaint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#80000000") // Semi-transparent black
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }

        statusBoxPaint.apply {
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }
    }
    override fun onDraw(canvas: Canvas) {
        viewModel?.let { vm ->
            Log.d("OverlayView", "onDraw - isTargetMode: ${vm.isTargetMode}, currentReps: ${vm.currentReps}, targetReps: ${vm.targetReps}")

            results?.let { poseLandmarkerResult ->
                for (landmarks in poseLandmarkerResult.landmarks()) {
                    // ... existing drawing code ...
                    calculateScaledPoints(landmarks)
                    drawBodySegments(canvas)
                    drawLandmarkPoints(canvas)

                    if (!isInRestPeriod) {
                        val detectedExercise = exerciseTracker.detectExerciseType(landmarks)
                        exerciseTracker.processExercise(landmarks, detectedExercise)
                        vm.currentReps = exerciseTracker.getRepCount()

                        if (vm.isTargetMode && vm.currentReps >= vm.targetReps) {
                            if (vm.currentSet < vm.targetSets) {
                                startRestPeriod(vm)
                            } else {
                                onExerciseCompleteListener?.invoke()
                            }
                        }
                    }
                    drawEnhancedExerciseInfo(canvas, vm)
                }
            }
        }
    }
    private fun startRestPeriod(viewModel: MainViewModel) {
        isInRestPeriod = true
        currentRestTime = viewModel.restTimeSeconds

        restTimer?.cancel()
        restTimer = object : CountDownTimer((viewModel.restTimeSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRestTime = (millisUntilFinished / 1000).toInt()
                invalidate()
            }

            override fun onFinish() {
                isInRestPeriod = false
                viewModel.currentSet++
                viewModel.currentReps = 0
                exerciseTracker.resetRepCount()
                invalidate()
            }
        }.start()
    }


    private fun calculateScaledPoints(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        landmarks.forEachIndexed { index, landmark ->
            points[index * 2] = landmark.x() * imageWidth * scaleFactor
            points[index * 2 + 1] = landmark.y() * imageHeight * scaleFactor
        }
    }

    private fun drawBodySegments(canvas: Canvas) {
        bodyPath.reset()
        bodySegments.forEach { segment ->
            val startIdx = segment[0] * 2
            bodyPath.moveTo(points[startIdx], points[startIdx + 1])
            for (i in 1 until segment.size) {
                val idx = segment[i] * 2
                bodyPath.lineTo(points[idx], points[idx + 1])
            }
        }
        canvas.drawPath(bodyPath, linePaint)
    }

    private fun drawLandmarkPoints(canvas: Canvas) {
        val radius = LANDMARK_STROKE_WIDTH / 2
        for (i in points.indices step 2) {
            canvas.drawCircle(points[i], points[i + 1], radius, pointPaint)
        }
    }


    private fun drawEnhancedExerciseInfo(canvas: Canvas, viewModel: MainViewModel) {
        val padding = 20f
        val boxSpacing = 20f
        var yOffset = padding

        Log.d("OverlayView", "Drawing info - isTargetMode: $viewModel.isTargetMode, currentReps: $viewModel.currentReps, targetReps: $viewModel.targetReps, currentSet: $viewModel.currentSet, targetSets: $viewModel.targetSets")

        // Draw target mode info if active
        if (viewModel.isTargetMode) {
            // Draw set counter
            val setCountText = "SET: ${viewModel.currentSet}/${viewModel.targetSets}"
            drawInfoBox(canvas, setCountText, yOffset, Color.parseColor("#80000000"))
            yOffset += 100f + boxSpacing

            // Draw target reps
            val targetText = "REPS: ${viewModel.currentReps}/${viewModel.targetReps}"
            drawInfoBox(canvas, targetText, yOffset, Color.parseColor("#80000000"))
            yOffset += 100f + boxSpacing

            // Draw rest timer if in rest period
            if (isInRestPeriod) {
                val restText = "REST: ${currentRestTime}s"
                drawInfoBox(canvas, restText, yOffset, Color.parseColor("#80FFA500"))
                yOffset += 100f + boxSpacing
            }
        } else {
            // Draw regular rep counter
            val repCountText = "REPS: ${exerciseTracker.getRepCount()}"
            drawInfoBox(canvas, repCountText, yOffset, Color.parseColor("#80000000"))
            yOffset += 100f + boxSpacing
        }

        // Draw status and form feedback
        val statusText = exerciseTracker.repStatus
        val statusColor = when {
            statusText.contains("Good") -> Color.parseColor("#80228B22")
            statusText.contains("Start") -> Color.parseColor("#80FFD700")
            else -> Color.parseColor("#80000000")
        }
        drawInfoBox(canvas, statusText, yOffset, statusColor)
        yOffset += 100f + boxSpacing

        val feedbackText = exerciseTracker.formFeedback
        val feedbackColor = when {
            feedbackText.contains("Good") -> Color.parseColor("#80228B22")
            else -> Color.parseColor("#80FF4444")
        }
        drawInfoBox(canvas, feedbackText, yOffset, feedbackColor)
    }


    private fun drawInfoBox(canvas: Canvas, text: String, yOffset: Float, backgroundColor: Int) {
        val padding = 20f
        textPaint.textSize = 60f
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        val boxRect = RectF(
            padding,
            yOffset,
            padding + bounds.width() + 60f,
            yOffset + bounds.height() + 40f
        )

        repBoxPaint.color = backgroundColor
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, repBoxPaint)
        canvas.drawText(
            text,
            boxRect.left + 30f,
            boxRect.bottom - 20f,
            textPaint.apply { color = Color.WHITE }
        )
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    )
    {
        Log.d("OverlayView", "setResults called - isTargetMode: ${viewModel}isTargetMode, reps: ${viewModel}currentReps/${viewModel}targetReps")
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO ->
                minOf(width * 1f / imageWidth, height * 1f / imageHeight)

            RunningMode.LIVE_STREAM ->
                maxOf(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        Log.d("OverlayView", "setResults completed - state preserved? isTargetMode: ${viewModel}isTargetMode")

        invalidate()
    }



    fun setExerciseType(type: ExerciseType) {
        Log.d("OverlayView", "Setting exercise type: ${type.name}")
        exerciseTracker.resetExercise()
        exerciseTracker.setExerciseType(type) // Add this line to use the type parameter
        // Reset target mode when exercise type changes

        restTimer?.cancel()
        invalidate()
    }

    // Example usage of setTargetModeParams in a workout session
    fun startWorkoutSession(reps: Int, sets: Int, restTime: Int) {
        setTargetModeParams(reps, sets, restTime)
        // Additional workout session initialization if needed
    }

    fun clear() {
        results = null
        bodyPath.reset()
        restTimer?.cancel()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        restTimer?.cancel() // Clean up timer when view is destroyed
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}