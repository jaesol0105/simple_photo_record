package com.beinny.android.photorecord.ui.record

import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.ItemRecordBinding
import com.beinny.android.photorecord.model.Record

class RecordAdapter(
    private val a_callbacks: RecordFragment.Callbacks?,
    private var f_callback: RecordFragment.adapterCallback
) : ListAdapter<Record, RecordAdapter.RecordHolder>(RecordDiffCallback()) {
    private lateinit var binding: ItemRecordBinding

    /** [뷰 객체를 담고있는 뷰 홀더들을 생성 (보통 10~15회 수행)]*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
        binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordHolder(binding)
    }

    /** [생성된 뷰홀더에 데이터를 바인딩] */
    override fun onBindViewHolder(holder: RecordHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class RecordHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(record: Record) {
            binding.record = record
            if (f_callback.isLongClick()) {
                binding.ivDeleteCheckbox.visibility = View.VISIBLE
            } else {
                binding.ivDeleteCheckbox.visibility = View.INVISIBLE
            }
            binding.executePendingBindings() // 바인딩된 데이터가 바로 뷰에 반영됨
        }

        override fun onClick(v: View) {
            if (f_callback.isLongClick()) {
                if (binding.record!!.isChecked) {
                    f_callback.changeCheck(binding.record!!.id,false)
                } else {
                    f_callback.changeCheck(binding.record!!.id,true)
                }
            } else {
                Log.d("adapteronclick","1")
                a_callbacks?.onSelected(binding.record!!.id)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (!f_callback.isLongClick()) {
                f_callback.activateLongClick(binding.record!!)
            }
            return true // false를 반환하면 손을 떼는 순간 onClick 리스너가 동작함
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
        return oldItem.equals(newItem)
    }
}
