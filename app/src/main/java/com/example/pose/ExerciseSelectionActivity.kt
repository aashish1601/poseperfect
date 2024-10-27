package com.example.pose


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pose.R.color.statuts_bar_colour
import com.example.pose.databinding.ActivityExerciseSelectionBinding

class ExerciseSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExerciseSelectionBinding

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, statuts_bar_colour)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Set up click listener for shoulder press exercise
        binding.shoulderPressCard.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXERCISE_TYPE", "SHOULDER_PRESS")
            }
            startActivity(intent)
        }
    }
}

