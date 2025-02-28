    package com.example.pose

    import android.content.Intent
    import android.os.Build
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.util.Log
    import android.view.View
    import android.widget.Button
    import android.widget.Toast
    import androidx.activity.viewModels
    import androidx.core.content.ContextCompat
    import androidx.navigation.fragment.NavHostFragment
    import androidx.navigation.ui.setupWithNavController
    import com.example.pose.databinding.ActivityMainBinding
    import com.google.android.datatransport.BuildConfig
    import java.util.Date

    class MainActivity : AppCompatActivity(), OverlayView.WorkoutCompletionListener {
        private lateinit var binding: ActivityMainBinding
        private val workoutViewModel: WorkoutViewModel by viewModels()
        val viewModel: MainViewModel by viewModels()

        // Add enum for rep modes
        enum class RepMode { TARGET, INFINITE,NO_COUNTING,COUNT_UP }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setupThemeAndStatusBar()

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupNavigation()
            setupWorkoutTracking()
            setupToolbar()
            handleIntentExtras()


            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        private fun setupThemeAndStatusBar() {
            setTheme(R.style.Theme_Pose)
            window.statusBarColor = ContextCompat.getColor(this, R.color.statuts_bar_colour)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        private fun setupToolbar() {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        private fun setupNavigation() {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragment_container) as NavHostFragment
            val navController = navHostFragment.navController

            // Set up navigation with controller
            binding.navigation.setupWithNavController(navController)

            // Add custom navigation handling
            binding.navigation.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.progress_tracking -> {
                        // Handle progress tracking activity launch
                        startActivity(Intent(this, ProgressActivity::class.java))
                        true
                    }
                    else -> {
                        // Let the Navigation UI handle other items
                        navController.navigate(item.itemId)
                        true
                    }
                }
            }

            binding.navigation.setOnItemReselectedListener { /* Ignore reselection */ }
        }

        private fun setupWorkoutTracking() {
            binding.overlayView.setWorkoutCompletionListener(this)
            binding.overlayView.setViewModel(viewModel) // Add this line
        }



        private fun handleIntentExtras() {
            intent?.extras?.let { extras ->
                val exerciseType = extras.getString("EXERCISE_TYPE", ExerciseType.NONE.name)
                val repMode = extras.getString("REP_MODE", RepMode.INFINITE.name)
                val targetReps = extras.getInt("TARGET_REPS", 0)
                val targetSets = extras.getInt("TARGET_SETS", 0)
                val restTime = extras.getInt("REST_TIME", 0)

                binding.overlayView.post {
                    binding.overlayView.setExerciseType(ExerciseType.valueOf(exerciseType))
                    when (RepMode.valueOf(repMode)) {
                        RepMode.TARGET -> {
                            if (targetReps > 0 && targetSets > 0) {
                                binding.overlayView.setTargetModeParams(targetReps, targetSets, restTime)
                                viewModel.isTargetMode = true
                            }
                        }
                        RepMode.COUNT_UP -> {
                            viewModel.isTargetMode = false
                            // No need to set target parameters
                        }
                        RepMode.NO_COUNTING -> {
                            viewModel.isTargetMode = false
                            // Disable rep counting in the overlay view
                        }
                        RepMode.INFINITE -> {
                            viewModel.isTargetMode = false
                            // Original behavior
                        }
                    }
                }
            }
        }

        override fun onWorkoutCompleted(session: WorkoutSession) {
            workoutViewModel.insert(session)
            Toast.makeText(
                this,
                "Saved ${session.totalSets} sets of ${session.exerciseType}",
                Toast.LENGTH_SHORT
            ).show()
        }
        override fun onSupportNavigateUp(): Boolean {
            saveAndExitWorkout()
            return true
        }
        private fun saveAndExitWorkout() {
            val overlayView = binding.overlayView
            val currentSession = WorkoutSession(
                exerciseType = overlayView.getCurrentExerciseType(),
                date = Date(),
                totalReps = overlayView.getCurrentReps(),
                totalSets = overlayView.getCurrentSet(),
                targetReps = overlayView.getTargetReps(),
                targetSets = overlayView.getTargetSets(),
                restTimeSeconds = overlayView.getRestTimeSeconds()
            )

            // Save workout data
            workoutViewModel.insert(currentSession)

            // Navigate back to ExerciseConfigActivity
            val intent = Intent(this, ExerciseConfigActivity::class.java)
            startActivity(intent)
            finish()
        }
        override fun onResume() {
            super.onResume()
            binding.overlayView.keepScreenOn = true // Prevent screen sleep
        }


        @Deprecated("Deprecated in Java")
        override fun onBackPressed() {
            super.onBackPressed()
            saveAndExitWorkout()
        }
    }