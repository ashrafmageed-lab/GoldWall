package com.example.data.repository

import com.example.data.calculations.PortfolioCalculator
import com.example.data.local.AppDatabase
import com.example.data.local.GoldDao
import com.example.data.local.SettingsDao
import com.example.data.local.TransactionDao
import com.example.data.local.ValuationDao
import com.example.data.model.GoldItem
import com.example.data.model.GoldPriceState
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.TransactionRecord
import com.example.data.model.ValuationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.data.backup.BackupCodec

class GoldRepository(
    private val goldDao: GoldDao,
    private val settingsDao: SettingsDao,
    private val valuationDao: ValuationDao,
    private val transactionDao: TransactionDao,
    /** Runs the block atomically (wired to Room's withTransaction by the ViewModel). */
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() }
) {
    val allItems: Flow<List<GoldItem>> = goldDao.getAllItems()
    val bullionItems: Flow<List<GoldItem>> = goldDao.getItemsByType(GoldType.BULLION)
    val jewelryItems: Flow<List<GoldItem>> = goldDao.getItemsByType(GoldType.JEWELRY)
    val settings: Flow<PortfolioSettingsEntity> = settingsDao.getSettings().map {
        it ?: PortfolioSettingsEntity()
    }
    val valuations: Flow<List<ValuationRecord>> = valuationDao.getAllValuations()
    val transactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()

    suspend fun getSettingsSnapshot(): PortfolioSettingsEntity {
        return settingsDao.getSettingsSnapshot() ?: PortfolioSettingsEntity()
    }

    suspend fun addItem(item: GoldItem): Long {
        val id = goldDao.insertItem(item)
        val typeName = if (item.type == GoldType.BULLION) "سبيكة" else "مشغولات"
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "إضافة $typeName",
                title = item.title,
                details = "الوزن: ${item.weightGrams} جم - العيار: ${item.karat} - التكلفة: ${item.purchaseCostEgp ?: "غير محددة"}",
                amountOrWeight = "${item.weightGrams} جم"
            )
        )
        return id
    }

    suspend fun updateItem(item: GoldItem) {
        goldDao.updateItem(item)
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "تعديل قطعة",
                title = item.title,
                details = "تم تحديث البيانات - الوزن: ${item.weightGrams} جم - الحالة: ${item.condition}",
                amountOrWeight = "${item.weightGrams} جم"
            )
        )
    }

    suspend fun deleteItem(item: GoldItem) {
        goldDao.deleteItem(item)
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "حذف قطعة",
                title = item.title,
                details = "تم حذف القطعة من المحفظة (وزن: ${item.weightGrams} جم)",
                amountOrWeight = "-${item.weightGrams} جم"
            )
        )
    }

    suspend fun updateSettings(settings: PortfolioSettingsEntity) {
        settingsDao.insertOrUpdate(settings)
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "تعديل الإعدادات",
                title = "تحديث الرصيد المرجعي أو الهدف",
                details = "الرصيد المرجعي: ${settings.initialBaselineEgp} ج.م - الهدف: ${settings.targetGoalEgp} ج.م - كاش باك مؤكد: ${settings.confirmedCashBackEgp} ج.م"
            )
        )
    }

    suspend fun updateManualPrices(gram24Buy: Double, gram21Buy: Double, enabled: Boolean) {
        val current = getSettingsSnapshot()
        val updated = current.copy(
            manual24BuyPrice = gram24Buy,
            manual21BuyPrice = gram21Buy,
            useManualPrices = enabled
        )
        settingsDao.insertOrUpdate(updated)
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "تحديث يدوي للأسعار",
                title = "تعديل أسعار شراء الصائغ",
                details = "عيار 24: $gram24Buy ج.م - عيار 21: $gram21Buy ج.م (تفعيل اليدوي: $enabled)"
            )
        )
    }

    suspend fun updateCashBack(amount: Double, sourceNotes: String) {
        val current = getSettingsSnapshot()
        val updated = current.copy(
            confirmedCashBackEgp = amount,
            cashBackSourceNotes = sourceNotes,
            cashBackConfirmedDate = System.currentTimeMillis()
        )
        settingsDao.insertOrUpdate(updated)
        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "تسجيل Cash Back",
                title = "تحديث الكاش باك المؤكد",
                details = "المبلغ: $amount ج.م - المصدر: $sourceNotes",
                amountOrWeight = "+$amount ج.م"
            )
        )
    }

    suspend fun recordValuationSnapshot(
        bullionVal: Double,
        jewelryVal: Double,
        cashBack: Double,
        totalVal: Double,
        priceState: GoldPriceState,
        baseline: Double,
        notes: String = ""
    ): Long {
        val profit = PortfolioCalculator.calculateProfitVsBaseline(totalVal, baseline)
        val pct = PortfolioCalculator.calculatePercentageChange(totalVal, baseline)

        val record = ValuationRecord(
            bullionValueEgp = bullionVal,
            jewelryValueEgp = jewelryVal,
            cashBackEgp = cashBack,
            totalLiquidationEgp = totalVal,
            gram24PriceEgp = priceState.gram24BuyEgp,
            gram21PriceEgp = priceState.gram21BuyEgp,
            ounceUsdPrice = priceState.ounceUsd,
            profitVsBaselineEgp = profit,
            percentChange = pct,
            notes = notes
        )
        val id = valuationDao.insertValuation(record)

        val currentSettings = getSettingsSnapshot()
        settingsDao.insertOrUpdate(
            currentSettings.copy(
                lastValuationEgp = totalVal,
                lastValuationTime = System.currentTimeMillis()
            )
        )

        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "حفظ تقييم",
                title = "تسجيل تقييم المحفظة",
                details = "قيمة التصفية: $totalVal ج.م - الربح المرجعي: $profit ج.م ($pct%)",
                amountOrWeight = "$totalVal ج.م"
            )
        )
        return id
    }

    /**
     * Full backup (v2): items, portfolio settings (never the PIN), valuation history and audit log.
     */
    suspend fun exportBackupJson(items: List<GoldItem>, settings: PortfolioSettingsEntity): String {
        return BackupCodec.encode(
            items = items,
            settings = settings,
            valuations = valuationDao.getAllValuationsSnapshot(),
            transactions = transactionDao.getAllTransactionsSnapshot()
        )
    }

    /**
     * Restores a backup atomically. The file is fully validated BEFORE anything is written, and
     * all writes happen in one transaction: any failure leaves the current portfolio untouched.
     */
    suspend fun importBackupJson(jsonString: String): Result<Int> {
        val decoded = when (val result = BackupCodec.decode(jsonString)) {
            is BackupCodec.DecodeResult.Failure ->
                return Result.failure(IllegalArgumentException(result.errors.joinToString("\n")))
            is BackupCodec.DecodeResult.Success -> result.backup
        }
        return try {
            inTransaction {
                decoded.settings?.let { patch ->
                    settingsDao.insertOrUpdate(patch.applyTo(getSettingsSnapshot()))
                }
                goldDao.clearAllItems()
                goldDao.insertItems(decoded.items)
                decoded.valuations?.let {
                    valuationDao.clearValuations()
                    valuationDao.insertValuations(it)
                }
                decoded.transactions?.let {
                    transactionDao.clearTransactions()
                    transactionDao.insertTransactions(it)
                }
                transactionDao.insertTransaction(
                    TransactionRecord(
                        actionType = "استعادة نسخة احتياطية",
                        title = "نجاح الاستعادة",
                        details = "تمت استعادة ${decoded.items.size} عنصر بنجاح من ملف النسخة الاحتياطية"
                    )
                )
            }
            Result.success(decoded.items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sellItem(
        item: GoldItem,
        weightSoldGrams: Double,
        sellPricePerGram: Double,
        cashBackReceived: Double = 0.0,
        notes: String = ""
    ) {
        val totalSaleValue = (weightSoldGrams * sellPricePerGram) + cashBackReceived
        val costBasis = if (item.cost != null && item.weight > 0) {
            (item.cost / item.weight) * weightSoldGrams
        } else null
        val profit = if (costBasis != null) totalSaleValue - costBasis else null

        if (weightSoldGrams >= item.weight - 0.0001) {
            // Sold in full: delete item
            goldDao.deleteItem(item)
        } else {
            // Partial sale: reduce weight and cost
            val remainingWeight = item.weight - weightSoldGrams
            val remainingCost = if (item.cost != null && item.weight > 0) {
                item.cost * (remainingWeight / item.weight)
            } else null
            goldDao.updateItem(
                item.copy(
                    weight = remainingWeight,
                    cost = remainingCost
                )
            )
        }

        val typeLabel = if (item.type == GoldType.BULLION) "سبيكة" else "مشغولات"
        val profitText = if (profit != null) {
            val sign = if (profit >= 0) "+" else ""
            " (صافي الربح: $sign${String.format(java.util.Locale.US, "%,.2f", profit)} ج.م)"
        } else ""
        val cashBackText = if (cashBackReceived > 0) " + كاش باك ${String.format(java.util.Locale.US, "%,.2f", cashBackReceived)} ج.م" else ""

        transactionDao.insertTransaction(
            TransactionRecord(
                actionType = "بيع $typeLabel",
                title = "بيع: ${item.name}",
                details = "تم بيع $weightSoldGrams جم بسعر ${String.format(java.util.Locale.US, "%,.2f", sellPricePerGram)} ج.م/جم$cashBackText. إجمالي النقدية: ${String.format(java.util.Locale.US, "%,.2f", totalSaleValue)} ج.م$profitText. ${if (notes.isNotBlank()) "ملاحظات: $notes" else ""}".trim(),
                amountOrWeight = "-$weightSoldGrams جم"
            )
        )
    }

    /**
     * First-run only: seeds the reference portfolio when NO settings row exists yet.
     * It never runs again for an existing database, so it can never overwrite user data.
     */
    suspend fun ensureInitialDataSeeded() {
        try {
            if (settingsDao.getSettingsSnapshot() != null) return
            inTransaction {
                if (goldDao.getItemsCount() == 0) {
                    goldDao.insertItems(AppDatabase.getInitialBullionList())
                    goldDao.insertItems(AppDatabase.getInitialJewelryList())
                }
                settingsDao.insertOrUpdate(
                    PortfolioSettingsEntity(
                        id = 1,
                        initialBaselineEgp = PortfolioCalculator.INITIAL_BASELINE_EGP,
                        targetGoalEgp = PortfolioCalculator.INITIAL_TARGET_EGP,
                        confirmedCashBackEgp = 0.0,
                        cashBackSourceNotes = "لا يوجد كاش باك مؤكد مسجل حالياً",
                        isPortfolioInitialized = true
                    )
                )
                transactionDao.insertTransaction(
                    TransactionRecord(
                        actionType = "تهيئة المحفظة",
                        title = "تهيئة محفظة الذهب الابتدائية",
                        details = "تمت تهيئة 5 سبائك BTC ومشغولات عيار 21 تلقائياً (إجمالي وزن 157.5035 جم)",
                        amountOrWeight = "157.5035 جم"
                    )
                )
            }
        } catch (e: Exception) {
            // Seeding is best-effort; the app works with an empty portfolio.
        }
    }

    /** Non-destructive: re-adds the reference items ONLY when the portfolio is completely empty. */
    suspend fun restoreReferencePortfolioIfEmpty(): Boolean {
        return try {
            var restored = false
            inTransaction {
                if (goldDao.getItemsCount() == 0) {
                    goldDao.insertItems(AppDatabase.getInitialBullionList())
                    goldDao.insertItems(AppDatabase.getInitialJewelryList())
                    transactionDao.insertTransaction(
                        TransactionRecord(
                            actionType = "استعادة المحفظة المرجعية",
                            title = "إعادة القطع المرجعية",
                            details = "أُعيدت القطع المرجعية لأن المحفظة كانت فارغة"
                        )
                    )
                    restored = true
                }
            }
            restored
        } catch (e: Exception) {
            false
        }
    }

    /** Wipes all assets and starts a new empty custom portfolio with user-specified baseline and target. */
    suspend fun clearPortfolioAndStartFresh(newBaselineEgp: Double = 0.0, newTargetGoalEgp: Double = 1000000.0) {
        inTransaction {
            goldDao.clearAllItems()
            val current = getSettingsSnapshot()
            settingsDao.insertOrUpdate(
                current.copy(
                    initialBaselineEgp = newBaselineEgp,
                    targetGoalEgp = newTargetGoalEgp,
                    confirmedCashBackEgp = 0.0,
                    cashBackSourceNotes = "محفظة جديدة فارغة",
                    lastValuationEgp = 0.0,
                    lastValuationTime = 0L,
                    isPortfolioInitialized = true
                )
            )
            transactionDao.insertTransaction(
                TransactionRecord(
                    actionType = "تصفير وبدء جديد",
                    title = "مسح كافة البيانات وبدء محفظة جديدة",
                    details = "تم مسح جميع السبائك والمشغولات وتعيين الرصيد المرجعي: $newBaselineEgp ج.م والهدف: $newTargetGoalEgp ج.م",
                    amountOrWeight = "0.0 جم"
                )
            )
        }
    }
}
