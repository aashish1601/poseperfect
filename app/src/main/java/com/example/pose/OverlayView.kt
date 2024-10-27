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
    private val bodyPath = Path()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private val points = FloatArray(33 * 2)
    private val exerciseTracker = ExerciseTracker()

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmarks in poseLandmarkerResult.landmarks()) {
                calculateScaledPoints(landmarks)
                drawBodySegments(canvas)
                drawLandmarkPoints(canvas)

                // Process exercise tracking
                val detectedExercise = exerciseTracker.detectExerciseType(landmarks)
                exerciseTracker.processExercise(landmarks, detectedExercise)

                // Draw exercise information
                drawExerciseInfo(canvas)
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

    private fun drawExerciseInfo(canvas: Canvas) {
        // Draw rep counter
        textPaint.apply {
            textSize = 80f
            color = Color.WHITE
        }
        canvas.drawText("Reps: ${exerciseTracker.getRepCount()}", 50f, 100f, textPaint)

        // Draw form feedback
        textPaint.apply {
            textSize = 60f
            color = when {
                exerciseTracker.formFeedback.contains("Good") -> Color.GREEN
                else -> Color.RED
            }
        }
        canvas.drawText(exerciseTracker.formFeedback, 50f, 180f, textPaint)

        // Draw rep status
        textPaint.apply {
            color = when {
                exerciseTracker.repStatus.contains("Good") -> Color.GREEN
                exerciseTracker.repStatus.contains("Start") -> Color.YELLOW
                else -> Color.WHITE
            }
        }
        canvas.drawText(exerciseTracker.repStatus, 50f, 260f, textPaint)
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