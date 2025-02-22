package com.example.pose

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
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
        super.onDraw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmarks in poseLandmarkerResult.landmarks()) {
                calculateScaledPoints(landmarks)
                drawBodySegments(canvas)
                drawLandmarkPoints(canvas)

                val detectedExercise = exerciseTracker.detectExerciseType(landmarks)
                exerciseTracker.processExercise(landmarks, detectedExercise)

                drawEnhancedExerciseInfo(canvas)
            }
        }
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

    private fun drawEnhancedExerciseInfo(canvas: Canvas) {
        val padding = 20f
        val boxSpacing = 20f

        // Draw rep counter box
        val repCountText = "REPS: ${exerciseTracker.getRepCount()}"
        textPaint.textSize = 80f
        val repCountBounds = Rect()
        textPaint.getTextBounds(repCountText, 0, repCountText.length, repCountBounds)

        val repBoxRect = RectF(
            padding,
            padding,
            padding + repCountBounds.width() + 60f,
            padding + repCountBounds.height() + 40f
        )

        canvas.drawRoundRect(repBoxRect, cornerRadius, cornerRadius, repBoxPaint)
        canvas.drawText(repCountText,
            repBoxRect.left + 30f,
            repBoxRect.bottom - 20f,
            textPaint.apply { color = Color.WHITE })

        // Draw status box
        val statusText = exerciseTracker.repStatus
        textPaint.textSize = 60f
        val statusBounds = Rect()
        textPaint.getTextBounds(statusText, 0, statusText.length, statusBounds)

        val statusBoxRect = RectF(
            padding,
            repBoxRect.bottom + boxSpacing,
            padding + statusBounds.width() + 60f,
            repBoxRect.bottom + boxSpacing + statusBounds.height() + 40f
        )

        statusBoxPaint.color = when {
            statusText.contains("Good") -> Color.parseColor("#80228B22") // Semi-transparent green
            statusText.contains("Start") -> Color.parseColor("#80FFD700") // Semi-transparent gold
            else -> Color.parseColor("#80000000") // Semi-transparent black
        }

        canvas.drawRoundRect(statusBoxRect, cornerRadius, cornerRadius, statusBoxPaint)
        canvas.drawText(statusText,
            statusBoxRect.left + 30f,
            statusBoxRect.bottom - 20f,
            textPaint.apply {
                color = when {
                    statusText.contains("Good") -> Color.WHITE
                    statusText.contains("Start") -> Color.BLACK
                    else -> Color.WHITE
                }
            })

        // Draw form feedback
        val feedbackText = exerciseTracker.formFeedback
        textPaint.textSize = 50f
        val feedbackBounds = Rect()
        textPaint.getTextBounds(feedbackText, 0, feedbackText.length, feedbackBounds)

        val feedbackBoxRect = RectF(
            padding,
            statusBoxRect.bottom + boxSpacing,
            padding + feedbackBounds.width() + 60f,
            statusBoxRect.bottom + boxSpacing + feedbackBounds.height() + 40f
        )

        statusBoxPaint.color = when {
            feedbackText.contains("Good") -> Color.parseColor("#80228B22") // Semi-transparent green
            else -> Color.parseColor("#80FF4444") // Semi-transparent red
        }

        canvas.drawRoundRect(feedbackBoxRect, cornerRadius, cornerRadius, statusBoxPaint)
        canvas.drawText(feedbackText,
            feedbackBoxRect.left + 30f,
            feedbackBoxRect.bottom - 20f,
            textPaint.apply {
                color = Color.WHITE
                textSize = 50f
            })
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
        exerciseTracker.resetExercise()
        invalidate()
    }

    fun clear() {
        results = null
        bodyPath.reset()
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}