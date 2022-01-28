package com.beinny.android.dailylook

import android.app.Dialog
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val PHOTO_FILE ="file"

class PhotoViewerFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(PHOTO_FILE) as File
        var dlg = Dialog(requireContext())
        dlg.setContentView(R.layout.dialog_photo)
        val photoView = dlg.findViewById(R.id.photo) as ImageView
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
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