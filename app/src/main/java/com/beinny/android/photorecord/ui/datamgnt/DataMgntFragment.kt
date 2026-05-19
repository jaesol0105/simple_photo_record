package com.beinny.android.photorecord.ui.datamgnt

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.DialogAlertBinding
import com.beinny.android.photorecord.databinding.FragmentDataMgntBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class DataMgntFragment : Fragment() {
    private lateinit var binding: FragmentDataMgntBinding
    private val viewModel: DataMgntViewModel by viewModels { ViewModelFactory(requireContext()) }

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri -> uri?.let { viewModel.backup(it) } }

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showRestoreConfirmDialog(it) } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDataMgntBinding.inflate(inflater, container, false)

        binding.btnDataMgntBackup.setOnClickListener {
            val fileName = "photorecord_backup_${
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            }.zip"
            createFileLauncher.launch(fileName)
        }

        binding.btnDataMgntRestore.setOnClickListener {
            openFileLauncher.launch(arrayOf("application/zip"))
        }

        binding.btnDataMgntRestoreLast.setOnClickListener {
            showRestoreConfirmDialog(null)
        }

        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressDataMgnt.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnDataMgntBackup.isEnabled = !loading
            binding.btnDataMgntRestore.isEnabled = !loading
            binding.btnDataMgntRestoreLast.isEnabled = !loading
        }

        viewModel.backupResult.observe(viewLifecycleOwner) { result ->
            val msg = if (result.isSuccess) R.string.datamgnt_backup_success else R.string.datamgnt_backup_fail
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.restoreResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.datamgnt_restore_success, count),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), R.string.datamgnt_restore_fail, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.lastBackupInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                binding.layoutLastBackup.visibility = View.VISIBLE
                binding.btnDataMgntRestoreLast.visibility = View.VISIBLE
                binding.tvLastBackupDate.text = getString(
                    R.string.datamgnt_last_backup_date,
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(info.date))
                )
                binding.tvLastBackupName.text = getString(R.string.datamgnt_last_backup_name, info.fileName)
            } else {
                binding.layoutLastBackup.visibility = View.GONE
                binding.btnDataMgntRestoreLast.visibility = View.GONE
            }
        }
    }

    private fun showRestoreConfirmDialog(uri: Uri?) {
        val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
        val dlgBinding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(dlgBinding.root)

        dlgBinding.tvDialogAlertMsg.text = getString(R.string.datamgnt_restore_confirm)
        dlgBinding.tvDialogAlertComplete.setOnClickListener {
            if (uri != null) viewModel.restore(uri) else viewModel.restoreFromLastBackup()
            dlg.dismiss()
        }
        dlgBinding.tvDialogAlertCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }
}
