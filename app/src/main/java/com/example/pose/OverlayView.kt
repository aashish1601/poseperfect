package com.example.pose

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.Locale

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    interface WorkoutCompletionListener {
        fun onWorkoutCompleted(session: WorkoutSession)
    }
    private var viewModel: MainViewModel? = null

    private var results: PoseLandmarkerResult? = null
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val infoBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressForegroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPath = Path()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private val points = FloatArray(33 * 2)
    private val cornerRadius = 24f
    private val circleRadius = 80f
    private val circlePadding = 40f
    private val circleStrokeWidth = 14f
    private val circleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        addUpdateListener { invalidate() }
    }
    private var progressValue = 0f
    private var targetProgressValue = 0f

    private var isInRestPeriod = false
    private var currentRestTime = 0
    private var restTimer: CountDownTimer? = null
    private var lastRepCount = 0

    fun setViewModel(viewModel: MainViewModel) {
        this.viewModel = viewModel
        invalidate()
    }

    private var workoutCompletionListener: WorkoutCompletionListener? = null
    fun setWorkoutCompletionListener(listener: WorkoutCompletionListener) {
        this.workoutCompletionListener = listener
    }

    fun getCurrentExerciseType(): String = viewModel?.exerciseTracker?.currentExerciseType?.name ?: "NONE"
    fun getCurrentReps(): Int = viewModel?.currentReps ?: 0
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
                exerciseTracker.resetRepCount() // Use view model's exercise tracker
            }
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
            textSize = 48f
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 0f, 2f, Color.BLACK)
        }

        titleTextPaint.apply {
            color = Color.WHITE
            textSize = 36f
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(2f, 0f, 2f, Color.BLACK)
        }

        infoBoxPaint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#AA000000") // More opaque black
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }

        progressBackgroundPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = circleStrokeWidth
            color = Color.parseColor("#55FFFFFF") // Semi-transparent white
            strokeCap = Paint.Cap.ROUND
        }

        progressForegroundPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = circleStrokeWidth
            color = Color.parseColor("#4CAF50") // Green
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onDraw(canvas: Canvas) {
        viewModel?.let { vm ->
            results?.let { poseLandmarkerResult ->
                for (landmarks in poseLandmarkerResult.landmarks()) {
                    // Draw body landmarks
                    calculateScaledPoints(landmarks)
                    drawBodySegments(canvas)
                    drawLandmarkPoints(canvas)

                    if (!isInRestPeriod) {
                        // Process the exercise using the ViewModel's exercise tracker
                        vm.exerciseTracker.processExercise(landmarks)

                        // Check if rep count has changed
                        val currentReps = vm.currentReps
                        if (lastRepCount != currentReps) {
                            animateProgress(currentReps.toFloat() / vm.targetReps.toFloat())
                            lastRepCount = currentReps
                        }

                        if (vm.isTargetMode && currentReps >= vm.targetReps) {
                            if (vm.currentSet < vm.targetSets) {
                                startRestPeriod(vm)
                            } else {
                                onExerciseCompleteListener?.invoke()
                            }
                        }
                    }
                    drawEnhancedUI(canvas, vm)
                }
            }
        }
    }

    private fun animateProgress(targetValue: Float) {
        targetProgressValue = targetValue.coerceIn(0f, 1f)
        ValueAnimator.ofFloat(progressValue, targetProgressValue).apply {
            duration = 300
            addUpdateListener { animator ->
                progressValue = animator.animatedValue as Float
                invalidate()
            }
            start()
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
                viewModel.exerciseTracker.resetRepCount() // Use the ViewModel's tracker
                progressValue = 0f
                targetProgressValue = 0f
                lastRepCount = 0
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

    private fun drawEnhancedUI(canvas: Canvas, viewModel: MainViewModel) {
        val padding = 24f
        val margin = 16f
        val boxHeight = 90f
        var yOffset = padding
        val rectWidth = 400f

        // Draw circular progress for rep counter
        if (viewModel.isTargetMode) {
            val circleX = padding + circleRadius + circlePadding
            val circleY = yOffset + circleRadius + circlePadding

            // Draw progress background
            canvas.drawCircle(circleX, circleY, circleRadius, infoBoxPaint)
            canvas.drawArc(
                circleX - circleRadius,
                circleY - circleRadius,
                circleX + circleRadius,
                circleY + circleRadius,
                -90f,
                360f,
                false,
                progressBackgroundPaint
            )

            // Draw progress arc
            canvas.drawArc(
                circleX - circleRadius,
                circleY - circleRadius,
                circleX + circleRadius,
                circleY + circleRadius,
                -90f,
                360f * progressValue,
                false,
                progressForegroundPaint
            )

            // Draw rep text
            val repText = "${viewModel.exerciseTracker.getRepCount()}"
            val targetText = "/${viewModel.targetReps}"

            textPaint.textSize = 64f
            val repBounds = Rect()
            textPaint.getTextBounds(repText, 0, repText.length, repBounds)

            textPaint.textSize = 36f
            val targetBounds = Rect()
            textPaint.getTextBounds(targetText, 0, targetText.length, targetBounds)

            // Draw rep count
            textPaint.textSize = 64f
            canvas.drawText(
                repText,
                circleX - repBounds.width() / 2,
                circleY + repBounds.height() / 3,
                textPaint
            )

            // Draw target
            textPaint.textSize = 36f
            canvas.drawText(
                targetText,
                circleX + repBounds.width() / 2 - 10f,
                circleY + repBounds.height() / 3,
                textPaint
            )

            // Draw set counter
            textPaint.textSize = 36f
            val setBox = RectF(
                circleX + circleRadius + margin,
                circleY - boxHeight / 2,
                circleX + circleRadius + margin + 120f,
                circleY + boxHeight / 2
            )
            canvas.drawRoundRect(setBox, cornerRadius, cornerRadius, infoBoxPaint)
            canvas.drawText(
                "SET",
                setBox.left + 16f,
                setBox.centerY() - 16f,
                titleTextPaint
            )
            canvas.drawText(
                "${viewModel.currentSet}/${viewModel.targetSets}",
                setBox.left + 16f,
                setBox.centerY() + 24f,
                textPaint
            )

            yOffset += circleRadius * 2 + circlePadding * 2 + margin

            // Draw rest timer if in rest period
            if (isInRestPeriod) {
                val restColor = Color.parseColor("#FF9800") // Orange
                progressForegroundPaint.color = restColor

                val restBox = RectF(
                    padding,
                    yOffset,
                    padding + rectWidth,
                    yOffset + boxHeight
                )
                infoBoxPaint.color = Color.parseColor("#AAFF9800") // Semi-transparent orange
                canvas.drawRoundRect(restBox, cornerRadius, cornerRadius, infoBoxPaint)
                infoBoxPaint.color = Color.parseColor("#AA000000") // Reset color

                titleTextPaint.textSize = 36f
                canvas.drawText(
                    "REST",
                    restBox.left + 16f,
                    restBox.centerY() - 16f,
                    titleTextPaint
                )

                textPaint.textSize = 48f
                canvas.drawText(
                    "${currentRestTime}s",
                    restBox.left + 16f,
                    restBox.centerY() + 24f,
                    textPaint
                )

                yOffset += boxHeight + margin
            }
        } else {
            // Draw simple rep counter for non-target mode
            val repBox = RectF(
                padding,
                yOffset,
                padding + rectWidth,
                yOffset + boxHeight
            )
            canvas.drawRoundRect(repBox, cornerRadius, cornerRadius, infoBoxPaint)

            titleTextPaint.textSize = 36f
            canvas.drawText(
                "REPS",
                repBox.left + 16f,
                repBox.centerY() - 16f,
                titleTextPaint
            )

            textPaint.textSize = 48f
            canvas.drawText(
                "${viewModel.exerciseTracker.getRepCount()}",
                repBox.left + 16f,
                repBox.centerY() + 24f,
                textPaint
            )

            yOffset += boxHeight + margin
        }

        // Draw status and form feedback
        val statusText = viewModel.exerciseTracker.repStatus
        val feedbackColor = when {
            statusText.contains("Good") -> Color.parseColor("#4CAF50") // Green
            statusText.contains("Start") -> Color.parseColor("#FFC107") // Yellow
            else -> Color.parseColor("#2196F3") // Blue
        }

        val statusBox = RectF(
            padding,
            yOffset,
            padding + rectWidth,
            yOffset + boxHeight
        )
        infoBoxPaint.color = Color.parseColor("#AA000000")
        canvas.drawRoundRect(statusBox, cornerRadius, cornerRadius, infoBoxPaint)

        // Draw colored status indicator
        val indicatorWidth = 8f
        val indicatorRect = RectF(
            statusBox.left,
            statusBox.top,
            statusBox.left + indicatorWidth,
            statusBox.bottom
        )
        val indicatorPaint = Paint().apply {
            color = feedbackColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(indicatorRect, cornerRadius/2, cornerRadius/2, indicatorPaint)

        titleTextPaint.textSize = 36f
        canvas.drawText(
            "STATUS",
            statusBox.left + indicatorWidth + 12f,
            statusBox.centerY() - 16f,
            titleTextPaint
        )

        // Adjust status text to be more concise if needed
        val displayStatus = if (statusText.length > 20) {
            statusText.substring(0, 20) + "..."
        } else {
            statusText
        }

        textPaint.textSize = 42f
        canvas.drawText(
            displayStatus,
            statusBox.left + indicatorWidth + 12f,
            statusBox.centerY() + 24f,
            textPaint
        )

        yOffset += boxHeight + margin

        // Draw form feedback
        val feedbackText = viewModel.exerciseTracker.formFeedback
        val formFeedbackColor = when {
            feedbackText.contains("Good") -> Color.parseColor("#4CAF50") // Green
            else -> Color.parseColor("#F44336") // Red
        }

        if (feedbackText.isNotEmpty() && !feedbackText.contains("Good")) {
            val feedbackBox = RectF(
                padding,
                yOffset,
                padding + rectWidth,
                yOffset + boxHeight
            )
            canvas.drawRoundRect(feedbackBox, cornerRadius, cornerRadius, infoBoxPaint)

            // Draw colored indicator
            val fbIndicatorRect = RectF(
                feedbackBox.left,
                feedbackBox.top,
                feedbackBox.left + indicatorWidth,
                feedbackBox.bottom
            )
            indicatorPaint.color = formFeedbackColor
            canvas.drawRoundRect(fbIndicatorRect, cornerRadius/2, cornerRadius/2, indicatorPaint)

            titleTextPaint.textSize = 36f
            canvas.drawText(
                "FORM",
                feedbackBox.left + indicatorWidth + 12f,
                feedbackBox.centerY() - 16f,
                titleTextPaint
            )

            // Adjust feedback text to be more concise if needed
            val displayFeedback = if (feedbackText.length > 20) {
                feedbackText.substring(0, 20) + "..."
            } else {
                feedbackText
            }

            textPaint.textSize = 42f
            canvas.drawText(
                displayFeedback,
                feedbackBox.left + indicatorWidth + 12f,
                feedbackBox.centerY() + 24f,
                textPaint
            )
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO ->
                minOf(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM ->
                maxOf(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        invalidate()
    }

    fun setExerciseType(type: ExerciseType) {
        Log.d("OverlayView", "Setting exercise type: ${type.name}")
        viewModel?.setExerciseType(type) // Use ViewModel to set exercise type

        // Reset progress and animation values
        progressValue = 0f
        targetProgressValue = 0f
        lastRepCount = 0
        restTimer?.cancel()
        invalidate()
    }

    fun startWorkoutSession(reps: Int, sets: Int, restTime: Int) {
        viewModel?.startWorkoutSession(reps, sets, restTime) // Use ViewModel's method
        // Reset progress and animation values
        progressValue = 0f
        targetProgressValue = 0f
        lastRepCount = 0
    }

    fun clear() {
        results = null
        bodyPath.reset()
        restTimer?.cancel()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        restTimer?.cancel()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}