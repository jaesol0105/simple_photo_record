package com.beinny.android.photorecord.ui.record

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.databinding.FragmentRecordBinding
import com.beinny.android.photorecord.databinding.ItemRecordBinding
import com.beinny.android.photorecord.model.Record
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

class RecordFragment : Fragment() {
    /*
    private lateinit var recordRecyclerView: RecyclerView
    private var adapter : RecordAdapter? = RecordAdapter(emptyList())

    private val recordListViewModel: RecordViewModel by lazy {
        ViewModelProvider(this).get(RecordViewModel::class.java)
    }
    */

    /** [Host Activity 에서 구현할 callback 함수의 인터페이스] */
    interface Callbacks {
        fun onSelected(dailyid:UUID)
    }

    /** [액티비티의 콜백 저장] */
    private var callbacks: Callbacks? =null

    /** [back press 처리 콜백] */
    private lateinit var callbacks_bp: OnBackPressedCallback
    private var backKeyPressedTime : Long = 0

    private val viewModel: RecordViewModel by viewModels { ViewModelFactory() }
    private lateinit var binding: FragmentRecordBinding

    override fun onAttach(context: Context) { // fragment가 add 될때 호출
        super.onAttach(context)
        /** [액티비티의 콜백 저장] */
        callbacks = context as Callbacks?

        /** [back press 처리 콜백] */
        callbacks_bp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(System.currentTimeMillis() > backKeyPressedTime + 2000) {
                    backKeyPressedTime = System.currentTimeMillis()
                    Toast.makeText(context,"'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
                }
                else {
                    requireActivity().finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacks_bp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** [RecordListFragment가 메뉴 콜백(onCreateOptionsMenu) 호출을 받아야함을 Fragment Manager 에게 알려준다] */
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*
        val view = inflater.inflate(R.layout.fragment_record, container, false)

        recordRecyclerView = view.findViewById(R.id.rv_record_list) as RecyclerView
        recordRecyclerView.layoutManager = GridLayoutManager(context,2)

        recordRecyclerView.adapter = adapter

        return view
        */
        binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvRecordList.layoutManager = GridLayoutManager(context,2)

        viewModel.recordListLiveData.observe(
            viewLifecycleOwner,
            Observer { records ->
                records?.let {
                    updateUI(records)
                }
            })
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        callbacks_bp.remove()
    }

    /** [메뉴 인플레이트] */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_record_list, menu)
    }

    /** [메뉴 선택] */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_record -> {
                // 새로운 Record 객체 생성
                val record = Record()
                // DB에 추가.
                viewModel.addRecord(record)
                // 액티비티에 구현된 onSelected 콜백함수를 호출. 새로 추가된 레코드의 상세화면이 화면에 보이도록.
                callbacks?.onSelected(record.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(records: List<Record>) {
        /*
        adapter = RecordAdapter(records)
        recordRecyclerView.adapter = adapter

        // 데이터 추가/변경시 ListAdapter에게 submitList()를 통해 알려준다.
        adapter?.submitList(records)
        */

        val recordAdapter = RecordAdapter(callbacks)
        binding.rvRecordList.adapter = recordAdapter
        recordAdapter.submitList(records)
    }

    /*
    private inner class RecordHolder(view:View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var record: Record

        val labelTextView: TextView = itemView.findViewById(R.id.tv_label_thumbnail)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date_thumbnail)
        val photoView: ImageView = itemView.findViewById(R.id.tv_photo_thumbnail)

        init{
            itemView.setOnClickListener(this)
        }

        fun bind(record:Record){
            this.record = record
            labelTextView.text = this.record.label
            val df = SimpleDateFormat("yyyy/M/d", Locale.KOREA)
            dateTextView.text = df.format(this.record.date)

            var thumbFile = viewModel.getThumbFile(record) // 썸네일 이미지 파일의 위치를 가르키는 속성
            var photoFile = viewModel.getPhotoFile(record) // 원본 이미지 파일의 위치를 가르키는 속성

            /** [photoView (thumbnail) 업데이트] */
            // 1. 썸네일 파일이 있을 경우
            if (thumbFile.exists()){
                val bitmap = getScaledBitmap(thumbFile.path, requireActivity(), GET_BIMAP_ORIGIN)
                photoView.setImageBitmap(bitmap)
            }
            else {
                // 2. 썸네일 없음 + 원본 이미지 파일은 존재할 경우 썸네일 생성
                if (photoFile.exists()){
                    val thumb_file = File(thumbFile.path)
                    try {
                        val bitmap_thumb = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_RESIZE)
                        val out = FileOutputStream(thumb_file)
                        bitmap_thumb.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.close()
                        val bitmap = getScaledBitmap(thumbFile.path, requireActivity(), GET_BIMAP_ORIGIN)
                        photoView.setImageBitmap(bitmap)
                    } catch(e:Exception) {
                        Toast.makeText(context,"error : 내부 저장소에 이미지를 저장 할 수 없음",Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
                // 3. 이미지를 등록하지 않은 레코드일 경우
                else{
                    photoView.setImageDrawable(null)
                }
            }
        }

        override fun onClick(v:View){
            callbacks?.onSelected(record.id)
        }
    }

    private inner class RecordAdapter(var records: List<Record>) : ListAdapter<Record, RecordHolder>(RecordDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder {
            val view = layoutInflater.inflate(R.layout.item_record,parent,false)
            return RecordHolder(view)
        }

        override fun onBindViewHolder(holder: RecordHolder, position: Int) {
            val record = records[position]
            holder.bind(record)
        }
    }

    private class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
            // return oldItem == newItem
            return oldItem.equals(newItem)
        }
    }
    */

    companion object {
        fun newInstance(): RecordFragment {
            return RecordFragment()
        }
    }
}