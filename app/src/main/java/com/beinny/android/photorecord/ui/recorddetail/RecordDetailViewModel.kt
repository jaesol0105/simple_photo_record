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

    /** [실시간으로 수정되는 로컬 record 객체] */
    var record = Record()

    /** [DB에 저장된 record 객체를 id를 통해 불러온다] */
    var recordLiveData: LiveData<Record?> =
        Transformations.switchMap(recordIdLiveData) { recordId -> recordRepository.getRecord(recordId) }

    /** [id를 recordIdLiveData에 할당] */
    fun loadRecordById(recordId:UUID) {
        recordIdLiveData.value = recordId
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