package com.example.pose

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pose.databinding.ActivityProgressDetailsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        setupChart()
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

    private fun setupChart() {
        val chart = com.github.mikephil.charting.charts.LineChart(this)
        binding.chartContainer.addView(chart)

        // Configure chart appearance
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)

        // Configure axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f

        // Remove right axis
        chart.axisRight.isEnabled = false

        // Add legend
        chart.legend.isEnabled = true

        // Create empty data
        val lineDataSet = LineDataSet(ArrayList(), "Reps")
        lineDataSet.color = Color.BLUE
        lineDataSet.setCircleColor(Color.BLUE)
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setDrawCircleHole(false)
        lineDataSet.valueTextSize = 10f
        lineDataSet.setDrawFilled(true)
        lineDataSet.fillColor = Color.parseColor("#4D0000FF") // Semi-transparent blue

        val lineData = LineData(lineDataSet)
        chart.data = lineData
        chart.invalidate()

        // Save chart to be updated later
        chart.tag = "progressChart"
    }

    private fun observeViewModel() {
        viewModel.getSessionsByExercise(exerciseType).observe(this) { sessions ->
            if (sessions.isNotEmpty()) {
                updateSummaryData(sessions)
                updateRecentWorkouts(sessions)
                updateProgressChart(sessions)
            }
        }
    }

    private fun updateProgressChart(sessions: List<WorkoutSession>) {
        // Get the chart
        val chart = binding.chartContainer.findViewWithTag<com.github.mikephil.charting.charts.LineChart>("progressChart") ?: return

        // Sort sessions by date (oldest to newest)
        val sortedSessions = sessions.sortedBy { it.date }

        // Create entries for the chart
        val entries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        // Add entries for each session
        sortedSessions.forEachIndexed { index, session ->
            entries.add(Entry(index.toFloat(), session.totalReps.toFloat()))
            dateLabels.add(dateFormat.format(session.date))
        }

        // Update chart data
        val dataSet = LineDataSet(entries, "Average Reps")
        dataSet.color = Color.BLUE
        dataSet.setCircleColor(Color.BLUE)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#4D0000FF") // Semi-transparent blue

        // Set X-axis labels to show dates
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

        // Update chart data and refresh
        val lineData = LineData(dataSet)
        chart.data = lineData

        // Add chart animation
        chart.animateX(1000)

        // If we have many data points, ensure the chart can be scrolled
        if (entries.size > 7) {
            chart.setVisibleXRangeMaximum(7f)
            chart.moveViewToX(entries.size.toFloat() - 7)
        }

        // Refresh chart
        chart.invalidate()
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
        // Clear existing workout items
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