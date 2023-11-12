package com.beinny.android.photorecord.datebase

import androidx.lifecycle.LiveData
import androidx.room.*
import com.beinny.android.photorecord.model.Record
import java.util.*

@Dao
interface RecordDao {
    @Query("SELECT * FROM record")
    fun getRecords(): LiveData<List<Record>>

    @Query("SELECT * FROM record WHERE id=(:id)")
    fun getRecord(id: UUID): LiveData<Record?>

    @Update
    fun updateRecord(record: Record)

    @Insert
    fun addRecord(record: Record)

    @Delete
    fun deleteRecord(record: Record)

    @Query("DELETE FROM record")
    fun deleteAllRecord()

    @Query("UPDATE record SET isChecked=:state")
    fun initCheck(state:Boolean=false)

    @Query("UPDATE record SET isChecked=:state WHERE id=(:id)")
    fun changeCheck(id: UUID, state: Boolean)

    @Query("DELETE FROM record WHERE isChecked=:state")
    fun deleteCheckedRecord(state:Boolean=true)

    @Delete
    fun deleteSelectedRecord(recordList : List<Record>)
}