package com.celmatech.studeco

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudeCoApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
