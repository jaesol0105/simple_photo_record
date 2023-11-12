package com.beinny.android.photorecord.repository.recorddetail

import androidx.lifecycle.LiveData
import com.beinny.android.photorecord.model.Record
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

interface RecordDataSource {
    fun getRecords(): LiveData<List<Record>>
    fun getRecord(id: UUID): LiveData<Record?>
    suspend fun updateRecord(record: Record)
    suspend fun addRecord(record: Record)
    suspend fun deleteRecord(record: Record)
    suspend fun deleteAllRecord()
    suspend fun initCheck()
    suspend fun changeCheck(id: UUID, state: Boolean)
    suspend fun deleteCheckedRecord()
}