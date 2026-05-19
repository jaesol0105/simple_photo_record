package com.beinny.android.photorecord.ui.datamgnt

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.common.SingleLiveEvent
import com.beinny.android.photorecord.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LastBackupInfo(val uri: Uri, val date: Long, val fileName: String)

class DataMgntViewModel(
    private val recordRepository: RecordRepository,
    private val appContext: Context
) : ViewModel() {

    val isLoading = MutableLiveData(false)
    val backupResult = SingleLiveEvent<Result<Unit>>()
    val restoreResult = SingleLiveEvent<Result<Int>>()
    val lastBackupInfo = MutableLiveData<LastBackupInfo?>()

    init {
        loadLastBackupInfo()
    }

    fun backup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            val records = recordRepository.getAllRecordsSync()
            val success = BackupManager.backup(appContext, uri, records, appContext.filesDir)
            if (success) {
                saveLastBackupUri(uri)
                backupResult.postValue(Result.success(Unit))
            } else {
                backupResult.postValue(Result.failure(Exception()))
            }
            isLoading.postValue(false)
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            val records = BackupManager.restore(appContext, uri, appContext.filesDir)
            if (records != null) {
                recordRepository.deleteAllRecord()
                recordRepository.insertAll(records)
                restoreResult.postValue(Result.success(records.size))
            } else {
                restoreResult.postValue(Result.failure(Exception()))
            }
            isLoading.postValue(false)
        }
    }

    fun restoreFromLastBackup() {
        val uriString = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URI, null) ?: return
        restore(Uri.parse(uriString))
    }

    private fun saveLastBackupUri(uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) { /* 권한 취득 실패 시 무시 */ }

        val displayPath = resolveDisplayPath(uri)

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_URI, uri.toString())
            .putLong(KEY_DATE, System.currentTimeMillis())
            .putString(KEY_NAME, displayPath)
            .apply()

        loadLastBackupInfo()
    }

    private fun loadLastBackupInfo() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_URI, null)
        val date = prefs.getLong(KEY_DATE, 0L)
        val name = prefs.getString(KEY_NAME, null)

        lastBackupInfo.postValue(
            if (uriString != null && name != null) LastBackupInfo(Uri.parse(uriString), date, name)
            else null
        )
    }

    private fun resolveDisplayPath(uri: Uri): String {
        return try {
            val decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8")
            val docId = decoded.substringAfter("/document/", "").takeIf { it.isNotEmpty() }
                ?: return displayName(uri)
            when {
                docId.startsWith("primary:") ->
                    "/storage/emulated/0/${docId.removePrefix("primary:")}"
                docId.startsWith("raw:") ->
                    docId.removePrefix("raw:")
                docId.contains(":") -> {
                    val (volume, path) = docId.split(":", limit = 2)
                    "/storage/$volume/$path"
                }
                else -> downloadManagerPath(docId) ?: displayName(uri)
            }
        } catch (e: Exception) {
            displayName(uri)
        }
    }

    private fun downloadManagerPath(docId: String): String? {
        val id = docId.toLongOrNull() ?: return null
        return try {
            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        ?.let { Uri.parse(it).path }
                } else null
            }
        } catch (e: Exception) { null }
    }

    private fun displayName(uri: Uri): String =
        appContext.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null } ?: uri.toString()

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_URI = "last_backup_uri"
        private const val KEY_DATE = "last_backup_date"
        private const val KEY_NAME = "last_backup_name"
    }
}
