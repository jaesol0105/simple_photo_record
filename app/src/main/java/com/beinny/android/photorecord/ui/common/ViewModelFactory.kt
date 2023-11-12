package com.beinny.android.photorecord.ui.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beinny.android.photorecord.ServiceLocator
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.datamgnt.DataMgntViewModel
import com.beinny.android.photorecord.ui.record.RecordViewModel
import com.beinny.android.photorecord.ui.recorddetail.RecordDetailViewModel
import java.lang.IllegalArgumentException

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java))
        {
            return RecordViewModel(ServiceLocator.provideRecordRepository(context)) as T
        }
        else if (modelClass.isAssignableFrom(RecordDetailViewModel::class.java))
        {
            return RecordDetailViewModel(ServiceLocator.provideRecordRepository(context)) as T
        }
        else if (modelClass.isAssignableFrom(DataMgntViewModel::class.java))
        {
            return DataMgntViewModel(ServiceLocator.provideRecordRepository(context)) as T
        }
        else
        {
            throw IllegalArgumentException("Failed to create ViewModel: ${modelClass.name}")
        }
    }
}