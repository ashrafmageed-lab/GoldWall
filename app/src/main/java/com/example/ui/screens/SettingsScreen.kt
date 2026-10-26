package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.LocalPinVerifier
import com.example.security.PinHasher
import com.example.security.PinResult
import com.example.security.toUserMessage
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters
import com.example.util.toDoubleOrNullLocalized
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsSection(val labelAr: String) {
    ALL("الكل"),
    PREFERENCES("العملات والوحدات"),
    PORTFOLIO("أهداف المحفظة"),
    CASHBACK("كاش باك السبائك"),
    ALERTS("تنبيهات الأسعار"),
    SECURITY("الأمان والخزانة"),
    DANGER("إدارة البيانات")
}

@Composable
fun SettingsScreen(
    state: PortfolioUiState,
    onBack: () -> Unit,
    onSaveSettings: (baseline: Double, target: Double) -> Unit,
    onSaveCashBack: (amount: Double, notes: String) -> Unit,
    onResetBaseline: () -> Unit,
    onSavePriceAlerts: (enabled: Boolean, threshold: Double, targetHigh: Double, targetLow: Double) -> Unit = { _, _, _, _ -> },
    onTriggerPriceAlertCheck: () -> Unit = {},
    onSendTestPriceAlert: () -> Unit = {},
    onToggleAutoBullionCashBack: (Boolean) -> Unit = {},
    onUpdateSecuritySettings: (isAppLockEnabled: Boolean, requireAuthOnSale: Boolean, isBiometricEnabled: Boolean, securityPin: String) -> Unit = { _, _, _, _ -> },
    onClearPortfolioAndStartFresh: (baseline: Double, target: Double) -> Unit = { _, _ -> },
    onUpdatePreferences: (currency: String, weightUnit: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedSection by remember { mutableStateOf(SettingsSection.ALL) }
    var showClearPortfolioDialog by remember { mutableStateOf(false) }

    // Display Preferences state
    var preferredCurrency by remember(state.settings.preferredCurrency) {
        mutableStateOf(state.settings.preferredCurrency)
    }
    var preferredWeightUnit by remember(state.settings.preferredWeightUnit) {
        mutableStateOf(state.settings.preferredWeightUnit)
    }

    // Portfolio Baseline & Target
    var baselineStr by remember(state.settings.initialBaselineEgp) {
        mutableStateOf(String.format(Locale.US, "%.2f", state.settings.initialBaselineEgp))
    }
    var targetStr by remember(state.settings.targetGoalEgp) {
        mutableStateOf(String.format(Locale.US, "%.2f", state.settings.targetGoalEgp))
    }

    // Cash Back
    var cashBackStr by remember(state.settings.confirmedCashBackEgp) {
        mutableStateOf(String.format(Locale.US, "%.2f", state.settings.confirmedCashBackEgp))
    }
    var cashBackNotes by remember(state.settings.cashBackSourceNotes) {
        mutableStateOf(state.settings.cashBackSourceNotes)
    }
    var autoCashBackEnabled by remember(state.settings.autoAddBullionCashBack) {
        mutableStateOf(state.settings.autoAddBullionCashBack)
    }

    // Price Alerts
    var priceAlertsEnabled by remember(state.settings.priceAlertsEnabled) {
        mutableStateOf(state.settings.priceAlertsEnabled)
    }
    var alertThresholdStr by remember(state.settings.priceAlertThresholdEgp) {
        mutableStateOf(state.settings.priceAlertThresholdEgp.toInt().toString())
    }
    var targetHighStr by remember(state.settings.priceTargetHighEgp) {
        mutableStateOf(if (state.settings.priceTargetHighEgp > 0) state.settings.priceTargetHighEgp.toInt().toString() else "")
    }
    var targetLowStr by remember(state.settings.priceTargetLowEgp) {
        mutableStateOf(if (state.settings.priceTargetLowEgp > 0) state.settings.priceTargetLowEgp.toInt().toString() else "")
    }

    // Security & Auth
    var appLockEnabled by remember(state.settings.isAppLockEnabled) {
        mutableStateOf(state.settings.isAppLockEnabled)
    }
    var requireAuthOnSale by remember(state.settings.requireAuthOnSale) {
        mutableStateOf(state.settings.requireAuthOnSale)
    }
    var biometricEnabled by remember(state.settings.isBiometricEnabled) {
        mutableStateOf(state.settings.isBiometricEnabled)
    }
    val hasPin = PinHasher.hasPin(state.settings.securityPin)
    val verifyPin = LocalPinVerifier.current
    val pinScope = rememberCoroutineScope()
    var pendingEnableLock by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    fun applyPreferences(newCur: String, newUnit: String) {
        preferredCurrency = newCur
        preferredWeightUnit = newUnit
        onUpdatePreferences(newCur, newUnit)
        successToast = "تم تطبيق التفضيلات: ${if (newCur == "USD") "الدولار ($)" else "الجنيه (ج.م)"} • ${if (newUnit == "OUNCE") "الأونصة (oz)" else "الجرام (جم)"}"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = GoldPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "إعدادات وتخصيص الخزانة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "العملات والوحدات • الأهداف المالية • التنبيهات • الأمان",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. Organized Section Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsSection.values().forEach { section ->
                    val isSelected = selectedSection == section
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSection = section },
                        label = {
                            Text(
                                text = section.labelAr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color(0xFF1E1700),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = GoldPrimary
                        )
                    )
                }
            }
        }

        // Notification / Feedback Banner
        if (successToast != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ProfitGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(successToast!!, color = ProfitGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION A: تفضيلات العرض والوحدات (العملة المفضلة ووحدات الوزن)
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.PREFERENCES) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .testTag("preferences_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Tune,
                            title = "تفضيلات العرض والوحدات",
                            subtitle = "اختر العملة ووحدات قياس وزن الذهب لعرض المحفظة والتسييل"
                        )

                        // 1. Preferred Currency Switch & Segmented Control
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.CurrencyExchange,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "العملة المفضلة",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (preferredCurrency == "USD") "الدولار الأمريكي ($ - USD)" else "الجنيه المصري (ج.م - EGP)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldSecondary
                                            )
                                        }
                                    }

                                    // Direct Toggle Switch
                                    Switch(
                                        checked = preferredCurrency == "USD",
                                        onCheckedChange = { isUsd ->
                                            applyPreferences(if (isUsd) "USD" else "EGP", preferredWeightUnit)
                                        },
                                        modifier = Modifier.testTag("currency_switch")
                                    )
                                }

                                // Interactive Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isEgp = preferredCurrency == "EGP"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isEgp) GoldContainer else Color(0x22FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isEgp) GoldPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { applyPreferences("EGP", preferredWeightUnit) }
                                    ) {
                                        Text(
                                            text = "🇪🇬 الجنيه المصري (ج.م)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isEgp) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isEgp) GoldPrimary else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }

                                    val isUsd = preferredCurrency == "USD"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isUsd) GoldContainer else Color(0x22FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUsd) GoldPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { applyPreferences("USD", preferredWeightUnit) }
                                    ) {
                                        Text(
                                            text = "🇺🇸 الدولار الأمريكي ($)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isUsd) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isUsd) GoldPrimary else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "سعر الصرف التقديري: 1 دولار أمريكي ≈ ${String.format(Locale.US, "%.2f", Formatters.usdExchangeRate)} جنيه مصري",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // 2. Preferred Weight Unit Switch & Segmented Control
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = null,
                                            tint = GoldSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "وحدة قياس وزن الذهب",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (preferredWeightUnit == "OUNCE") "أونصة تروي (oz - Troy Ounce)" else "جرام ذهب (جم - Gram)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldSecondary
                                            )
                                        }
                                    }

                                    // Direct Toggle Switch
                                    Switch(
                                        checked = preferredWeightUnit == "OUNCE",
                                        onCheckedChange = { isOunce ->
                                            applyPreferences(preferredCurrency, if (isOunce) "OUNCE" else "GRAM")
                                        },
                                        modifier = Modifier.testTag("weight_unit_switch")
                                    )
                                }

                                // Interactive Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isGram = preferredWeightUnit == "GRAM"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isGram) GoldContainer else Color(0x22FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGram) GoldPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { applyPreferences(preferredCurrency, "GRAM") }
                                    ) {
                                        Text(
                                            text = "⚖️ جرام ذهب (جم)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isGram) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isGram) GoldPrimary else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }

                                    val isOunce = preferredWeightUnit == "OUNCE"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOunce) GoldContainer else Color(0x22FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOunce) GoldPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { applyPreferences(preferredCurrency, "OUNCE") }
                                    ) {
                                        Text(
                                            text = "🪙 أونصة تروي (oz)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isOunce) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isOunce) GoldPrimary else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "1 أونصة تروي دولية = 31.1035 جرام ذهب عيار 24",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Live Preview Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "معاينة العرض الحالي:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${Formatters.formatWeight(100.0, preferredWeightUnit)} • ${Formatters.formatCurrency(state.activeGram24BuyPrice * 100.0, preferredCurrency)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION B: أهداف المحفظة ورأس المال
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.PORTFOLIO) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "أهداف المحفظة ورأس المال المرجعي",
                            subtitle = "الرصيد الابتدائي هو الأساس لحساب صافي الأرباح ونسبة تحقيق الهدف"
                        )

                        // Current Status summary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("رأس المال الحالي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(Formatters.formatCurrency(state.settings.initialBaselineEgp, preferredCurrency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الهدف المالي المستهدف", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(Formatters.formatCurrency(state.settings.targetGoalEgp, preferredCurrency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GoldSecondary)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = baselineStr,
                            onValueChange = { baselineStr = it },
                            label = { Text("الرصيد الابتدائي للمحفظة (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("baseline_input")
                        )

                        OutlinedTextField(
                            value = targetStr,
                            onValueChange = { targetStr = it },
                            label = { Text("الهدف المالي للمحفظة (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_input")
                        )

                        Button(
                            onClick = {
                                val b = baselineStr.toDoubleOrNullLocalized()
                                val t = targetStr.toDoubleOrNullLocalized()
                                if (b == null || b <= 0.0 || t == null || t <= 0.0) {
                                    errorMessage = "يرجى إدخال قيم مالية صحيحة أكبر من صفر"
                                    return@Button
                                }
                                errorMessage = null
                                onSaveSettings(b, t)
                                successToast = "تم حفظ الرصيد الابتدائي والهدف المالي بنجاح"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_baseline_target_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ الرصيد والهدف المالي")
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_baseline_button")
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استعادة الرصيد الافتراضي (1,000,462 ج.م)")
                        }
                    }
                }
            }
        }

        // SECTION C: كاش باك السبائك والمستردات
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.CASHBACK) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Redeem,
                            title = "كاش باك السبائك والمستردات",
                            subtitle = "معدل الاسترداد القياسي لسبائك BTC والشركات المعتمدة (28 ج.م/جم)"
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي وزن سبائك المحفظة:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(Formatters.formatWeight(state.totalBullionWeight, preferredWeightUnit), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الكاش باك المستحق عند الإرجاع:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(Formatters.formatCurrency(state.expectedBullionCashBack, preferredCurrency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                }
                            }
                        }

                        // Auto Cash Back Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إضافة الكاش باك القياسي (28 ج.م/جم) تلقائياً", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("تضمين 28 ج.م لكل جرام سبائك مباشرة في إجمالي تصفية المحفظة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = autoCashBackEnabled,
                                onCheckedChange = {
                                    autoCashBackEnabled = it
                                    onToggleAutoBullionCashBack(it)
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val standardCb = state.expectedBullionCashBack
                                cashBackStr = String.format(Locale.US, "%.2f", standardCb)
                                cashBackNotes = "كاش باك معتمد للسبائك الأصلية (28 ج.م/جرام)"
                                onSaveCashBack(standardCb, cashBackNotes)
                                successToast = "تم تثبيت الكاش باك القياسي بنجاح"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تثبيت الكاش باك القياسي (${Formatters.formatCurrency(state.expectedBullionCashBack, preferredCurrency)})")
                        }

                        // Manual override fields
                        OutlinedTextField(
                            value = cashBackStr,
                            onValueChange = { cashBackStr = it },
                            label = { Text("قيمة مخصصة للكاش باك المؤكد (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashback_input")
                        )

                        OutlinedTextField(
                            value = cashBackNotes,
                            onValueChange = { cashBackNotes = it },
                            label = { Text("مصدر الكاش باك وملاحظات التوثيق") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashback_notes_input")
                        )

                        Button(
                            onClick = {
                                val cb = cashBackStr.toDoubleOrNullLocalized() ?: 0.0
                                if (cb < 0.0) {
                                    errorMessage = "لا يمكن أن يكون الكاش باك بالسالب"
                                    return@Button
                                }
                                errorMessage = null
                                onSaveCashBack(cb, cashBackNotes)
                                successToast = "تم حفظ الكاش باك المخصص بنجاح"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_cashback_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ الكاش باك المخصص")
                        }
                    }
                }
            }
        }

        // SECTION D: تنبيهات تحرك الأسعار
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.ALERTS) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Notifications,
                            title = "تنبيهات حركة أسعار الذهب",
                            subtitle = "إشعارك فورياً كلما تحرك سعر جرام الذهب في السوق المصري"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تفعيل التنبيهات اللحظية", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("إرسال إشعار في شريط التنبيهات عند رصد تغير السعر", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = priceAlertsEnabled,
                                onCheckedChange = { priceAlertsEnabled = it }
                            )
                        }

                        if (priceAlertsEnabled) {
                            Text(
                                text = "مقدار تحرك السعر المشترط للتنبيه (جنيه/جرام):",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(10, 25, 50, 100).forEach { th ->
                                    val isSelected = alertThresholdStr == th.toString()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) GoldContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { alertThresholdStr = th.toString() }
                                            .border(1.dp, if (isSelected) GoldPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "$th ج.م",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) GoldPrimary else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = alertThresholdStr,
                                onValueChange = { alertThresholdStr = it },
                                label = { Text("قيمة مخصصة لهامش التنبيه (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = { Text("ج.م", color = GoldSecondary, modifier = Modifier.padding(end = 12.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Text(
                                text = "أهداف الأسعار اللحظية (سقف صعودي وقاع هبوطي):",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ينبهك التطبيق تلقائياً فور وصول سعر عيار 21 للهدف الصعودي (جني أرباح) أو انخفاضه للقاع (فرصة شراء وتعزيز). اتركه فارغاً لتعطيل الهدف.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            OutlinedTextField(
                                value = targetHighStr,
                                onValueChange = { targetHighStr = it },
                                label = { Text("السقف المستهدف (>= ج.م/جرام)") },
                                placeholder = { Text("مثال: 5200") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = { Text("ج.م", color = ProfitGreen, modifier = Modifier.padding(end = 12.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_high_input")
                            )

                            OutlinedTextField(
                                value = targetLowStr,
                                onValueChange = { targetLowStr = it },
                                label = { Text("القاع المستهدف (<= ج.م/جرام)") },
                                placeholder = { Text("مثال: 4400") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = { Text("ج.م", color = LossRed, modifier = Modifier.padding(end = 12.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_low_input")
                            )

                            Button(
                                onClick = {
                                    val threshold = alertThresholdStr.toDoubleOrNullLocalized() ?: 25.0
                                    val high = targetHighStr.toDoubleOrNullLocalized() ?: 0.0
                                    val low = targetLowStr.toDoubleOrNullLocalized() ?: 0.0
                                    if (threshold <= 0.0) {
                                        errorMessage = "يرجى تحديد هامش تنبيه أكبر من صفر"
                                        return@Button
                                    }
                                    if (high > 0.0 && low > 0.0 && high <= low) {
                                        errorMessage = "سقف السعر المستهدف يجب أن يكون أعلى من قاع السعر"
                                        return@Button
                                    }
                                    errorMessage = null
                                    onSavePriceAlerts(priceAlertsEnabled, threshold, high, low)
                                    successToast = "تم حفظ إعدادات تنبيهات الأسعار والأهداف بنجاح"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_price_alerts_button")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ إعدادات التنبيهات")
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Text(
                                text = "فحص واختبار التنبيهات (WorkManager):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onSendTestPriceAlert()
                                        successToast = "تم إرسال إشعار تجريبي لشريط الإشعارات بنجاح 🔔"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_price_alert_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إشعار تجريبي 🔔", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onTriggerPriceAlertCheck()
                                        successToast = "تم إطلاق فحص WorkManager في الخلفية فورياً ⚡"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("trigger_workmanager_check_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldSecondary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فحص الآن ⚡", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION E: أمان وقفل الخزانة
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.SECURITY) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Shield,
                            title = "الأمان وقفل الخزانة (Vault Security)",
                            subtitle = "تأمين بيانات المحفظة وتأكيد عمليات البيع عبر البصمة الحيوية أو PIN"
                        )

                        // 1. App Lock Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("قفل التطبيق (App Lock)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("طلب رمز المرور أو البصمة عند فتح التطبيق", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = {
                                    if (it && !hasPin) {
                                        pendingEnableLock = true
                                        showChangePinDialog = true
                                    } else {
                                        appLockEnabled = it
                                        onUpdateSecuritySettings(it, requireAuthOnSale, biometricEnabled, "")
                                    }
                                },
                                modifier = Modifier.testTag("app_lock_switch")
                            )
                        }

                        // 2. Require Auth on Sale Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تأكيد البيع برمز المرور أو البصمة", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("طلب تأكيد أمني إلزامي قبل اعتماد بيع أي قطعة ذهبية", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = requireAuthOnSale,
                                onCheckedChange = {
                                    requireAuthOnSale = it
                                    onUpdateSecuritySettings(appLockEnabled, it, biometricEnabled, "")
                                },
                                modifier = Modifier.testTag("require_auth_on_sale_switch")
                            )
                        }

                        // 3. Biometric Enabled Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("السماح بالبصمة الحيوية (Biometrics)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("استخدام مستشعر بصمة الإصبع للدخول والتأكيد السريع", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    onUpdateSecuritySettings(appLockEnabled, requireAuthOnSale, it, "")
                                },
                                modifier = Modifier.testTag("biometric_enabled_switch")
                            )
                        }

                        // 4. Change PIN
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangePinDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("رمز المرور السري (PIN)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(if (hasPin) "تم تعيين رمز مرور مشفر (PBKDF2)" else "لم يُعيَّن رمز مرور بعد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                                Text("تعديل", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                        }
                    }
                }
            }
        }

        // SECTION F: إدارة البيانات ومسح المحفظة
        if (selectedSection == SettingsSection.ALL || selectedSection == SettingsSection.DANGER) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LossRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.DeleteForever,
                            title = "مسح البيانات وبدء محفظة جديدة",
                            subtitle = "تصفير كافة السبائك والمشغولات وتحديد رأس مال أولي جديد",
                            tint = LossRed
                        )

                        Text(
                            text = "يتيح لك هذا الخيار تصفير كافة القطع وسجل العمليات وتعيين رصيد وهدف مالي جديدين لبدء إدارة محفظتك الخاصة من الصفر.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { showClearPortfolioDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_portfolio_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مسح كافة البيانات والبدء من الصفر")
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            item {
                Text(text = errorMessage!!, color = LossRed, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Reset Baseline Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("تأكيد استعادة الرصيد الافتراضي", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد استعادة الرصيد المرجعي الأولي إلى 1,000,462 جنيه مصري والهدف إلى 1,250,000 جنيه مصري؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetBaseline()
                        showResetDialog = false
                        successToast = "تمت استعادة الرصيد الافتراضي بنجاح"
                    }
                ) {
                    Text("نعم، استعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var oldPinStr by remember { mutableStateOf("") }
        var newPinStr by remember { mutableStateOf("") }
        var confirmPinStr by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                pendingEnableLock = false
            },
            title = { Text("تعيين رمز مرور جديد (PIN)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "الرمز يجب أن يتكون من 4 أرقام. سيُستخدم هذا الرمز لفتح الخزانة وتأكيد عمليات البيع والتسييل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    if (hasPin) {
                        OutlinedTextField(
                            value = oldPinStr,
                            onValueChange = { if (it.length <= 4) oldPinStr = it },
                            label = { Text("رمز المرور الحالي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("old_pin_input")
                        )
                    }

                    OutlinedTextField(
                        value = newPinStr,
                        onValueChange = { if (it.length <= 4) newPinStr = it },
                        label = { Text("رمز المرور الجديد (4 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPinStr,
                        onValueChange = { if (it.length <= 4) confirmPinStr = it },
                        label = { Text("تأكيد رمز المرور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = LossRed) } },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!PinHasher.isValidFormat(newPinStr)) {
                            pinError = "يجب أن يتكون الرمز من 4 أرقام عددية"
                            return@Button
                        }
                        if (PinHasher.isWeak(newPinStr)) {
                            pinError = "الرمز سهل التخمين (مثل 1234 أو 0000). اختر رمزاً آخر"
                            return@Button
                        }
                        if (newPinStr != confirmPinStr) {
                            pinError = "الرمزان غير متطابقين، يرجى إعادة المحاولة"
                            return@Button
                        }
                        pinScope.launch {
                            if (hasPin) {
                                val check = verifyPin(oldPinStr)
                                if (check != PinResult.Success) {
                                    pinError = check.toUserMessage()
                                    oldPinStr = ""
                                    return@launch
                                }
                            }
                            val enableLock = appLockEnabled || pendingEnableLock
                            appLockEnabled = enableLock
                            onUpdateSecuritySettings(enableLock, requireAuthOnSale, biometricEnabled, newPinStr)
                            pendingEnableLock = false
                            showChangePinDialog = false
                            successToast = "تم حفظ رمز المرور الجديد بنجاح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF1E1700)),
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("حفظ الرمز")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    pendingEnableLock = false
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Clear Portfolio Confirmation Dialog
    if (showClearPortfolioDialog) {
        var newBaselineInput by remember { mutableStateOf("0.0") }
        var newTargetInput by remember { mutableStateOf("1000000.0") }
        var pinInput by remember { mutableStateOf("") }
        var clearError by remember { mutableStateOf<String?>(null) }
        val dialogScope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showClearPortfolioDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = LossRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد مسح البيانات وبدء محفظة جديدة",
                    fontWeight = FontWeight.Bold,
                    color = LossRed,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تنبيه: سيتم مسح كافة السبائك والمشغولات المحفوظة نهائياً وبدء محفظة جديدة فارغة. يمكنك دائماً تصدير نسخة احتياطية أولاً من شاشة النسخ الاحتياطي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newBaselineInput,
                        onValueChange = { newBaselineInput = it },
                        label = { Text("الرصيد الابتدائي للمحفظة الجديدة (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("new_portfolio_baseline_input")
                    )

                    OutlinedTextField(
                        value = newTargetInput,
                        onValueChange = { newTargetInput = it },
                        label = { Text("الهدف المالي للمحفظة الجديدة (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("new_portfolio_target_input")
                    )

                    if (hasPin) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("رمز المرور (PIN) لتأكيد المسح") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = clearError != null,
                            supportingText = clearError?.let { { Text(it, color = LossRed) } },
                            modifier = Modifier.fillMaxWidth().testTag("clear_portfolio_pin_input")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = newBaselineInput.toDoubleOrNullLocalized() ?: 0.0
                        val target = newTargetInput.toDoubleOrNullLocalized() ?: 1000000.0
                        if (hasPin) {
                            dialogScope.launch {
                                val result = verifyPin(pinInput)
                                if (result == PinResult.Success) {
                                    showClearPortfolioDialog = false
                                    onClearPortfolioAndStartFresh(base, target)
                                } else {
                                    clearError = result.toUserMessage()
                                    pinInput = ""
                                }
                            }
                        } else {
                            showClearPortfolioDialog = false
                            onClearPortfolioAndStartFresh(base, target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_clear_portfolio_btn")
                ) {
                    Text("مسح البيانات والبدء الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPortfolioDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = GoldPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
