package com.beinny.android.photorecord.ui.recorddetail

import androidx.lifecycle.*
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.common.SingleLiveEvent
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class RecordDetailViewModel(private val recordRepository: RecordRepository) : ViewModel() {
    private val recordIdLiveData = MutableLiveData<UUID>()

    lateinit var photoFile: File
    lateinit var thumbFile: File
    lateinit var tempFile: File

    lateinit var initialLabel: String
    lateinit var initialMemo: String

    lateinit var record: Record  // 편집 중인 로컬 Record 객체

    var isDateEdit = false  // 변경 감지용
    var isPhotoEdit = false

    val toastMessage = SingleLiveEvent<String>()

    /** id 변경 시 자동으로 DB를 조회하는 LiveData */
    var recordLiveData: LiveData<Record?> =
        recordIdLiveData.switchMap { recordId -> recordRepository.getRecord(recordId) }

    fun loadRecordById(recordId: UUID) {
        recordIdLiveData.value = recordId
    }

    /** 변경 감지용 초기값 저장 (label, memo) */
    fun setInitialValues() {
        initialLabel = record.label
        initialMemo = record.memo
    }

    fun setPhotoFiles() {
        photoFile = File(PhotoRecordApplication.applicationContext().filesDir, record.photoFileName)
        thumbFile = File(PhotoRecordApplication.applicationContext().filesDir, record.thumbFileName)
        tempFile = File(PhotoRecordApplication.applicationContext().filesDir, record.tempFileName)
    }

    fun saveRecord(record: Record) {
        viewModelScope.launch {
            recordRepository.updateRecord(record)
        }
    }

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            recordRepository.deleteRecord(record)
        }
    }
}
