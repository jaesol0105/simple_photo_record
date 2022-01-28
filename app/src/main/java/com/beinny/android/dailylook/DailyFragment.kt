package com.beinny.android.dailylook

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getBitmap
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.content.contentValuesOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity

private const val ARG_LOOK_ID = "daily_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_DATE = 0  // datePicker 요청코드
private const val REQUEST_PHOTO = 1 // 카메라 인텐트 요청코드
private const val DATE_FORMAT = "yyyy년 M월 d일, E요일" // 날짜 포맷

class DailyFragment : Fragment(), DatePickerFragment.CallBacks {
    private lateinit var dailylook: Daily
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var photoView: ImageView
    private lateinit var photoButton: ImageButton
    private lateinit var memoField: EditText
    private lateinit var labelField: EditText
    private lateinit var dateButton: Button
    private lateinit var uploadButton: Button
    private lateinit var deleteButton: Button

    private val dailyDetailViewModel: DailyDetailViewModel by lazy {
        ViewModelProvider(this).get(DailyDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 액션바 제거
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()

        dailylook = Daily()
        val crimeId: UUID = arguments?.getSerializable(ARG_LOOK_ID) as UUID
        dailyDetailViewModel.loadDaily(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_daily,container,false)

        photoView = view.findViewById(R.id.photo_view) as ImageView
        photoButton = view.findViewById(R.id.photo_btn) as ImageButton
        memoField = view.findViewById(R.id.memo) as EditText
        labelField = view.findViewById(R.id.label) as EditText
        dateButton = view.findViewById(R.id.date) as Button
        uploadButton = view.findViewById(R.id.upload_btn) as Button
        deleteButton = view.findViewById(R.id.delete_btn) as Button

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dailyDetailViewModel.dailyLiveData.observe(
            viewLifecycleOwner,
            Observer { dailylook ->
                dailylook?.let {
                    this.dailylook = dailylook
                    photoFile = dailyDetailViewModel.getPhotoFile(dailylook) // 사진파일의 위치를 가르키는 속성
                    // fileProvider이 로컬파일 시스템의 파일경로(File)를 카메라앱에서 알 수 있는 Uri로 반환.
                    // photoUri = FileProvider.getUriForFile(requireActivity(),"com.beinny.android.dailylook.fileprovider",photoFile)
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

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
                dailylook.label = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // 제목 글자수 제한
                if (labelField.length() > 10) {
                    labelField.text = Editable.Factory.getInstance().newEditable(previousString)
                    labelField.setSelection(labelField.length())
                }
            }
        }

        labelField.addTextChangedListener(labelWatcher)

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
                dailylook.memo = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // 입력 가능 line(줄) 수 제한
                if (memoField.lineCount > 7) {
                    memoField.text = Editable.Factory.getInstance().newEditable(previousString)
                    memoField.setSelection(memoField.length())
                }
            }
        }

        memoField.addTextChangedListener(memoWatcher)

        // CrimeFragment에서 호출을 위해 this@CrimeFragment 사용
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(dailylook.date).apply {
                // 대상 프레그먼트 설정: 프레그먼트로부터 결과 돌려받기
                setTargetFragment(this@DailyFragment, REQUEST_DATE)

                show(this@DailyFragment.getParentFragmentManager(), DIALOG_DATE)
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val getImageFromAlbum: Intent = Intent().apply {
                action = Intent.ACTION_PICK
                type = MediaStore.Images.Media.CONTENT_TYPE // 또는 type = "image/*"
            }

            setOnClickListener {
                // 갤러리 앱으로 부터 고해상도 파일 반환 받기
                // getImageFromAlbum.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                // val galleryActivities: List<ResolveInfo> = packageManager.queryIntentActivities(getImageFromAlbum, PackageManager.MATCH_DEFAULT_ONLY)
                // 인텐트를 처리할 수 있는 모든 액티비티에 대해, Uri에 대한 쓰기 권한 부여
                // for (galleryActivity in galleryActivities) {
                //     requireActivity().grantUriPermission(galleryActivity.activityInfo.packageName,photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // }

                try {
                    startActivityForResult(getImageFromAlbum, REQUEST_PHOTO)
                } catch (e: ActivityNotFoundException){
                    Toast.makeText(context,"갤러리 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 챌린지 16 : photoView 클릭시 대화상자로 출력
        photoView.setOnClickListener {
            if (photoFile.exists()) {
                PhotoViewerFragment.newInstance(photoFile).apply {
                    show(this@DailyFragment.getParentFragmentManager(), DIALOG_PHOTO)
                }
            }
        }

        photoView.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        uploadButton.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()
        // 액션바 제거
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        dailyDetailViewModel.saveDaily(dailylook)
        // 액션바 생성
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    // 부적합한 응답 가능성 대비, URI PERMISSION을 취소.
    override fun onDetach() {
        super.onDetach()
        // requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        dailylook.date = date
        updateUI()
    }

    private fun updateUI() {
        labelField.setText(dailylook.label)
        memoField.setText(dailylook.memo)
        var df : DateFormat = SimpleDateFormat(DATE_FORMAT)
        dateButton.text = df.format(dailylook.date)

        // photoView 업데이트
        updatePhotoView()
    }

    // photoView 업데이트
    private fun updatePhotoView() {
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_PHOTO && data != null -> {
                val selectedUri : Uri = data.data?: return

                // 이미지파일의 이름 불러오기.
                val cursor = requireActivity().contentResolver.query(selectedUri,null,null,null,null,null)
                var fileName : String = "not init"
                cursor?.use {
                    if (it.count == 0) {
                        return
                    }
                    it.moveToFirst()
                    val idx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    fileName = it.getString(idx)
                    // Toast.makeText(context, imagePath, Toast.LENGTH_SHORT).show()
                    it.close()
                }

                // 내부 저장소에 이미지파일 저장하기.
                val file = File(photoFile.path)
                // Toast.makeText(context, file.toString(), Toast.LENGTH_SHORT).show()
                try {
                    // uri를 통해 이미지에 필요한 데이터를 가져온다.
                    val inputStream = requireActivity().contentResolver.openInputStream(selectedUri)
                    if (inputStream == null) {
                        Toast.makeText(context,"inputstream",Toast.LENGTH_SHORT).show()
                        return
                    }
                    // 가져온 이미지 데이터를 파일에 저장한다.
                    val outputStream = FileOutputStream(file)
                    IOUtils.copy(inputStream, outputStream);
                } catch(e:Exception) {
                    Toast.makeText(context,"파일 불러오기 실패",Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


                // 갤러리 앱에서 photoUri에 사진 파일을 쓴 이후, URI PERMISSION을 취소
                // requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // photoView 업데이트
                updatePhotoView()
            }
        }
    }

    companion object {
        fun newInstance(dailyId: UUID): DailyFragment {
            val args = Bundle().apply {
                putSerializable(ARG_LOOK_ID,dailyId)
            }
            return DailyFragment().apply {
                arguments = args
            }
        }
    }
}