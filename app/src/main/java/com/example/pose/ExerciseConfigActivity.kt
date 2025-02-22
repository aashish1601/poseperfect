package com.example.pose

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.pose.databinding.ActivityExerciseConfigBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

enum class RepMode {
    NO_COUNTING,
    COUNT_UP,
    TARGET
}

class ExerciseConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExerciseConfigBinding
    private var exerciseType: String = ""
    private var validTargetReps = false
    private var selectedRepMode: RepMode = RepMode.NO_COUNTING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: "SHOULDER_PRESS"
        binding.exerciseNameText.text = formatExerciseName(exerciseType)

        setupUI()
        setupListeners()
    }

    private fun formatExerciseName(name: String): String {
        return name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT)
                    .replaceFirstChar { char ->
                        char.titlecase(Locale.ROOT)
                    }
            }
    }

    private fun setupUI() {
        // Initialize target settings layout as hidden
        binding.targetSettingsLayout.visibility = View.GONE

        // Configure number picker for sets
        binding.setsNumberPicker.apply {
            minValue = 1
            maxValue = 10
            value = 3 // Default value
        }

        // Setup rest time slider
        binding.restTimeSlider.apply {
            value = 60f // Default value
            addOnChangeListener { _, value, _ ->
                updateRestTimeText(value.toInt())
            }
        }

        // Set initial rest time text
        updateRestTimeText(60)
    }

    private fun updateRestTimeText(seconds: Int) {
        binding.restTimeText.text = getString(R.string.rest_time_seconds, seconds)
    }

    private fun setupListeners() {
        // Setup mode selection listener with improved visibility handling
        binding.noCountingMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = RepMode.NO_COUNTING
                binding.targetSettingsLayout.visibility = View.GONE
                binding.targetRepsInput.text?.clear()
                binding.targetRepsInputLayout.error = null

                // Ensure only one mode is selected
                binding.countUpMode.isChecked = false
                binding.targetMode.isChecked = false
            }
        }

        binding.countUpMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = RepMode.COUNT_UP
                binding.targetSettingsLayout.visibility = View.GONE
                binding.targetRepsInput.text?.clear()
                binding.targetRepsInputLayout.error = null

                // Ensure only one mode is selected
                binding.noCountingMode.isChecked = false
                binding.targetMode.isChecked = false
            }
        }

        binding.targetMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = RepMode.TARGET
                binding.targetSettingsLayout.visibility = View.VISIBLE
                binding.targetRepsInput.requestFocus()

                // Ensure only one mode is selected
                binding.noCountingMode.isChecked = false
                binding.countUpMode.isChecked = false
            }
        }

        // Get references to the MaterialCardViews that contain the RadioButtons
        (binding.noCountingMode.parent as? com.google.android.material.card.MaterialCardView)?.setOnClickListener {
            binding.noCountingMode.isChecked = true
        }

        (binding.countUpMode.parent as? com.google.android.material.card.MaterialCardView)?.setOnClickListener {
            binding.countUpMode.isChecked = true
        }

        (binding.targetMode.parent as? com.google.android.material.card.MaterialCardView)?.setOnClickListener {
            binding.targetMode.isChecked = true
        }

        // Setup target reps input validation
        binding.targetRepsInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validTargetReps = validateTargetReps(s?.toString())
            }
        })

        // Setup start button
        binding.startButton.setOnClickListener {
            handleStartButtonClick()
        }
    }

    private fun validateTargetReps(input: String?): Boolean {
        return when {
            input.isNullOrEmpty() -> {
                binding.targetRepsInputLayout.error = getString(R.string.error_enter_target_reps)
                false
            }
            input.toIntOrNull() == null -> {
                binding.targetRepsInputLayout.error = getString(R.string.error_enter_valid_number)
                false
            }
            input.toInt() < 1 -> {
                binding.targetRepsInputLayout.error = getString(R.string.error_minimum_reps)
                false
            }
            input.toInt() > 999 -> {
                binding.targetRepsInputLayout.error = getString(R.string.error_maximum_reps)
                false
            }
            else -> {
                binding.targetRepsInputLayout.error = null
                true
            }
        }
    }

    private fun handleStartButtonClick() {
        // Check if any mode is selected
        if (selectedRepMode == RepMode.NO_COUNTING) {
            startExercise(selectedRepMode, 0, 0, 0)
        } else if (selectedRepMode == RepMode.COUNT_UP) {
            startExercise(selectedRepMode, 0, 0, 0)
        } else if (selectedRepMode == RepMode.TARGET) {
            if (!validTargetReps) {
                Snackbar.make(
                    binding.root,
                    R.string.error_enter_valid_target_reps,
                    Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            val targetReps = binding.targetRepsInput.text.toString().toInt()
            val targetSets = binding.setsNumberPicker.value
            val restTime = binding.restTimeSlider.value.toInt()

            if (targetSets < 1 || targetReps < 1) {
                Snackbar.make(
                    binding.root,
                    R.string.error_invalid_targets,
                    Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            startExercise(selectedRepMode, targetSets, targetReps, restTime)
        } else {
            Snackbar.make(binding.root, R.string.error_select_mode, Snackbar.LENGTH_SHORT).show()
            return
        }
    }

    private fun startExercise(
        repMode: RepMode,
        targetSets: Int = 0,
        targetReps: Int = 0,
        restTime: Int = 0
    ) {
        if (binding.noCountingMode.isChecked || binding.countUpMode.isChecked || binding.targetMode.isChecked) {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXERCISE_TYPE", exerciseType)
                putExtra("REP_MODE", repMode.name)
                putExtra("TARGET_SETS", targetSets)
                putExtra("TARGET_REPS", targetReps)
                putExtra("REST_TIME", restTime)
            }
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, R.string.error_select_mode, Snackbar.LENGTH_SHORT).show()
        }
    }
}