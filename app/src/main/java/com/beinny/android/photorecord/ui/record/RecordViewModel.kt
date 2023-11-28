package com.beinny.android.photorecord.ui.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class RecordViewModel(private val recordRepository: RecordRepository): ViewModel() {
    // private val recordRepository = RecordRepository.get()
    val recordListLiveData = recordRepository.getRecords()

    /** [새로운 Record 추가] */
    fun addRecord (record: Record) {
        viewModelScope.launch {
            recordRepository.addRecord(record)
        }
    }

    /** [Record 삭제] */
    fun deleteRecord (record: Record) {
        viewModelScope.launch {
            recordRepository.deleteRecord(record)
        }
    }

    /** [Record 체크 상태 초기화] */
    fun initCheck(id:UUID, state:Boolean) {
        viewModelScope.launch {
            recordRepository.initCheck()
            recordRepository.changeCheck(id, state) // 처음 롱 클릭한 레코드 체크
        }
    }

    /** [Record 체크 상태 변경] */
    fun changeCheck (id:UUID, state:Boolean) {
        viewModelScope.launch {
            recordRepository.changeCheck(id, state)
        }
    }

    /** [체크된 Record 일괄 삭제] */
    fun deleteCheckedRecord() {
        viewModelScope.launch {
            recordRepository.deleteCheckedRecord()
        }
    }

    /*
    // 사진 파일이 가르킬 위치(File객체)를 RecordDetailFragment에 제공.
    fun getPhotoFile(record: Record): File {
        return recordRepository.getPhotoFile(record)
    }

    fun getThumbFile(record: Record): File {
        return recordRepository.getThumbFile(record)
    }
    */
}