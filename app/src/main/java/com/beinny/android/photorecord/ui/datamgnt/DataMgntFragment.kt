package com.beinny.android.photorecord.ui.datamgnt

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.DialogAlertBinding
import com.beinny.android.photorecord.databinding.FragmentDataMgntBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.beinny.android.photorecord.ui.record.RecordViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class DataMgntFragment : Fragment() {
    // private lateinit var backUpViewModel: DataMgntViewModel
    private lateinit var binding: FragmentDataMgntBinding

    private val viewModel: DataMgntViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //backUpViewModel = ViewModelProvider(this).get(DataMgntViewModel::class.java)

        binding = FragmentDataMgntBinding.inflate(inflater, container, false)
        binding.btnDataMgntReset.setOnClickListener { view ->
            showWarningDialog()
        }

        return binding.root
    }

    fun showWarningDialog() {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlg_binding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(dlg_binding.root)

        dlg_binding.tvDialogAlertMsg.text = getString(R.string.datamgnt_delete_warning)

        dlg_binding.tvDialogAlertComplete.setOnClickListener {
            viewModel.deleteAllData()
            dlg_binding.tvDialogAlertMsg.text = getString(R.string.datamgnt_delete_complete)
            dlg_binding.tvDialogAlertComplete.text = getString(R.string.all_done)
            dlg_binding.tvDialogAlertCancel.visibility = View.GONE
            dlg_binding.viewDialogAlertMiddleLine.visibility = View.GONE
            dlg_binding.tvDialogAlertComplete.setOnClickListener {
                findNavController().navigate(R.id.action_dataMgntFragment_to_recordFragment)
                dlg.dismiss()
            }
        }
        dlg_binding.tvDialogAlertCancel.setOnClickListener {
            dlg.dismiss()
        }
        dlg.show()
        /*
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("경고")
            .setMessage("삭제하신 데이터는 다시는 복구할 수 없습니다." +
                    "정말 삭제 하시겠습니까?")
            .setPositiveButton("모든 데이터 삭제", DialogInterface.OnClickListener{ dialog, id->
                backUpViewModel.deleteAllData()
                val complete_builder = AlertDialog.Builder(activity)
                complete_builder.setTitle("삭제가 완료되었습니다.")
                    .setPositiveButton("확인",DialogInterface.OnClickListener{ dialog, id->
                        findNavController().navigate(R.id.action_dataMgntFragment_to_recordFragment)
                    })
                complete_builder.show()
            })
            .setNegativeButton("취소", DialogInterface.OnClickListener{ dialog, id->

            })
        builder.show()
        */
    }
}