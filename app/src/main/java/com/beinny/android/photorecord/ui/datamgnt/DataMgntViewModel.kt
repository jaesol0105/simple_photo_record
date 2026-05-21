package com.beinny.android.photorecord.ui.datamgnt

import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beinny.android.photorecord.repository.recorddetail.RecordRepository
import com.beinny.android.photorecord.ui.common.SingleLiveEvent
import com.beinny.android.photorecord.util.BackupManager
import com.beinny.android.photorecord.util.GalleryExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

enum class RestoreMode { REPLACE, MERGE }
enum class ActiveOperation { NONE, BACKUP, RESTORE, EXPORT }

data class LastBackupInfo(val uri: Uri, val date: Long, val fileName: String)

class DataMgntViewModel(
    private val recordRepository: RecordRepository,
    private val appContext: Context
) : ViewModel() {

    val isLoading = MutableLiveData(false)
    val backupResult = SingleLiveEvent<Result<Unit>>()
    val restoreResult = SingleLiveEvent<Result<Int>>()
    val exportResult = SingleLiveEvent<Result<Int>>()
    val lastBackupInfo = MutableLiveData<LastBackupInfo?>()

    // 진행률 (0-100), null = indeterminate (복원 등 퍼센트 측정 불가)
    val progress = MutableLiveData<Int?>(null)
    val progressLabel = MutableLiveData<String>("")
    val activeOperation = MutableLiveData(ActiveOperation.NONE)

    init {
        loadLastBackupInfo()
    }

    // 백업 — 임시 파일에 완성 후 목적지 uri로 복사하여 손상 방지
    // 백업 — 임시 파일에 완성 후 목적지 uri로 복사하여 손상 방지
    fun backup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            activeOperation.postValue(ActiveOperation.BACKUP)
            progress.postValue(0)
            val records = recordRepository.getAllRecordsSync()
            val success = BackupManager.backup(appContext, uri, records, appContext.filesDir) { cur, total ->
                val pct = if (total > 0) cur * 100 / total else 100
                progress.postValue(pct)
                progressLabel.postValue("백업 중  $cur / $total")
            }
            if (success) {
                saveLastBackupUri(uri)
                backupResult.postValue(Result.success(Unit))
            } else {
                backupResult.postValue(Result.failure(Exception()))
            }
            progress.postValue(null)
            activeOperation.postValue(ActiveOperation.NONE)
            isLoading.postValue(false)
        }
    }

    // 복원 — 임시 디렉토리 → DB 트랜잭션 → 이미지 이동 순서로 원자성 보장
    fun restore(uri: Uri, mode: RestoreMode) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            activeOperation.postValue(ActiveOperation.RESTORE)
            progress.postValue(null)
            progressLabel.postValue("복원 중…")

            // 1단계: zip 추출 → 임시 디렉토리
            val records = BackupManager.extractToTemp(appContext, uri, appContext.filesDir)
            if (records == null) {
                restoreResult.postValue(Result.failure(Exception()))
                isLoading.postValue(false)
                return@launch
            }

            try {
                // 2단계: DB 작업 (트랜잭션 또는 IGNORE)
                when (mode) {
                    RestoreMode.REPLACE -> recordRepository.replaceAll(records)
                    RestoreMode.MERGE   -> recordRepository.insertAllOrIgnore(records)
                }
                // 3단계: 이미지 이동 (성공)
                BackupManager.commitImages(
                    appContext.filesDir,
                    overwrite = (mode == RestoreMode.REPLACE)
                )
                restoreResult.postValue(Result.success(records.size))
            } catch (e: Exception) {
                // DB 실패 → 임시 파일 롤백, 원본 filesDir 유지
                BackupManager.rollbackImages(appContext.filesDir)
                restoreResult.postValue(Result.failure(e))
            }

            progress.postValue(null)
            activeOperation.postValue(ActiveOperation.NONE)
            isLoading.postValue(false)
        }
    }

    // 레코드 원본 사진을 기기 갤러리로 내보내기 — 성공 개수를 exportResult로 전달
    fun exportToGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            activeOperation.postValue(ActiveOperation.EXPORT)
            progress.postValue(0)
            try {
                val records = recordRepository.getAllRecordsSync()
                val count = GalleryExporter.export(appContext, appContext.filesDir, records) { cur, total ->
                    val pct = if (total > 0) cur * 100 / total else 100
                    progress.postValue(pct)
                    progressLabel.postValue("내보내기 중  $cur / $total")
                }
                exportResult.postValue(Result.success(count))
            } catch (e: Exception) {
                exportResult.postValue(Result.failure(e))
            }
            progress.postValue(null)
            activeOperation.postValue(ActiveOperation.NONE)
            isLoading.postValue(false)
        }
    }

    // 현재 DB 레코드 수 반환 — 복원 다이얼로그 표시 전 사용
    suspend fun getCurrentRecordCount(): Int =
        recordRepository.getAllRecordsSync().size

    // 마지막 백업 URI 반환 — 접근 불가 시 null
    fun getLastBackupUri(): Uri? {
        val uriString = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URI, null) ?: return null
        val uri = Uri.parse(uriString)
        return if (isUriAccessible(uri)) uri else null
    }

    private fun isUriAccessible(uri: Uri): Boolean = try {
        appContext.contentResolver.openInputStream(uri)?.close()
        true
    } catch (e: Exception) { false }

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
                // Downloads 폴더 (Android 10+): msf:<MediaStore ID>
                docId.startsWith("msf:") -> {
                    val id = docId.removePrefix("msf:").toLongOrNull()
                    mediaStoreDownloadPath(id) ?: "/storage/emulated/0/Download/${displayName(uri)}"
                }
                docId.contains(":") -> {
                    val (volume, path) = docId.split(":", limit = 2)
                    "/storage/$volume/$path"
                }
                // Downloads 폴더 (older): numeric DownloadManager ID
                else -> downloadManagerPath(docId)
                    ?: if (uri.authority == "com.android.providers.downloads.documents")
                        "/storage/emulated/0/Download/${displayName(uri)}"
                    else displayName(uri)
            }
        } catch (e: Exception) {
            displayName(uri)
        }
    }

    private fun mediaStoreDownloadPath(id: Long?): String? {
        if (id == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            appContext.contentResolver.query(
                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                arrayOf(MediaStore.MediaColumns.DATA),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) { null }
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
