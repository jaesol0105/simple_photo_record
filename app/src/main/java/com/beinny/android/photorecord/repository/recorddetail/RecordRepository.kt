package com.beinny.android.photorecord.repository.recorddetail

import androidx.lifecycle.LiveData
import com.beinny.android.photorecord.model.Record
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class RecordRepository(
    private val localDataSource: RecordLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getRecords(): LiveData<List<Record>> = localDataSource.getRecords()
    fun getRecord(id: UUID): LiveData<Record?> = localDataSource.getRecord(id)

    suspend fun updateRecord(record: Record) {
        withContext(ioDispatcher) {
            localDataSource.updateRecord(record)
        }
    }

    suspend fun addRecord(record: Record) {
        withContext(ioDispatcher) {
            localDataSource.addRecord(record)
        }
    }

    suspend fun deleteRecord(record: Record) {
        withContext(ioDispatcher) {
            localDataSource.deleteRecord(record)
        }
    }

    suspend fun deleteAllRecord() {
        withContext(ioDispatcher) {
            localDataSource.deleteAllRecord()
        }
    }

    suspend fun initCheck() {
        withContext(ioDispatcher) {
            localDataSource.initCheck()
        }
    }

    suspend fun changeCheck(id: UUID, state: Boolean) {
        withContext(ioDispatcher) {
            localDataSource.changeCheck(id, state)
        }
    }

    suspend fun deleteCheckedRecord() {
        withContext(ioDispatcher) {
            localDataSource.deleteCheckedRecord()
        }
    }

    suspend fun getAllRecordsSync(): List<Record> = withContext(ioDispatcher) {
        localDataSource.getAllRecordsSync()
    }

    suspend fun insertAll(records: List<Record>) {
        withContext(ioDispatcher) {
            localDataSource.insertAll(records)
        }
    }

    suspend fun deleteRecordsByIds(ids: List<String>) {
        withContext(ioDispatcher) {
            localDataSource.deleteRecordsByIds(ids)
        }
    }
}