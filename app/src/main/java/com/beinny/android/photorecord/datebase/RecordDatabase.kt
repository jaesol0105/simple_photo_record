package com.beinny.android.photorecord.datebase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.beinny.android.photorecord.model.Record

@Database(entities = [ Record::class ], version=1)
@TypeConverters(RecordTypeConverters::class)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}