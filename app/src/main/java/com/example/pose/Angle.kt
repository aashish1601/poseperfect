package com.example.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2
import kotlin.math.absoluteValue

class PoseAngleCalculator {
    companion object {
        // Landmark indices
        private val RIGHT_SHOULDER_INDICES = Triple(12, 11, 13)
        private val LEFT_SHOULDER_INDICES = Triple(11, 12, 14)
        private val RIGHT_ELBOW_INDICES = Triple(13, 11, 15)
        private val LEFT_ELBOW_INDICES = Triple(14, 12, 16)
        private val RIGHT_HIP_INDICES = Triple(24, 23, 26)
        private val LEFT_HIP_INDICES = Triple(23, 24, 25)
        private val RIGHT_KNEE_INDICES = Triple(26, 24, 28)
        private val LEFT_KNEE_INDICES = Triple(25, 23, 27)
        private val RIGHT_SHOULDER_PRESS_INDICES = Triple(12, 14, 16)
        private val LEFT_SHOULDER_PRESS_INDICES = Triple(11, 13, 15)

        // Angle ranges for exercise detection
        private val SQUAT_DETECTION_KNEE_RANGE = 60.0..120.0
        private val PUSHUP_DETECTION_ELBOW_RANGE = 70.0..110.0
        private val SHOULDER_PRESS_DETECTION_RANGE = 150.0..180.0
        private const val MAX_ELBOW_FLARE_ANGLE = 15.0 // Maximum allowed angle deviation for elbow alignment
        private const val SYMMETRY_THRESHOLD = 10.0


        fun calculateShoulderAngle(landmarks: List<NormalizedLandmark>, isRight: Boolean): Double {
            val indices = if (isRight) RIGHT_SHOULDER_PRESS_INDICES else LEFT_SHOULDER_PRESS_INDICES
            return calculateAngle(
                landmarks[indices.first],
                landmarks[indices.second],
                landmarks[indices.third]
            )
        }

        fun areElbowsInline(landmarks: List<NormalizedLandmark>): Boolean {
            val leftShoulder = landmarks[11]
            val rightShoulder = landmarks[12]
            val leftElbow = landmarks[13]
            val rightElbow = landmarks[14]
            // Calculate the angle between shoulders line and each elbow
            val shoulderLineAngle = Math.toDegrees(
                atan2(
                    (rightShoulder.y() - leftShoulder.y()).toDouble(),
                    (rightShoulder.x() - leftShoulder.x()).toDouble()
                )
            )

            // Calculate elbow angles relative to shoulder line
            val leftElbowAngle = Math.toDegrees(
                atan2(
                    (leftElbow.y() - leftShoulder.y()).toDouble(),
                    (leftElbow.x() - leftShoulder.x()).toDouble()
                )
            )

            val rightElbowAngle = Math.toDegrees(
                atan2(
                    (rightElbow.y() - rightShoulder.y()).toDouble(),
                    (rightElbow.x() - rightShoulder.x()).toDouble()
                )
            )

            // Calculate the deviation from the ideal alignment
            val leftDeviation = (leftElbowAngle - shoulderLineAngle).absoluteValue
            val rightDeviation = (rightElbowAngle - shoulderLineAngle).absoluteValue

            // Elbows are considered inline if their deviation is within the acceptable range
            return leftDeviation <= MAX_ELBOW_FLARE_ANGLE && rightDeviation <= MAX_ELBOW_FLARE_ANGLE
        }
        fun isGoodForm(landmarks: List<NormalizedLandmark>, exerciseType: ExerciseType): Boolean {
            return when (exerciseType) {
                ExerciseType.SHOULDER_PRESS -> {
                    val isVertical = isBodyVertical(landmarks)

                    // Check symmetry between left and right shoulders
                    val leftShoulderAngle = calculateShoulderAngle(landmarks, false)
                    val rightShoulderAngle = calculateShoulderAngle(landmarks, true)
                    val isSymmetrical = (leftShoulderAngle - rightShoulderAngle).absoluteValue <= SYMMETRY_THRESHOLD

                    // Check if elbows are properly aligned
                    val elbowsNotFlared = areElbowsInline(landmarks)

                    isVertical && isSymmetrical && elbowsNotFlared
                }

                ExerciseType.PUSHUP -> {
                    val isHorizontal = isBodyHorizontal(landmarks)
                    val leftElbowAngle = calculateElbowAngle(landmarks, false)
                    val rightElbowAngle = calculateElbowAngle(landmarks, true)
                    val isSymmetrical = (leftElbowAngle - rightElbowAngle).absoluteValue <= SYMMETRY_THRESHOLD
                    val elbowsNotFlared = areElbowsInline(landmarks)

                    isHorizontal && isSymmetrical && elbowsNotFlared
                }

                ExerciseType.SQUAT -> {
                    val isVertical = isBodyVertical(landmarks)
                    val leftKneeAngle = calculateKneeAngle(landmarks, false)
                    val rightKneeAngle = calculateKneeAngle(landmarks, true)
                    val isSymmetrical = (leftKneeAngle - rightKneeAngle).absoluteValue <= SYMMETRY_THRESHOLD

                    isVertical && isSymmetrical
                }

                ExerciseType.BICEP_CURL -> {
                    val isVertical = isBodyVertical(landmarks)
                    val leftElbowAngle = calculateElbowAngle(landmarks, false)
                    val rightElbowAngle = calculateElbowAngle(landmarks, true)
                    val isSymmetrical = (leftElbowAngle - rightElbowAngle).absoluteValue <= SYMMETRY_THRESHOLD

                    isVertical && isSymmetrical
                }

                ExerciseType.NONE -> false
            }
        }

        fun calculateHipAngle(landmarks: List<NormalizedLandmark>, isRight: Boolean): Double {
            val indices = if (isRight) RIGHT_HIP_INDICES else LEFT_HIP_INDICES
            return calculateAngle(
                landmarks[indices.first],
                landmarks[indices.second],
                landmarks[indices.third]
            )
        }

        fun calculateElbowAngle(landmarks: List<NormalizedLandmark>, isRight: Boolean): Double {
            val indices = if (isRight) RIGHT_ELBOW_INDICES else LEFT_ELBOW_INDICES
            return calculateAngle(
                landmarks[indices.first],
                landmarks[indices.second],
                landmarks[indices.third]
            )
        }

        fun calculateKneeAngle(landmarks: List<NormalizedLandmark>, isRight: Boolean): Double {
            val indices = if (isRight) RIGHT_KNEE_INDICES else LEFT_KNEE_INDICES
            return calculateAngle(
                landmarks[indices.first],
                landmarks[indices.second],
                landmarks[indices.third]
            )
        }



        fun isBodyVertical(landmarks: List<NormalizedLandmark>): Boolean {
            val shoulder = landmarks[11]
            val hip = landmarks[23]
            val angleToVertical = Math.toDegrees(
                atan2(
                    (hip.x() - shoulder.x()).toDouble(),
                    (hip.y() - shoulder.y()).toDouble()
                )
            ).absoluteValue
            return angleToVertical <= 30.0
        }

        fun isBodyHorizontal(landmarks: List<NormalizedLandmark>): Boolean {
            val shoulder = landmarks[11]
            val hip = landmarks[23]
            val angleToHorizontal = Math.toDegrees(
                atan2(
                    (hip.y() - shoulder.y()).toDouble(),
                    (hip.x() - shoulder.x()).toDouble()
                )
            ).absoluteValue
            return angleToHorizontal in 0.0..30.0
        }

        private fun calculateAngle(
            firstPoint: NormalizedLandmark,
            midPoint: NormalizedLandmark,
            lastPoint: NormalizedLandmark
        ): Double {
            val angle = Math.toDegrees(
                (atan2(
                    lastPoint.y() - midPoint.y(),
                    lastPoint.x() - midPoint.x()
                ) - atan2(
                    firstPoint.y() - midPoint.y(),
                    firstPoint.x() - midPoint.x()
                )).toDouble()
            ).absoluteValue

            return if (angle > 180.0) 360.0 - angle else angle
        }
    }
}