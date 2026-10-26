package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Utility for copying picked photos to internal app storage so that
 * file references persist permanently without URI permission expiration.
 */
object MediaUtils {
    fun saveImageLocally(context: Context, sourceUri: Uri, prefix: String): String? {
        return try {
            val dir = File(context.filesDir, "gold_media").apply { mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
