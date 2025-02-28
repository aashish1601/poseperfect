package com.example.pose

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pose.databinding.ActivityProgressDetailsBinding

class ProgressDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressDetailsBinding
    private lateinit var exerciseType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: ExerciseType.NONE.name

        // Setup the UI with the exercise type
        setupUI()

        // Load progress data for the selected exercise
        loadProgressData()

        binding.viewAllButton.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupUI() {
        // Format exercise name for display
        val formattedName = exerciseType.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase()
                    .replaceFirstChar { char -> char.uppercase() }
            }

        // Set the title with the exercise name
        binding.exerciseNameText.text = formattedName

        // Configure back button
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadProgressData() {
        // Here you would load the actual progress data for the selected exercise
        // For now, we'll just set up the UI with placeholder data

        // This is where you would implement fetching progress data from your database
        // and populate the charts/stats in the UI
    }
}