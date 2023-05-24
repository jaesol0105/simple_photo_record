package com.beinny.android.photorecord.ui.datamgnt

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.FragmentDataMgntBinding

class DataMgntFragment : Fragment() {
    private lateinit var backUpViewModel: DataMgntViewModel
    private lateinit var binding: FragmentDataMgntBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        backUpViewModel =
            ViewModelProvider(this).get(DataMgntViewModel::class.java)

        binding = FragmentDataMgntBinding.inflate(inflater, container, false)
        binding.btnDataReset.setOnClickListener { view ->
            showWarningDialog()
        }

        return binding.root
    }

    fun showWarningDialog() {
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
    }
}