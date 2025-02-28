package com.example.pose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pose.ProgressFragment

class ProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load the ProgressFragment into the activity
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProgressFragment())
            .commit()

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}