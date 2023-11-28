package com.beinny.android.photorecord

import android.app.Application
import android.content.Context
import android.graphics.Point
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.common.PreferenceUtil

class PhotoRecordApplication : Application() {
    lateinit var context: Context

    init{
        instance = this
    }

    override fun onCreate() {
        prefs = PreferenceUtil(applicationContext) // onCreate 이전에 SharedPreference 초기화
        super.onCreate()
        // RecordRepository.initialize(this)
    }

    companion object {
        /** [SharedPreference 변수] */
        lateinit var prefs: PreferenceUtil

        private var instance: PhotoRecordApplication? = null
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }
}