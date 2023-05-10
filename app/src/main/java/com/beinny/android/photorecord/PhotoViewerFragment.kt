package com.beinny.android.photorecord

import android.app.Dialog
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val PHOTO_FILE ="file"

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

class PhotoViewerFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(PHOTO_FILE) as File
        var dlg = Dialog(requireContext())
        dlg.setContentView(R.layout.dialog_photo)
        val photoView = dlg.findViewById(R.id.photo) as ImageView
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity(), GET_BIMAP_ORIGIN)
            photoView.setImageBitmap(bitmap)
        }

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