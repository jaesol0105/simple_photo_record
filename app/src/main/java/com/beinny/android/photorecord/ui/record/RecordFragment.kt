package com.beinny.android.photorecord.ui.record

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import java.util.*
import com.beinny.android.photorecord.databinding.*
import com.beinny.android.photorecord.ui.common.OrderKoreanFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordFragment : Fragment() {
    interface Callbacks {
        fun onLongClick(longclick: Boolean, count: Int, total: Int)
    }

    private var callbacks: Callbacks? = null
    private lateinit var callbacksBp: OnBackPressedCallback
    private var backKeyPressedTime: Long = 0

    private val viewModel: RecordViewModel by viewModels { ViewModelFactory(requireContext()) }
    private lateinit var binding: FragmentRecordBinding

    private var longClick: Boolean = false
    private var countOfCheckedRecord: Int = 0
    private lateinit var recordAdapter: RecordAdapter

    val adapterCallback = AdapterCallback()

    private var isDragChecking = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?

        callbacksBp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (longClick) {
                    disableLongClick()
                } else if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                    backKeyPressedTime = System.currentTimeMillis()
                    Toast.makeText(context, getString(R.string.record_app_close_warning), Toast.LENGTH_SHORT).show()
                } else {
                    requireActivity().finish()
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
            records?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    val sortedRecords = withContext(Dispatchers.Default) {
                        sortRecords(PhotoRecordApplication.prefs.getInt(SORT_BY_PREF_KEY, 3), records)
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

        val sortMenu = menu.findItem(R.id.sort_record)
        val deleteMenu = menu.findItem(R.id.delete_record)

        deleteMenu.icon?.mutate()?.setTint(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.font)
        )

        if (longClick) {
            sortMenu.isVisible = false
            deleteMenu.isVisible = countOfCheckedRecord > 0
        } else {
            sortMenu.isVisible = true
            deleteMenu.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_record -> { showSortDialog(); true }
            R.id.delete_record -> { deleteCheckedRecords(); true }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun sortRecords(sortBy: Int, records: List<Record>): List<Record> {
        val korEngNumSpec = Comparator<Record> { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) }
        return when (sortBy) {
            0 -> records.sortedWith(korEngNumSpec)
            1 -> records.sortedWith(korEngNumSpec).reversed()
            2 -> records.sortedWith(compareBy<Record> { it.date }.thenComparator { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) })
            3 -> records.sortedWith(compareByDescending<Record> { it.date }.thenComparator { d1, d2 -> OrderKoreanFirst.compare(d1.label, d2.label) })
            else -> records
        }
    }

    private fun disableLongClick() {
        longClick = false
        viewModel.clearChecked()
        binding.fabRecordAdd.visibility = View.VISIBLE
        requireActivity().invalidateOptionsMenu()
        callbacks?.onLongClick(false, 0, 0)
        recordAdapter.setLongClickMode(false)
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
            disableLongClick()
            viewModel.deleteCheckedRecords()
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
            binding.fabRecordAdd.visibility = View.INVISIBLE
            val total = viewModel.recordListLiveData.value?.size ?: 0
            callbacks?.onLongClick(true, 1, total)
            recordAdapter.setLongClickMode(true)
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
