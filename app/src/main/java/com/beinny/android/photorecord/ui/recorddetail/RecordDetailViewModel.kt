package com.beinny.android.photorecord.ui.recorddetail

import androidx.lifecycle.*
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class RecordDetailViewModel(private val recordRepository: RecordRepository) : ViewModel() {
    private val recordIdLiveData = MutableLiveData<UUID>()

    lateinit var photoFile: File
    lateinit var thumbFile : File
    lateinit var tempFile : File

    lateinit var initialLabel : String
    lateinit var initialMemo : String

    /** [실시간으로 수정되는 로컬 Record 객체] */
    lateinit var record : Record

    /** [데이터 변경 여부 확인 용도 - 날짜, 사진] */
    var isDateEdit = false
    var isPhotoEdit = false

    /** [DB에 저장된 Record 객체를 id를 통해 불러온다] */
    var recordLiveData: LiveData<Record?> =
        Transformations.switchMap(recordIdLiveData) { recordId -> recordRepository.getRecord(recordId) }

    /** [id를 recordIdLiveData에 할당] */
    fun loadRecordById(recordId:UUID) {
        recordIdLiveData.value = recordId
    }

    /** [초기 label, memo 값 초기화 (데이터 변경 여부 확인 용도 - 제목, 메모)] */
    fun setInitialValues() {
        initialLabel = record.label
        initialMemo = record.memo
    }

    /** [사진 파일이 가르킬 위치(File)를 초기화] */
    fun setPhotoFiles() {
        photoFile = File(PhotoRecordApplication.applicationContext().filesDir,record.photoFileName)
        thumbFile = File(PhotoRecordApplication.applicationContext().filesDir,record.thumbFileName)
        tempFile = File(PhotoRecordApplication.applicationContext().filesDir,record.tempFileName)
    }

    /** [DB record UPDATE] */
    fun saveRecord(record: Record){
        viewModelScope.launch {
            recordRepository.updateRecord(record)
        }
    }

    /** [DB record DELETE] */
    fun deleteRecord(record: Record){
        viewModelScope.launch {
            recordRepository.deleteRecord(record)
        }
    }
}

/*
    private val recordRepository = RecordRepository.get()
*/