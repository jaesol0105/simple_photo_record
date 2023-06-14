package com.beinny.android.photorecord.ui.recorddetail

import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.*
import android.widget.ImageView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.beinny.android.photorecord.*
import com.beinny.android.photorecord.databinding.DialogPhotoBinding
import com.beinny.android.photorecord.databinding.FragmentDateTimePickerBinding
import com.davemorrissey.labs.subscaleview.ImageSource
import java.io.File

private const val PHOTO_FILE ="file"

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

class PhotoViewerFragment : DialogFragment() {
    /** [Dialog를 생성하여 반환] */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(PHOTO_FILE) as File

        var dlg = Dialog(requireContext(), R.style.photoDialog)
        val binding = DialogPhotoBinding.inflate(LayoutInflater.from(requireContext()))
        binding.ivCloseDialog.setOnClickListener {
            dlg.dismiss()
        }
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_ORIGIN)
            binding.ssivPhoto.setImage(ImageSource.bitmap(bitmap))
        }
        dlg.window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        dlg.setContentView(binding.root)
        return dlg
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