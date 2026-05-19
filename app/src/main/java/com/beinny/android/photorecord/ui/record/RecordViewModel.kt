package com.beinny.android.photorecord.ui.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import kotlinx.coroutines.launch
import java.util.*

class RecordViewModel(private val recordRepository: RecordRepository) : ViewModel() {
    val recordListLiveData = recordRepository.getRecords()

    private val _checkedIds = MutableLiveData<Set<UUID>>(emptySet())
    val checkedIds: LiveData<Set<UUID>> = _checkedIds

    fun addRecord(record: Record) {
        viewModelScope.launch { recordRepository.addRecord(record) }
    }

    fun deleteRecord(record: Record) {
        viewModelScope.launch { recordRepository.deleteRecord(record) }
    }

    fun initChecked(id: UUID) {
        _checkedIds.value = setOf(id)
    }

    fun toggleCheck(id: UUID) {
        val current = _checkedIds.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _checkedIds.value = current
    }

    fun clearChecked() {
        _checkedIds.value = emptySet()
    }

    fun selectAll(ids: List<UUID>) {
        _checkedIds.value = ids.toSet()
    }

    fun setCheckedRange(ids: List<UUID>, check: Boolean) {
        val current = _checkedIds.value?.toMutableSet() ?: mutableSetOf()
        if (check) current.addAll(ids) else current.removeAll(ids.toSet())
        _checkedIds.value = current
    }

    fun deleteCheckedRecords() {
        val ids = _checkedIds.value?.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            recordRepository.deleteRecordsByIds(ids.map { it.toString() })
            _checkedIds.value = emptySet()
        }
    }
}
