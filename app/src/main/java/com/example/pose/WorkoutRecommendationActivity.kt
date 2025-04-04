package com.example.pose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class WorkoutRecommendationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_recommendation)

        // Extract workout data from intent
        val exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: "NONE"
        val totalReps = intent.getIntExtra("TOTAL_REPS", 0)
        val totalSets = intent.getIntExtra("TOTAL_SETS", 0)
        val targetReps = intent.getIntExtra("TARGET_REPS", 0)
        val targetSets = intent.getIntExtra("TARGET_SETS", 0)
        val formFeedback = intent.getStringExtra("FORM_FEEDBACK") ?: "Good form"
        val weight = intent.getFloatExtra("WEIGHT", 0f)
        val weightUnit = intent.getStringExtra("WEIGHT_UNIT") ?: "kg"

        setupHeader(exerciseType, weight, weightUnit)
        setupSummary(totalSets, targetSets, totalReps, targetReps, formFeedback)
        setupRecommendations(exerciseType, totalReps, targetReps, formFeedback, weight)
        setupFinishButton()
    }

    private fun setupHeader(exerciseType: String, weight: Float, weightUnit: String) {
        // Set date
        val tvDate = findViewById<TextView>(R.id.tv_date)
        val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        tvDate.text = currentDate

        // Set exercise name
        val tvExerciseName = findViewById<TextView>(R.id.tv_exercise_name)
        tvExerciseName.text = formatExerciseName(exerciseType)

        // Set weight
        val tvWeight = findViewById<TextView>(R.id.tv_weight)
        tvWeight.text = "$weight $weightUnit"
    }

    private fun setupSummary(totalSets: Int, targetSets: Int, totalReps: Int, targetReps: Int, formFeedback: String) {
        // Set sets count
        val tvSets = findViewById<TextView>(R.id.tv_sets)
        tvSets.text = "$totalSets${if (targetSets > 0) "/$targetSets" else ""}"

        // Set reps count
        val tvReps = findViewById<TextView>(R.id.tv_reps)
        tvReps.text = "$totalReps${if (targetReps > 0) "/$targetReps" else ""}"

        // Set form quality
        val tvForm = findViewById<TextView>(R.id.tv_form)
        val formText = if (formFeedback.contains("Good", ignoreCase = true)) "Good" else "Needs Work"
        tvForm.text = formText

        val formColor = if (formFeedback.contains("Good", ignoreCase = true))
            ContextCompat.getColor(this, R.color.green)
        else
            ContextCompat.getColor(this, R.color.red)

        tvForm.setTextColor(formColor)
    }

    private fun setupRecommendations(
        exerciseType: String,
        totalReps: Int,
        targetReps: Int,
        formFeedback: String,
        weight: Float
    ) {
        val recommendationsContainer = findViewById<LinearLayout>(R.id.recommendations_container)
        val recommendations = generateRecommendations(exerciseType, totalReps, targetReps, formFeedback, weight)

        for (recommendation in recommendations) {
            val view = LayoutInflater.from(this).inflate(
                R.layout.item_recommendation,
                recommendationsContainer,
                false
            )

            val iconView = view.findViewById<ImageView>(R.id.iv_icon)
            val titleView = view.findViewById<TextView>(R.id.tv_title)
            val descriptionView = view.findViewById<TextView>(R.id.tv_description)

            // Set icon resource
            val iconResId = getIconResourceForName(recommendation.iconName)
            iconView.setImageResource(iconResId)
            iconView.setColorFilter(ContextCompat.getColor(this, recommendation.colorResId))

            // Set title and description
            titleView.text = recommendation.title
            titleView.setTextColor(ContextCompat.getColor(this, recommendation.colorResId))
            descriptionView.text = recommendation.description

            recommendationsContainer.addView(view)
        }
    }

    private fun setupFinishButton() {
        val btnFinish = findViewById<MaterialButton>(R.id.btn_finish)
        btnFinish.setOnClickListener {
            // Navigate back to exercise config activity
            val intent = Intent(this, ExerciseConfigActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getIconResourceForName(iconName: String): Int {
        return when (iconName) {
            "ic_trending_up" -> R.drawable.ic_trending_up
            "ic_trending_down" -> R.drawable.ic_trending_down
            "ic_assignment_late" -> R.drawable.ic_assignment_late
            "ic_thumb_up" -> R.drawable.ic_thumb_up
            "ic_fitness_center" -> R.drawable.ic_fitness_center
            "ic_bedtime" -> R.drawable.ic_bedtime
            else -> R.drawable.ic_fitness_center // Default icon
        }
    }

    data class RecommendationXml(
        val title: String,
        val description: String,
        val iconName: String,
        val colorResId: Int
    )

    private fun generateRecommendations(
        exerciseType: String,
        totalReps: Int,
        targetReps: Int,
        formFeedback: String,
        weight: Float
    ): List<RecommendationXml> {
        val recommendations = mutableListOf<RecommendationXml>()

        // Form-based recommendation
        if (!formFeedback.contains("Good", ignoreCase = true)) {
            recommendations.add(
                RecommendationXml(
                    title = "Focus on Form",
                    description = "Your form needs improvement. Try reducing the weight slightly and focus on proper technique before increasing intensity.",
                    iconName = "ic_assignment_late",
                    colorResId = R.color.red
                )
            )
        } else {
            recommendations.add(
                RecommendationXml(
                    title = "Great Form",
                    description = "You maintained good form throughout your workout. Keep it up!",
                    iconName = "ic_thumb_up",
                    colorResId = R.color.green
                )
            )
        }

        // Weight-based recommendation
        if (totalReps > targetReps * 1.2 && formFeedback.contains("Good", ignoreCase = true)) {
            recommendations.add(
                RecommendationXml(
                    title = "Increase Weight",
                    description = "You completed ${totalReps} reps with good form. Consider increasing the weight by 5-10% for your next workout to continue progressing.",
                    iconName = "ic_trending_up",
                    colorResId = R.color.blue
                )
            )
        } else if (totalReps < targetReps * 0.8) {
            recommendations.add(
                RecommendationXml(
                    title = "Adjust Weight",
                    description = "You completed fewer reps than targeted. Consider reducing the weight by 5-10% to complete your full set targets.",
                    iconName = "ic_trending_down",
                    colorResId = R.color.orange
                )
            )
        }

        // Exercise-specific recommendation
        val exerciseRecommendation = when (exerciseType) {
            "SQUAT" -> RecommendationXml(
                title = "Squat Tip",
                description = "Focus on keeping your chest up and knees tracking over your toes. Consider adding some mobility work to improve squat depth.",
                iconName = "ic_fitness_center",
                colorResId = R.color.purple
            )
            "PUSH_UP" -> RecommendationXml(
                title = "Push-Up Tip",
                description = "Keep your core tight and body in a straight line. For more challenge, try diamond push-ups or elevate your feet.",
                iconName = "ic_fitness_center",
                colorResId = R.color.purple
            )
            "PLANK" -> RecommendationXml(
                title = "Plank Tip",
                description = "Focus on maintaining a neutral spine and keeping your core engaged. Try side planks for a balanced core workout.",
                iconName = "ic_fitness_center",
                colorResId = R.color.purple
            )
            "BICEP_CURL" -> RecommendationXml(
                title = "Bicep Curl Tip",
                description = "Avoid swinging and keep your elbows fixed at your sides. Try alternating arms for better focus on each bicep.",
                iconName = "ic_fitness_center",
                colorResId = R.color.purple
            )
            else -> RecommendationXml(
                title = "Recovery Tip",
                description = "Make sure to get proper rest and nutrition. Muscles grow during recovery, not during the workout.",
                iconName = "ic_bedtime",
                colorResId = R.color.purple
            )
        }

        recommendations.add(exerciseRecommendation)

        return recommendations
    }

    private fun formatExerciseName(exerciseType: String): String {
        return exerciseType.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}