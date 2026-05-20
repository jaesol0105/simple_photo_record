package com.beinny.android.photorecord.util

import android.content.Context
import android.net.Uri
import com.beinny.android.photorecord.model.Record
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.util.*
import java.util.zip.*

object BackupManager {

    private val gson = Gson()
    private const val TEMP_DIR_NAME = "restore_tmp"

    private data class RecordJson(
        val id: String,
        val label: String,
        val memo: String,
        val date: Long,
        val isNew: Boolean
    )

    // 백업 — cacheDir 임시 파일에 먼저 zip 완성 후 목적지 uri로 복사 (손상 방지)
    fun backup(
        context: Context,
        uri: Uri,
        records: List<Record>,
        filesDir: File,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean {
        val tempFile = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}.zip")
        return try {
            tempFile.outputStream().use { fileOut ->
                ZipOutputStream(BufferedOutputStream(fileOut)).use { zip ->
                    val json = gson.toJson(records.map {
                        RecordJson(it.id.toString(), it.label, it.memo, it.date.time, it.isNew)
                    })
                    zip.putNextEntry(ZipEntry("records.json"))
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    records.forEachIndexed { i, record ->
                        listOf(record.photoFileName, record.thumbFileName).forEach { name ->
                            val file = File(filesDir, name)
                            if (file.exists()) {
                                zip.putNextEntry(ZipEntry("images/$name"))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                        onProgress(i + 1, records.size)
                    }
                }
            }
            // zip 완성 후 목적지로 복사
            context.contentResolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            tempFile.delete()
        }
    }

    // 복원 1단계 — zip에서 이미지를 임시 디렉토리로 추출하고 레코드 목록 반환
    fun extractToTemp(context: Context, uri: Uri, filesDir: File): List<Record>? {
        val tempDir = getTempDir(filesDir)
        return try {
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            var jsonContent: String? = null

            context.contentResolver.openInputStream(uri)?.use { inStream ->
                ZipInputStream(BufferedInputStream(inStream)).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        when {
                            entry.name == "records.json" -> {
                                jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith("images/") -> {
                                val name = entry.name.removePrefix("images/")
                                File(tempDir, name).outputStream().use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }

            jsonContent?.let { json ->
                val type = object : TypeToken<List<RecordJson>>() {}.type
                val list: List<RecordJson> = gson.fromJson(json, type)
                list.map { rj ->
                    Record(
                        id = UUID.fromString(rj.id),
                        label = rj.label,
                        memo = rj.memo,
                        date = Date(rj.date),
                        isNew = rj.isNew,
                        isChecked = false
                    )
                }
            }
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            null
        }
    }

    // 복원 2단계 (성공) — 임시 디렉토리 이미지를 filesDir로 이동
    fun commitImages(filesDir: File, overwrite: Boolean) {
        val tempDir = getTempDir(filesDir)
        tempDir.listFiles()?.forEach { file ->
            val dest = File(filesDir, file.name)
            if (overwrite || !dest.exists()) {
                file.copyTo(dest, overwrite = overwrite)
            }
        }
        tempDir.deleteRecursively()
    }

    // 복원 2단계 (실패) — 임시 디렉토리 삭제, filesDir 원본 유지
    fun rollbackImages(filesDir: File) {
        getTempDir(filesDir).deleteRecursively()
    }

    private fun getTempDir(filesDir: File) = File(filesDir, TEMP_DIR_NAME)
}
