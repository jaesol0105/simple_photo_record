package com.beinny.android.photorecord.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.databinding.ItemRecordBinding
import com.beinny.android.photorecord.model.Record

class RecordAdapter(private val callbacks: RecordFragment.Callbacks?) : ListAdapter<Record, RecordAdapter.RecordHolder>(RecordDiffCallback()) {
    private lateinit var binding: ItemRecordBinding

    /** [뷰 객체를 담고있는 뷰 홀더들을 생성 (보통 10~15회 수행)]*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
        binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return RecordHolder(binding)
    }

    /** [생성된 뷰홀더에 데이터를 바인딩] */
    override fun onBindViewHolder(holder: RecordHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordHolder(private val binding: ItemRecordBinding): RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init{
            itemView.setOnClickListener(this)
        }

        fun bind(record: Record){
            binding.record = record
            binding.executePendingBindings() // 바인딩된 데이터가 바로 뷰에 반영됨
        }

        override fun onClick(v: View){
            callbacks?.onSelected(binding.record!!.id)
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
