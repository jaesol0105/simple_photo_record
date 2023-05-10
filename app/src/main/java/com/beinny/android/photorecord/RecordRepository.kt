package com.beinny.android.photorecord

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.beinny.android.photorecord.datebase.RecordDatabase
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "photorecord-database"

class RecordRepository private constructor(context: Context){
    private val database : RecordDatabase = Room.databaseBuilder(
        context.applicationContext,
        RecordDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val recordDao = database.recordDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun getRecords() : LiveData<List<Record>> = recordDao.getRecords()
    fun getRecord(id: UUID) : LiveData<Record?> = recordDao.getRecord(id)

    fun updateRecord(record:Record) {
        executor.execute {
            recordDao.updateRecord(record)
        }
    }

    fun addRecord(record:Record) {
        executor.execute {
            recordDao.addRecord(record)
        }
    }

    fun deleteRecord(record:Record) {
        executor.execute {
            recordDao.deleteRecord(record)
        }
    }

    // photoFileName를 인자로 받고, 파일의 위치를 가리킬 File 객체를 반환
    fun getPhotoFile(record:Record): File = File(filesDir, record.photoFileName)

    // thumbFileName를 인자로 받고, 파일의 위치를 가리킬 File 객체를 반환
    fun getThumbFile(record:Record): File = File(filesDir, record.thumbFileName)

    // 이미지 업로드 임시저장용
    fun getTempFile(record:Record): File = File(filesDir, record.tempFileName)

    companion object {
        private var INSTANCE: RecordRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = RecordRepository(context)
            }
        }
        fun get(): RecordRepository {
            return INSTANCE ?:
            throw IllegalStateException("RecordRepository must be initialized!")
        }
    }
}