package com.beinny.android.photorecord.ui.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.common.SORT_BY_PREF_KEY
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.common.OrderKoreanFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class RecordViewModel(private val recordRepository: RecordRepository) : ViewModel() {
    val recordListLiveData = recordRepository.getRecords()

    private val _checkedIds = MutableLiveData<Set<UUID>>(emptySet())
    val checkedIds: LiveData<Set<UUID>> = _checkedIds

    private val _searchQuery = MutableLiveData<String?>(null)
    val searchQuery: LiveData<String?> = _searchQuery

    private val _searchResults = MutableLiveData<List<Record>?>(null)
    val searchResults: LiveData<List<Record>?> = _searchResults

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

    // 검색 실행 — label/memo 필드를 대소문자 무시하여 필터링하고 기존 정렬 적용
    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val records = recordListLiveData.value ?: emptyList()
            val results = withContext(Dispatchers.Default) {
                val filtered = records.filter {
                    it.label.contains(trimmed, ignoreCase = true) ||
                    it.memo.contains(trimmed, ignoreCase = true)
                }
                sortRecords(PhotoRecordApplication.prefs.getInt(SORT_BY_PREF_KEY, 3), filtered)
            }
            _searchQuery.value = trimmed
            _searchResults.value = results
        }
    }

    // 검색 쿼리 및 결과 초기화 (검색 모드는 유지, SearchView 재전개 시 사용)
    fun clearSearch() {
        _searchQuery.value = null
        _searchResults.value = null
    }

    // 검색 모드 비활성화 시 상태 전체 초기화
    fun exitSearch() {
        _searchQuery.value = null
        _searchResults.value = null
    }

    // 정렬 기준에 따라 레코드 목록 정렬
    fun sortRecords(sortBy: Int, records: List<Record>): List<Record> {
        val korEngNumSpec = Comparator<Record> { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) }
        return when (sortBy) {
            0 -> records.sortedWith(korEngNumSpec)
            1 -> records.sortedWith(korEngNumSpec).reversed()
            2 -> records.sortedWith(compareBy<Record> { it.date }.thenComparator { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) })
            3 -> records.sortedWith(compareByDescending<Record> { it.date }.thenComparator { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) })
            else -> records
        }
    }
}
