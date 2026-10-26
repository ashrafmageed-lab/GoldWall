package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calculations.PortfolioCalculator
import com.example.data.local.AppDatabase
import com.example.data.model.GoldItem
import com.example.data.model.GoldPriceState
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.PriceStatus
import com.example.data.model.TransactionRecord
import com.example.data.model.ValuationRecord
import com.example.data.remote.GoldPriceFetcher
import com.example.data.repository.GoldRepository
import androidx.room.withTransaction
import com.example.data.backup.BackupFiles
import com.example.data.local.PriceCache
import com.example.security.PinGuard
import com.example.security.PinHasher
import com.example.security.PinResult
import com.example.ui.util.Formatters
import com.example.work.PriceAlertWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** LOADING until settings are read from Room, so no financial data is drawn before the lock decision. */
enum class VaultState { LOADING, LOCKED, UNLOCKED }

data class PortfolioUiState(
    val bullionList: List<GoldItem> = emptyList(),
    val jewelryList: List<GoldItem> = emptyList(),
    val settings: PortfolioSettingsEntity = PortfolioSettingsEntity(),
    val priceState: GoldPriceState = GoldPriceState(),
    val valuations: List<ValuationRecord> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val historicalPointsMap: Map<String, List<com.example.data.remote.HistoricalPricePoint>> = emptyMap(),
    val isRefreshing: Boolean = false,
    val userMessage: String? = null
) {
    // Derived values are computed once per state instance (lazy), not on every read
    // inside the Compose tree.
    private val useManualPriceSource: Boolean
        get() = settings.useManualPrices || priceState.sourceStatus == PriceStatus.UNAVAILABLE

    // Active jeweler buy prices considering manual price toggle
    val activeGram24BuyPrice: Double by lazy {
        if (useManualPriceSource) {
            if (settings.manual24BuyPrice > 0) settings.manual24BuyPrice else priceState.gram24BuyEgp
        } else {
            if (priceState.gram24BuyEgp > 0) priceState.gram24BuyEgp else (if (settings.manual24BuyPrice > 0) settings.manual24BuyPrice else 7131.5)
        }
    }

    val activeGram21BuyPrice: Double by lazy {
        if (useManualPriceSource) {
            if (settings.manual21BuyPrice > 0) settings.manual21BuyPrice else priceState.gram21BuyEgp
        } else {
            if (priceState.gram21BuyEgp > 0) priceState.gram21BuyEgp else (if (settings.manual21BuyPrice > 0) settings.manual21BuyPrice else 6240.0)
        }
    }

    // In manual mode there is no manual 18k input, so it follows the manual 24k price.
    val activeGram18BuyPrice: Double by lazy {
        if (useManualPriceSource) activeGram24BuyPrice * 18.0 / 24.0
        else if (priceState.gram18BuyEgp > 0) priceState.gram18BuyEgp
        else activeGram24BuyPrice * 18.0 / 24.0
    }

    // Accurate weights
    val totalBullionWeight: Double by lazy { PortfolioCalculator.calculateBullionWeight(bullionList) }

    val totalJewelryWeight: Double by lazy { PortfolioCalculator.calculateJewelryWeight(jewelryList) }

    val totalPhysicalWeight: Double by lazy { PortfolioCalculator.calculateTotalWeight(bullionList + jewelryList) }

    // Liquidation calculations (سعر شراء الصائغ من العميل)
    val bullionLiquidation: Double by lazy {
        PortfolioCalculator.calculateBullionLiquidation(
            bullionList, activeGram24BuyPrice, activeGram21BuyPrice, activeGram18BuyPrice
        )
    }

    val jewelryLiquidation: Double by lazy {
        PortfolioCalculator.calculateJewelryLiquidation(
            jewelryList, activeGram24BuyPrice, activeGram21BuyPrice, activeGram18BuyPrice
        )
    }

    // Bullion Cash Back ~28 EGP / gram (sealed, intact bars only)
    val expectedBullionCashBack: Double by lazy { PortfolioCalculator.calculateExpectedBullionCashBack(bullionList) }

    val effectiveCashBack: Double by lazy {
        if (settings.confirmedCashBackEgp > 0.0) {
            settings.confirmedCashBackEgp
        } else if (settings.autoAddBullionCashBack) {
            expectedBullionCashBack
        } else 0.0
    }

    val totalLiquidation: Double by lazy {
        PortfolioCalculator.calculateTotalLiquidation(bullionLiquidation, jewelryLiquidation, effectiveCashBack)
    }

    val profitVsBaseline: Double by lazy {
        PortfolioCalculator.calculateProfitVsBaseline(totalLiquidation, settings.initialBaselineEgp)
    }

    val percentChange: Double by lazy {
        PortfolioCalculator.calculatePercentageChange(totalLiquidation, settings.initialBaselineEgp)
    }

    val targetProgress: Double by lazy {
        PortfolioCalculator.calculateTargetProgress(totalLiquidation, settings.targetGoalEgp)
    }

    val diffFromLastValuation: Double by lazy {
        PortfolioCalculator.calculateValuationDifference(totalLiquidation, settings.lastValuationEgp)
    }
}

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val customPriceProvider: com.example.data.remote.GoldPriceProvider? = null
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = GoldRepository(
        database.goldDao(),
        database.settingsDao(),
        database.valuationDao(),
        database.transactionDao(),
        inTransaction = { block -> database.withTransaction { block() } }
    )
    private val priceProvider = customPriceProvider ?: com.example.data.remote.GoldPriceProvider()

    private val priceCache = PriceCache(application)

    // Start from the last REAL prices (disk cache). Without a cache the state is UNAVAILABLE, and the
    // user's own manual prices are used until the first successful fetch: no fake "recent" prices.
    private val _priceState = MutableStateFlow(
        priceCache.load() ?: GoldPriceState(
            lastUpdated = 0L,
            sourceStatus = PriceStatus.UNAVAILABLE,
            warningMessage = "لم تُحدَّث الأسعار بعد. تُستخدم أسعارك اليدوية حتى ينجح الاتصال."
        )
    )
    private val _isRefreshing = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _historicalPointsMap = MutableStateFlow<Map<String, List<com.example.data.remote.HistoricalPricePoint>>>(emptyMap())
    private val _isVaultManuallyLocked = MutableStateFlow(true)

    private val pinGuard = PinGuard(application)
    private var backgroundedAtElapsed = 0L

    val vaultState: StateFlow<VaultState> = combine(repository.settings, _isVaultManuallyLocked) { settings, manuallyLocked ->
        if (!settings.isAppLockEnabled) VaultState.UNLOCKED
        else if (manuallyLocked) VaultState.LOCKED
        else VaultState.UNLOCKED
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VaultState.LOADING)

    /** Emits only after settings were read from Room (no placeholder value). */
    val priceAlertsEnabledFlow = repository.settings.map { it.priceAlertsEnabled }

    /** Keep FLAG_SECURE on until we know the lock is disabled (hides screenshots / recents preview). */
    val secureWindowRequired: StateFlow<Boolean> = repository.settings
        .map { it.isAppLockEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val portfolioFlow = combine(
        repository.bullionItems,
        repository.jewelryItems,
        repository.settings,
        _priceState
    ) { bullion, jewelry, settings, prices ->
        Triple(bullion, jewelry, Pair(settings, prices))
    }

    private val auditFlow = combine(
        repository.valuations,
        repository.transactions,
        _isRefreshing,
        _userMessage
    ) { valuations, transactions, refreshing, msg ->
        Triple(valuations, transactions, Pair(refreshing, msg))
    }

    val uiState: StateFlow<PortfolioUiState> = combine(
        portfolioFlow,
        auditFlow,
        _historicalPointsMap
    ) { portfolioData, auditData, historicalPoints ->
        val bullion = portfolioData.first
        val jewelry = portfolioData.second
        val settings = portfolioData.third.first
        val prices = portfolioData.third.second
        val valuations = auditData.first
        val transactions = auditData.second
        val refreshing = auditData.third.first
        val msg = auditData.third.second

        Formatters.preferredCurrency = settings.preferredCurrency
        Formatters.preferredWeightUnit = settings.preferredWeightUnit
        if (prices.ounceUsd > 0 && prices.gram24BuyEgp > 0) {
            Formatters.usdExchangeRate = (prices.gram24BuyEgp * 31.1034768) / prices.ounceUsd
        }

        PortfolioUiState(
            bullionList = bullion,
            jewelryList = jewelry,
            settings = settings,
            priceState = if (settings.useManualPrices || prices.isManual) {
                prices.copy(
                    gram24BuyEgp = if (settings.manual24BuyPrice > 0) settings.manual24BuyPrice else prices.gram24BuyEgp,
                    gram21BuyEgp = if (settings.manual21BuyPrice > 0) settings.manual21BuyPrice else prices.gram21BuyEgp,
                    sourceName = if (prices.sourceName.isNotEmpty()) prices.sourceName else "إدخال يدوي مباشر",
                    sourceStatus = PriceStatus.MANUAL,
                    isManual = true
                )
            } else prices,
            valuations = valuations,
            transactions = transactions,
            historicalPointsMap = historicalPoints,
            isRefreshing = refreshing,
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PortfolioUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
            val startupSettings = repository.getSettingsSnapshot()
            Formatters.preferredCurrency = startupSettings.preferredCurrency
            Formatters.preferredWeightUnit = startupSettings.preferredWeightUnit
            // Older versions shipped a public default PIN (1234). A lock that only that PIN opens
            // gives no protection, so switch it off and ask the user to choose a real PIN.
            if (startupSettings.isAppLockEnabled && !PinHasher.hasPin(startupSettings.securityPin)) {
                database.settingsDao().insertOrUpdate(startupSettings.copy(isAppLockEnabled = false))
                _userMessage.value = "تم إيقاف قفل التطبيق لأنه كان يعتمد على الرمز الافتراضي. عيّن رمزاً جديداً من الإعدادات لإعادة تفعيله."
            }
            // Initial silent price check
            refreshPrices(showLoading = false)
            // Load real market historical points from global market
            loadHistoricalPoints()
        }
    }

    fun loadHistoricalPoints() {
        viewModelScope.launch {
            try {
                val map = mutableMapOf<String, List<com.example.data.remote.HistoricalPricePoint>>()
                listOf("1D", "1W", "1M", "3M").forEach { code ->
                    val pts = priceProvider.fetchHistoricalPoints(code)
                    if (pts.isNotEmpty()) {
                        map[code] = pts
                    }
                }
                if (map.isNotEmpty()) {
                    _historicalPointsMap.value = map
                }
            } catch (e: Exception) {
                // Keep default or previous points
            }
        }
    }

    fun refreshPrices(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _isRefreshing.value = true
            try {
                val result = priceProvider.fetchGoldPrices()
                if (result.isSuccess) {
                    val previous = _priceState.value
                    // A baseline exists only when the previous prices were real (never the placeholder state).
                    val previousPrice21 =
                        if (previous.sourceStatus != PriceStatus.UNAVAILABLE && previous.lastUpdated > 0L) previous.gram21BuyEgp else 0.0
                    val updated = result.getOrThrow()
                    _priceState.value = updated
                    withContext(Dispatchers.IO) { priceCache.save(updated) }

                    val currentSettings = repository.getSettingsSnapshot()
                    val diff = updated.gram21BuyEgp - previousPrice21
                    if (currentSettings.priceAlertsEnabled && currentSettings.priceAlertThresholdEgp > 0 &&
                        previousPrice21 > 0 && Math.abs(diff) >= currentSettings.priceAlertThresholdEgp
                    ) {
                        val direction = if (diff > 0) "ارتفاع 🟢" else "انخفاض 🔴"
                        _userMessage.value = "تنبيه حركة الذهب: $direction في عيار 21 بمقدار ${String.format(java.util.Locale.US, "%.1f", Math.abs(diff))} ج.م (السعر: ${updated.gram21BuyEgp} ج.م)"
                        repository.updateSettings(currentSettings.copy(lastNotifiedPrice21Egp = updated.gram21BuyEgp))
                    } else if (showLoading) {
                        _userMessage.value = "تم تحديث الأسعار بنجاح من ${updated.sourceName}"
                    }
                } else {
                    markPricesStale(result.exceptionOrNull()?.message, showLoading)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                markPricesStale(e.message, showLoading)
            } finally {
                if (showLoading) _isRefreshing.value = false
            }
        }
    }

    /**
     * A failed refresh keeps the last known prices and flags them as stale. It must NOT change the
     * user's settings (it used to flip the app into manual mode permanently after one dropped request).
     */
    private fun markPricesStale(reason: String?, showLoading: Boolean) {
        val current = _priceState.value
        val warning = reason?.takeIf { it.isNotBlank() }
            ?: "تعذر تحديث الأسعار عبر الإنترنت. الأسعار المعروضة هي آخر أسعار معروفة."
        _priceState.value = current.copy(
            sourceStatus = if (current.sourceStatus == PriceStatus.UNAVAILABLE) PriceStatus.UNAVAILABLE else PriceStatus.STALE,
            warningMessage = warning
        )
        if (showLoading) {
            _userMessage.value = "تعذر تحديث الأسعار. تُعرض آخر أسعار معروفة وسيعاد المحاولة لاحقاً."
        }
    }

    fun togglePriceSource(useManual: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(current.copy(useManualPrices = useManual))
            if (!useManual) {
                _userMessage.value = "جاري الاتصال بـ API أسعار الذهب..."
                refreshPrices(showLoading = true)
            } else {
                _priceState.value = _priceState.value.copy(
                    sourceStatus = PriceStatus.MANUAL,
                    sourceName = "إدخال يدوي مباشر",
                    warningMessage = null,
                    isManual = true
                )
                _userMessage.value = "تم التبديل إلى وضع الإدخال اليدوي للأسعار"
            }
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun addGoldItem(
        title: String,
        type: GoldType,
        weightGrams: Double,
        karat: Int,
        purchaseCostEgp: Double?,
        brand: String,
        condition: String,
        notes: String,
        barcode: String? = null,
        imageUri: String? = null,
        invoiceUri: String? = null
    ) {
        viewModelScope.launch {
            repository.addItem(
                GoldItem(
                    title = title,
                    type = type,
                    weightGrams = weightGrams,
                    karat = karat,
                    purchaseCostEgp = purchaseCostEgp,
                    brand = brand,
                    condition = condition,
                    notes = notes,
                    barcode = barcode,
                    imageUri = imageUri,
                    invoiceUri = invoiceUri
                )
            )
            _userMessage.value = "تمت إضافة القطعة بنجاح"
        }
    }

    fun clearPortfolioAndStartFresh(newBaseline: Double = 0.0, newTarget: Double = 1000000.0) {
        viewModelScope.launch {
            repository.clearPortfolioAndStartFresh(newBaseline, newTarget)
            _userMessage.value = "تم مسح المحفظة بنجاح وبدء محفظة جديدة"
        }
    }

    /**
     * Simple gold item insertion with minimal fields (weight and karat)
     */
    fun addSimpleGoldItem(
        weightGrams: Double,
        karat: Int,
        type: GoldType = if (karat == 24) GoldType.BULLION else GoldType.JEWELRY,
        name: String = if (type == GoldType.BULLION) "سبيكة ذهب ${weightGrams}جم عيار $karat" else "مشغولات ذهب ${weightGrams}جم عيار $karat"
    ) {
        viewModelScope.launch {
            repository.addItem(
                GoldItem(
                    name = name,
                    weight = weightGrams,
                    karat = karat,
                    type = type,
                    status = "سليمة"
                )
            )
            _userMessage.value = "تمت إضافة القطعة بنجاح (${weightGrams} جم - عيار $karat)"
        }
    }

    fun updateGoldItem(item: GoldItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            _userMessage.value = "تم تحديث القطعة بنجاح"
        }
    }

    fun deleteGoldItem(item: GoldItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _userMessage.value = "تم حذف القطعة"
        }
    }

    fun sellGoldItem(
        item: GoldItem,
        weightSoldGrams: Double,
        sellPricePerGram: Double,
        cashBackReceived: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.sellItem(item, weightSoldGrams, sellPricePerGram, cashBackReceived, notes)
            val totalCash = (weightSoldGrams * sellPricePerGram) + cashBackReceived
            _userMessage.value = "تم تسجيل بيع ${weightSoldGrams} جم بنجاح (المحصل: ${String.format(java.util.Locale.US, "%,.2f", totalCash)} ج.م)"
        }
    }

    fun updateSettings(baselineEgp: Double, targetGoalEgp: Double) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(
                current.copy(
                    initialBaselineEgp = baselineEgp,
                    targetGoalEgp = targetGoalEgp
                )
            )
            _userMessage.value = "تم حفظ إعدادات المحفظة"
        }
    }

    fun updateCashBack(amountEgp: Double, notes: String) {
        viewModelScope.launch {
            repository.updateCashBack(amountEgp, notes)
            _userMessage.value = "تم حفظ الكاش باك المؤكد"
        }
    }

    fun updatePriceAlertSettings(
        enabled: Boolean,
        threshold: Double,
        targetHigh: Double = 0.0,
        targetLow: Double = 0.0
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(
                current.copy(
                    priceAlertsEnabled = enabled,
                    priceAlertThresholdEgp = threshold,
                    priceTargetHighEgp = targetHigh,
                    priceTargetLowEgp = targetLow
                )
            )
            PriceAlertWorker.setEnabled(getApplication(), enabled)
            val msg = if (enabled) {
                val targets = mutableListOf<String>()
                if (threshold > 0) targets.add("تحرك ${threshold.toInt()} ج.م")
                if (targetHigh > 0) targets.add("هدف صعودي >= ${targetHigh.toInt()} ج.م")
                if (targetLow > 0) targets.add("هدف هبوطي <= ${targetLow.toInt()} ج.م")
                "تم تفعيل تنبيهات الأسعار عبر WorkManager (${targets.joinToString(" • ")})"
            } else {
                "تم إيقاف تنبيهات الأسعار"
            }
            _userMessage.value = msg
        }
    }

    fun triggerPriceAlertCheckNow() {
        viewModelScope.launch {
            PriceAlertWorker.checkNow(getApplication())
            _userMessage.value = "تم إرسال أمر فحص فوري للأسعار عبر WorkManager في الخلفية"
        }
    }

    fun sendTestPriceAlertNotification() {
        viewModelScope.launch {
            PriceAlertWorker.sendTestNotification(getApplication())
            _userMessage.value = "تم إرسال إشعار تجريبي بنجاح"
        }
    }

    fun updateAutoBullionCashBack(autoInclude: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(current.copy(autoAddBullionCashBack = autoInclude))
            _userMessage.value = if (autoInclude) "تم تفعيل إضافة كاش باك السبائك (28 ج.م/جم) تلقائياً" else "تم إلغاء الإضافة التلقائية للكاش باك"
        }
    }

    fun updatePreferences(preferredCurrency: String, preferredWeightUnit: String) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(
                current.copy(
                    preferredCurrency = preferredCurrency,
                    preferredWeightUnit = preferredWeightUnit
                )
            )
            Formatters.preferredCurrency = preferredCurrency
            Formatters.preferredWeightUnit = preferredWeightUnit
            val curLabel = if (preferredCurrency == "USD") "الدولار ($)" else "الجنيه (ج.م)"
            val weightLabel = if (preferredWeightUnit == "OUNCE") "الأونصة (oz)" else "الجرام (جم)"
            _userMessage.value = "تم تحديث تفضيلات العرض: $curLabel • $weightLabel"
        }
    }

    fun updateManualPrices(k24Buy: Double, k21Buy: Double, useManual: Boolean) {
        viewModelScope.launch {
            repository.updateManualPrices(k24Buy, k21Buy, useManual)
            _userMessage.value = if (useManual) "تم تفعيل الأسعار اليدوية" else "تم تفعيل الأسعار التلقائية"
        }
    }

    fun recordValuationSnapshot(notes: String = "") {
        viewModelScope.launch {
            val state = uiState.value
            repository.recordValuationSnapshot(
                bullionVal = state.bullionLiquidation,
                jewelryVal = state.jewelryLiquidation,
                cashBack = state.effectiveCashBack,
                totalVal = state.totalLiquidation,
                priceState = state.priceState,
                baseline = state.settings.initialBaselineEgp,
                notes = notes
            )
            _userMessage.value = "تم حفظ التقييم الحالي في السجل التاريخي"
        }
    }

    suspend fun exportBackupJson(): String {
        val state = uiState.value
        return repository.exportBackupJson(state.bullionList + state.jewelryList, state.settings)
    }

    fun importBackupJson(jsonString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            // Safety copy of the current portfolio BEFORE it is replaced.
            try {
                val snapshot = exportBackupJson()
                withContext(Dispatchers.IO) { BackupFiles.save(getApplication(), "pre-restore", snapshot) }
            } catch (e: Exception) {
                // Continue: a failed safety copy must not block a user-requested restore.
            }
            val result = repository.importBackupJson(jsonString)
            if (result.isSuccess) {
                _userMessage.value = "تمت استعادة النسخة الاحتياطية بنجاح"
            }
            onResult(result)
        }
    }

    fun resetToInitialBaseline() {
        viewModelScope.launch {
            // Non-destructive: user items are never deleted. Reference items return only if empty.
            val restoredItems = repository.restoreReferencePortfolioIfEmpty()
            val current = repository.getSettingsSnapshot()
            repository.updateSettings(
                current.copy(
                    initialBaselineEgp = PortfolioCalculator.INITIAL_BASELINE_EGP,
                    targetGoalEgp = PortfolioCalculator.INITIAL_TARGET_EGP
                )
            )
            _userMessage.value = if (restoredItems) {
                "أُعيدت القطع المرجعية والرصيد المرجعي (1,000,462 ج.م)"
            } else {
                "أُعيد الرصيد المرجعي والهدف فقط، ولم تُمسّ قطعك الحالية"
            }
        }
    }

    fun unlockVault() {
        _isVaultManuallyLocked.value = false
    }

    fun lockVault() {
        _isVaultManuallyLocked.value = true
    }

    /** The only PIN check in the app: hashed, rate-limited and persisted across restarts. */
    suspend fun verifyPin(enteredPin: String): PinResult {
        val now = System.currentTimeMillis()
        val remaining = pinGuard.remainingLockMillis(now)
        if (remaining > 0) return PinResult.LockedOut(remaining)

        val stored = repository.getSettingsSnapshot().securityPin
        val ok = withContext(Dispatchers.Default) { PinHasher.verify(enteredPin, stored) }
        if (ok) {
            pinGuard.reset()
            if (PinHasher.isLegacyPlain(stored)) {
                val upgraded = withContext(Dispatchers.Default) { PinHasher.hash(enteredPin) }
                val current = repository.getSettingsSnapshot()
                if (current.securityPin == stored) {
                    database.settingsDao().insertOrUpdate(current.copy(securityPin = upgraded))
                }
            }
            return PinResult.Success
        }
        val lockMs = pinGuard.recordFailure(now)
        return if (lockMs > 0) PinResult.LockedOut(lockMs) else PinResult.Wrong(pinGuard.attemptsLeft())
    }

    fun onAppBackgrounded() {
        backgroundedAtElapsed = SystemClock.elapsedRealtime()
    }

    /** Re-locks when the app was in the background longer than the grace period. */
    fun onAppForegrounded() {
        val at = backgroundedAtElapsed
        backgroundedAtElapsed = 0L
        if (at > 0L && SystemClock.elapsedRealtime() - at >= AUTO_LOCK_GRACE_MS) {
            _isVaultManuallyLocked.value = true
        }
    }

    private companion object {
        const val AUTO_LOCK_GRACE_MS = 15_000L
    }

    fun updateSecuritySettings(
        isAppLockEnabled: Boolean,
        requireAuthOnSale: Boolean,
        securityPin: String,
        isBiometricEnabled: Boolean
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsSnapshot()
            // [securityPin] is a NEW raw 4-digit PIN to hash, or anything else = keep the stored one.
            val isNewPin = PinHasher.isValidFormat(securityPin) && !PinHasher.isWeak(securityPin)
            val storedPin = if (isNewPin) {
                withContext(Dispatchers.Default) { PinHasher.hash(securityPin) }
            } else current.securityPin
            if (isNewPin) pinGuard.reset()

            // The lock can only be enabled when a real PIN exists.
            val lockOn = isAppLockEnabled && PinHasher.hasPin(storedPin)
            if (!current.isAppLockEnabled && lockOn) _isVaultManuallyLocked.value = false

            repository.updateSettings(
                current.copy(
                    isAppLockEnabled = lockOn,
                    requireAuthOnSale = requireAuthOnSale,
                    securityPin = storedPin,
                    isBiometricEnabled = isBiometricEnabled
                )
            )
            _userMessage.value = if (isAppLockEnabled && !lockOn) {
                "لا يمكن تفعيل القفل قبل تعيين رمز مرور"
            } else "تم تحديث إعدادات الأمان بنجاح"
        }
    }
}
