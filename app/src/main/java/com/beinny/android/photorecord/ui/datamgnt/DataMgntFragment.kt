package com.beinny.android.photorecord.ui.datamgnt

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.DialogRestoreModeBinding
import com.beinny.android.photorecord.databinding.FragmentDataMgntBinding
import com.beinny.android.photorecord.ui.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DataMgntFragment : Fragment() {
    private lateinit var binding: FragmentDataMgntBinding
    private val viewModel: DataMgntViewModel by viewModels { ViewModelFactory(requireContext()) }

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.backup(it) } }

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showRestoreModeDialog(it) }
    }

    // API < 29 에서 갤러리 내보내기에 필요한 저장소 권한 요청
    private val exportPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.exportToGallery()
        else Toast.makeText(requireContext(), R.string.datamgnt_export_permission_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val color = ContextCompat.getColor(requireContext(), R.color.list_container_bg)
        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.setBackgroundColor(color)
        requireActivity().window.statusBarColor = color
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val color = ContextCompat.getColor(requireContext(), R.color.background)
        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.setBackgroundColor(color)
        requireActivity().window.statusBarColor = color
    }

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

        // 마지막 백업 URI 유효성 체크 후 다이얼로그 표시
        binding.btnDataMgntRestoreLast.setOnClickListener {
            lifecycleScope.launch {
                val uri = withContext(Dispatchers.IO) { viewModel.getLastBackupUri() }
                if (uri == null) {
                    Toast.makeText(requireContext(), R.string.datamgnt_restore_uri_expired, Toast.LENGTH_LONG).show()
                } else {
                    showRestoreModeDialog(uri)
                }
            }
        }

        binding.btnDataMgntExportGallery.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.exportToGallery()
            }
        }

        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnDataMgntBackup.isEnabled = !loading
            binding.btnDataMgntRestore.isEnabled = !loading
            binding.btnDataMgntRestoreLast.isEnabled = !loading
            binding.btnDataMgntExportGallery.isEnabled = !loading
            if (!loading) {
                binding.layoutProgressBackup.visibility = View.GONE
                binding.layoutProgressRestore.visibility = View.GONE
                binding.layoutProgressExport.visibility = View.GONE
            }
        }

        viewModel.activeOperation.observe(viewLifecycleOwner) { op ->
            binding.layoutProgressBackup.visibility  = if (op == ActiveOperation.BACKUP)  View.VISIBLE else View.GONE
            binding.layoutProgressRestore.visibility = if (op == ActiveOperation.RESTORE) View.VISIBLE else View.GONE
            binding.layoutProgressExport.visibility  = if (op == ActiveOperation.EXPORT)  View.VISIBLE else View.GONE
        }

        viewModel.progress.observe(viewLifecycleOwner) { percent ->
            val isIndeterminate = (percent == null)
            listOf(binding.progressBackup, binding.progressRestore, binding.progressExport).forEach {
                it.isIndeterminate = isIndeterminate
                if (!isIndeterminate) it.progress = percent!!
            }
        }

        viewModel.progressLabel.observe(viewLifecycleOwner) { label ->
            binding.tvProgressLabelBackup.text = label
            binding.tvProgressLabelRestore.text = label
            binding.tvProgressLabelExport.text = label
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
                // 복원 완료 후 메인 화면으로 이동
                findNavController().navigate(R.id.recordFragment)
            } else {
                Toast.makeText(requireContext(), R.string.datamgnt_restore_fail, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                val msg = if (count == 0) getString(R.string.datamgnt_export_no_photos)
                          else getString(R.string.datamgnt_export_success, count)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.datamgnt_export_fail, Toast.LENGTH_SHORT).show()
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

    // 교체/추가 선택 다이얼로그 — 현재 레코드 수 표시 후 모드 선택
    private fun showRestoreModeDialog(uri: Uri) {
        lifecycleScope.launch {
            val currentCount = withContext(Dispatchers.IO) { viewModel.getCurrentRecordCount() }

            val dlg = BottomSheetDialog(requireContext(), R.style.transparentDialog)
            val dlgBinding = DialogRestoreModeBinding.inflate(LayoutInflater.from(requireContext()))
            dlg.setContentView(dlgBinding.root)

            dlgBinding.tvRestoreCurrentCount.text =
                getString(R.string.datamgnt_restore_current_count, currentCount)

            dlgBinding.llRestoreReplace.setOnClickListener {
                viewModel.restore(uri, RestoreMode.REPLACE)
                dlg.dismiss()
            }
            dlgBinding.llRestoreMerge.setOnClickListener {
                viewModel.restore(uri, RestoreMode.MERGE)
                dlg.dismiss()
            }
            dlgBinding.tvRestoreCancel.setOnClickListener { dlg.dismiss() }

            dlg.show()
        }
    }
}
