package com.beinny.android.dailylook.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.beinny.android.dailylook.Daily

@Database(entities = [ Daily::class ], version=1)
@TypeConverters(DailyTypeConverters::class)
abstract class DailyDatabase : RoomDatabase() {
    abstract fun dailyDao(): DailyDao
}