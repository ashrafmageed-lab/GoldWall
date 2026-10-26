package com.example.ui.screens

import com.example.util.toDoubleOrNullLocalized
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculations.PortfolioCalculator
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters

@Composable
fun JewelryScreen(
    state: PortfolioUiState,
    onBack: () -> Unit,
    onAddItem: (title: String, weight: Double, karat: Int, cost: Double?, condition: String, notes: String) -> Unit,
    onUpdateItem: (GoldItem) -> Unit,
    onDeleteItem: (GoldItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GoldItem?>(null) }
    var itemToDelete by remember { mutableStateOf<GoldItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldPrimary,
                contentColor = Color(0xFF1E1700),
                modifier = Modifier.testTag("add_jewelry_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة مشغولات")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Jewelry Portfolio Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jewelry_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "محفظة المشغولات الذهبية (عيار 21)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E3A5F)
                            ) {
                                Text(
                                    text = "ذهب كسر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF90CAF9),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "إجمالي وزن المشغولات", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = Formatters.formatWeight(state.totalJewelryWeight),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "القيمة التقديرية (شراء الصائغ)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = Formatters.formatCurrency(state.jewelryLiquidation),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Disclaimer Note
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تكلفة الشراء غير مسجلة. التقييم يحسب بسعر جرام عيار 21 شراء الصائغ (${Formatters.formatCurrency(state.activeGram21BuyPrice)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "قائمة المشغولات والحلي",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
            }

            // List of jewelry items
            items(state.jewelryList, key = { it.id }) { item ->
                val estimatedValue = PortfolioCalculator.calculateItemLiquidation(
                    item,
                    state.activeGram24BuyPrice,
                    state.activeGram21BuyPrice,
                    state.activeGram18BuyPrice
                )

                JewelryItemCard(
                    item = item,
                    estimatedValue = estimatedValue,
                    onEdit = { itemToEdit = item },
                    onDelete = { itemToDelete = item }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || itemToEdit != null) {
        JewelryDialog(
            initialItem = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onConfirm = { title, weight, karat, cost, condition, notes ->
                if (itemToEdit != null) {
                    onUpdateItem(
                        itemToEdit!!.copy(
                            name = title,
                            weight = weight,
                            karat = karat,
                            cost = cost,
                            status = condition,
                            notes = notes
                        )
                    )
                } else {
                    onAddItem(title, weight, karat, cost, condition, notes)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف ${itemToDelete!!.title} (وزن: ${itemToDelete!!.weightGrams} جم) من محفظة المشغولات؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_delete_jewelry_button")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun JewelryItemCard(
    item: GoldItem,
    estimatedValue: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("jewelry_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A5F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.karat}K",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF90CAF9)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.condition} • عيار ${item.karat}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = LossRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "الوزن", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = Formatters.formatWeight(item.weightGrams),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "تكلفة الشراء", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    val itemCost = item.cost
                    Text(
                        text = if (itemCost != null) Formatters.formatCurrency(itemCost) else "غير محددة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "قيمة التصفية (شراء الصائغ)", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                    Text(
                        text = Formatters.formatCurrency(estimatedValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                }
            }

            if (item.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun JewelryDialog(
    initialItem: GoldItem?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, weight: Double, karat: Int, cost: Double?, condition: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "مشغولات ذهبية عيار 21") }
    var weightStr by remember { mutableStateOf(initialItem?.weightGrams?.toString() ?: "64.900") }
    var karatStr by remember { mutableStateOf(initialItem?.karat?.toString() ?: "21") }
    var costStr by remember { mutableStateOf(initialItem?.purchaseCostEgp?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var condition by remember { mutableStateOf(initialItem?.condition ?: "سليمة") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "إضافة مشغولات ذهبية جديدة" else "تعديل المشغولات الذهبية", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم القطعة أو الوصف (مثال: أساور، سلاسل)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("الوزن بالجرام (مثال: 64.900)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = karatStr,
                        onValueChange = { karatStr = it },
                        label = { Text("العيار (21 أو 18)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("الحالة") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("تكلفة الشراء الأصلية ج.م (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightStr.toDoubleOrNullLocalized()
                    val karat = karatStr.toIntOrNull() ?: 21
                    val cost = costStr.toDoubleOrNullLocalized()

                    if (weight == null || weight <= 0.0) {
                        errorMessage = "يرجى إدخال وزن صحيح أكبر من صفر"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "يرجى إدخال وصف المشغولات"
                        return@Button
                    }
                    onConfirm(title, weight, karat, cost, condition, notes)
                },
                modifier = Modifier.testTag("dialog_confirm_jewelry_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
