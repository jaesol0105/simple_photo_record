package com.beinny.android.photorecord.ui.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beinny.android.photorecord.ui.record.RecordViewModel
import com.beinny.android.photorecord.ui.recorddetail.RecordDetailViewModel
import java.lang.IllegalArgumentException

class ViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java))
        {
            return RecordViewModel() as T
        }
        else if (modelClass.isAssignableFrom(RecordDetailViewModel::class.java))
        {
            return RecordDetailViewModel() as T
        }
        else
        {
            throw IllegalArgumentException("Failed to create ViewModel: ${modelClass.name}")
        }
    }
}