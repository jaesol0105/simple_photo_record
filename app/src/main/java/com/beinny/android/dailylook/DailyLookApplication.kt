package com.beinny.android.dailylook

import android.app.Application

class DailyLookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyRepository.initialize(this)
    }
}