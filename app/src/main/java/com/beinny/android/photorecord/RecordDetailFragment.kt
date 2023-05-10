package com.beinny.android.photorecord

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.lifecycle.ViewModelProvider
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import androidx.activity.OnBackPressedCallback

private const val ARG_RECORD_ID = "record_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_DATE = 0  // datePicker 요청코드
private const val REQUEST_PHOTO = 1 // 카메라 인텐트 요청코드
private const val DATE_FORMAT = "yyyy년 M월 d일, E요일" // 날짜 포맷
private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

class RecordDetailFragment : Fragment(), DatePickerFragment.CallBacks {
    private lateinit var record: Record
    private lateinit var old_record: Record
    private lateinit var photoFile: File
    private lateinit var thumbFile : File
    private lateinit var tempFile : File
    private lateinit var photoUri: Uri
    private lateinit var photoView: ImageView
    private lateinit var photoButton: ImageButton
    private lateinit var memoField: EditText
    private lateinit var labelField: EditText
    private lateinit var dateButton: Button
    private lateinit var uploadButton: Button
    private lateinit var deleteButton: Button

    private lateinit var bitmap_temp: Bitmap
    private lateinit var bitmap_temp_thumb: Bitmap

    private val recordDetailViewModel: RecordDetailViewModel by lazy {
        ViewModelProvider(this).get(RecordDetailViewModel::class.java)
    }

    /** [back press 처리 콜백] */
    private lateinit var callbacks_bp: OnBackPressedCallback
    private var backKeyPressedTime : Long =0

    override fun onAttach(context: Context) { // fregment가 add 될때 호출 됨
        super.onAttach(context)
        /** [back press 처리 콜백] */
        callbacks_bp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(System.currentTimeMillis() > backKeyPressedTime + 2000) {
                    backKeyPressedTime = System.currentTimeMillis()
                    Toast.makeText(context,"'뒤로' 버튼을 누르면 저장되지 않은 변경 사항은 저장되지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                else {
                    if (record.isNew){
                        recordDetailViewModel.deleteRecord(record)
                    }
                    // requireActivity().finish()
                    parentFragmentManager.popBackStack()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacks_bp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** [ARGUMENT를 참고하여 뷰모델로부터 데이터 불러오기] */
        val recordId: UUID = arguments?.getSerializable(ARG_RECORD_ID) as UUID
        recordDetailViewModel.loadRecord(recordId)
        record = Record()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /** [데이터 변경시 UI 갱신] */
        recordDetailViewModel.recordLiveData.observe(
            viewLifecycleOwner,
            Observer { record ->
                record?.let {
                    this.record = record
                    photoFile = recordDetailViewModel.getPhotoFile(record) // 사진파일의 위치를 가르키는 속성
                    thumbFile = recordDetailViewModel.getThumbFile(record)
                    tempFile = recordDetailViewModel.getTempFile(record)
                    /*
                    // fileProvider이 로컬파일 시스템의 파일경로(File)를 카메라앱에서 알 수 있는 Uri로 반환.
                    photoUri = FileProvider.getUriForFile(requireActivity(),"com.beinny.android.record.fileprovider",photoFile)
                    */
                    updateUI()
                    /** [이전 데이터 저장] */
                    if (!record.isNew){
                        old_record = Record()
                        old_record = record
                    }
                }
            })
    }

    override fun onStart() {
        super.onStart()

        /** label(제목) watcher */
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
                record.label = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                /** [제목 글자수 제한] */
                if (labelField.length() > 10) {
                    labelField.text = Editable.Factory.getInstance().newEditable(previousString)
                    labelField.setSelection(labelField.length())
                }
            }
        }
        labelField.addTextChangedListener(labelWatcher)

        /*** memo watcher */
        val memoWatcher = object : TextWatcher {
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
                record.memo = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                /** [입력 가능 line(줄) 수 제한] */
                if (memoField.lineCount > 7) {
                    memoField.text = Editable.Factory.getInstance().newEditable(previousString)
                    memoField.setSelection(memoField.length())
                }
            }
        }

