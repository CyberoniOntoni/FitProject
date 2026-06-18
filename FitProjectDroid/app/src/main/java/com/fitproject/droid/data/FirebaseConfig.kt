package com.fitproject.droid.data

/**
 * FitPros Firebase project constants (from app.fitpros.io web config).
 *
 * Android also reads [com.google.firebase:firebase] values from `app/google-services.json`.
 * The checked-in file uses the correct project number but a placeholder `mobilesdk_app_id`
 * until FitPros registers `com.fitproject.droid` in Firebase Console and provides the
 * official download. Replace `google-services.json` with that file when available.
 */
object FirebaseConfig {
    const val ApiKey = "AIzaSyDcR92CtVhDqtxeEGRu_8JGuGw2UicjQQ0"
    const val ProjectId = "workouts-67e5d"
    const val MessagingSenderId = "921130967436"
    const val StorageBucket = "workouts-67e5d.appspot.com"
    const val PackageName = "com.fitproject.droid"
}