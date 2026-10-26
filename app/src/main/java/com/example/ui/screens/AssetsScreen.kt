package com.example.ui.screens

import com.example.util.toDoubleOrNullLocalized
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import coil.compose.AsyncImage
import com.example.util.MediaUtils
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.security.BiometricAuth
import com.example.security.LocalPinVerifier
import com.example.security.PinHasher
import com.example.security.PinResult
import com.example.security.findFragmentActivity
import com.example.security.toUserMessage
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.LossRedContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    state: PortfolioUiState,
    onAddItem: (title: String, type: GoldType, weight: Double, karat: Int, cost: Double?, brand: String, condition: String, notes: String, barcode: String?, imageUri: String?, invoiceUri: String?) -> Unit,
    onUpdateItem: (GoldItem) -> Unit,
    onDeleteItem: (GoldItem) -> Unit,
    onSellItem: (item: GoldItem, weightSoldGrams: Double, sellPricePerGram: Double, cashBackReceived: Double, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Bullion, 1 = Jewelry
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GoldItem?>(null) }
    var itemToDelete by remember { mutableStateOf<GoldItem?>(null) }
    var itemToSell by remember { mutableStateOf<GoldItem?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldPrimary,
                contentColor = Color(0xFF1E1700),
                modifier = Modifier.testTag("add_asset_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة أصل ذهبي جديد"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Assets Overview Summary Card
                AssetsSummaryHeaderCard(state = state)
            }

            // Tabs for Bullion vs Jewelry
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "السبائك (${state.bullionList.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_bullions")
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "المشغولات (${state.jewelryList.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_jewelry")
                    )
                }
            }

            val currentItems = if (selectedTab == 0) state.bullionList else state.jewelryList

            if (currentItems.isEmpty()) {
                item {
                    EmptyAssetsCard(
                        type = if (selectedTab == 0) GoldType.BULLION else GoldType.JEWELRY,
                        onAddClick = { showAddDialog = true }
                    )
                }
            } else {
                items(currentItems, key = { it.id }) { item ->
                    val estimatedLiquidation = PortfolioCalculator.calculateItemLiquidation(
                        item,
                        state.activeGram24BuyPrice,
                        state.activeGram21BuyPrice,
                        state.activeGram18BuyPrice
                    )

                    val itemCashBack = if (PortfolioCalculator.isCashBackEligible(item) && (item.cashBackRatePerGram ?: 0.0) > 0.0) {
                        item.weight * (item.cashBackRatePerGram ?: 28.0)
                    } else 0.0

                    AssetItemCard(
                        item = item,
                        estimatedLiquidation = estimatedLiquidation,
                        cashBack = itemCashBack,
                        onSell = { itemToSell = item },
                        onEdit = { itemToEdit = item },
                        onDelete = { itemToDelete = item },
                        onPreviewImage = { title, path ->
                            previewImageTitle = title
                            previewImagePath = path
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 1. Smart Sell Dialog
    if (itemToSell != null) {
        val item = itemToSell!!
        val buyPricePerGram = when (item.karat) {
            24 -> state.activeGram24BuyPrice
            21 -> state.activeGram21BuyPrice
            else -> state.activeGram18BuyPrice
        }
        val cashBackRate = if (PortfolioCalculator.isCashBackEligible(item)) (item.cashBackRatePerGram ?: 28.0) else 0.0

        SmartSellDialog(
            item = item,
            currentBuyPricePerGram = buyPricePerGram,
            cashBackRatePerGram = cashBackRate,
            requireAuth = state.settings.requireAuthOnSale,
            hasPin = PinHasher.hasPin(state.settings.securityPin),
            isBiometricEnabled = state.settings.isBiometricEnabled,
            onDismiss = { itemToSell = null },
            onConfirmSell = { weightSold, sellPrice, cashBack, notes ->
                onSellItem(item, weightSold, sellPrice, cashBack, notes)
                itemToSell = null
            }
        )
    }

    // 2. Add / Edit Dialog
    if (showAddDialog || itemToEdit != null) {
        AssetEditDialog(
            initialItem = itemToEdit,
            defaultType = if (selectedTab == 0) GoldType.BULLION else GoldType.JEWELRY,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onConfirm = { title, type, weight, karat, cost, brand, condition, notes, barcode, imageUri, invoiceUri ->
                if (itemToEdit != null) {
                    onUpdateItem(
                        itemToEdit!!.copy(
                            name = title,
                            type = type,
                            weight = weight,
                            karat = karat,
                            cost = cost,
                            brand = brand,
                            status = condition,
                            notes = notes,
                            barcode = barcode,
                            imageUri = imageUri,
                            invoiceUri = invoiceUri
                        )
                    )
                } else {
                    onAddItem(title, type, weight, karat, cost, brand, condition, notes, barcode, imageUri, invoiceUri)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // 3. Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("تأكيد حذف الأصل", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف \"${itemToDelete!!.name}\" (وزن ${Formatters.formatWeight(itemToDelete!!.weight)}) نهائياً من المحفظة؟\nإذا كنت قد قمت ببيعها، يفضل استخدام زر (بيع ذكي) لتوثيق الأرباح وسجل المعاملات.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_delete_asset_button")
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 4. Attachment Photo Preview Modal
    if (previewImagePath != null) {
        ImagePreviewModal(
            title = previewImageTitle ?: "معاينة الصورة",
            imagePath = previewImagePath!!,
            onDismiss = {
                previewImagePath = null
                previewImageTitle = null
            }
        )
    }
}

@Composable
fun AssetsSummaryHeaderCard(state: PortfolioUiState) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF221C0E), Color(0xFF191611))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("assets_summary_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي أصول الذهب المحفوظة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
                Text(
                    text = "${Formatters.formatWeight(state.totalPhysicalWeight)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bullion Summary Mini Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السبائك (24k)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatWeight(state.totalBullionWeight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.bullionList.size} قطع • ${Formatters.formatCurrency(state.bullionLiquidation)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldSecondary
                        )
                    }
                }

                // Jewelry Summary Mini Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المشغولات (21k/18k)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatWeight(state.totalJewelryWeight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.jewelryList.size} قطع • ${Formatters.formatCurrency(state.jewelryLiquidation)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetItemCard(
    item: GoldItem,
    estimatedLiquidation: Double,
    cashBack: Double,
    onSell: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPreviewImage: (title: String, path: String) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("asset_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Thumbnail (if present) + Karat Badge + Title + Brand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!item.imageUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { onPreviewImage("صورة القطعة: ${item.name}", item.imageUri) }
                        ) {
                            AsyncImage(
                                model = File(item.imageUri).takeIf { it.exists() } ?: item.imageUri,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.karat}K",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${item.brand} • ${item.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Edit and Delete icons
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

            // Details Surface
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("الوزن الصافي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = Formatters.formatWeight(item.weight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تكلفة الشراء", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = if (item.cost != null) Formatters.formatCurrency(item.cost) else "غير محددة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("سعر التسييل الحالي", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                        Text(
                            text = Formatters.formatCurrency(estimatedLiquidation),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldSecondary
                        )
                    }
                }
            }

            // Attached Barcode & Invoice Badges
            if (!item.barcode.isNullOrBlank() || !item.invoiceUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.barcode.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "باركود: ${item.barcode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (!item.invoiceUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { onPreviewImage("فاتورة شراء: ${item.name}", item.invoiceUri) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "فاتورة الشراء",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (cashBack > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كاش باك سبيكة معتمد (28 ج.م/جم):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProfitGreen
                    )
                    Text(
                        text = "+${Formatters.formatCurrency(cashBack)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Sell Action Button
            Button(
                onClick = onSell,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color(0xFF1E1700)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("smart_sell_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بيع ذكي / تسييل جزئي أو كلي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SmartSellDialog(
    item: GoldItem,
    currentBuyPricePerGram: Double,
    cashBackRatePerGram: Double,
    requireAuth: Boolean = true,
    hasPin: Boolean = false,
    isBiometricEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onConfirmSell: (weightSold: Double, sellPricePerGram: Double, cashBack: Double, notes: String) -> Unit
) {
    var weightToSellStr by remember { mutableStateOf(item.weight.toString()) }
    var sellPriceStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", currentBuyPricePerGram)) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAuthConfirmDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    val verifyPin = LocalPinVerifier.current
    val authScope = rememberCoroutineScope()
    val authContext = LocalContext.current
    val authActivity = authContext.findFragmentActivity()
    val biometricReady = isBiometricEnabled && authActivity != null && BiometricAuth.isAvailable(authContext)
    // Auth can only be demanded when there is a real factor to check (a chosen PIN or a biometric).
    val authRequired = requireAuth && (hasPin || biometricReady)

    val weightToSell = weightToSellStr.toDoubleOrNullLocalized() ?: 0.0
    val sellPrice = sellPriceStr.toDoubleOrNullLocalized() ?: currentBuyPricePerGram
    val isBullion = item.type == GoldType.BULLION

    val goldLiquidationAmount = weightToSell * sellPrice
    val cashBackAmount = if (isBullion && cashBackRatePerGram > 0.0) weightToSell * cashBackRatePerGram else 0.0
    val totalCashReceived = goldLiquidationAmount + cashBackAmount

    val costBasis = if (item.cost != null && item.weight > 0) {
        (item.cost / item.weight) * weightToSell
    } else null
    val profit = if (costBasis != null) totalCashReceived - costBasis else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("بيع وتسييل ذكي: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "يمكنك بيع جزء من الوزن أو السبيكة بالكامل. سيتم احتساب الكاش باك وتحديث رصيد المحفظة وتسجيل قيد في سجل العمليات تلقائياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = weightToSellStr,
                    onValueChange = {
                        weightToSellStr = it
                        val parsed = it.toDoubleOrNullLocalized()
                        if (parsed == null || parsed <= 0 || parsed > item.weight + 0.0001) {
                            error = "الوزن يجب أن يكون بين 0.1 و ${item.weight} جم"
                        } else {
                            error = null
                        }
                    },
                    label = { Text("الوزن المراد بيعه (جم) - المتاح: ${item.weight} جم") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = LossRed) } },
                    modifier = Modifier.fillMaxWidth().testTag("sell_weight_input")
                )

                OutlinedTextField(
                    value = sellPriceStr,
                    onValueChange = { sellPriceStr = it },
                    label = { Text("سعر جرام الشراء المتفق عليه (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("sell_price_input")
                )

                // Breakdown Calculation Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ذهب خالص (${weightToSell} جم):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(Formatters.formatCurrency(goldLiquidationAmount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        if (cashBackAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("+ كاش باك مسترد ($cashBackRatePerGram ج.م/جم):", style = MaterialTheme.typography.labelSmall, color = ProfitGreen)
                                Text("+${Formatters.formatCurrency(cashBackAmount)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي النقدية المحصلة:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Text(Formatters.formatCurrency(totalCashReceived), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = GoldSecondary)
                        }

                        if (profit != null) {
                            val isPos = profit >= 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("صافي ${if (isPos) "الربح المحقق" else "الخسارة"}:", style = MaterialTheme.typography.labelSmall, color = if (isPos) ProfitGreen else LossRed)
                                Text("${if (isPos) "+" else ""}${Formatters.formatCurrency(profit)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isPos) ProfitGreen else LossRed)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظة البيع (مثال: محل الصاغة / سبب التسييل)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (weightToSell > 0 && weightToSell <= item.weight + 0.0001 && sellPrice > 0) {
                        if (authRequired) {
                            showAuthConfirmDialog = true
                        } else {
                            onConfirmSell(weightToSell, sellPrice, cashBackAmount, notes)
                        }
                    }
                },
                enabled = error == null && weightToSell > 0,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                modifier = Modifier.testTag("confirm_smart_sell_button")
            ) {
                Text("تأكيد البيع واستلام النقدية")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    if (showAuthConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showAuthConfirmDialog = false
                authError = null
                enteredPin = ""
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد أمني لتنفيذ البيع",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لتأكيد بيع ${Formatters.formatWeight(weightToSell)} من ${item.name} بمبلغ ${Formatters.formatCurrency(totalCashReceived)}، يرجى إدخال رمز المرور أو التحقق بالبصمة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (hasPin) OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 8) {
                                enteredPin = it
                                authError = null
                            }
                        },
                        label = { Text("رمز المرور (PIN)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = authError != null,
                        supportingText = authError?.let { { Text(it, color = LossRed) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sell_auth_pin_input")
                    )

                    if (biometricReady) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    BiometricAuth.authenticate(
                                        activity = authActivity!!,
                                        title = "تأكيد عملية البيع",
                                        subtitle = "تحقق من هويتك لتنفيذ البيع",
                                        negativeText = "إلغاء",
                                        onSuccess = {
                                            showAuthConfirmDialog = false
                                            enteredPin = ""
                                            onConfirmSell(weightToSell, sellPrice, cashBackAmount, notes)
                                        }
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "التحقق بالبصمة",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تأكيد العملية بالبصمة الحيوية",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (!hasPin) {
                        Text(
                            text = "لم يُعيَّن رمز مرور بعد. يمكنك تعيينه من الإعدادات > الأمان.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authScope.launch {
                            val result = verifyPin(enteredPin)
                            if (result == PinResult.Success) {
                                showAuthConfirmDialog = false
                                enteredPin = ""
                                onConfirmSell(weightToSell, sellPrice, cashBackAmount, notes)
                            } else {
                                authError = result.toUserMessage()
                                enteredPin = ""
                            }
                        }
                    },
                    enabled = hasPin && enteredPin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                    modifier = Modifier.testTag("confirm_sell_auth_button")
                ) {
                    Text("تنفيذ البيع الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAuthConfirmDialog = false
                    authError = null
                    enteredPin = ""
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AssetEditDialog(
    initialItem: GoldItem?,
    defaultType: GoldType,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: GoldType,
        weight: Double,
        karat: Int,
        cost: Double?,
        brand: String,
        condition: String,
        notes: String,
        barcode: String?,
        imageUri: String?,
        invoiceUri: String?
    ) -> Unit
) {
    var type by remember { mutableStateOf(initialItem?.type ?: defaultType) }
    var title by remember { mutableStateOf(initialItem?.name ?: if (type == GoldType.BULLION) "سبيكة ذهب BTC عيار 24" else "مشغولات ذهبية عيار 21") }
    var weightStr by remember { mutableStateOf(initialItem?.weight?.toString() ?: "10.0") }
    var karatStr by remember { mutableStateOf(initialItem?.karat?.toString() ?: if (type == GoldType.BULLION) "24" else "21") }
    var costStr by remember { mutableStateOf(initialItem?.cost?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var brand by remember { mutableStateOf(initialItem?.brand ?: if (type == GoldType.BULLION) "BTC" else "محلي") }
    var condition by remember { mutableStateOf(initialItem?.status ?: "سليمة ومغلفة") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var barcode by remember { mutableStateOf(initialItem?.barcode ?: "") }
    var imageUri by remember { mutableStateOf(initialItem?.imageUri) }
    var invoiceUri by remember { mutableStateOf(initialItem?.invoiceUri) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val piecePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = MediaUtils.saveImageLocally(context, uri, "piece")
            if (saved != null) imageUri = saved
        }
    }

    val invoicePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = MediaUtils.saveImageLocally(context, uri, "invoice")
            if (saved != null) invoiceUri = saved
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "إضافة أصل ذهبي جديد" else "تعديل بيانات الأصل", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Selector (Bullion vs Jewelry)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            type = GoldType.BULLION
                            karatStr = "24"
                            if (initialItem == null) title = "سبيكة ذهب BTC عيار 24"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == GoldType.BULLION) GoldContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سبيكة (Bullion)", color = if (type == GoldType.BULLION) GoldPrimary else TextSecondary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            type = GoldType.JEWELRY
                            karatStr = "21"
                            if (initialItem == null) title = "مشغولات ذهبية عيار 21"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == GoldType.JEWELRY) GoldContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مشغولات (Jewelry)", color = if (type == GoldType.JEWELRY) GoldPrimary else TextSecondary, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم أو وصف القطعة") },
                    modifier = Modifier.fillMaxWidth().testTag("asset_title_input")
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("الوزن الصافي بالجرام") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("asset_weight_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = karatStr,
                        onValueChange = { karatStr = it },
                        label = { Text("العيار") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("asset_karat_input")
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("الماركة/الصانع") },
                        modifier = Modifier.weight(1f).testTag("asset_brand_input")
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("تكلفة الشراء الكلية (اختياري - ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("asset_cost_input")
                )

                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("الحالة (سليمة، مغلفة، كسر)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("الباركود أو الرقم التسلسلي (اختياري)") },
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldSecondary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("asset_barcode_input")
                )

                // Piece Image Picker & Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (type == GoldType.BULLION) "صورة السبيكة والغلاف" else "صورة المشغولات",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (imageUri != null) {
                                TextButton(
                                    onClick = { imageUri = null },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("حذف", color = LossRed, fontSize = 11.sp)
                                }
                            }
                        }

                        if (imageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = File(imageUri!!).takeIf { it.exists() } ?: imageUri,
                                    contentDescription = "صورة القطعة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                piecePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pick_piece_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (imageUri == null) "التقاط أو اختيار صورة للقطعة" else "تغيير صورة القطعة",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Invoice Image Picker & Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "فاتورة الشراء / إيصال الصائغ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (invoiceUri != null) {
                                TextButton(
                                    onClick = { invoiceUri = null },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("حذف", color = LossRed, fontSize = 11.sp)
                                }
                            }
                        }

                        if (invoiceUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldSecondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = File(invoiceUri!!).takeIf { it.exists() } ?: invoiceUri,
                                    contentDescription = "فاتورة الشراء",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                invoicePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pick_invoice_image_button")
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (invoiceUri == null) "إرفاق صورة فاتورة الشراء" else "تغيير صورة الفاتورة",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(text = error!!, color = LossRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightStr.toDoubleOrNullLocalized()
                    val karat = karatStr.toIntOrNull() ?: 24
                    val cost = costStr.toDoubleOrNullLocalized()

                    if (weight == null || weight <= 0) {
                        error = "يرجى إدخال وزن صحيح"
                        return@Button
                    }
                    onConfirm(
                        title,
                        type,
                        weight,
                        karat,
                        cost,
                        brand,
                        condition,
                        notes,
                        barcode.trim().ifEmpty { null },
                        imageUri,
                        invoiceUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                modifier = Modifier.testTag("submit_asset_button")
            ) {
                Text(if (initialItem == null) "إضافة القطعة" else "حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ImagePreviewModal(
    title: String,
    imagePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    AsyncImage(
                        model = File(imagePath).takeIf { it.exists() } ?: imagePath,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700))
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun EmptyAssetsCard(type: GoldType, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (type == GoldType.BULLION) Icons.Default.AccountBalance else Icons.Default.Diamond,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (type == GoldType.BULLION) "لا توجد سبائك حالياً في المحفظة" else "لا توجد مشغولات ذهبية مسجلة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "يمكنك إضافة قطع جديدة أو استعادة التشكيلة الابتدائية من الإعدادات.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة قطعة جديدة")
            }
        }
    }
}
