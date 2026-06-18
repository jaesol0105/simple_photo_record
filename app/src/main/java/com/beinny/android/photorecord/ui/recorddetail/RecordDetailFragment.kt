package com.beinny.android.photorecord.ui.recorddetail

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap


import android.graphics.Paint
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.common.*
import com.beinny.android.photorecord.databinding.DialogAlert3TypeBinding
import com.beinny.android.photorecord.databinding.DialogAlertBinding
import com.beinny.android.photorecord.databinding.FragmentRecordDetailBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class RecordDetailFragment : Fragment(), DateTimePickerFragment.CallBacks, CropPhotoFragment.CallBacks {
    private lateinit var bitmapTemp: Bitmap
    private lateinit var bitmapTempThumb: Bitmap

    private lateinit var binding: FragmentRecordDetailBinding
    private val viewModel: RecordDetailViewModel by viewModels { ViewModelFactory(requireContext()) }

    val deviceXY = Point()  // 이미지 리사이즈에 사용

    private lateinit var dlgClose: BottomSheetDialog
    private lateinit var callbacksBp: OnBackPressedCallback

    private val galleryIntent = Intent(Intent.ACTION_PICK).apply {
        type = MediaStore.Images.Media.CONTENT_TYPE
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(context, getString(R.string.recorddetail_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    /** 수정 여부에 따라 확인 다이얼로그 표시 또는 뒤로 이동 */
    private fun backToRecordList() {
        if (isEdit())
            dlgClose.show()
        else
            notSaveRecord()
    }

    private fun isEdit(): Boolean {
        return (viewModel.initialLabel != viewModel.record.label) ||
               (viewModel.initialMemo != viewModel.record.memo) ||
               viewModel.isDateEdit ||
               viewModel.isPhotoEdit
    }

    private fun openGallery() {
        try {
            startActivityForResult(galleryIntent, REQUEST_PHOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, getString(R.string.recorddetail_cannot_find_gallery_app), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacksBp = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backToRecordList()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callbacksBp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recordId: UUID = arguments?.getSerializable(ARG_RECORD_ID) as UUID
        viewModel.loadRecordById(recordId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        viewModel.recordLiveData.observe(viewLifecycleOwner, Observer { record ->
            record?.let {
                binding.record = record         // recordLiveData → 바인딩
                viewModel.record = record       // 편집용 로컬 객체
                viewModel.setInitialValues()
                viewModel.setPhotoFiles()
            }
        })

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        initDlgClose()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = requireActivity().windowManager.currentWindowMetrics.bounds
            deviceXY.set(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getSize(deviceXY)
        }

        binding.etRecordDetailLabel.addTextChangedListener(labelWatcher())
        binding.etRecordDetailLabel.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvRecordDetailDate.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.etRecordDetailMemo.addTextChangedListener(memoWatcher())

        binding.ivRecordDetailClose.setOnClickListener { backToRecordList() }

        binding.tvRecordDetailDate.setOnClickListener {
            DateTimePickerFragment.newInstance(viewModel.record.date).apply {
                setTargetFragment(this@RecordDetailFragment, REQUEST_DATE) // 결과 수신 대상 설정
                show(this@RecordDetailFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        binding.ivRecordDetailPhoto.setOnClickListener {
            if (viewModel.photoFile.exists() && !viewModel.isPhotoEdit) {
                PhotoViewerFragment.newInstance(viewModel.photoFile).apply {
                    show(this@RecordDetailFragment.parentFragmentManager, DIALOG_PHOTO)
                }
            }
        }

        binding.ivRecordDetailPhoto.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.ivRecordDetailPhoto.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        binding.ivRecordDetailAddPhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                openGallery()
            }
        }

        binding.tvRecordDetailDelete.setOnClickListener { deleteRecord() }
        binding.btnRecordDetailCancel.setOnClickListener { notSaveRecord() }
        binding.btnRecordDetailSave.setOnClickListener { saveRecord() }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onDetach() {
        super.onDetach()
        callbacksBp.remove()
    }

    private fun initDlgClose() {
        dlgClose = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgCloseBinding = DialogAlert3TypeBinding.inflate(LayoutInflater.from(requireContext()))
        dlgClose.setContentView(dlgCloseBinding.root)
        dlgCloseBinding.tvDa3tSave.setOnClickListener { // 저장하고 나가기
            dlgClose.dismiss()
            saveRecord()
        }
        dlgCloseBinding.tvDa3tNotSave.setOnClickListener { // 저장 없이 나가기
            dlgClose.dismiss()
            notSaveRecord()
        }
        dlgCloseBinding.tvDa3tCancel.setOnClickListener { dlgClose.dismiss() }
    }

    private fun labelWatcher() = object : TextWatcher {
        var previousString: String = ""

        override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
            previousString = sequence.toString()
        }

        override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.record.label = sequence.toString()
        }

        override fun afterTextChanged(sequence: Editable?) {
            // 글자 수 초과 시 이전 값으로 되돌림
            if (binding.etRecordDetailLabel.length() > LABEL_MAX_LENGTH) {
                binding.etRecordDetailLabel.text = Editable.Factory.getInstance().newEditable(previousString)
                binding.etRecordDetailLabel.setSelection(binding.etRecordDetailLabel.length())
            }
        }
    }

    private fun memoWatcher() = object : TextWatcher {
        override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.record.memo = sequence.toString()
            binding.tvRecordDetailMemoCount.text = if (sequence.toString().isNotEmpty()) {
                sequence.toString().length.toString() + getString(R.string.recorddetail_memo_length)
            } else {
                getString(R.string.recorddetail_non_text)
            }
        }

        override fun afterTextChanged(sequence: Editable?) {}
    }

    /** DB 저장 및 이미지/썸네일 파일 기록 후 뒤로 이동 */
    private fun saveRecord() {
        if (viewModel.record.isNew) viewModel.record.isNew = false

        try {
            if (::bitmapTemp.isInitialized) {
                FileOutputStream(viewModel.photoFile).use { out ->
                    bitmapTemp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            if (::bitmapTempThumb.isInitialized) {
                FileOutputStream(viewModel.thumbFile).use { out ->
                    bitmapTempThumb.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
        } catch (e: Exception) {
            viewModel.toastMessage.value = getString(R.string.recorddetail_storage_error)
            return
        }

        viewModel.saveRecord(viewModel.record)
        findNavController().navigateUp()
    }

    /** 미저장 상태로 이동 — 신규 Record면 DB에서 삭제 */
    private fun notSaveRecord() {
        if (viewModel.record.isNew) // 신규 Record는 진입 시 DB에 insert되므로, 취소 시 명시적으로 삭제 필요
            viewModel.deleteRecord(viewModel.record)
        findNavController().navigateUp()
    }

    private fun deleteRecord() {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgBinding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(dlgBinding.root)

        dlgBinding.tvDialogAlertMsg.text = getString(R.string.recorddetail_delete_warning)
        dlgBinding.tvDialogAlertComplete.setOnClickListener {
            if (viewModel.photoFile.exists()) viewModel.photoFile.delete()
            if (viewModel.thumbFile.exists()) viewModel.thumbFile.delete()
            viewModel.deleteRecord(viewModel.record)
            dlg.dismiss()
            findNavController().navigateUp()
        }
        dlgBinding.tvDialogAlertCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    /** DateTimePicker에서 선택한 날짜를 뷰에 반영 */
    override fun onDateSelected(date: Date) {
        viewModel.record.date = date
        binding.tvRecordDetailDate.text = SimpleDateFormat(DATE_FORMAT).format(date)
        checkEdit(1)
    }

    override fun onPhotoCropped(bitmap: Bitmap) {
        bitmapTemp = resizeBitmapToBitmap(bitmap, max(deviceXY.x, deviceXY.y))         // 원본 크기 Bitmap
        bitmapTempThumb = resizeBitmapToBitmap(bitmap, max(deviceXY.x / 2, deviceXY.y / 2)) // 썸네일 Bitmap
        if (::bitmapTemp.isInitialized) binding.ivRecordDetailPhoto.setImageBitmap(bitmapTemp)
        checkEdit(2)
    }

    private fun checkEdit(type: Int) {
        if (!viewModel.isDateEdit && type == 1) viewModel.isDateEdit = true
        if (!viewModel.isPhotoEdit && type == 2) viewModel.isPhotoEdit = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == REQUEST_PHOTO && data != null) {
            val selectedUri: Uri = data.data ?: return
            CropPhotoFragment.newInstance(selectedUri.toString()).apply {
                setTargetFragment(this@RecordDetailFragment, REQUEST_CROP) // 결과 수신 대상 설정
                show(this@RecordDetailFragment.parentFragmentManager, DIALOG_CROP)
            }
        }
    }

    companion object {
        fun newInstance(recordId: UUID): RecordDetailFragment {
            val args = Bundle().apply { putSerializable(ARG_RECORD_ID, recordId) }
            return RecordDetailFragment().apply { arguments = args }
        }
    }
}
