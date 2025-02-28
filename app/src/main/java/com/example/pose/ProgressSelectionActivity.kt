package com.example.pose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pose.databinding.ActivityProgressSelectionBinding

class ProgressSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressSelectionBinding
    private lateinit var adapter: ExerciseAdapter

    // List of exercises
    private val exercises = listOf(
        Exercise(
            ExerciseType.SHOULDER_PRESS.name,
            "Shoulder Press",
            "Track your shoulder strength improvements",
            R.drawable.sdp
        ),
        Exercise(
            ExerciseType.BICEP_CURL.name,
            "Bicep Curl",
            "Monitor your arm development progress",
            R.drawable.bp
        ),
        Exercise(
            ExerciseType.SQUAT.name,
            "Squat",
            "View your lower body strength progression",
            R.drawable.squat
        ),
        Exercise(
            ExerciseType.PUSHUP.name,
            "Pull-Up",
            "Check your chest and core strength records",
            R.drawable.pullup
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = ExerciseAdapter(exercises) { exerciseType ->
            navigateToProgressDetails(exerciseType)
        }

        binding.exerciseRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProgressSelectionActivity)
            adapter = this@ProgressSelectionActivity.adapter
        }
    }

    private fun navigateToProgressDetails(exerciseType: String) {
        // This would navigate to a progress details screen
        // For now, we'll just create a placeholder intent
        val intent = Intent(this, ProgressDetailsActivity::class.java).apply {
            putExtra("EXERCISE_TYPE", exerciseType)
        }
        startActivity(intent)
    }

    // Data class for exercise items
    data class Exercise(
        val type: String,
        val name: String,
        val description: String,
        val iconResId: Int
    )

    // Adapter for the RecyclerView
    inner class ExerciseAdapter(
        private val exercises: List<Exercise>,
        private val onExerciseClick: (String) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise, parent, false)
            return ExerciseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            val exercise = exercises[position]
            holder.bind(exercise)
        }

        override fun getItemCount() = exercises.size

        inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val exerciseIcon: ImageView = itemView.findViewById(R.id.exerciseIcon)
            private val exerciseName: TextView = itemView.findViewById(R.id.exerciseName)
            private val exerciseDescription: TextView = itemView.findViewById(R.id.exerciseDescription)

            fun bind(exercise: Exercise) {
                exerciseIcon.setImageResource(exercise.iconResId)
                exerciseName.text = exercise.name
                exerciseDescription.text = exercise.description

                itemView.setOnClickListener {
                    onExerciseClick(exercise.type)
                }
            }
        }
    }
}