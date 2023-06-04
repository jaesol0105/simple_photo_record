package com.beinny.android.photorecord.ui.record

import androidx.lifecycle.ViewModel
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import java.io.File
import java.util.*

class RecordViewModel: ViewModel() {
    private val recordRepository = RecordRepository.get()
    val recordListLiveData = recordRepository.getRecords()

    /** [새로운 레코드 추가] */
    fun addRecord (record: Record) {
        recordRepository.addRecord(record)
    }

    /** [레코드 삭제] */
    fun deleteRecord (record: Record) {
        recordRepository.deleteRecord(record)
    }

    fun initCheck() {
        recordRepository.initCheck()
    }

    fun changeCheck (id:UUID, state:Boolean) {
        recordRepository.changeCheck(id,state)
    }

    fun deleteCheckedRecord() {
        recordRepository.deleteCheckedRecord()
    }

    // 사진 파일이 가르킬 위치(File객체)를 RecordDetailFragment에 제공.
    fun getPhotoFile(record: Record): File {
        return recordRepository.getPhotoFile(record)
    }

    fun getThumbFile(record: Record): File {
        return recordRepository.getThumbFile(record)
    }
}