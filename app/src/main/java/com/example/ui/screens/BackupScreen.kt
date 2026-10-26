package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.backup.BackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(
    state: PortfolioUiState,
    onBack: () -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: (json: String, onResult: (Result<Int>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportedJson by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(true) }

    // Save the backup as a real .json file through the system file picker (no clipboard exposure).
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onExportBackup()
                val saved = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri, "wt")?.use {
                            it.write(json.toByteArray(Charsets.UTF_8))
                        } != null
                    } catch (e: Exception) {
                        false
                    }
                }
                feedbackMessage = if (saved) "تم حفظ النسخة الاحتياطية في الملف بنجاح" else "تعذّر حفظ الملف، جرّب موقعاً آخر"
                isSuccessMessage = saved
            }
        }
    }

    // Pick a backup file; it is validated and confirmed before anything is replaced.
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readNBytesCompat(BackupCodec.MAX_FILE_CHARS * 2 + 1)
                            if (bytes.size > BackupCodec.MAX_FILE_CHARS * 2) null else String(bytes, Charsets.UTF_8)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (text.isNullOrBlank()) {
                    feedbackMessage = "تعذّرت قراءة الملف أو أنه كبير جداً"
                    isSuccessMessage = false
                } else {
                    importJsonText = text
                    showImportConfirmDialog = true
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي والاستعادة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يتم حفظ جميع بياناتك محلياً على جهازك فقط. يمكنك تصدير نسخة احتياطية كاملة بصيغة JSON لحفظها أو نقلها إلى جهاز آخر بأمان تام.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Export Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تصدير نسخة احتياطية (Export JSON)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )

                    Button(
                        onClick = { createFileLauncher.launch("gold-vault-backup.json") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ النسخة الاحتياطية كملف")
                    }

                    Text(
                        text = "النسخ إلى الحافظة أقل أماناً: قد تقرأها تطبيقات أخرى، ولذلك تُمسح تلقائياً بعد دقيقة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = onExportBackup()
                                exportedJson = json
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gold Vault Backup", json)
                                clipboard.setPrimaryClip(clip)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            clipboard.clearPrimaryClip()
                                        } else {
                                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                                        }
                                    } catch (e: Exception) {
                                        // Clipboard may be unavailable in the background.
                                    }
                                }, 60_000L)
                                feedbackMessage = "تم نسخ النسخة الاحتياطية للحافظة وستُمسح تلقائياً بعد دقيقة"
                                isSuccessMessage = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ إلى الحافظة (مؤقت)")
                    }

                    if (exportedJson.isNotEmpty()) {
                        OutlinedTextField(
                            value = exportedJson,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("محتوى ملف النسخة الاحتياطية") },
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Import Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "استعادة نسخة احتياطية (Import JSON)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                    Text(
                        text = "الصق كود الـ JSON هنا لاستعادة بيانات محفظتك. يتم التحقق من سلامة وصحة الملف أولاً قبل الاعتماد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = { openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف نسخة احتياطية")
                    }

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("الصق نص ملف الـ JSON هنا") },
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_json_input")
                    )

                    Button(
                        onClick = {
                            if (importJsonText.isBlank()) {
                                feedbackMessage = "يرجى لصق نص النسخة الاحتياطية أولاً"
                                isSuccessMessage = false
                                return@Button
                            }
                            showImportConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_backup_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التحقق واستعادة البيانات")
                    }
                }
            }
        }

        if (feedbackMessage != null) {
            item {
                Text(
                    text = feedbackMessage!!,
                    color = if (isSuccessMessage) ProfitGreen else LossRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("سيتم التحقق من الملف أولاً. إن كان سليماً تُستبدل عناصر المحفظة الحالية (وسجل التقييمات والعمليات إن وُجد في الملف) بمحتواه، بعد حفظ نسخة تلقائية من محفظتك الحالية. وإن كان الملف تالفاً فلن يتغير شيء. هل تريد المتابعة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmDialog = false
                        onImportBackup(importJsonText) { result ->
                            if (result.isSuccess) {
                                feedbackMessage = "تمت استعادة ${result.getOrNull()} عنصر بنجاح!"
                                isSuccessMessage = true
                                importJsonText = ""
                            } else {
                                feedbackMessage = "فشل استيراد النسخة الاحتياطية: ${result.exceptionOrNull()?.message}"
                                isSuccessMessage = false
                            }
                        }
                    }
                ) {
                    Text("نعم، استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/** InputStream.readNBytes needs API 33; this bounded read works on every supported API level. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
