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
import android.graphics.Point
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.databinding.FragmentRecordDetailBinding
import com.beinny.android.photorecord.common.*
import com.beinny.android.photorecord.databinding.DialogAlert3TypeBinding
import com.beinny.android.photorecord.databinding.DialogAlertBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.max

class RecordDetailFragment : Fragment(), DateTimePickerFragment.CallBacks, CropPhotoFragment.CallBacks {
    private lateinit var bitmapTemp: Bitmap
    private lateinit var bitmapTempThumb: Bitmap

    private lateinit var binding: FragmentRecordDetailBinding
    private val viewModel: RecordDetailViewModel by viewModels { ViewModelFactory(requireContext()) }

    /** [디바이스의 화면 크기] */
    val deviceXY = Point()

    /** [뒤로 가기 - 대화상자] */
    private lateinit var dlgClose : BottomSheetDialog

    /** [뒤로가기 - back press 처리 콜백] */
    private lateinit var callbacksBp: OnBackPressedCallback

    /** [뒤로 가기 - 동작] */
    private fun backToRecordList() {
        if(isEdit())// 수정했을 경우
            dlgClose.show()

        else // 수정안했을 경우
            notSaveRecord() // new-삭제+나가기, old-나가기
    }

    /** [데이터 변경 여부 확인] */
    private fun isEdit() : Boolean {
        return (viewModel.initialLabel != viewModel.record.label)||(viewModel.initialMemo != viewModel.record.memo)||viewModel.isDateEdit||viewModel.isPhotoEdit
    }

    /** [onAttach : fragment 가 add 될때 호출된다] */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        /** [back press 처리 콜백] */
        callbacksBp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backToRecordList()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacksBp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** [id로(arg)로 Record 불러오기] */
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

