package com.beinny.android.photorecord

import android.app.Application
import android.content.Context
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository

class PhotoRecordApplication : Application() {
    lateinit var context: Context

    init{
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        RecordRepository.initialize(this)
    }

    companion object {
        private var instance: PhotoRecordApplication? = null
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }
}