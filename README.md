Pose Perfect

Pose Perfect is an Android application designed to help fitness enthusiasts and gym-goers maintain the correct posture during workouts, helping prevent exercise-related injuries. Built using Kotlin, the app integrates Mediapipe and TensorFlow Lite’s MoveNet for real-time pose estimation, allowing users to receive immediate feedback on their form and make adjustments to ensure safe, effective workouts.

Features

1) Real-Time Pose Detection: Uses advanced AI-powered pose estimation to analyze and provide feedback on user posture.
2) Injury Prevention: Helps users avoid common mistakes in form that can lead to injuries, especially in strength training and other high-intensity workouts.
3) User-Friendly Interface: Easy-to-navigate design that allows users of all fitness levels to check their form during exercises.
4) Privacy Focused: No data is stored; all analysis is performed locally on the device to ensure user privacy.

Technologies Used

Kotlin: The core language for app development, leveraging Android’s Jetpack Compose for a seamless UI experience.
Mediapipe and MoveNet: Implements Mediapipe and TensorFlow Lite’s MoveNet model for precise, real-time pose estimation and analysis.
Android Camera: Integrated with the Android camera for live feedback, allowing users to see their pose in real-time.


Installation
Clone the repository:
bash
Copy code
git clone https://github.com/aashish1601/Pose-Perfect.git
Open the project in Android Studio.
Sync project dependencies.
Build and run on an Android device with camera access.
Usage
Open the app and grant necessary permissions (e.g., camera).
Position yourself in front of the camera to allow the app to detect your body posture.
Start your exercise routine, and the app will provide real-time feedback to help you maintain proper form.
Adjust your posture as per feedback to avoid injury.
Future Enhancements
Exercise Recommendations: Adding guided workouts based on user posture.
Workout History Tracking: Allow users to view and track improvements over time.
Augmented Reality Feedback: Enhanced AR-based overlays for more intuitive guidance.
