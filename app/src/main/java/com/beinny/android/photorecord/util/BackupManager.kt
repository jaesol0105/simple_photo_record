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

    private data class RecordJson(
        val id: String,
        val label: String,
        val memo: String,
        val date: Long,
        val isNew: Boolean
    )

    fun backup(context: Context, uri: Uri, records: List<Record>, filesDir: File): Boolean {
        return try {
            val json = gson.toJson(records.map {
                RecordJson(it.id.toString(), it.label, it.memo, it.date.time, it.isNew)
            })

            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                    zip.putNextEntry(ZipEntry("records.json"))
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    records.forEach { record ->
                        listOf(record.photoFileName, record.thumbFileName).forEach { name ->
                            val file = File(filesDir, name)
                            if (file.exists()) {
                                zip.putNextEntry(ZipEntry("images/$name"))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restore(context: Context, uri: Uri, filesDir: File): List<Record>? {
        return try {
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
                                File(filesDir, name).outputStream().use { zip.copyTo(it) }
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
            null
        }
    }
}
