package com.beinny.android.photorecord

import android.content.Context
import androidx.room.Room
import com.beinny.android.photorecord.common.DATABASE_NAME
import com.beinny.android.photorecord.datebase.RecordDatabase
import com.beinny.android.photorecord.repository.recorddetail.RecordLocalDataSource
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository

object ServiceLocator {
    private var database : RecordDatabase? = null
    private var recordRepository : RecordRepository? = null

    private fun provideDatabase(applicationContext: Context) :RecordDatabase {
        return database ?: kotlin.run {
            Room.databaseBuilder(
                applicationContext,
                RecordDatabase::class.java,
                DATABASE_NAME
            ).build().also {
                database = it
            }
        }
    }

    fun provideRecordRepository(context: Context) : RecordRepository {
        return recordRepository ?: kotlin.run {
            val dao = provideDatabase(context.applicationContext).recordDao()
            RecordRepository(RecordLocalDataSource(dao)).also {
                recordRepository = it
            }
        }
    }
}