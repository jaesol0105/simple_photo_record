package com.beinny.android.photorecord

import androidx.lifecycle.ViewModel
import java.io.File

class RecordListViewModel: ViewModel() {
    private val recordRepository = RecordRepository.get()
    val recordListLiveData = recordRepository.getRecords()

    /** [새로운 레코드 추가] */
    fun addRecord (record:Record) {
        recordRepository.addRecord(record)
    }

    // 사진 파일이 가르킬 위치(File객체)를 RecordDetailFragment에 제공.
    fun getPhotoFile(record:Record): File {
        return recordRepository.getPhotoFile(record)
    }

    fun getThumbFile(record:Record): File {
        return recordRepository.getThumbFile(record)
    }
}