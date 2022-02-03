package com.beinny.android.dailylook

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

class DailyListFragment : Fragment() {

    // 호스팅 액티비티에서 구현할 콜백함수 인터페이스
    interface Callbacks {
        fun onSelected(dailyid:UUID)
    }

    private var callbacks: Callbacks? =null

    private lateinit var dailyRecyclerView: RecyclerView
    private var adapter : DailyAdapter? = DailyAdapter(emptyList())

    private val dailyListViewModel: DailyListViewModel by lazy {
        ViewModelProvider(this).get(DailyListViewModel::class.java)
    }

    // back press 콜백
    private lateinit var callbacks_bp: OnBackPressedCallback
    private var backKeyPressedTime : Long =0

    // 프레그먼트가 add 될때 호출
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // [액티비티의 콜백 저장]
        callbacks = context as Callbacks?

        // [back press 처리 콜백]
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

    // DailyListFragment가 메뉴 콜백(onCreateOptionsMenu) 호출을 받아야함을 Fragment Manager에게 알려준다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //binding = FragmentDailyListBinding.inflate(inflater, container, false)
        val view = inflater.inflate(R.layout.fragment_daily_list, container, false)

        //dailyRecyclerView = binding.dailyRecyclerView
        dailyRecyclerView = view.findViewById(R.id.daily_recycler_view) as RecyclerView
        dailyRecyclerView.layoutManager = GridLayoutManager(context,2)

        dailyRecyclerView.adapter = adapter

        return view
        //return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dailyListViewModel.dailyListLiveData.observe(
            viewLifecycleOwner,
            Observer { dailylooks ->
                dailylooks?.let {
                    updateUI(dailylooks)
                }
            })
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        callbacks_bp.remove()
    }

    // [메뉴 인플레이트]
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_daily_list, menu)
    }

    // [메뉴 선택]
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_look -> {
                // 새로운 DailyLook 객체 생성
                val dailylook = Daily()
                // DB에 추가.
                dailyListViewModel.addDaily(dailylook)
                // 액티비티에 구현된 onSelected 콜백함수를 호출. 새로 추가된 데일리룩의 상세화면이 화면에 보이도록.
                callbacks?.onSelected(dailylook.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    private fun updateUI(dailylooks: List<Daily>) {
        adapter = DailyAdapter(dailylooks)
        dailyRecyclerView.adapter = adapter

        // 데이터 추가/변경시 ListAdapter에게 submitList()를 통해 알려준다. 참고: https://june0122.github.io/2021/05/26/android-bnr-12/
        adapter?.submitList(dailylooks)
    }

    private inner class DailyHolder(view:View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var dailylook: Daily

        val labelTextView: TextView = itemView.findViewById(R.id.itemview_label)
        val dateTextView: TextView = itemView.findViewById(R.id.itemview_date)
        val photoView: ImageView = itemView.findViewById(R.id.itemview_photoview)

        init{
            itemView.setOnClickListener(this)
        }

        fun bind(dailylook:Daily){
            this.dailylook = dailylook
            labelTextView.text = this.dailylook.label
            val df = SimpleDateFormat("yyyy/M/d", Locale.KOREA)
            dateTextView.text = df.format(this.dailylook.date)

            var thumbFile = dailyListViewModel.getThumbFile(dailylook) // 썸네일 이미지 파일의 위치를 가르키는 속성
            var photoFile = dailyListViewModel.getPhotoFile(dailylook) // 원본 이미지 파일의 위치를 가르키는 속성

            // [photoView (thumbnail) 업데이트]
            // 1. 썸네일 파일이 있을 경우
            if (thumbFile.exists()){
                val bitmap = getScaledBitmap(thumbFile.path, requireActivity(), GET_BIMAP_ORIGIN)
                photoView.setImageBitmap(bitmap)
            }
            else {
                // 2. 썸네일 없음 + 원본이미지 파일은 존재할 경우 썸네일 생성
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
                // 3. 이미지를 등록하지 않은 DailyLook일 경우
                else{
                    photoView.setImageDrawable(null)
                }
            }
        }

        override fun onClick(v:View){
            callbacks?.onSelected(dailylook.id)
        }
    }

    private inner class DailyAdapter(var dailylooks: List<Daily>) : ListAdapter<Daily, DailyHolder>(DailyLookDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyHolder {
            val view = layoutInflater.inflate(R.layout.list_item_daily,parent,false)
            return DailyHolder(view)
        }

        override fun onBindViewHolder(holder: DailyHolder, position: Int) {
            val dailylook = dailylooks[position]
            holder.bind(dailylook)
        }
    }

    private class DailyLookDiffCallback : DiffUtil.ItemCallback<Daily>() {
        override fun areItemsTheSame(oldItem: Daily, newItem: Daily): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Daily, newItem: Daily): Boolean {
            // return oldItem == newItem
            return oldItem.equals(newItem)
        }
    }

    companion object {
        fun newInstance(): DailyListFragment {
            return DailyListFragment()
        }
    }
}