        memoField.addTextChangedListener(memoWatcher)

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(record.date).apply {
                // 대상 프레그먼트 설정 : Fragment로부터 결과 돌려받기위함
                setTargetFragment(this@RecordDetailFragment, REQUEST_DATE)

                show(this@RecordDetailFragment.getParentFragmentManager(), DIALOG_DATE)
            }
        }

        /** [갤러리 앱에서 사진 선택] */
        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val getImageFromAlbum: Intent = Intent().apply {
                action = Intent.ACTION_PICK
                type = MediaStore.Images.Media.CONTENT_TYPE // 또는 type = "image/*"
            }

            setOnClickListener {
                try {
                    startActivityForResult(getImageFromAlbum, REQUEST_PHOTO)
                } catch (e: ActivityNotFoundException){
                    Toast.makeText(context,"갤러리 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /** [photoView 클릭시 dialog로 원본 이미지 출력] */
        photoView.setOnClickListener {
            if (photoFile.exists()) {
                PhotoViewerFragment.newInstance(photoFile).apply {
                    show(this@RecordDetailFragment.getParentFragmentManager(), DIALOG_PHOTO)
                }
            }
        }

        photoView.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        /** [업로드 버튼 - ROOM DB에 반영, 원본 비트맵과 썸네일 비트맵을 내부저장소에 저장] */
        uploadButton.setOnClickListener {
            if(record.isNew) {
                record.isNew = false
            }
            if(::bitmap_temp.isInitialized) {
                val out = FileOutputStream(photoFile)
                // compress 함수를 사용해 스트림에 비트맵을 저장.
                bitmap_temp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                // 스트림 사용후 닫기.
                out.close()
            }
            if(::bitmap_temp_thumb.isInitialized) {
                val out_thumb = FileOutputStream(thumbFile)
                // compress 함수를 사용해 스트림에 비트맵을 저장.
                bitmap_temp_thumb.compress(Bitmap.CompressFormat.JPEG, 100, out_thumb)
                // 스트림 사용후 닫기.
                out_thumb.close()
            }
            recordDetailViewModel.saveRecord(record)
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        /** [액션바 제거] */
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        // dailyDetailViewModel.saveDaily(dailylook)
        /** [액션바 생성] */
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onDetach() {
        super.onDetach()
        callbacks_bp.remove()
        // 부적합한 응답 가능성 대비, URI PERMISSION을 취소.
        // requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    /** [날짜 선택 반영] */
    override fun onDateSelected(date: Date) {
        record.date = date
        updateUI()
    }

    /** [UI 갱신] */
    private fun updateUI() {
        labelField.setText(record.label)
        memoField.setText(record.memo)
        var df : DateFormat = SimpleDateFormat(DATE_FORMAT) // 날짜를 문자열로 변환
        dateButton.text = df.format(record.date)

        // photoView 업데이트
        updatePhotoView()
    }

    /** [photoView 업데이트] */
    private fun updatePhotoView() {
        if (::bitmap_temp.isInitialized) { // 사진을 선택했을경우
            photoView.setImageBitmap(bitmap_temp)
        } else if (photoFile.exists()){ // 사진파일이 존재할 경우
            val bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_ORIGIN)
            photoView.setImageBitmap(bitmap)
        } else { // 사진파일이 존재하지 않을 경우
            photoView.setImageDrawable(null)
        }
    }

    /** [인텐트 결과 처리] */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            /** [갤러리 앱으로 부터 사진 선택] */
            requestCode == REQUEST_PHOTO && data != null -> {
                val selectedUri : Uri = data.data?: return

                /*
                // [이미지 파일의 이름을 참조.]
                val cursor = requireActivity().contentResolver.query(selectedUri,null,null,null,null,null)
                var fileName : String = "not init"
                cursor?.use {
                    if (it.count == 0) {
                        return
                    }
                    it.moveToFirst()
                    val idx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    fileName = it.getString(idx)
                    it.close()
                }
                */

                /*
                val file = File(photoFile.path)
                val thumb_file = File(thumbFile.path)
                */

                /** [원본 이미지파일 & 썸네일 비트맵 생성하기.] */
                val temp_file = File(tempFile.path)
                try {
                    // -0. 내부저장소에 원본 이미지 temp_file 저장. (files 폴더 아래)
                    // uri를 통해 이미지 데이터를 가져옴.
                    val inputStream = requireActivity().contentResolver.openInputStream(selectedUri)
                    // 데이터를 저장할 경로를 File 로 지정.
                    val outputStream = FileOutputStream(temp_file)
                    // 가져온 이미지 데이터를 내부저장소에 copy.
                    IOUtils.copy(inputStream, outputStream)

                    // -1. 원본 이미지 bitmap 파일 생성
                    bitmap_temp = getScaledBitmap(tempFile.path,requireActivity(), GET_BIMAP_ORIGIN)

                    /*
                    // 2. 내부저장소에 썸네일 생성.
                    // RESIZE된 비트맵 파일 얻어오기
                    val bitmap_thumb = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_RESIZE)
                    // 데이터를 저장할 경로를 File 로 지정.
                    val out = FileOutputStream(thumb_file)
                    // compress 함수를 사용해 스트림에 비트맵을 저장.
                    bitmap_thumb.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    // 스트림 사용후 닫기.
                    out.close()
                    */

                    // -2. 썸네일 bitmap 생성
                    bitmap_temp_thumb = getScaledBitmap(tempFile.path,requireActivity(), GET_BIMAP_RESIZE)

                    // -3. temp_file 삭제
                    if(temp_file.exists()){
                        temp_file.delete()
                    }

                } catch(e:Exception) {
                    Toast.makeText(context,"error : 내부 저장소에 이미지를 저장 할 수 없음",Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                /*
                // 갤러리 앱에서 photoUri에 사진 파일을 쓴 이후, URI PERMISSION을 취소
                requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                */

                // photoView 업데이트
                updatePhotoView()
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
}