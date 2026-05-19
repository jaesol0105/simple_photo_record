package com.beinny.android.photorecord.ui.recorddetail

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.common.GET_BITMAP_ORIGIN
import com.beinny.android.photorecord.common.PHOTO_FILE_PATH
import com.beinny.android.photorecord.common.PHOTO_MAX_ZOOM
import com.beinny.android.photorecord.databinding.DialogPhotoBinding
import com.davemorrissey.labs.subscaleview.ImageSource
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val PHOTO_FILE = "file"

class PhotoViewerFragment : DialogFragment() {
    private var bitmap: Bitmap? = null

    private val permissionStorage = arrayOf( // Android Q 이하 저장소 권한
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val isAllGranted = permissionStorage.all { e -> resultMap[e] == true }
        if (isAllGranted) {
            bitmap?.let { bmp ->
                saveImage(bmp)
                Toast.makeText(context, getString(R.string.photoviewer_save_done), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(PHOTO_FILE) as File

        val dlg = Dialog(requireContext(), R.style.photoDialog)
        val binding = DialogPhotoBinding.inflate(LayoutInflater.from(requireContext()))

        binding.ivDialogPhotoClose.setOnClickListener {
            dlg.dismiss()
        }

        if (photoFile.exists()) {
            bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BITMAP_ORIGIN)
            binding.ssivDialogPhoto.setImage(ImageSource.bitmap(bitmap!!))
            binding.ssivDialogPhoto.maxScale = PHOTO_MAX_ZOOM // 최대 줌 배율

            binding.ivDialogPhotoDownload.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android Q 이상
                    saveImageAndroidQ(bitmap!!)
                    Toast.makeText(context, getString(R.string.photoviewer_save_done), Toast.LENGTH_SHORT).show()
                } else { // Android Q 미만 — 저장소 권한 확인 후 저장
                    val writePermission = ActivityCompat.checkSelfPermission(
                        PhotoRecordApplication.applicationContext(),
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    if (writePermission == PackageManager.PERMISSION_GRANTED) {
                        saveImage(bitmap!!)
                        Toast.makeText(context, getString(R.string.photoviewer_save_done), Toast.LENGTH_SHORT).show()
                    } else {
                        activityResultLauncher.launch(permissionStorage)
                    }
                }
            }
        }

        dlg.window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) // 상/하단바 영역 포함 전체화면
        dlg.window!!.attributes.windowAnimations = R.style.AnimationPopupStyle
        dlg.setContentView(binding.root)
        return dlg
    }

    /** Android Q (API 29) 이상 이미지 저장 */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, PHOTO_FILE_PATH)
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1) // 쓰기 완료 전 다른 앱의 접근 차단
        }

        val uri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if (uri != null) {
                val image = requireActivity().contentResolver.openFileDescriptor(uri, "w", null)
                if (image != null) {
                    FileOutputStream(image.fileDescriptor).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 쓰기 완료, 접근 허용
                    requireActivity().contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Android Q 미만 이미지 저장 */
    private fun saveImage(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val dir = File("$externalStorage/$PHOTO_FILE_PATH")

        if (!dir.exists()) dir.mkdirs()

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile()
            FileOutputStream(fileItem).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            @Suppress("DEPRECATION")
            requireActivity().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(file: File): PhotoViewerFragment {
            val args = Bundle().apply {
                putSerializable(PHOTO_FILE, file)
            }
            return PhotoViewerFragment().apply {
                arguments = args
            }
        }
    }
}
