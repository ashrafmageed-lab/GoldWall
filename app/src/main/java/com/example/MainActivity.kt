package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.example.work.PriceAlertWorker
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.security.BiometricAuth
import com.example.security.LocalPinVerifier
import com.example.security.PinResult
import com.example.ui.VaultState
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GoldType
import com.example.ui.MainViewModel
import com.example.ui.PortfolioUiState
import com.example.ui.components.VaultLockScreen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AssetsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PricesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimpleGoldWeightScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.ValuationsScreen
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class NavDestination(val titleAr: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Dashboard),
    ASSETS("أصول الذهب", Icons.Default.AccountBalance),
    PRICES("أسعار السوق", Icons.Default.TrendingUp),
    MORE("المزيد", Icons.Default.MoreHoriz),

    // Sub screens accessed via More
    VALUATIONS("التقييمات السابقة", Icons.Default.History),
    SIMPLE_WEIGHT("حاسبة وإضافة الوزن", Icons.Default.Scale),
    TRANSACTIONS("سجل العمليات", Icons.Default.ReceiptLong),
    SETTINGS("إعدادات المحفظة", Icons.Default.Settings),
    BACKUP("النسخ الاحتياطي", Icons.Default.Backup),
    ABOUT("عن التطبيق", Icons.Default.Info)
}

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* alerts simply stay silent if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Secure by default until we know the lock is off: hides screenshots and the recents preview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.secureWindowRequired.collect { required ->
                    if (required) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.priceAlertsEnabledFlow.distinctUntilChanged().collect { enabled ->
                    PriceAlertWorker.setEnabled(applicationContext, enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
        enableEdgeToEdge()
        val pinVerifier: suspend (String) -> PinResult = { pin -> viewModel.verifyPin(pin) }
        setContent {
            MyApplicationTheme(darkTheme = true) {
                // Enforce RTL Layout Direction for complete Arabic native experience
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalPinVerifier provides pinVerifier
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    GoldVaultApp(
                        state = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.onAppBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldVaultApp(
    state: PortfolioUiState,
    viewModel: MainViewModel
) {
    // 3 primary screens pager: 0: Dashboard, 1: Assets, 2: Prices
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var activeSubScreen by rememberSaveable { mutableStateOf<NavDestination?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Handle Android system back button when in a subscreen
    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    // Show transient messages as snackbars
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val biometricUsable = remember(state.settings.isBiometricEnabled) {
        state.settings.isBiometricEnabled && BiometricAuth.isAvailable(appContext)
    }

    when (vaultState) {
        // Nothing financial is drawn until the lock decision is known.
        VaultState.LOADING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return
        }
        VaultState.LOCKED -> {
            VaultLockScreen(
                isBiometricAvailable = biometricUsable,
                onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                onUnlockSuccess = { viewModel.unlockVault() }
            )
            return
        }
        VaultState.UNLOCKED -> Unit
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (activeSubScreen != null) {
                        IconButton(onClick = { activeSubScreen = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الرجوع",
                                tint = GoldPrimary
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    activeSubScreen != null -> activeSubScreen!!.icon
                                    pagerState.currentPage == 1 -> Icons.Default.AccountBalance
                                    pagerState.currentPage == 2 -> Icons.Default.TrendingUp
                                    else -> Icons.Default.Dashboard
                                },
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "خزنة الذهب",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Text(
                                text = if (activeSubScreen != null) {
                                    activeSubScreen!!.titleAr
                                } else {
                                    when (pagerState.currentPage) {
                                        0 -> "الملخص المالي والتسييل"
                                        1 -> "أصول الذهب (السبائك والمشغولات)"
                                        2 -> "أسعار السوق والحاسبة"
                                        else -> ""
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    if (state.settings.isAppLockEnabled) {
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.testTag("appbar_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل الخزانة",
                                tint = GoldSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refreshPrices() },
                        modifier = Modifier.testTag("appbar_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = GoldSecondary
                        )
                    }
                    IconButton(
                        onClick = { showMoreSheet = true },
                        modifier = Modifier.testTag("appbar_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "القائمة",
                            tint = GoldSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // 3 Core Pager Items + More
                val navTabs = listOf(
                    Triple(0, NavDestination.DASHBOARD.titleAr, Icons.Default.Dashboard),
                    Triple(1, NavDestination.ASSETS.titleAr, Icons.Default.AccountBalance),
                    Triple(2, NavDestination.PRICES.titleAr, Icons.Default.TrendingUp),
                    Triple(-1, NavDestination.MORE.titleAr, Icons.Default.MoreHoriz)
                )

                navTabs.forEach { (pageIndex, label, icon) ->
                    val isSelected = if (pageIndex == -1) {
                        activeSubScreen != null
                    } else {
                        activeSubScreen == null && pagerState.currentPage == pageIndex
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (pageIndex == -1) {
                                showMoreSheet = true
                            } else {
                                activeSubScreen = null
                                scope.launch {
                                    pagerState.animateScrollToPage(pageIndex)
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1E1700),
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${if (pageIndex == -1) "more" else "page_$pageIndex"}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeSubScreen != null) {
                // Render Sub-Screen (Transactions, Settings, Backup, About, Valuations, SimpleWeight)
                AnimatedContent(
                    targetState = activeSubScreen!!,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "SubScreenTransition"
                ) { subScreen ->
                    when (subScreen) {
                        NavDestination.VALUATIONS -> ValuationsScreen(
                            state = state,
                            onBack = { activeSubScreen = null },
                            onTakeSnapshot = { viewModel.recordValuationSnapshot() }
                        )

                        NavDestination.TRANSACTIONS -> TransactionsScreen(
                            transactions = state.transactions,
                            onBack = { activeSubScreen = null }
                        )

                        NavDestination.SETTINGS -> SettingsScreen(
                            state = state,
                            onBack = { activeSubScreen = null },
                            onSaveSettings = { baseline, target -> viewModel.updateSettings(baseline, target) },
                            onSaveCashBack = { amount, notes -> viewModel.updateCashBack(amount, notes) },
                            onResetBaseline = { viewModel.resetToInitialBaseline() },
                            onSavePriceAlerts = { enabled, threshold, targetHigh, targetLow ->
                                viewModel.updatePriceAlertSettings(enabled, threshold, targetHigh, targetLow)
                            },
                            onTriggerPriceAlertCheck = { viewModel.triggerPriceAlertCheckNow() },
                            onSendTestPriceAlert = { viewModel.sendTestPriceAlertNotification() },
                            onToggleAutoBullionCashBack = { enabled -> viewModel.updateAutoBullionCashBack(enabled) },
                            onUpdateSecuritySettings = { isAppLockEnabled, requireAuthOnSale, isBiometricEnabled, securityPin ->
                                viewModel.updateSecuritySettings(isAppLockEnabled, requireAuthOnSale, securityPin, isBiometricEnabled)
                            },
                            onClearPortfolioAndStartFresh = { baseline, target ->
                                viewModel.clearPortfolioAndStartFresh(baseline, target)
                            },
                            onUpdatePreferences = { currency, weightUnit ->
                                viewModel.updatePreferences(currency, weightUnit)
                            }
                        )

                        NavDestination.BACKUP -> BackupScreen(
                            state = state,
                            onBack = { activeSubScreen = null },
                            onExportBackup = { viewModel.exportBackupJson() },
                            onImportBackup = { json, onResult -> viewModel.importBackupJson(json, onResult) }
                        )

                        NavDestination.ABOUT -> AboutScreen(
                            state = state,
                            onBack = { activeSubScreen = null }
                        )

                        NavDestination.SIMPLE_WEIGHT -> SimpleGoldWeightScreen(
                            state = state,
                            onBack = { activeSubScreen = null },
                            onAddGoldPiece = { weight, karat, type ->
                                viewModel.addSimpleGoldItem(weight, karat, type)
                            }
                        )

                        else -> {
                            activeSubScreen = null
                        }
                    }
                }
            } else {
                // Horizontal Swipe Pager: Bidirectional swipe right and left
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_horizontal_pager")
                ) { page ->
                    when (page) {
                        0 -> DashboardScreen(
                            state = state,
                            onRefreshPrices = { viewModel.refreshPrices() },
                            onRecordSnapshot = { notes -> viewModel.recordValuationSnapshot(notes) },
                            onNavigateToBullion = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onNavigateToJewelry = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onNavigateToPrices = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onNavigateToSettings = {
                                activeSubScreen = NavDestination.SETTINGS
                            },
                            onTogglePriceSource = { useManual -> viewModel.togglePriceSource(useManual) },
                            onAddSimplePiece = { weight, karat, type -> viewModel.addSimpleGoldItem(weight, karat, type) },
                            onResetBaseline = { viewModel.resetToInitialBaseline() }
                        )

                        1 -> AssetsScreen(
                            state = state,
                            onAddItem = { title, type, weight, karat, cost, brand, condition, notes, barcode, imageUri, invoiceUri ->
                                viewModel.addGoldItem(title, type, weight, karat, cost, brand, condition, notes, barcode, imageUri, invoiceUri)
                            },
                            onUpdateItem = { viewModel.updateGoldItem(it) },
                            onDeleteItem = { viewModel.deleteGoldItem(it) },
                            onSellItem = { item, weightSold, sellPrice, cashBack, notes ->
                                viewModel.sellGoldItem(item, weightSold, sellPrice, cashBack, notes)
                            }
                        )

                        2 -> PricesScreen(
                            state = state,
                            onBack = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onRefresh = { viewModel.refreshPrices() },
                            onSaveManualPrices = { k24, k21, useManual ->
                                viewModel.updateManualPrices(k24, k21, useManual)
                            },
                            onAddSimplePiece = { weight, karat, type ->
                                viewModel.addSimpleGoldItem(weight, karat, type)
                            }
                        )
                    }
                }
            }
        }
    }

    // More Options Modal Bottom Sheet
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "أقسام المحفظة والعمليات والإعدادات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                MoreSheetItem(
                    icon = Icons.Default.History,
                    title = "سجل التقييمات التاريخية",
                    subtitle = "اللقطات المحفوظة لقيمة المحفظة عبر الزمن",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.VALUATIONS
                    }
                )

                MoreSheetItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "سجل العمليات والمبيعات (Audit Log)",
                    subtitle = "متابعة توثيق عمليات البيع والشراء والكاش باك",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.TRANSACTIONS
                    }
                )

                MoreSheetItem(
                    icon = Icons.Default.Scale,
                    title = "حاسبة وزن الذهب والتحويل",
                    subtitle = "حساب القيمة والوزن بالجرامات وإضافة قطع فورية",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.SIMPLE_WEIGHT
                    }
                )

                MoreSheetItem(
                    icon = Icons.Default.Settings,
                    title = "إعدادات المحفظة والهدف المالي",
                    subtitle = "تعديل الرصيد المرجعي والكاش باك والتنبيهات",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.SETTINGS
                    }
                )

                MoreSheetItem(
                    icon = Icons.Default.Backup,
                    title = "النسخ الاحتياطي والاستعادة",
                    subtitle = "تصدير واستيراد بيانات المحفظة بصيغة JSON",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.BACKUP
                    }
                )

                MoreSheetItem(
                    icon = Icons.Default.Info,
                    title = "عن التطبيق وقواعد التصفية",
                    subtitle = "مبادئ تقييم الصائغ للسبائك والكسر وشفافية المصادر",
                    onClick = {
                        showMoreSheet = false
                        activeSubScreen = NavDestination.ABOUT
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MoreSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
