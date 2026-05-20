package com.beinny.android.photorecord.ui.record

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.common.*
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import java.util.*
import com.beinny.android.photorecord.databinding.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordFragment : Fragment() {
    interface Callbacks {
        fun onLongClick(longclick: Boolean, count: Int, total: Int)
        fun onSearchModeChanged(active: Boolean)
    }

    private var callbacks: Callbacks? = null
    private lateinit var callbacksBp: OnBackPressedCallback
    private var backKeyPressedTime: Long = 0

    private val viewModel: RecordViewModel by viewModels { ViewModelFactory(requireContext()) }
    private lateinit var binding: FragmentRecordBinding

    private var keyboardHeightListener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null

    private var longClick: Boolean = false
    private var countOfCheckedRecord: Int = 0
    private lateinit var recordAdapter: RecordAdapter

    val adapterCallback = AdapterCallback()

    private var isDragChecking = false
    private var isSearchMode = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?

        callbacksBp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    longClick -> disableLongClick()
                    isSearchMode && isKeyboardVisible() -> hideKeyboard()
                    isSearchMode && viewModel.searchQuery.value != null -> {
                        // 결과 상태(D) → 초기 상태(B)
                        viewModel.clearSearch()
                        binding.etSearch.setText("")
                        showSearchInitialState()
                    }
                    isSearchMode -> deactivateSearchMode()
                    System.currentTimeMillis() > backKeyPressedTime + 2000 -> {
                        backKeyPressedTime = System.currentTimeMillis()
                        Toast.makeText(context, getString(R.string.record_app_close_warning), Toast.LENGTH_SHORT).show()
                    }
                    else -> requireActivity().finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacksBp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvRecordList.layoutManager = GridLayoutManager(context, GRID_COLUMN_COUNT)

        recordAdapter = RecordAdapter(
            onItemClick = { id -> navigateToDetail(id) },
            adapterCallback = adapterCallback
        )
        binding.rvRecordList.adapter = recordAdapter

        binding.rvRecordList.addOnItemTouchListener(
            DragSelectTouchListener(
                isActive = { longClick },
                onLongPress = { idx ->
                    adapterCallback.activateLongClick(recordAdapter.getItemAt(idx))
                },
                onDragStart = { startIdx ->
                    val startId = recordAdapter.getItemAt(startIdx).id
                    val checked = viewModel.checkedIds.value ?: emptySet()
                    // 시작 아이템만 체크된 상태 = 롱클릭 직후 → 무조건 선택 방향
                    isDragChecking = if (checked == setOf(startId)) true
                                     else !checked.contains(startId)
                },
                onDragRange = { start, end ->
                    val rangeIds = (minOf(start, end)..maxOf(start, end))
                        .map { recordAdapter.getItemAt(it).id }
                    viewModel.setCheckedRange(rangeIds, isDragChecking)
                }
            )
        )

        viewModel.recordListLiveData.observe(viewLifecycleOwner, Observer { records ->
            if (isSearchMode) {
                // 검색 모드 중 데이터 변경 시 현재 쿼리로 재필터링
                val query = viewModel.searchQuery.value
                if (records != null && query != null) viewModel.executeSearch(query)
                return@Observer
            }
            records?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    val sortedRecords = withContext(Dispatchers.Default) {
                        viewModel.sortRecords(PhotoRecordApplication.prefs.getInt(SORT_BY_PREF_KEY, 3), records)
                    }
                    recordAdapter.submitList(sortedRecords)
                }
            }
        })

        // 체크 상태는 DB 갱신 없이 관리 > 깜빡임 원인 제거
        viewModel.checkedIds.observe(viewLifecycleOwner) { ids ->
            countOfCheckedRecord = ids.size
            val total = viewModel.recordListLiveData.value?.size ?: 0
            recordAdapter.updateCheckedIds(ids)
            if (longClick) {
                callbacks?.onLongClick(true, countOfCheckedRecord, total)
                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results != null && isSearchMode) showSearchResults(results)
        }

        setupSearchInput()

        // RecordDetailFragment 등에서 돌아올 때 검색 모드 UI 복원 (isSearchMode는 Fragment 인스턴스에 유지됨)
        if (isSearchMode) restoreSearchModeUi()
    }

    // 검색 모드 중 뷰 재생성(Detail 진입 후 복귀) 시 검색 바 및 상태 복원
    private fun restoreSearchModeUi() {
        binding.layoutSearchBar.visibility = View.VISIBLE
        binding.vSearchBarSpacer.visibility = View.VISIBLE
        binding.fabRecordAdd.visibility = View.GONE
        callbacks?.onSearchModeChanged(true)
        requireActivity().invalidateOptionsMenu()
        viewModel.searchQuery.value?.let { binding.etSearch.setText(it) }
        // searchQuery == null이면 초기 상태, 있으면 searchResults observer가 결과를 복원
        if (viewModel.searchQuery.value == null) showSearchInitialState()
    }

    override fun onStart() {
        super.onStart()
        binding.fabRecordAdd.setOnClickListener {
            val record = Record()
            viewModel.addRecord(record)
            navigateToDetail(record.id)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        callbacksBp.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_record_sort_or_delete, menu)

        val searchMenu = menu.findItem(R.id.search_record)
        val sortMenu = menu.findItem(R.id.sort_record)
        val deleteMenu = menu.findItem(R.id.delete_record)

        deleteMenu.icon?.mutate()?.setTint(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.font)
        )

        when {
            longClick -> {
                searchMenu.isVisible = false
                sortMenu.isVisible = false
                deleteMenu.isVisible = countOfCheckedRecord > 0
            }
            isSearchMode -> {
                searchMenu.isVisible = false
                sortMenu.isVisible = false
                deleteMenu.isVisible = false
            }
            else -> {
                searchMenu.isVisible = true
                sortMenu.isVisible = true
                deleteMenu.isVisible = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_record -> { showSortDialog(); true }
            R.id.delete_record -> { deleteCheckedRecords(); true }
            R.id.search_record -> { activateSearchMode(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 검색 입력 바 이벤트 설정 (커스텀 pill EditText)
    private fun setupSearchInput() {
        binding.ivSearchBack.setOnClickListener { deactivateSearchMode() }

        binding.ivSearchClear.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.etSearch.addTextChangedListener { text ->
            binding.ivSearchClear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeSearchFromInput()
                true
            } else false
        }
    }

    // 검색 모드 진입 — pill 검색 바 표시 및 키보드 자동 표시
    private fun activateSearchMode() {
        isSearchMode = true
        binding.layoutSearchBar.visibility = View.VISIBLE
        binding.vSearchBarSpacer.visibility = View.VISIBLE
        binding.fabRecordAdd.visibility = View.GONE
        callbacks?.onSearchModeChanged(true)
        requireActivity().invalidateOptionsMenu()
        showSearchInitialState()
        binding.root.post { showKeyboard() }
    }

    // 검색 모드 종료 — 검색 상태 초기화, UI 복원
    private fun deactivateSearchMode() {
        isSearchMode = false
        detachKeyboardHeightListener()
        viewModel.exitSearch()
        hideKeyboard()
        binding.layoutSearchBar.visibility = View.GONE
        binding.vSearchBarSpacer.visibility = View.GONE
        binding.etSearch.setText("")
        binding.layoutSearchInitial.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutSearchEmpty.visibility = View.GONE
        binding.rvRecordList.visibility = View.VISIBLE
        binding.fabRecordAdd.visibility = View.VISIBLE
        callbacks?.onSearchModeChanged(false)
        requireActivity().invalidateOptionsMenu()
        val records = viewModel.recordListLiveData.value
        records?.let {
            CoroutineScope(Dispatchers.Main).launch {
                val sorted = withContext(Dispatchers.Default) {
                    viewModel.sortRecords(PhotoRecordApplication.prefs.getInt(SORT_BY_PREF_KEY, 3), it)
                }
                recordAdapter.submitList(sorted)
            }
        }
    }

    // 검색 초기 상태 표시 — pill + chip + 힌트 (결과 없음)
    private fun showSearchInitialState() {
        // 1. 리스너 먼저 분리 — 전환 중 GlobalLayoutListener 발화 시 추가 레이아웃 패스로 깜빡임 발생 방지
        detachKeyboardHeightListener()
        // 2. chips 먼저 채움 — GONE 상태에서 populate 후 VISIBLE 처리해야 단일 레이아웃 패스
        updateRecentSearchChips()
        // 3. 기존 뷰 숨기고 초기 상태 표시 (같은 레이아웃 패스에서 처리)
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutSearchEmpty.visibility = View.GONE
        binding.rvRecordList.visibility = View.GONE
        binding.layoutSearchInitial.visibility = View.VISIBLE
        // 4. 레이아웃 안정 후 다음 프레임에 리스너 재연결
        binding.layoutSearchInitial.post { attachKeyboardHeightListener() }
    }

    // 키보드 높이만큼 bottom padding 조정 — 힌트가 항상 키보드 위에 표시되도록
    private fun attachKeyboardHeightListener() {
        if (keyboardHeightListener != null) return
        keyboardHeightListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
            val keyboardHeight = requireActivity().window.decorView.height - rect.bottom
            binding.layoutSearchInitial.setPadding(0, 0, 0, maxOf(0, keyboardHeight))
        }
        binding.layoutSearchInitial.viewTreeObserver
            .addOnGlobalLayoutListener(keyboardHeightListener)
    }

    // 키보드 높이 감지 리스너 해제
    private fun detachKeyboardHeightListener() {
        keyboardHeightListener?.let {
            if (binding.layoutSearchInitial.viewTreeObserver.isAlive) {
                binding.layoutSearchInitial.viewTreeObserver
                    .removeOnGlobalLayoutListener(it)
            }
            keyboardHeightListener = null
        }
        binding.layoutSearchInitial.setPadding(0, 0, 0, 0)
    }

    // EditText에서 검색 실행 — 최근 검색어 저장 후 ViewModel로 위임
    private fun executeSearchFromInput() {
        val query = binding.etSearch.text.toString()
        if (query.isBlank()) return
        PhotoRecordApplication.prefs.addRecentSearch(query)
        hideKeyboard()
        viewModel.executeSearch(query)
    }

    // 검색 결과를 RecyclerView 또는 empty state로 표시
    private fun showSearchResults(results: List<Record>) {
        detachKeyboardHeightListener()
        val query = viewModel.searchQuery.value ?: ""
        binding.layoutSearchInitial.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE
        binding.tvSearchResultCount.text = getString(R.string.search_result_count, results.size)
        if (results.isEmpty()) {
            binding.rvRecordList.visibility = View.GONE
            binding.layoutSearchEmpty.visibility = View.VISIBLE
            binding.tvSearchEmptySubtitle.text = getString(R.string.search_empty_subtitle)
        } else {
            binding.rvRecordList.visibility = View.VISIBLE
            binding.layoutSearchEmpty.visibility = View.GONE
            recordAdapter.submitList(results)
        }
    }

    // 최근 검색어 chip 목록 갱신 — X 버튼으로 개별 삭제 가능
    private fun updateRecentSearchChips() {
        val recent = PhotoRecordApplication.prefs.getRecentSearches()
        binding.cgRecentSearches.removeAllViews()
        if (recent.isEmpty()) {
            binding.cgRecentSearches.visibility = View.GONE
            return
        }
        binding.cgRecentSearches.visibility = View.VISIBLE
        recent.forEach { query ->
            val chip = Chip(requireContext()).apply {
                text = query
                isClickable = true
                isCheckable = false
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.search_background))
                // 동그라미 없는 단순 X 아이콘
                closeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_close_30)
                closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.font))
                setOnClickListener {
                    PhotoRecordApplication.prefs.addRecentSearch(query)
                    binding.etSearch.setText(query)
                    hideKeyboard()
                    viewModel.executeSearch(query)
                }
                setOnCloseIconClickListener {
                    PhotoRecordApplication.prefs.removeRecentSearch(query)
                    binding.cgRecentSearches.removeView(this)
                    if (binding.cgRecentSearches.childCount == 0) {
                        binding.cgRecentSearches.visibility = View.GONE
                    }
                }
            }
            binding.cgRecentSearches.addView(chip)
        }
    }

    // 키보드 표시 여부 확인
    private fun isKeyboardVisible(): Boolean {
        val insets = ViewCompat.getRootWindowInsets(binding.root) ?: return false
        return insets.isVisible(WindowInsetsCompat.Type.ime())
    }

    // 키보드 표시
    private fun showKeyboard() {
        binding.etSearch.requestFocus()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    // 키보드 숨기기
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun navigateToDetail(id: UUID) {
        val bundle = Bundle().apply { putSerializable(ARG_RECORD_ID, id) }
        findNavController().navigate(R.id.action_recordFragment_to_recordDetailFragment, bundle)
    }

    private fun showSortDialog() {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgBinding = DialogSortingBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(dlgBinding.root)

        when (PhotoRecordApplication.prefs.getInt(SORT_BY_PREF_KEY, 3)) {
            0 -> dlgBinding.rbDialogSortingSortbyNameAsc.isChecked = true
            1 -> dlgBinding.rbDialogSortingSortbyNameDesc.isChecked = true
            2 -> dlgBinding.rbDialogSortingSortbyDateAsc.isChecked = true
            3 -> dlgBinding.rbDialogSortingSortbyDateDesc.isChecked = true
        }

        var selected = 3
        dlgBinding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selected = when (checkedId) {
                R.id.rb_dialog_sorting_sortby_name_asc -> 0
                R.id.rb_dialog_sorting_sortby_name_desc -> 1
                R.id.rb_dialog_sorting_sortby_date_asc -> 2
                R.id.rb_dialog_sorting_sortby_date_desc -> 3
                else -> -1
            }
        }

        dlgBinding.tvDialogSortingComplete.setOnClickListener {
            PhotoRecordApplication.prefs.setInt(SORT_BY_PREF_KEY, selected)
            val record = Record()
            viewModel.addRecord(record)
            viewModel.deleteRecord(record)
            dlg.dismiss()
        }

        dlgBinding.tvDialogSortingCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    private fun disableLongClick() {
        longClick = false
        viewModel.clearChecked()
        // 검색 모드 중에는 FAB 유지 숨김, 일반 모드에서만 복원
        binding.fabRecordAdd.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        requireActivity().invalidateOptionsMenu()
        callbacks?.onLongClick(false, 0, 0)
        recordAdapter.setLongClickMode(false)
        if (isSearchMode) exitLongClickToSearch()
    }

    // 검색 모드 중 롱클릭 진입 — 검색 바 숨기고 상단 바 복원하여 선택 UI 표시
    private fun enterLongClickFromSearch() {
        binding.layoutSearchBar.visibility = View.GONE
        binding.vSearchBarSpacer.visibility = View.GONE
        hideKeyboard()
        callbacks?.onSearchModeChanged(false)
    }

    // 롱클릭 종료 후 검색 모드 복원 — 검색 바 재표시 및 상단 바 숨김
    private fun exitLongClickToSearch() {
        binding.layoutSearchBar.visibility = View.VISIBLE
        binding.vSearchBarSpacer.visibility = View.VISIBLE
        callbacks?.onSearchModeChanged(true)
        val results = viewModel.searchResults.value
        if (results != null) showSearchResults(results) else showSearchInitialState()
    }

    private fun deleteCheckedRecords() {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgBinding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(dlgBinding.root)

        dlgBinding.tvDialogAlertMsg.text =
            getString(R.string.record_selected_delete_warning_1) +
            countOfCheckedRecord.toString() +
            getString(R.string.record_selected_delete_warning_2)

        dlgBinding.tvDialogAlertComplete.setOnClickListener {
            // deleteCheckedRecords 먼저 호출 — disableLongClick의 clearChecked가 먼저 실행되면 ID가 비워져 삭제 불가
            viewModel.deleteCheckedRecords()
            disableLongClick()
            dlg.dismiss()
        }
        dlgBinding.tvDialogAlertCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    inner class AdapterCallback {
        fun activateLongClick(record: Record) {
            longClick = true
            viewModel.initChecked(record.id)
            requireActivity().invalidateOptionsMenu()
            // 검색 모드 중에는 FAB이 이미 GONE — 일반 모드에서만 INVISIBLE 처리
            if (!isSearchMode) binding.fabRecordAdd.visibility = View.INVISIBLE
            val total = viewModel.recordListLiveData.value?.size ?: 0
            callbacks?.onLongClick(true, 1, total)
            recordAdapter.setLongClickMode(true)
            if (isSearchMode) enterLongClickFromSearch()
        }

        fun isLongClick(): Boolean = longClick

        fun toggleCheck(id: UUID) {
            viewModel.toggleCheck(id)
        }

        fun selectAll() {
            val allIds = viewModel.recordListLiveData.value?.map { it.id } ?: return
            viewModel.selectAll(allIds)
        }

        fun clearAll() = viewModel.clearChecked()
    }

    companion object {
        fun newInstance(): RecordFragment = RecordFragment()
    }
}
