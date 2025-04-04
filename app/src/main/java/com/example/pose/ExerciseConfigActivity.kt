package com.example.pose

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.example.pose.databinding.ActivityExerciseConfigBinding
import java.util.Locale



class ExerciseConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExerciseConfigBinding
    private var exerciseType: String = ""
    private var validTargetReps = false
    private var validWeight = false
    private var selectedRepMode: MainActivity.RepMode = MainActivity.RepMode.NO_COUNTING
    private var weightUnit: String = "kg" // Default unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)


        exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: "SHOULDER_PRESS"
        binding.exerciseNameText.text = formatExerciseName(exerciseType)


        setupUI()
        setupListeners()
        setupTargetModeSwitch()
        setupWeightInput()
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

        // Set default selected weight unit
        binding.kgButton.isChecked = true
    }

    private fun setupWeightInput() {
        // Set up weight input validation
        binding.weightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validWeight = validateWeight(s?.toString())
            }
        })

        // Set up weight unit toggle
        binding.weightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                weightUnit = when (checkedId) {
                    R.id.kgButton -> "kg"
                    R.id.lbButton -> "lb"
                    else -> "kg"
                }
                Log.d("ExerciseConfig", "Weight unit selected: $weightUnit")
            }
        }
    }

    private fun validateWeight(input: String?): Boolean {
        return when {
            input.isNullOrEmpty() -> {
                binding.weightInputLayout.error = "Please enter weight"
                false
            }
            input.toDoubleOrNull() == null -> {
                binding.weightInputLayout.error = "Please enter a valid number"
                false
            }
            input.toDouble() <= 0 -> {
                binding.weightInputLayout.error = "Weight must be greater than 0"
                false
            }
            input.toDouble() > 1000 -> {
                binding.weightInputLayout.error = "Weight is too high"
                false
            }
            else -> {
                binding.weightInputLayout.error = null
                true
            }
        }
    }

    private fun updateRestTimeText(seconds: Int) {
        binding.restTimeText.text = getString(R.string.rest_time_seconds, seconds)
    }

    private fun setupListeners() {
        // Setup mode selection listener with improved visibility handling
        binding.noCountingMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = MainActivity.RepMode.NO_COUNTING
                binding.targetSettingsLayout.visibility = View.GONE
                binding.targetRepsInput.text?.clear()
                binding.targetRepsInputLayout.error = null

                // Ensure only one mode is selected
                binding.countUpMode.isChecked = false
                binding.targetMode.isChecked = false

                // Log the state for debugging
                Log.d("ExerciseConfig", "No counting mode enabled by user")
            }
        }


        binding.countUpMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = MainActivity.RepMode.COUNT_UP
                binding.targetSettingsLayout.visibility = View.GONE
                binding.targetRepsInput.text?.clear()
                binding.targetRepsInputLayout.error = null

                // Ensure only one mode is selected
                binding.noCountingMode.isChecked = false
                binding.targetMode.isChecked = false

                // Log the state for debugging
                Log.d("ExerciseConfig", "Count up mode enabled by user")
            }
        }

        binding.targetMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRepMode = MainActivity.RepMode.TARGET
                binding.targetSettingsLayout.visibility = View.VISIBLE
                binding.targetRepsInput.requestFocus()

                // Ensure only one mode is selected
                binding.noCountingMode.isChecked = false
                binding.countUpMode.isChecked = false

                // Log the state for debugging
                Log.d("ExerciseConfig", "Target mode enabled by user")
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

        binding.startButton.setOnClickListener {
            if (!validWeight) {
                Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
                binding.weightInput.requestFocus()
                return@setOnClickListener
            }

            if (selectedRepMode == MainActivity.RepMode.TARGET && !validTargetReps) {
                Toast.makeText(this, "Please enter valid target reps", Toast.LENGTH_SHORT).show()
                binding.targetRepsInput.requestFocus()
                return@setOnClickListener
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXERCISE_TYPE", exerciseType)
                putExtra("REP_MODE", selectedRepMode.name)
                putExtra("TARGET_REPS", binding.targetRepsInput.text.toString().toIntOrNull() ?: 0)
                putExtra("TARGET_SETS", binding.setsNumberPicker.value)
                putExtra("REST_TIME", binding.restTimeSlider.value.toInt())
                val weightValue = binding.weightInput.text.toString().toFloatOrNull() ?: 0f
                putExtra("WEIGHT", weightValue)
                putExtra("WEIGHT_UNIT", weightUnit)

                Log.d("ExerciseConfig", "Sending weight: $weightValue $weightUnit")
            }
            startActivity(intent)
        }

        binding.showprogressbutton.setOnClickListener {
            val intent = Intent(this, ProgressSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupTargetModeSwitch() {
        // This function is now integrated within the setOnCheckedChangeListener
        // for the target mode radio button and setupListeners()

        // Additional logging for initial state
        Log.d("ExerciseConfig", "Target mode initial state: ${selectedRepMode == MainActivity.RepMode.TARGET}")
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
}