        /** [UI 갱신] */
        viewModel.recordLiveData.observe(
            viewLifecycleOwner,
            Observer { record ->
                record?.let {
                    // updateUI()
                    binding.record = record // record : recordLiveData
                    viewModel.record = record // viewModel.record : 로컬 Record 객체
                    viewModel.setInitialValues()
                    viewModel.setPhotoFiles()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        /** [뒤로 가기 대화상자 초기화] */
        initDlgClose()

        /** [디바이스 화면 크기 구하기] */
        getDeviceXYInfo()

        /** [뷰 설정] */
        binding.etRecordDetailLabel.addTextChangedListener(labelWatcher())
        binding.etRecordDetailLabel.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvRecordDetailDate.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.etRecordDetailMemo.addTextChangedListener(memoWatcher())

        /** [백 버튼 리스너] */
        binding.ivRecordDetailClose.setOnClickListener {
            backToRecordList()
        }

        /** [날짜 선택 리스너] */
        binding.tvRecordDetailDate.setOnClickListener {
            DateTimePickerFragment.newInstance(viewModel.record.date).apply {
                /** [대상 프레그먼트 설정 : 결과 돌려받기 위함] */
                setTargetFragment(this@RecordDetailFragment, REQUEST_DATE)
                show(this@RecordDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        /** [원본 이미지 출력 리스너] */
        binding.ivRecordDetailPhoto.setOnClickListener {
            if (viewModel.photoFile.exists() && !viewModel.isPhotoEdit) {
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

        /** [갤러리 앱에서 이미지 선택 리스너] */
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

        /** [삭제 버튼 리스너] */
        binding.tvRecordDetailDelete.setOnClickListener {
            deleteRecord()
        }

        /** [취소 버튼 리스너] */
        binding.btnRecordDetailCancel.setOnClickListener {
            notSaveRecord()
        }

        /** [저장 버튼 리스너] */
        binding.btnRecordDetailSave.setOnClickListener {
            saveRecord()
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
        callbacksBp.remove()
    }

    /** [뒤로 가기 대화상자 초기화] */
    private fun initDlgClose() {
        dlgClose = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgCloseBinding = DialogAlert3TypeBinding.inflate(LayoutInflater.from(requireContext()))
        dlgClose.setContentView(dlgCloseBinding.root)
        dlgCloseBinding.tvDa3tSave.setOnClickListener { // 저장하고 나가기
            dlgClose.dismiss()
            saveRecord()
        }
        dlgCloseBinding.tvDa3tNotSave.setOnClickListener { // 저장안하고 나가기
            dlgClose.dismiss()
            notSaveRecord() // new-삭제+나가기, old-나가기
        }
        dlgCloseBinding.tvDa3tCancel.setOnClickListener {
            dlgClose.dismiss()
        }
    }

    /** [디바이스 화면 크기 구하기] */
    private fun getDeviceXYInfo() {
        @Suppress("DEPRECATION")
        requireActivity().windowManager.defaultDisplay.getSize(deviceXY)
    }

    /** [label watcher] */
    private fun labelWatcher() = object : TextWatcher {
        var previousString: String = ""

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
            if (binding.etRecordDetailLabel.length() > 30) {
                binding.etRecordDetailLabel.text =
                    Editable.Factory.getInstance().newEditable(previousString)
                binding.etRecordDetailLabel.setSelection(binding.etRecordDetailLabel.length())
            }
        }
    }

    /** [memo watcher] */
    private fun memoWatcher() = object : TextWatcher {
        var previousString: String = ""

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
                binding.tvRecordDetailMemoCount.text =
                    sequence.toString().length.toString() + getString(R.string.recorddetail_memo_length)
            } else {
                binding.tvRecordDetailMemoCount.text = getString(R.string.recorddetail_non_text)
            }
        }

        override fun afterTextChanged(sequence: Editable?) {}
    }

    /** [저장하고 나가기 - DB에 저장, 이미지 bitmap, 썸네일 bitmap 내부저장소에 저장] */
    private fun saveRecord() {
        if (viewModel.record.isNew)
            viewModel.record.isNew = false

        if (::bitmapTemp.isInitialized) {
            val out = FileOutputStream(viewModel.photoFile)
            // compress 함수를 사용해 스트림에 bitmap을 저장.
            bitmapTemp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            // 스트림 사용후 닫기.
            out.close()
        }
        if (::bitmapTempThumb.isInitialized) {
            val outThumb = FileOutputStream(viewModel.thumbFile)
            bitmapTempThumb.compress(Bitmap.CompressFormat.JPEG, 100, outThumb)
            outThumb.close()
        }

        viewModel.saveRecord(viewModel.record) // DB에 반영
        parentFragmentManager.popBackStack() // 뒤로 가기
    }

    /** [저장하지 않고 나가기] */
    private fun notSaveRecord() {
        if (viewModel.record.isNew) // fab(add) -> detailFragment 이동 시, DB에 insert 하기 때문에, new Record를 저장하지 않을 경우 delete 해줘야 함
            viewModel.deleteRecord(viewModel.record)
        parentFragmentManager.popBackStack()
    }

    /** [삭제 대화상자 출력 - DB의 Record 삭제, 내부저장소의 이미지 삭제] */
    private fun deleteRecord() {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgBinding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))

        dlg.setContentView(dlgBinding.root)

        dlgBinding.tvDialogAlertMsg.text = getString(R.string.recorddetail_delete_warning)
        dlgBinding.tvDialogAlertComplete.setOnClickListener {
            if (viewModel.photoFile.exists())
                viewModel.photoFile.delete() // 내부저장소의 이미지 파일 삭제

            if (viewModel.thumbFile.exists())
                viewModel.thumbFile.delete() // 내부저장소의 썸네일 파일 삭제

            viewModel.deleteRecord(viewModel.record) // DB 반영
            dlg.dismiss() // 대화상자 닫기
            parentFragmentManager.popBackStack() // 뒤로 가기
        }
        dlgBinding.tvDialogAlertCancel.setOnClickListener {
            dlg.dismiss()
        }

        dlg.show()
    }

    /** [DateTimePicker 콜백 함수 : 선택한 날짜를 뷰에 반영] */
    override fun onDateSelected(date: Date) {
        viewModel.record.date = date
        binding.tvRecordDetailDate.text = SimpleDateFormat(DATE_FORMAT).format(date) // 뷰 업데이트
        checkEdit(1)
    }

    override fun onPhotoCropped(bitmap: Bitmap){
        // [-1. bitmap 파일 생성]
        bitmapTemp = resizeBitmapToBitmap(bitmap, max(deviceXY.x,deviceXY.y))

        // [-2. 썸네일 bitmap 생성]
        bitmapTempThumb = resizeBitmapToBitmap(bitmap, max(deviceXY.x/2,deviceXY.y/2))

        if (::bitmapTemp.isInitialized)
            binding.ivRecordDetailPhoto.setImageBitmap(bitmapTemp) // 뷰 업데이트

        checkEdit(2)
    }

    /** [데이터 변경시 기록] */
    private fun checkEdit(type: Int) {
        if(!viewModel.isDateEdit && type == 1)
            viewModel.isDateEdit = true
        if(!viewModel.isPhotoEdit && type == 2)
            viewModel.isPhotoEdit = true
    }

    /** [인텐트 결과 처리] */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            /** [갤러리 앱 - 이미지 선택] */
            requestCode == REQUEST_PHOTO && data != null -> {
                val selectedUri : Uri = data.data?: return
                CropPhotoFragment.newInstance(selectedUri.toString()).apply {
                    /** [대상 프레그먼트 설정 : 결과 돌려받기 위함] */
                    setTargetFragment(this@RecordDetailFragment, REQUEST_CROP)
                    show(this@RecordDetailFragment.parentFragmentManager, DIALOG_CROP)
                }
                // createBitmap(selectedUri) // bitmap 생성
            }
        }
    }

    /** [원본 이미지 & 썸네일 비트맵 생성하기.] */
    private fun createBitmap(selectedUri: Uri) {
        val tempFile = File(viewModel.tempFile.path)
        try {
            // [-0. 내부 저장소(tempFile)에 원본 이미지 파일을 임시 저장]
            val inputStream = requireActivity().contentResolver.openInputStream(selectedUri) // uri의 이미지 데이터를 가져온다
            val outputStream = FileOutputStream(tempFile) // 데이터를 저장할 경로를 내부 저장소(tempFile) 로 지정.
            IOUtils.copy(inputStream, outputStream) // input(이미지 데이터)를 output(내부 저장소)에 copy.

            // [-1. bitmap 파일 생성]
            bitmapTemp = getScaledBitmap(viewModel.tempFile.path, requireActivity(), GET_BIMAP_ORIGIN)

            // [-2. 썸네일 bitmap 생성]
            bitmapTempThumb = getScaledBitmap(viewModel.tempFile.path, requireActivity(), GET_BIMAP_RESIZE)

            // [-3. tempFile 삭제]
            if (tempFile.exists())
                tempFile.delete()

        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.recorddetail_storage_error), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        if (::bitmapTemp.isInitialized)
            binding.ivRecordDetailPhoto.setImageBitmap(bitmapTemp) // 뷰 업데이트
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