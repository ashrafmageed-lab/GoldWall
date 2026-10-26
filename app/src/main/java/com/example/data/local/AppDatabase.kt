package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.calculations.PortfolioCalculator
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.TransactionRecord
import com.example.data.model.ValuationRecord

@Database(
    entities = [
        GoldItem::class,
        PortfolioSettingsEntity::class,
        ValuationRecord::class,
        TransactionRecord::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goldDao(): GoldDao
    abstract fun settingsDao(): SettingsDao
    abstract fun valuationDao(): ValuationDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN isPortfolioInitialized INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Handled if column already exists
                }
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN securityPin TEXT NOT NULL DEFAULT '1234'")
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN isAppLockEnabled INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN requireAuthOnSale INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN isBiometricEnabled INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Handled if columns already exist
                }
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE gold_items ADD COLUMN barcode TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE gold_items ADD COLUMN imageUri TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE gold_items ADD COLUMN invoiceUri TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    // Handled if columns already exist
                }
            }
        }

        /** gold_bars duplicated gold_items (bullion) and was never read by any screen. */
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS gold_bars")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN preferredCurrency TEXT NOT NULL DEFAULT 'EGP'")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN preferredWeightUnit TEXT NOT NULL DEFAULT 'GRAM'")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN priceTargetHighEgp REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN priceTargetLowEgp REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN lastHighAlertNotifiedPrice REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE portfolio_settings ADD COLUMN lastLowAlertNotifiedPrice REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gold_vault_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    // Only databases older than v3 (never shipped with a schema we can migrate from)
                    // may be rebuilt. Newer versions must migrate explicitly: no silent data loss.
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInitialBullionList(): List<GoldItem> {
            val totalWeight = PortfolioCalculator.EXPECTED_INITIAL_BULLION_WEIGHT // 92.6034768
            val totalCost = PortfolioCalculator.INITIAL_BULLION_REFERENCE_COST_EGP // 673,000

            // Proportionally divide 673,000 EGP purchase cost across the 5 bars so each bar has its reference share
            return listOf(
                GoldItem(
                    id = 1,
                    title = "سبيكة BTC عيار 24 (0.5 جرام)",
                    type = GoldType.BULLION,
                    weightGrams = 0.5,
                    karat = 24,
                    purchaseCostEgp = (0.5 / totalWeight) * totalCost,
                    brand = "BTC",
                    condition = "مغلفة وسليمة",
                    notes = "سبيكة ذهب نقي 999.9 معتمدة بغلاف أمني أصلي"
                ),
                GoldItem(
                    id = 2,
                    title = "سبيكة BTC عيار 24 (1 جرام)",
                    type = GoldType.BULLION,
                    weightGrams = 1.0,
                    karat = 24,
                    purchaseCostEgp = (1.0 / totalWeight) * totalCost,
                    brand = "BTC",
                    condition = "مغلفة وسليمة",
                    notes = "سبيكة ذهب نقي 999.9 معتمدة بغلاف أمني أصلي"
                ),
                GoldItem(
                    id = 3,
                    title = "سبيكة BTC عيار 24 (10 جرامات)",
                    type = GoldType.BULLION,
                    weightGrams = 10.0,
                    karat = 24,
                    purchaseCostEgp = (10.0 / totalWeight) * totalCost,
                    brand = "BTC",
                    condition = "مغلفة وسليمة",
                    notes = "سبيكة ذهب نقي 999.9 معتمدة بغلاف أمني أصلي"
                ),
                GoldItem(
                    id = 4,
                    title = "سبيكة BTC عيار 24 (50 جرامًا)",
                    type = GoldType.BULLION,
                    weightGrams = 50.0,
                    karat = 24,
                    purchaseCostEgp = (50.0 / totalWeight) * totalCost,
                    brand = "BTC",
                    condition = "مغلفة وسليمة",
                    notes = "سبيكة ذهب نقي 999.9 معتمدة بغلاف أمني أصلي"
                ),
                GoldItem(
                    id = 5,
                    title = "سبيكة BTC أونصة تروي عيار 24 (31.1035 جرام)",
                    type = GoldType.BULLION,
                    weightGrams = 31.1034768,
                    karat = 24,
                    purchaseCostEgp = (31.1034768 / totalWeight) * totalCost,
                    brand = "BTC",
                    condition = "مغلفة وسليمة",
                    notes = "سبيكة أونصة تروي عالمية 1 Troy Ounce ذهب نقي 999.9"
                )
            )
        }

        fun getInitialJewelryList(): List<GoldItem> {
            return listOf(
                GoldItem(
                    id = 6,
                    title = "مشغولات ذهبية عيار 21 (مجموعة أولية)",
                    type = GoldType.JEWELRY,
                    weightGrams = PortfolioCalculator.EXPECTED_INITIAL_JEWELRY_WEIGHT, // 64.900
                    karat = 21,
                    purchaseCostEgp = null, // تكلفة الشراء غير محددة حسب المتطلبات
                    brand = "مشغولات ذهب مصري",
                    condition = "سليمة",
                    notes = "مشغولات وحلي ذهبية عيار 21، تكلفة الشراء غير مسجلة ويتم تقييمها بصافي سعر شراء الصائغ للذهب الكسر"
                )
            )
        }
    }
}
