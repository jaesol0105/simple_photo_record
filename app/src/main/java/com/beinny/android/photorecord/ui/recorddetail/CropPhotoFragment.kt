package com.beinny.android.photorecord.ui.recorddetail

import android.app.Dialog
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.beinny.android.photorecord.common.ARG_FILE_URI_STRING
import com.beinny.android.photorecord.databinding.FragmentCropPhotoBinding
import android.graphics.Bitmap
import android.view.ViewTreeObserver
import android.graphics.BitmapFactory
import com.beinny.android.photorecord.R
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import com.beinny.android.photorecord.exifOrientationToDegrees
import com.beinny.android.photorecord.getExifDegree
import com.beinny.android.photorecord.rotate
import com.fenchtose.nocropper.*
import com.fenchtose.nocropper.CropperView.GridCallback
import java.io.IOException


class CropPhotoFragment : DialogFragment() {
    private lateinit var binding: FragmentCropPhotoBinding
    private lateinit var dlg: Dialog

    private lateinit var mBitmap : Bitmap // 비트맵
    private lateinit var originalBitmap : Bitmap // 원본 비트맵
    private var rotationCount = 0 // 이미지 회전 수
    private var isSnappedToCenter = false // 이미지 스냅
    private lateinit var currentFilePath: String

    /** [콜백 인터페이스, RecordDetailFragment에 구현] */
    interface CallBacks {
        fun onPhotoCropped(bitmap: Bitmap)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        /** [ARG로 받은 String형태의 URI를 URI형태로 복원] */
        val fileUriString = arguments?.getSerializable(ARG_FILE_URI_STRING) as String
        val fileUri: Uri = Uri.parse(fileUriString)

        /** [전체화면 Dialog] */
        dlg = Dialog(requireContext(),R.style.FullScreenDialogTheme)
        binding = FragmentCropPhotoBinding.inflate(LayoutInflater.from(requireContext()))

        /** [백 버튼] */
        binding.ivCropPhotoClose.setOnClickListener {
            dlg.dismiss()
        }
        /** [이미지 회전 버튼] */
        binding.btnCropPhotoRotateButton.setOnClickListener {
            rotateImage()
        }
        /** [이미지 스냅 버튼] */
        binding.btnCropPhotoSnapButton.setOnClickListener {
            snapImage()
        }
        /** [테스트 버튼] */
        binding.btnTest.setOnClickListener {
            targetFragment?.let { fragment -> (fragment as CropPhotoFragment.CallBacks).onPhotoCropped(originalBitmap) }
            dlg.dismiss()
        }
        /** [이미지 자르기 버튼] */
        binding.tvCropPhotoComplete.setOnClickListener {
            cropImageAsync()
        }
        /** [격자 콜백] */
        binding.cropperviewCropPhoto.setGridCallback(object : GridCallback {
            override fun onGestureStarted(): Boolean {
                return true
            }

            override fun onGestureCompleted(): Boolean {
                return false
            }
        })
        /** [Uri로부터 이미지 불러오기] */
        loadNewImage(getFilePathFromUri(fileUri))

        dlg.setContentView(binding.root)
        return dlg
    }

    /** [이미지 자르기: 비동기, 원본 사용] */
    private fun prepareCropForOriginalImage(): ScaledCropper? {
        val result: CropResult = binding.cropperviewCropPhoto.cropInfo
        if (result.cropInfo == null) {
            return null
        }
        val scale: Float = if (rotationCount % 2 == 0) {
            originalBitmap.width.toFloat() / mBitmap.width
        } else {
            originalBitmap.width.toFloat() / mBitmap.height
        }
        Log.d("CROP", rotationCount.toString())
        val cropInfo: CropInfo = result.cropInfo.rotate90XTimes(mBitmap.width, mBitmap.height, rotationCount)
        return ScaledCropper(cropInfo, originalBitmap, scale)
    }

    /** [이미지 자르기: 비동기, 원본 사용] */
    private fun cropImageAsync() {
        if (originalBitmap != null) {
            val cropper: ScaledCropper = prepareCropForOriginalImage() ?: return
            cropper.crop(object : CropperCallback() {
                override fun onCropped(bitmap: Bitmap) {
                    if (bitmap != null) {
                        try {
                            targetFragment?.let { fragment -> (fragment as CropPhotoFragment.CallBacks).onPhotoCropped(rotate(bitmap,90*(rotationCount%4))) }
                            dlg.dismiss()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        }
    }

    /** [Uri -> abs Path 변환] */
    private fun getFilePathFromUri(uri: Uri): String {
        val cursor: Cursor = requireActivity().contentResolver.query(uri, null, null, null, null)!!
        cursor.moveToNext()
        val path = cursor.getString(cursor.getColumnIndexOrThrow("_data"))
        cursor.close()
        return path
    }

    /** [Uri로부터 이미지 불러오기] */
    private fun loadNewImage(filePath: String) {
        currentFilePath = filePath

        mBitmap = rotate(BitmapFactory.decodeFile(filePath), getExifDegree(filePath)) // 절대경로로부터 비트맵 불러오기, 이미지 회전현상 해결

        //mBitmap = BitmapFactory.decodeFile(filePath) // 절대경로로부터 비트맵 불러오기
        originalBitmap = mBitmap // 원본 비트맵 저장

        val maxP: Int = Math.max(mBitmap.width, mBitmap.height)
        val scale1280 = maxP.toFloat() / 1280

        // Max Zoom
        if (binding.cropperviewCropPhoto.width !== 0) {
            binding.cropperviewCropPhoto.maxZoom = binding.cropperviewCropPhoto.width * 4 / 1280f
        } else {
            val vto: ViewTreeObserver = binding.cropperviewCropPhoto.viewTreeObserver
            vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.cropperviewCropPhoto.viewTreeObserver.removeOnPreDrawListener(this)
                    binding.cropperviewCropPhoto.maxZoom = binding.cropperviewCropPhoto.width * 4 / 1280f
                    return true
                }
            })
        }

        // 비트맵 스케일링 후 뷰에 적재
        mBitmap = Bitmap.createScaledBitmap(mBitmap, Math.round(mBitmap.width / scale1280), Math.round(mBitmap.height / scale1280), true)
        binding.cropperviewCropPhoto.setImageBitmap(mBitmap)
    }

    /** [이미지 90도 회전] */
    private fun rotateImage() {
        if (mBitmap == null)
            return
        val matrix = Matrix()
        matrix.postRotate(90F)
        mBitmap =  Bitmap.createBitmap(
            mBitmap,
            0,
            0,
            mBitmap.width,
            mBitmap.height,
            matrix,
            true
        )
        binding.cropperviewCropPhoto.setImageBitmap(mBitmap)
        rotationCount++
    }

    /** [이미지 스냅] */
    private fun snapImage() {
        if (isSnappedToCenter) {
            binding.cropperviewCropPhoto.cropToCenter()
        } else {
            binding.cropperviewCropPhoto.fitToCenter()
        }
        isSnappedToCenter = !isSnappedToCenter
    }

    companion object {
        fun newInstance(fileUriString: String): CropPhotoFragment {
            val args = Bundle().apply {
                putSerializable(ARG_FILE_URI_STRING,fileUriString)
            }
            return CropPhotoFragment().apply {
                arguments = args
            }
        }
    }
}