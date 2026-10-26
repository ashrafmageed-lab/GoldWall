package com.example.data.backup

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Automatic local safety copies taken before any destructive operation (restore). */
object BackupFiles {
    private const val DIR = "backups"
    const val KEEP_LAST = 5

    fun save(context: Context, prefix: String, json: String): File? = try {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "$prefix-$stamp.json")
        file.writeText(json, Charsets.UTF_8)
        prune(dir)
        file
    } catch (e: Exception) {
        null
    }

    fun list(context: Context): List<File> =
        File(context.filesDir, DIR).listFiles { f -> f.isFile && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    private fun prune(dir: File) {
        dir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP_LAST)
            ?.forEach { it.delete() }
    }
}
