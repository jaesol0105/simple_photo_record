package com.beinny.android.photorecord.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy
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

    @Query("SELECT * FROM record")
    fun getAllRecordsSync(): List<Record>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(records: List<Record>)

    // 기존 전체 삭제 후 새 레코드 삽입 — 트랜잭션으로 묶어 중간 실패 시 자동 롤백
    @Transaction
    fun replaceAll(records: List<Record>) {
        deleteAllRecord()
        insertAll(records)
    }

    // 중복 UUID는 건너뜀 (추가 모드 — 기존 레코드 보존)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllOrIgnore(records: List<Record>)

    @Query("DELETE FROM record WHERE id IN (:ids)")
    fun deleteRecordsByIds(ids: List<String>)
}