package com.beinny.android.photorecord.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.beinny.android.photorecord.model.Record
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GalleryExporter {

    // 레코드 원본 사진을 갤러리 공용 폴더로 내보내기 — 성공 개수 반환
    fun export(
        context: Context,
        filesDir: File,
        records: List<Record>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Int {
        val folderName = "PhotoRecord_${
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        }"
        val photoRecords = records.filter { File(filesDir, it.photoFileName).exists() }
        var successCount = 0

        photoRecords.forEachIndexed { index, record ->
            val srcFile = File(filesDir, record.photoFileName)
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportViaMediaStore(context, srcFile, folderName)
            } else {
                exportToExternalStorage(context, srcFile, folderName)
            }
            if (success) successCount++
            onProgress(index + 1, photoRecords.size)
        }
        return successCount
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportViaMediaStore(context: Context, srcFile: File, folderName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, srcFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoRecord/$folderName")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                srcFile.inputStream().use { it.copyTo(out) }
            }
            true
        } catch (e: Exception) { false }
    }

    private fun exportToExternalStorage(context: Context, srcFile: File, folderName: String): Boolean {
        return try {
            val dir = File(
                @Suppress("DEPRECATION")
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PhotoRecord/$folderName"
            )
            dir.mkdirs()
            val destFile = File(dir, srcFile.name)
            srcFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context, arrayOf(destFile.path), arrayOf("image/jpeg"), null
            )
            true
        } catch (e: Exception) { false }
    }
}
