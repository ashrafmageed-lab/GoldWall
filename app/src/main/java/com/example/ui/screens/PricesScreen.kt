package com.example.ui.screens

import com.example.util.toDoubleOrNullLocalized
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters

@Composable
fun PricesScreen(
    state: PortfolioUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSaveManualPrices: (k24Buy: Double, k21Buy: Double, useManual: Boolean) -> Unit,
    onAddSimplePiece: ((weight: Double, karat: Int, type: com.example.data.model.GoldType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Back handling is managed by main pager and top bar

    var useManual by remember { mutableStateOf(state.settings.useManualPrices) }
    var k24BuyStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", state.settings.manual24BuyPrice)) }
    var k21BuyStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", state.settings.manual21BuyPrice)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Karats Rates (24k, 21k, 18k, Gold Coin)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            MarketPricesCard(
                state = state,
                onRefresh = onRefresh,
                onManagePrices = {},
                onToggleSource = { isManual ->
                    onSaveManualPrices(state.settings.manual24BuyPrice, state.settings.manual21BuyPrice, isManual)
                }
            )
        }

        // 2. Interactive Gold Weight Calculator & Quick Add
        if (onAddSimplePiece != null) {
            item {
                com.example.ui.components.SimpleGoldWeightCard(
                    totalWeight = state.totalPhysicalWeight,
                    bullionWeight = state.totalBullionWeight,
                    jewelryWeight = state.totalJewelryWeight,
                    onAddGoldPiece = onAddSimplePiece
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إدارة مصادر أسعار الذهب",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusPill(status = state.priceState.sourceStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المصدر الحالي: ${state.priceState.sourceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (state.priceState.lastUpdated > 0L) "آخر تحديث: ${Formatters.formatDateTime(state.priceState.lastUpdated)}" else "لم تُحدَّث الأسعار بعد",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Manual vs Live Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل الأسعار اليدوية",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحديد سعر شراء الصائغ بنفسك وفقاً لأحدث تسعير في منطقتك",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = useManual,
                            onCheckedChange = { useManual = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("toggle_manual_prices")
                        )
                    }

                    if (useManual) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "أسعار شراء الصائغ من العميل (ج.م / جرام):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = k24BuyStr,
                            onValueChange = { k24BuyStr = it },
                            label = { Text("سعر شراء الصائغ لعيار 24 (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_24_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = k21BuyStr,
                            onValueChange = { k21BuyStr = it },
                            label = { Text("سعر شراء الصائغ لعيار 21 (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_21_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = errorMessage!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val k24 = k24BuyStr.toDoubleOrNullLocalized()
                                val k21 = k21BuyStr.toDoubleOrNullLocalized()
                                if (k24 == null || k24 <= 0 || k21 == null || k21 <= 0) {
                                    errorMessage = "يرجى إدخال أسعار صحيحة أكبر من صفر"
                                    return@Button
                                }
                                errorMessage = null
                                onSaveManualPrices(k24, k21, true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_manual_prices_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ وتطبيق الأسعار اليدوية")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onSaveManualPrices(state.settings.manual24BuyPrice, state.settings.manual21BuyPrice, false) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تطبيق التحديث التلقائي")
                        }
                    }
                }
            }
        }

        // Manual Refresh Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تحديث فوري من المصادر الحية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جلب أحدث أسعار الذهب الفورية من السوق المصري وبورصة المعادن العالمية XAU/USD.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refresh_feeds_button")
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التحديث...")
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحديث الأسعار الآن")
                        }
                    }
                }
            }
        }

        // Live prices comparison
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الأسعار المعتمدة في الحسابات حالياً",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• عيار 24 شراء الصائغ: ${Formatters.formatCurrency(state.activeGram24BuyPrice)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• عيار 21 شراء الصائغ: ${Formatters.formatCurrency(state.activeGram21BuyPrice)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• عيار 18 شراء الصائغ: ${Formatters.formatCurrency(state.activeGram18BuyPrice)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• أونصة الذهب عالمياً: $${state.priceState.ounceUsd}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
