package com.example.ui.screens

import com.example.util.toDoubleOrNullLocalized
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
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
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters

@Composable
fun BullionScreen(
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

    val totalReferenceCost = state.bullionList.sumOf { it.purchaseCostEgp ?: 0.0 }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldPrimary,
                contentColor = Color(0xFF1E1700),
                modifier = Modifier.testTag("add_bullion_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة سبيكة")
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
                // Bullion Portfolio Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bullion_summary_card"),
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
                                text = "محفظة سبائك BTC عيار 24",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ProfitGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مغلفة وسليمة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ProfitGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "إجمالي وزن السبائك", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = Formatters.formatWeight(state.totalBullionWeight),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "القيمة التقديرية (شراء الصائغ)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = Formatters.formatCurrency(state.bullionLiquidation),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "عدد القطع: ${state.bullionList.size} سبائك",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = "التكلفة المرجعية: ${Formatters.formatCurrency(totalReferenceCost)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "قائمة السبائك المحفوظة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
            }

            // List of bullion bars
            items(state.bullionList, key = { it.id }) { item ->
                val estimatedValue = PortfolioCalculator.calculateItemLiquidation(
                    item,
                    state.activeGram24BuyPrice,
                    state.activeGram21BuyPrice,
                    state.activeGram18BuyPrice
                )

                BullionItemCard(
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
        BullionDialog(
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
                Text("هل أنت متأكد من حذف ${itemToDelete!!.title} (وزن: ${itemToDelete!!.weightGrams} جم) من محفظة السبائك؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_delete_bullion_button")
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
fun BullionItemCard(
    item: GoldItem,
    estimatedValue: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bullion_item_${item.id}"),
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
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "24K",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
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
                            text = "${item.brand} • ${item.condition}",
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
                    Text(text = "الوزن الدقيق", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = Formatters.formatWeight(item.weightGrams),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "تكلفة الشراء المرجعية", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    val itemCost = item.cost
                    Text(
                        text = if (itemCost != null) Formatters.formatCurrency(itemCost) else "غير محددة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "تصفية الصائغ التقديرية", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
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
fun BullionDialog(
    initialItem: GoldItem?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, weight: Double, karat: Int, cost: Double?, condition: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "سبيكة BTC عيار 24") }
    var weightStr by remember { mutableStateOf(initialItem?.weightGrams?.toString() ?: "10.0") }
    var karatStr by remember { mutableStateOf(initialItem?.karat?.toString() ?: "24") }
    var costStr by remember { mutableStateOf(initialItem?.purchaseCostEgp?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var condition by remember { mutableStateOf(initialItem?.condition ?: "مغلفة وسليمة") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "إضافة سبيكة ذهب جديدة" else "تعديل سبيكة الذهب", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم السبيكة أو الوصف") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("الوزن بالجرام (مثال: 10 أو 31.1035)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = karatStr,
                        onValueChange = { karatStr = it },
                        label = { Text("العيار (24)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("الحالة (مغلفة وسليمة)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("تكلفة الشراء المرجعية ج.م (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية أو رقم أمني") },
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
                    val karat = karatStr.toIntOrNull() ?: 24
                    val cost = costStr.toDoubleOrNullLocalized()

                    if (weight == null || weight <= 0.0) {
                        errorMessage = "يرجى إدخال وزن صحيح أكبر من صفر"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "يرجى إدخال اسم أو وصف السبيكة"
                        return@Button
                    }
                    onConfirm(title, weight, karat, cost, condition, notes)
                },
                modifier = Modifier.testTag("dialog_confirm_bullion_button")
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
