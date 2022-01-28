package com.beinny.android.dailylook

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.beinny.android.dailylook.database.DailyDatabase
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "dailylook-database"

class DailyRepository private constructor(context: Context){
    private val database : DailyDatabase = Room.databaseBuilder(
        context.applicationContext,
        DailyDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val dailyDao = database.dailyDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun getDailys() : LiveData<List<Daily>> = dailyDao.getDailys()
    fun getDaily(id: UUID) : LiveData<Daily?> = dailyDao.getDaily(id)

    fun updateDaily(dailylook:Daily) {
        executor.execute {
            dailyDao.updateDailyLook(dailylook)
        }
    }

    fun addDailyLook(dailylook:Daily) {
        executor.execute {
            dailyDao.addDailyLook(dailylook)
        }
    }

    // photoFileName를 인자로 받고, 파일의 위치를 가리킬 File 객체를 반환
    fun getPhotoFile(dailylook:Daily): File = File(filesDir, dailylook.photoFileName)

    companion object {
        private var INSTANCE: DailyRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = DailyRepository(context)
            }
        }
        fun get(): DailyRepository {
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized!")
        }
    }
}