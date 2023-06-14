package com.beinny.android.photorecord.ui.datamgnt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DataMgntViewModel : ViewModel() {
    private val recordRepository = RecordRepository.get()

    fun deleteAllData() {
        viewModelScope.launch {
            recordRepository.deleteAllRecord()
        }
    }
}