package com.beinny.android.dailylook.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beinny.android.dailylook.Daily
import java.util.*

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily")
    fun getDailys(): LiveData<List<Daily>>

    @Query("SELECT * FROM daily WHERE id=(:id)")
    fun getDaily(id: UUID): LiveData<Daily?>

    @Update
    fun updateDailyLook(dailylook:Daily)

    @Insert
    fun addDailyLook(dailylook:Daily)
}