package com.example.pose

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pose.databinding.ActivityUserRegistrationBinding
import java.text.DecimalFormat

class UserRegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserRegistrationBinding

    // Define fitness levels and goals
    private val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced", "Athletic")
    private val fitnessGoals = listOf(
        "Build Muscle", "Lose Weight", "Improve Strength",
        "Increase Flexibility", "Improve Posture", "Sports Performance"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.statuts_bar_colour)

        // Optional: Set light status bar icons if background is light (requires Android M+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setupFitnessLevelDropdown()
        setupFitnessGoalDropdown()
        setupWorkoutDaysSlider()
        setupInputValidation()
        setupButtons()
    }

    private fun setupFitnessLevelDropdown() {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, fitnessLevels)
        (binding.fitnessLevelDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupFitnessGoalDropdown() {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, fitnessGoals)
        (binding.fitnessGoalDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupWorkoutDaysSlider() {
        binding.workoutDaysSlider.addOnChangeListener { _, value, _ ->
            binding.workoutDaysLabel.text = "Workout Days Per Week: ${value.toInt()}"
        }
    }

    private fun setupInputValidation() {
        // Add text watchers to validate input fields
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        binding.nameEditText.addTextChangedListener(textWatcher)
        binding.ageEditText.addTextChangedListener(textWatcher)
        binding.heightEditText.addTextChangedListener(textWatcher)
        binding.weightEditText.addTextChangedListener(textWatcher)
    }

    private fun setupButtons() {
        // Calculate BMI button click listener
        binding.calculateBmiButton.setOnClickListener {
            calculateAndDisplayBMI()
        }

        // Continue button click listener
        binding.continueButton.setOnClickListener {
            if (validateInputs()) {
                saveUserData()
                navigateToNextScreen()
            } else {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.nameEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val height = binding.heightEditText.text.toString().trim()
        val weight = binding.weightEditText.text.toString().trim()
        val fitnessLevel = binding.fitnessLevelDropdown.text.toString().trim()
        val fitnessGoal = binding.fitnessGoalDropdown.text.toString().trim()

        val isValid = name.isNotEmpty() && age.isNotEmpty() &&
                height.isNotEmpty() && weight.isNotEmpty() &&
                fitnessLevel.isNotEmpty() && fitnessGoal.isNotEmpty()

        binding.calculateBmiButton.isEnabled = height.isNotEmpty() && weight.isNotEmpty()

        return isValid
    }

    private fun calculateAndDisplayBMI() {
        try {
            val height = binding.heightEditText.text.toString().toFloat() / 100 // cm to m
            val weight = binding.weightEditText.text.toString().toFloat()

            if (height <= 0 || weight <= 0) {
                Toast.makeText(this, "Please enter valid height and weight values", Toast.LENGTH_SHORT).show()
                return
            }

            val bmi = weight / (height * height)
            val df = DecimalFormat("#.#")
            val formattedBmi = df.format(bmi)

            binding.bmiValueText.text = formattedBmi
            binding.bmiCategoryText.text = getBMICategory(bmi)
            binding.bmiCategoryText.setTextColor(getBMICategoryColor(bmi))

            binding.bmiCard.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Error calculating BMI. Please check your inputs.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBMICategory(bmi: Float): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal weight"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    private fun getBMICategoryColor(bmi: Float): Int {
        return when {
            bmi < 18.5 -> ContextCompat.getColor(this, R.color.bmi_underweight) // Define these colors in colors.xml
            bmi < 25 -> ContextCompat.getColor(this, R.color.bmi_normal)
            bmi < 30 -> ContextCompat.getColor(this, R.color.bmi_overweight)
            else -> ContextCompat.getColor(this, R.color.bmi_obese)
        }
    }

    private fun saveUserData() {
        // Create a UserProfile object
        val userProfile = UserProfile(
            name = binding.nameEditText.text.toString().trim(),
            age = binding.ageEditText.text.toString().toInt(),
            height = binding.heightEditText.text.toString().toFloat(),
            weight = binding.weightEditText.text.toString().toFloat(),
            fitnessLevel = binding.fitnessLevelDropdown.text.toString(),
            fitnessGoal = binding.fitnessGoalDropdown.text.toString(),
            workoutDaysPerWeek = binding.workoutDaysSlider.value.toInt()
        )

        // Save to SharedPreferences or local database
        // For brevity, I'm using a simple approach here
        // In a production app, you'd want to use a repository pattern or ViewModel
        val sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("name", userProfile.name)
            putInt("age", userProfile.age)
            putFloat("height", userProfile.height)
            putFloat("weight", userProfile.weight)
            putString("fitness_level", userProfile.fitnessLevel)
            putString("fitness_goal", userProfile.fitnessGoal)
            putInt("workout_days", userProfile.workoutDaysPerWeek)
            apply()
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, ExerciseSelectionActivity::class.java)
        startActivity(intent)
        finish() // Prevent going back to registration screen
    }
}

// Data class to represent user profile
data class UserProfile(
    val name: String,
    val age: Int,
    val height: Float,  // in cm
    val weight: Float,  // in kg
    val fitnessLevel: String,
    val fitnessGoal: String,
    val workoutDaysPerWeek: Int
) {
    fun calculateBMI(): Float {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }
}