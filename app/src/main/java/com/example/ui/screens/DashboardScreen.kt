package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceStatus
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.data.model.GoldType
import com.example.ui.components.PortfolioDistributionDashboardView
import com.example.ui.components.PortfolioValuationChart
import com.example.ui.components.SimpleGoldWeightCard
import com.example.ui.theme.WarningAmber
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: PortfolioUiState,
    onRefreshPrices: () -> Unit,
    onRecordSnapshot: (notes: String) -> Unit,
    onNavigateToBullion: () -> Unit,
    onNavigateToJewelry: () -> Unit,
    onNavigateToPrices: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTogglePriceSource: (Boolean) -> Unit = {},
    onAddSimplePiece: (weight: Double, karat: Int, type: GoldType) -> Unit = { _, _, _ -> },
    onResetBaseline: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var snapshotNotes by remember { mutableStateOf("") }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefreshPrices,
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (state.bullionList.isEmpty() && state.jewelryList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("empty_portfolio_seed_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                        Text("جاري تهيئة محفظة الذهب المرجعية...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        Text("انقر أدناه لإعادة تحميل السبائك والمشغولات (157.50 جم) فوراً", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                        Button(
                            onClick = onResetBaseline,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Text("استعادة المحفظة المرجعية", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Prominent Fallback Notice or Stale Price Warning Banner
            if (state.priceState.warningMessage != null && (state.settings.useManualPrices || state.priceState.isManual)) {
                ApiFallbackNoticeBanner(
                    message = state.priceState.warningMessage,
                    onRetryApi = onRefreshPrices,
                    onEditManual = onNavigateToPrices
                )
            } else if (state.priceState.sourceStatus == PriceStatus.STALE || state.priceState.sourceStatus == PriceStatus.UNAVAILABLE) {
                StalePriceWarningBanner(
                    status = state.priceState.sourceStatus,
                    lastUpdated = state.priceState.lastUpdated,
                    onManualEdit = onNavigateToPrices
                )
            }
        }

        // 1. قيمة المحفظة وتفصيل التسييل والكاش باك
        item {
            HeroLiquidationCard(
                state = state,
                onTakeSnapshot = { showSnapshotDialog = true }
            )
        }

        // 2. توزيع المحفظة الاحترافي (حسب العيار، الوزن، والقيمة السوقية - D3/Recharts inspired)
        item {
            PortfolioDistributionDashboardView(
                state = state
            )
        }

        // 3. سعر الجرام الآن (شراء الصائغ 24k, 21k, 18k)
        item {
            LiveGramPricesNowCard(
                state = state,
                onRefresh = onRefreshPrices
            )
        }

        // 3. سعر الأونصة عالمياً (Gold-API XAU/USD)
        item {
            GlobalOunceCard(
                state = state,
                onRefresh = onRefreshPrices
            )
        }

        // 4. وأخيراً الرسم البياني لحركة السعر المعتمد على شموع السوق الحية
        item {
            PortfolioValuationChart(
                currentLiquidation = state.totalLiquidation,
                baselineEgp = state.settings.initialBaselineEgp,
                valuations = state.valuations,
                historicalMarketPoints = state.historicalPointsMap
            )
        }

        // 5. الهدف المالي للمحفظة
        item {
            TargetProgressCard(
                currentLiquidation = state.totalLiquidation,
                baseline = state.settings.initialBaselineEgp,
                target = state.settings.targetGoalEgp,
                progress = state.targetProgress,
                onEditTarget = onNavigateToSettings
            )
        }

        // 5. Valuation Comparison to Last Saved (if available)
        if (state.settings.lastValuationEgp > 0.0) {
            item {
                LastValuationCard(
                    currentVal = state.totalLiquidation,
                    lastVal = state.settings.lastValuationEgp,
                    lastTime = state.settings.lastValuationTime,
                    diff = state.diffFromLastValuation
                )
            }
        }

        // 6. Navigation Quick Chips (Seamless jump to Assets or Market screens)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "الوصول السريع للأصول والأسعار",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToBullion,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                            modifier = Modifier.weight(1f).testTag("quick_nav_assets")
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أصول الذهب (${state.totalPhysicalWeight.toInt()} جم)", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPrices,
                            modifier = Modifier.weight(1f).testTag("quick_nav_prices")
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أسعار العيارات", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 9. Important Liquidation Notice
        item {
            LiquidationNoticeCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

    // Snapshot Dialog
    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = {
                Text("حفظ تقييم المحفظة اللحظي", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "سيتم تسجيل القيمة الإجمالية (${Formatters.formatCurrency(state.totalLiquidation)}) وأسعار الذهب الحالية في السجل التاريخي.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = snapshotNotes,
                        onValueChange = { snapshotNotes = it },
                        label = { Text("ملاحظة اختيارية (مثل: بعد تحديث أسعار الصاغة)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("snapshot_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordSnapshot(snapshotNotes)
                        showSnapshotDialog = false
                        snapshotNotes = ""
                    },
                    modifier = Modifier.testTag("confirm_snapshot_button")
                ) {
                    Text("حفظ التقييم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun HeroLiquidationCard(
    state: PortfolioUiState,
    onTakeSnapshot: () -> Unit
) {
    val isProfit = state.profitVsBaseline >= 0
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF252011),
            Color(0xFF1B1812),
            Color(0xFF141312)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .testTag("hero_liquidation_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "قيمة التصفية التقديرية للصائغ",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onTakeSnapshot,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .testTag("take_snapshot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "حفظ تقييم",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Liquidation Amount
                Text(
                    text = Formatters.formatCurrency(state.totalLiquidation),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = Color.White
                )

                // Explicit Liquidation Components Breakdown (Proof of Cash Back Inclusion)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ذهب السبائك (شراء الصائغ 24k):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = Formatters.formatCurrency(state.bullionLiquidation),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ذهب المشغولات (شراء الكسر 21k):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = Formatters.formatCurrency(state.jewelryLiquidation),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+ كاش باك السبائك (28 ج.م/جم):",
                                style = MaterialTheme.typography.bodySmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = "+${Formatters.formatCurrency(state.effectiveCashBack)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProfitGreen,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Profit / Loss and Percentage Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profit / Loss Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isProfit) ProfitGreenContainer else LossRedContainer,
                        modifier = Modifier.border(
                            1.dp,
                            if (isProfit) ProfitGreen.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isProfit) ProfitGreen else LossRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (isProfit) "ربح مرجعي: " else "تراجع: "}${Formatters.formatCurrency(Math.abs(state.profitVsBaseline))}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isProfit) ProfitGreen else LossRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Percentage Badge
                    Text(
                        text = Formatters.formatPercent(state.percentChange),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isProfit) ProfitGreen else LossRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Baseline note and Source indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المرجع: ${Formatters.formatCurrency(state.settings.initialBaselineEgp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (state.settings.useManualPrices || state.priceState.isManual) "المصدر: يدوي محمي" else "المصدر: API حي",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.settings.useManualPrices || state.priceState.isManual) WarningAmber else ProfitGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TargetProgressCard(
    currentLiquidation: Double,
    baseline: Double,
    target: Double,
    progress: Double,
    onEditTarget: () -> Unit
) {
    val progressFraction = (progress / 100.0).coerceIn(0.0, 1.0).toFloat()
    val remaining = (target - currentLiquidation).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_progress_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الهدف المالي للمحفظة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onEditTarget, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل الهدف",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الهدف: ${Formatters.formatCurrency(target)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format(java.util.Locale.US, "%.1f", progress)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GoldPrimary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (remaining > 0) "المتبقي للوصول إلى الهدف: ${Formatters.formatCurrency(remaining)}" else "تم تجاوز الهدف المالي المحدد بنجاح!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun MarketPricesCard(
    state: PortfolioUiState,
    onRefresh: () -> Unit,
    onManagePrices: () -> Unit,
    onToggleSource: (Boolean) -> Unit = {}
) {
    val isManual = state.settings.useManualPrices || state.priceState.isManual

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_prices_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status and refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "أسعار الذهب اللحظية ومصدر التسعير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isManual) "المصدر النشط: إدخال يدوي محمي (حسابات مستقرة)" else "المصدر النشط: ${state.priceState.sourceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isManual) WarningAmber else GoldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(status = if (isManual) PriceStatus.MANUAL else state.priceState.sourceStatus)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                        modifier = Modifier.testTag("refresh_prices_button")
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث الأسعار",
                                tint = GoldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-Tab Source Switcher (API vs Manual)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "التبديل بين مزود الـ API والإدخال اليدوي:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // API Source Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isManual) GoldContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleSource(false) }
                                .border(
                                    1.5.dp,
                                    if (!isManual) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("source_tab_api")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (!isManual) ProfitGreen else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "API مباشر (GoldAPI)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (!isManual) GoldPrimary else TextSecondary,
                                    fontWeight = if (!isManual) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // Manual Source Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isManual) GoldContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleSource(true) }
                                .border(
                                    1.5.dp,
                                    if (isManual) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("source_tab_manual")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isManual) WarningAmber else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إدخال يدوي محمي",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isManual) GoldPrimary else TextSecondary,
                                    fontWeight = if (isManual) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!isManual) {
                            "● يتم جلب الأسعار تلقائياً عبر GoldPriceProvider (Retrofit). في حال حدوث أي خطأ في الاتصال، يتم التحويل للوضع اليدوي تلقائياً."
                        } else {
                            "● وضع الإدخال اليدوي نشط: التقييم يعتمد على الأسعار المحددة من قبلك (محمي تماماً من انقطاع الإنترنت)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isManual) ProfitGreen else WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }

            // Warning message banner if API failed
            if (state.priceState.warningMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF332009),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.priceState.warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFE0B2),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "تعديل الأسعار اليدوية",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onManagePrices() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price Rows
            PriceItemRow(
                karatLabel = "عيار 24",
                buyPrice = state.activeGram24BuyPrice,
                sellPrice = state.priceState.gram24SellEgp
            )

            Spacer(modifier = Modifier.height(10.dp))

            PriceItemRow(
                karatLabel = "عيار 21",
                buyPrice = state.activeGram21BuyPrice,
                sellPrice = state.priceState.gram21SellEgp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Global Ounce Spot (Investing.com)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الأونصة عالمياً (Spot XAU/USD):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "المصدر: ${state.priceState.ounceSourceName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "$${String.format(java.util.Locale.US, "%,.2f", state.priceState.ounceUsd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.priceState.lastUpdated > 0L) "آخر تحديث: ${Formatters.formatDateTime(state.priceState.lastUpdated)}" else "لم تُحدَّث الأسعار بعد",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "تعديل يدوي",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary,
                    modifier = Modifier.clickable { onManagePrices() }
                )
            }
        }
    }
}

@Composable
fun PriceItemRow(
    karatLabel: String,
    buyPrice: Double,
    sellPrice: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = karatLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "شراء الصائغ (التصفية)",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPrimary,
                    fontSize = 11.sp
                )
                Text(
                    text = Formatters.formatCurrency(buyPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "بيع المحل",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = Formatters.formatCurrency(sellPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StatusPill(status: PriceStatus) {
    val (bgColor, textColor) = when (status) {
        PriceStatus.LIVE -> Pair(ProfitGreenContainer, ProfitGreen)
        PriceStatus.RECENT -> Pair(Color(0xFF1E3A2F), Color(0xFF6EE7B7))
        PriceStatus.STALE -> Pair(Color(0xFF3B2D11), WarningAmber)
        PriceStatus.UNAVAILABLE -> Pair(LossRedContainer, LossRed)
        PriceStatus.MANUAL -> Pair(GoldContainer, OnGoldContainer)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Text(
            text = status.labelAr,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ApiFallbackNoticeBanner(
    message: String,
    onRetryApi: () -> Unit,
    onEditManual: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("api_fallback_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E0A))
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, WarningAmber.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تنبيه: تم التحويل لوضع الإدخال اليدوي",
                        style = MaterialTheme.typography.titleSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFE0B2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditManual,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("تعديل الأسعار اليدوية", color = GoldSecondary)
                }
                Button(
                    onClick = onRetryApi,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة محاولة الاتصال بـ API", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StalePriceWarningBanner(
    status: PriceStatus,
    lastUpdated: Long,
    onManualEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stale_price_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF332009))
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, WarningAmber, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تنبيه: أسعار الذهب المعروضة قديمة",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lastUpdated > 0L) "آخر تحديث كان في ${Formatters.formatDateTime(lastUpdated)}. يمكنك إدخال السعر الحالي يدوياً." else "لم تُحدَّث الأسعار بعد. يمكنك إدخال السعر الحالي يدوياً حتى ينجح الاتصال.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFE0B2)
                )
            }
            TextButton(onClick = onManualEdit) {
                Text("إدخال يدوي", color = GoldSecondary)
            }
        }
    }
}

@Composable
fun PhysicalWeightCard(
    bullionWeight: Double,
    jewelryWeight: Double,
    totalWeight: Double,
    bullionCount: Int,
    jewelryCount: Int,
    bullionVal: Double,
    jewelryVal: Double,
    onBullionClick: () -> Unit,
    onJewelryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("physical_weight_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إجمالي الوزن المادي المحفوظ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = Formatters.formatWeight(totalWeight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bullion summary tile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBullionClick() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "سبائك BTC عيار 24",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$bullionCount قطع (${Formatters.formatWeight(bullionWeight)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(bullionVal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                    Text(
                        text = "تصفية تقديرية",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Jewelry summary tile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onJewelryClick() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مشغولات ذهب عيار 21",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$jewelryCount قطع (${Formatters.formatWeight(jewelryWeight)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(jewelryVal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                    Text(
                        text = "تصفية كسر",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun LiveGramPricesNowCard(
    state: PortfolioUiState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_gram_prices_now_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (state.settings.useManualPrices || state.priceState.isManual) WarningAmber else ProfitGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سعر جرام الذهب الآن (شراء الصائغ)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.size(30.dp).testTag("refresh_gram_prices_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث الأسعار",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Column Karats Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 24K
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("عيار 24 (سبائك)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(state.activeGram24BuyPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 21K
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("عيار 21 (مشغولات)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(state.activeGram21BuyPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 18K
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("عيار 18", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(state.activeGram18BuyPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalOunceCard(
    state: PortfolioUiState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("global_ounce_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "سعر أونصة الذهب العالمية (XAU/USD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = state.priceState.ounceSourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "$${String.format(java.util.Locale.US, "%,.2f", state.priceState.ounceUsd)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
            }
        }
    }
}

@Composable
fun LastValuationCard(
    currentVal: Double,
    lastVal: Double,
    lastTime: Long,
    diff: Double
) {
    val isUp = diff >= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_valuation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المقارنة بآخر تقييم محفوظ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${if (isUp) "+" else ""}${Formatters.formatCurrency(diff)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isUp) ProfitGreen else LossRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "آخر تقييم: ${Formatters.formatCurrency(lastVal)} (${Formatters.formatDateTime(lastTime)})",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CashBackCard(
    cashBackAmount: Double,
    sourceNotes: String,
    expectedBullionCashBack: Double = 0.0,
    onEditCashBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cashback_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cash Back السبائك (المضاف للمحفظة)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sourceNotes.ifEmpty { "معدل 28 ج.م / جرام مسترد من غلاف سبائك الذهب" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(cashBackAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cashBackAmount > 0) ProfitGreen else TextMuted
                    )
                    Text(
                        text = "تعديل",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        modifier = Modifier.clickable { onEditCashBack() }
                    )
                }
            }

            if (expectedBullionCashBack > 0 && Math.abs(cashBackAmount - expectedBullionCashBack) > 1.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الكاش باك المحسوب لسبائك المحفظة (28 ج.م/جم): ${Formatters.formatCurrency(expectedBullionCashBack)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary
                )
            }
        }
    }
}

@Composable
fun LiquidationNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = GoldSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "تنبيه التصفية المرجعية: يمثل التقييم القيمة التقديرية الحقيقية لبيع الذهب للصائغ (سعر شراء الصائغ للذهب الكسر والسبائك بدون مصنعية). تم حساب الربح المرجعي بمقارنة التصفية الإجمالية بالرصيد المرجعي المحفوظ (1,000,462 ج.م)، علماً بأن تكلفة شراء المشغولات غير مسجلة.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
