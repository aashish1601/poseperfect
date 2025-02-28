package com.example.pose

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pose.databinding.ActivityProgressDetailsBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProgressDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressDetailsBinding
    private val viewModel: WorkoutViewModel by viewModels()
    private lateinit var exerciseType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: ""
        if (exerciseType.isEmpty()) {
            finish()
            return
        }

        setupUI()
        observeViewModel()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.viewAllButton.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupUI() {
        val formattedName = exerciseType.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase()
                    .replaceFirstChar { char -> char.uppercase() }
            }

        binding.exerciseNameText.text = "$formattedName Progress"
    }

    private fun observeViewModel() {
        viewModel.getSessionsByExercise(exerciseType).observe(this) { sessions ->
            if (sessions.isNotEmpty()) {
                updateSummaryData(sessions)
                updateRecentWorkouts(sessions)
            }
        }
    }

    private fun updateSummaryData(sessions: List<WorkoutSession>) {
        val totalWorkouts = sessions.size
        val totalReps = sessions.sumOf { it.totalReps }
        val bestSet = sessions.maxOf { it.totalReps }

        binding.totalWorkouts.text = totalWorkouts.toString()
        binding.totalReps.text = totalReps.toString()
        binding.bestSet.text = bestSet.toString()
    }

    private fun updateRecentWorkouts(sessions: List<WorkoutSession>) {
        // Clear existing workout items (except the sample ones that are in XML)
        binding.recentWorkoutsContainer.removeAllViews()

        // Get the 3 most recent workouts
        val recentWorkouts = sessions.sortedByDescending { it.date }.take(3)

        val dateTimeFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        // Add recent workouts to the container
        recentWorkouts.forEachIndexed { index, session ->
            val layout = layoutInflater.inflate(
                R.layout.item_recent_workout,
                binding.recentWorkoutsContainer,
                false
            )

            // Set the date text
            val dateTextView = layout.findViewById<android.widget.TextView>(R.id.workout_date)
            dateTextView.text = dateTimeFormat.format(session.date)

            // Set the reps text
            val repsTextView = layout.findViewById<android.widget.TextView>(R.id.workout_reps)
            repsTextView.text = "${session.totalReps} reps"

            // Add the layout to the container
            binding.recentWorkoutsContainer.addView(layout)

            // Add a divider if it's not the last item
            if (index < recentWorkouts.size - 1) {
                val divider = android.view.View(this)
                divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"))
                binding.recentWorkoutsContainer.addView(divider)
            }
        }
    }
}