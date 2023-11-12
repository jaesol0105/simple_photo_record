package com.beinny.android.photorecord.ui.recorddetail

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.databinding.FragmentRecordDetailBinding
import com.beinny.android.photorecord.common.*
import com.beinny.android.photorecord.databinding.DialogAlertBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class RecordDetailFragment : Fragment(), DateTimePickerFragment.CallBacks {
    private lateinit var bitmap_temp: Bitmap
    private lateinit var bitmap_temp_thumb: Bitmap

    private lateinit var binding: FragmentRecordDetailBinding
    private val viewModel: RecordDetailViewModel by viewModels { ViewModelFactory(requireContext()) }

    /** [back press 처리 콜백] */
    private lateinit var callbacks_bp: OnBackPressedCallback
    private var backKeyPressedTime : Long =0

    /** [onAttach : fragment 가 add 될때 호출 된다] */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        /** [back press 처리 콜백] */
        callbacks_bp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(System.currentTimeMillis() > backKeyPressedTime + 2000)
                {
                    backKeyPressedTime = System.currentTimeMillis()
                    Toast.makeText(context,getString(R.string.recorddetail_back_press_warning), Toast.LENGTH_SHORT).show()
                }
                else
                {
                    // add 버튼을 눌러서 detailFragment로 이동할 때, DB에 insert하기 때문에, new record를 저장하지 않을 경우 delete 해준다
                    if (viewModel.record.isNew)
                        viewModel.deleteRecord(viewModel.record)
                    parentFragmentManager.popBackStack() // 뒤로 가기 : 액티비티의 백스택에서 pop
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacks_bp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** [id로(arg)로 record 불러오기] */
        val recordId: UUID = arguments?.getSerializable(ARG_RECORD_ID) as UUID
        viewModel.loadRecordById(recordId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecordDetailBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /** [LifeCycleOwner, ViewModel 바인딩] */
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        /** [데이터 변경시 UI 갱신] */
        viewModel.recordLiveData.observe(
            viewLifecycleOwner,
            Observer { record ->
                record?.let {
                    // updateUI()
                    binding.record = record // record : recordLiveData
                    viewModel.record = record // viewModel.record : 로컬 Record 객체
                    viewModel.setPhotoFiles()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        /** [뒤로 가기] */
        binding.ivRecordDetailClose.setOnClickListener {

        }

        /** [label watcher] */
        val labelWatcher = object : TextWatcher {
            var previousString : String = ""

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                previousString = sequence.toString();
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                viewModel.record.label = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                /** [제목 글자수 제한] */
                if (binding.etRecordDetailLabel.length() > 10) {
                    binding.etRecordDetailLabel.text = Editable.Factory.getInstance().newEditable(previousString)
                    binding.etRecordDetailLabel.setSelection(binding.etRecordDetailLabel.length())
                }
            }
        }
        binding.etRecordDetailLabel.addTextChangedListener(labelWatcher)
        binding.etRecordDetailLabel.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        /** [날짜 선택 리스너] */
        binding.tvRecordDetailDate.setOnClickListener {
            DateTimePickerFragment.newInstance(viewModel.record.date).apply {
                /** [대상 프레그먼트 설정 : 결과 돌려받기 위함] */
                setTargetFragment(this@RecordDetailFragment, REQUEST_DATE)
                show(this@RecordDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }
        binding.tvRecordDetailDate.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        /** [memo watcher] */
        val memoWatcher = object : TextWatcher {
            var previousString : String = ""

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                previousString = sequence.toString()
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                viewModel.record.memo = sequence.toString()
                if (sequence.toString().isNotEmpty()) {
                    binding.tvRecordDetailMemoCount.text = sequence.toString().length.toString() + getString(R.string.all_memo_text_length)
                } else {
                    binding.tvRecordDetailMemoCount.text = getString(R.string.all_non_text)
                }
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        binding.etRecordDetailMemo.addTextChangedListener(memoWatcher)

        /** [원본 이미지 출력하기] */
        binding.ivRecordDetailPhoto.setOnClickListener {
            if (viewModel.photoFile.exists()) {
                PhotoViewerFragment.newInstance(viewModel.photoFile).apply {
                    show(this@RecordDetailFragment.parentFragmentManager, DIALOG_PHOTO)
                }
            }
        }

        binding.ivRecordDetailPhoto.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.ivRecordDetailPhoto.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        /** [갤러리 앱에서 사진 선택] */
        binding.ivRecordDetailAddPhoto.apply {
            val getImageFromAlbum: Intent = Intent().apply {
                action = Intent.ACTION_PICK
                type = MediaStore.Images.Media.CONTENT_TYPE // 또는 type = "image/*"
            }

            setOnClickListener {
                try {
                    startActivityForResult(getImageFromAlbum, REQUEST_PHOTO)
                } catch (e: ActivityNotFoundException){
                    Toast.makeText(context,getString(R.string.recorddetail_cannot_find_gallery_app), Toast.LENGTH_SHORT).show()
                }
            }
        }

        /** [삭제 버튼 - DB에 반영, 내부저장소의 사진을 삭제] */
        binding.btnRecordDetailDelete.setOnClickListener {
            val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
            val dlg_binding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))

            dlg.setContentView(dlg_binding.root)

            dlg_binding.tvDialogAlertMsg.text = getString(R.string.recorddetail_delete_warning)
            dlg_binding.tvDialogAlertComplete.setOnClickListener {
                if(viewModel.photoFile.exists())
                    viewModel.photoFile.delete() // 내부저장소의 사진 파일 삭제

                if(viewModel.thumbFile.exists())
                    viewModel.thumbFile.delete() // 내부저장소의 썸네일 파일 삭제

                viewModel.deleteRecord(viewModel.record) // DB 반영
                dlg.dismiss() // 대화상자 닫기
                parentFragmentManager.popBackStack() // 뒤로 가기
            }
            dlg_binding.tvDialogAlertCancel.setOnClickListener {
                dlg.dismiss()
            }

            dlg.show()
        }

        /** [저장 버튼 - DB에 반영, 이미지 비트맵과 썸네일 비트맵을 내부저장소에 저장] */
        binding.btnRecordDetailSave.setOnClickListener {
            if(viewModel.record.isNew)
                viewModel.record.isNew = false

            if(::bitmap_temp.isInitialized)
            {
                val out = FileOutputStream(viewModel.photoFile)
                // compress 함수를 사용해 스트림에 비트맵을 저장.
                bitmap_temp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                // 스트림 사용후 닫기.
                out.close()
            }
            if(::bitmap_temp_thumb.isInitialized)
            {
                val out_thumb = FileOutputStream(viewModel.thumbFile)
                bitmap_temp_thumb.compress(Bitmap.CompressFormat.JPEG, 100, out_thumb)
                out_thumb.close()
            }

            viewModel.saveRecord(viewModel.record) // DB에 반영
            parentFragmentManager.popBackStack() // 뒤로 가기
        }
    }

    override fun onResume() {
        super.onResume()
        /** [액션바 제거] */
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        /** [액션바 생성] */
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onDetach() {
        super.onDetach()
        callbacks_bp.remove()
    }

    /** [선택한 날짜 뷰에 반영] */
    override fun onDateSelected(date: Date) {
        viewModel.record.date = date
        binding.tvRecordDetailDate.text = SimpleDateFormat(DATE_FORMAT).format(date) // 뷰 업데이트
    }

    /** [인텐트 결과 처리] */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            /** [갤러리 앱 - 사진 선택] */
            requestCode == REQUEST_PHOTO && data != null -> {
                val selectedUri : Uri = data.data?: return

                /** [원본 이미지파일 & 썸네일 비트맵 생성하기.] */
                val temp_file = File(viewModel.tempFile.path)
                try {
                    // [-0. 내부 저장소(temp_file)에 원본 이미지 파일을 임시 저장]
                    val inputStream = requireActivity().contentResolver.openInputStream(selectedUri) // uri에 이미지 데이터를 가져온다
                    val outputStream = FileOutputStream(temp_file) // 데이터를 저장할 경로를 내부 저장소(temp_file) 로 지정.
                    IOUtils.copy(inputStream, outputStream) // input(이미지 데이터)를 output(내부 저장소)에 copy.

                    // [-1. bitmap 파일 생성]
                    bitmap_temp = getScaledBitmap(viewModel.tempFile.path,requireActivity(), GET_BIMAP_ORIGIN)

                    // [-2. 썸네일 bitmap 생성]
                    bitmap_temp_thumb = getScaledBitmap(viewModel.tempFile.path,requireActivity(), GET_BIMAP_RESIZE)

                    // [-3. temp_file 삭제]
                    if(temp_file.exists())
                        temp_file.delete()
                }
                catch(e:Exception)
                {
                    Toast.makeText(context,getString(R.string.recorddetail_storage_error),Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                if (::bitmap_temp.isInitialized)
                    binding.ivRecordDetailPhoto.setImageBitmap(bitmap_temp) // 뷰 업데이트
            }
        }
    }

    companion object {
        fun newInstance(recordId: UUID): RecordDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_RECORD_ID,recordId)
            }
            return RecordDetailFragment().apply {
                arguments = args
            }
        }
    }

    /*
    private lateinit var photoUri: Uri
    private lateinit var photoView: ImageView
    private lateinit var photoButton: ImageButton
    private lateinit var memoField: EditText
    private lateinit var labelField: EditText
    private lateinit var dateButton: Button
    private lateinit var uploadButton: Button
    private lateinit var deleteButton: Button

    private val recordDetailViewModel: RecordDetailViewModel by lazy {
        ViewModelProvider(this).get(RecordDetailViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_record_detail,container,false)

        photoView = view.findViewById(R.id.iv_record_detail_photo) as ImageView
        photoButton = view.findViewById(R.id.btn_record_detail_add_photo) as ImageButton
        memoField = view.findViewById(R.id.et_memo) as EditText
        labelField = view.findViewById(R.id.et_label) as EditText
        dateButton = view.findViewById(R.id.btn_date) as Button
        uploadButton = view.findViewById(R.id.btn_upload) as Button
        deleteButton = view.findViewById(R.id.btn_delete) as Button

        return view
    }

    // [UI 갱신]
    private fun updateUI() {
        labelField.setText(record.label)
        memoField.setText(record.memo)
        var df : DateFormat = SimpleDateFormat(DATE_FORMAT) // 날짜를 문자열로 변환
        dateButton.text = df.format(record.date)

        // photoView 업데이트
        updatePhotoView()
    }

    // [photoView 업데이트]
    private fun updatePhotoView() {
        if (::bitmap_temp.isInitialized) { // 갤러리에서 사진을 선택했을경우
            photoView.setImageBitmap(bitmap_temp)
        } else if (photoFile.exists()){ // 사진파일이 존재할 경우
            val bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_ORIGIN)
            photoView.setImageBitmap(bitmap)
        } else { // 사진파일이 존재하지 않을 경우
            photoView.setImageDrawable(null)
        }
    }
    */
}