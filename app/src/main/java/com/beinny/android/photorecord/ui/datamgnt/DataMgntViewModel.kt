package com.beinny.android.photorecord.ui.datamgnt

import androidx.lifecycle.ViewModel
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository

class DataMgntViewModel : ViewModel() {
    private val recordRepository = RecordRepository.get()

    fun deleteAllData() {
        recordRepository.deleteAllRecord()
    }
}