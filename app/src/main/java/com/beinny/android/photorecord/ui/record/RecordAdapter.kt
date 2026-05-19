package com.beinny.android.photorecord.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beinny.android.photorecord.databinding.ItemRecordBinding
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.ui.common.applyCheckSign
import java.util.UUID

private const val PAYLOAD_CHECK = "check"
private const val PAYLOAD_LONGCLICK = "longclick"

class RecordAdapter(
    private val onItemClick: (UUID) -> Unit,
    private val adapterCallback: RecordFragment.AdapterCallback
) : ListAdapter<Record, RecordAdapter.RecordHolder>(RecordDiffCallback()) {

    private var checkedIds: Set<UUID> = emptySet()
    private var isLongClickMode: Boolean = false

    fun getItemAt(position: Int): Record = getItem(position)

    /** 체크 상태 변경 — 실제로 바뀐 아이템만 PAYLOAD_CHECK로 부분 업데이트 (Glide 없음) */
    fun updateCheckedIds(newIds: Set<UUID>) {
        val oldIds = checkedIds
        checkedIds = newIds
        for (i in 0 until itemCount) {
            val id = getItem(i).id
            if (oldIds.contains(id) != newIds.contains(id)) {
                notifyItemChanged(i, PAYLOAD_CHECK)
            }
        }
    }

    /** 롱클릭 모드 진입/해제 — 전체 아이템 체크박스 visibility만 업데이트 (Glide 없음) */
    fun setLongClickMode(active: Boolean) {
        isLongClickMode = active
        notifyItemRangeChanged(0, itemCount, PAYLOAD_LONGCLICK)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: RecordHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(getItem(position))
            return
        }
        val record = getItem(position)
        payloads.forEach { payload ->
            when (payload) {
                PAYLOAD_CHECK -> holder.updateCheckState(record)
                PAYLOAD_LONGCLICK -> holder.updateLongClickVisibility()
            }
        }
    }

    /** 포지션을 고유 타입으로 사용 — 중복 이미지 렌더링 오류 방지 */
    override fun getItemViewType(position: Int): Int = position

    inner class RecordHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            itemView.isLongClickable = false  // 시스템 롱클릭 비활성화 — DragSelectTouchListener에서 처리
            itemView.setOnClickListener(this)
        }

        fun bind(record: Record) {
            binding.record = record
            binding.checked = checkedIds.contains(record.id)
            val visibility = if (isLongClickMode) View.VISIBLE else View.INVISIBLE
            binding.ivItemRecordCheckbox.visibility = visibility
            binding.viewItemRecordCheckboxFrame.visibility = visibility
            binding.executePendingBindings()
        }

        /** 체크 상태만 업데이트 — Glide 호출 없음 */
        fun updateCheckState(record: Record) {
            val isChecked = checkedIds.contains(record.id)
            applyCheckSign(binding.ivItemRecordCheckbox, !isChecked, isChecked)
            binding.viewItemRecordDim.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        /** 체크박스 visibility만 업데이트 — Glide 호출 없음 */
        fun updateLongClickVisibility() {
            val visibility = if (isLongClickMode) View.VISIBLE else View.INVISIBLE
            binding.ivItemRecordCheckbox.visibility = visibility
            binding.viewItemRecordCheckboxFrame.visibility = visibility
            if (!isLongClickMode) binding.viewItemRecordDim.visibility = View.GONE
        }

        override fun onClick(v: View) {
            if (adapterCallback.isLongClick()) {
                adapterCallback.toggleCheck(binding.record!!.id)
            } else {
                onItemClick(binding.record!!.id)
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean =
        oldItem.id    == newItem.id    &&
        oldItem.label == newItem.label &&
        oldItem.date  == newItem.date  &&
        oldItem.memo  == newItem.memo  &&
        oldItem.isNew == newItem.isNew
}
