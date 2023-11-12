package com.beinny.android.photorecord.repository.recorddetail

import androidx.lifecycle.LiveData
import com.beinny.android.photorecord.datebase.RecordDao
import com.beinny.android.photorecord.model.Record
import kotlinx.coroutines.withContext
import java.util.*

class RecordLocalDataSource (private val dao: RecordDao) : RecordDataSource {
    override fun getRecords(): LiveData<List<Record>> = dao.getRecords()

    override fun getRecord(id: UUID): LiveData<Record?> = dao.getRecord(id)

    override suspend fun updateRecord(record: Record) {
        dao.updateRecord(record)
    }

    override suspend fun addRecord(record: Record) {
        dao.addRecord(record)
    }

    override suspend fun deleteRecord(record: Record) {
        dao.deleteRecord(record)
    }

    override suspend fun deleteAllRecord() {
        dao.deleteAllRecord()
    }

    override suspend fun initCheck() {
        dao.initCheck()
    }

    override suspend fun changeCheck(id: UUID, state: Boolean) {
        dao.changeCheck(id, state)
    }

    override suspend fun deleteCheckedRecord() {
        dao.deleteCheckedRecord()
    }
}