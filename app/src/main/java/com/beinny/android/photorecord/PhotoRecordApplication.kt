package com.beinny.android.photorecord

import android.app.Application

class PhotoRecordApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecordRepository.initialize(this)
    }
}