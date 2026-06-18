package com.fitproject.droid

import android.app.Application
import com.google.firebase.FirebaseApp

class FitProjectDroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}