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
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.common.PHOTO_FILE_PATH
import com.beinny.android.photorecord.databinding.DialogPhotoBinding
import com.davemorrissey.labs.subscaleview.ImageSource
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception

private const val PHOTO_FILE ="file"

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기

class PhotoViewerFragment : DialogFragment() {
    /** [Android Q 이하: 저장소 R/W 권한] */
    private val permissionStorage = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val contract = ActivityResultContracts.RequestMultiplePermissions()
    private val activityResultLauncher = registerForActivityResult(contract) { resultMap ->
        val isAllGranted = permissionStorage.all { e -> resultMap[e] == true }
    }

    /** [Dialog를 생성하여 반환] */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(PHOTO_FILE) as File

        var dlg = Dialog(requireContext(), R.style.photoDialog) // 투명 상단바 스타일 적용
        val binding = DialogPhotoBinding.inflate(LayoutInflater.from(requireContext()))

        /** [백 버튼 리스너] */
        binding.ivDialogPhotoClose.setOnClickListener {
            dlg.dismiss()
        }

        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_ORIGIN)
            binding.ssivDialogPhoto.setImage(ImageSource.bitmap(bitmap))
            binding.ssivDialogPhoto.maxScale = 8.0F // zoom-in 최대 배율

            /** [다운로드 리스너] */
            binding.ivDialogPhotoDownload.setOnClickListener {
                /** [Q (안드로이드 10, API 29) 이상] */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    saveImageAndroidQ(bitmap)
                    Toast.makeText(context,getString(R.string.photoviewer_save_done),Toast.LENGTH_SHORT).show()
                }
                /** [Q 이하, 저장소 권한을 얻어온다.] */
                else
                {
                    val writePermission = ActivityCompat.checkSelfPermission(PhotoRecordApplication.applicationContext(),android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

                    if(writePermission == PackageManager.PERMISSION_GRANTED){
                        saveImage(bitmap)
                        Toast.makeText(context,getString(R.string.photoviewer_save_done),Toast.LENGTH_SHORT).show()
                    } else {
                        activityResultLauncher.launch(permissionStorage) // 권한 요청 팝업
                    }
                }
            }
        }

        dlg.window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) // dlg 전체화면 사용 (상/하단바 영역 포함)
        dlg.window!!.attributes.windowAnimations = R.style.AnimationPopupStyle
        dlg.setContentView(binding.root)
        return dlg
    }

    /** [Q (안드로이드 10, API 29) 이상 이미지 다운로드] */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageAndroidQ(bitmap:Bitmap){
        val fileName = System.currentTimeMillis().toString() + ".png"
        val contentValues = ContentValues() // contentValues는 contentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용된다.
        contentValues.apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, PHOTO_FILE_PATH) // 경로설정
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // 파일이름
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1) // 현재 is_pending 상태 (다른곳에서 이 데이터를 요구하면 무시, 해당 저장소를 독점)
        }

        val uri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

        try {
            if (uri != null){
                val image = requireActivity().contentResolver.openFileDescriptor(uri,"w",null)
                if (image != null){
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,fos)
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점 해제
                    requireActivity().contentResolver.update(uri,contentValues,null,null)
                }
            }
        } catch (e: FileNotFoundException){
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    /** [Q 이하 이미지 다운로드] */
    private fun saveImage(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/$PHOTO_FILE_PATH"
        val dir = File(path)

        if(dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile() // 0KB 파일 생성.
            val fos = FileOutputStream(fileItem) // 파일 아웃풋 스트림
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) // 파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.
            fos.close() // 파일 아웃풋 스트림 객체 close
            requireActivity().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem))) // 브로드캐스트 수신자에게 파일 미디어 스캔 액션 요청. 그리고 데이터로 추가된 파일에 Uri를 넘겨준다.
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(file : File): PhotoViewerFragment {
            val args = Bundle().apply {
                putSerializable(PHOTO_FILE,file)
            }
            return PhotoViewerFragment().apply {
                arguments = args
            }
        }
    }
}