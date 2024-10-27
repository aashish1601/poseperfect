package com.example.pose

data class JointAngle(
    val angle: Double,
    val jointName: String,
    val isCorrect: Boolean
)