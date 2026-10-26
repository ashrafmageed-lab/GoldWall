package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_settings")
data class PortfolioSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val initialBaselineEgp: Double = 1000462.0,
    val targetGoalEgp: Double = 1250000.0,
    val confirmedCashBackEgp: Double = 0.0,
    val cashBackSourceNotes: String = "",
    val cashBackConfirmedDate: Long? = null,
    val lastValuationEgp: Double = 0.0,
    val lastValuationTime: Long = 0L,
    val manual24BuyPrice: Double = 7131.5,
    val manual21BuyPrice: Double = 6240.0,
    val useManualPrices: Boolean = false,
    val isDarkTheme: Boolean = true,
    val autoAddBullionCashBack: Boolean = true,
    val priceAlertsEnabled: Boolean = true,
    val priceAlertThresholdEgp: Double = 25.0,
    val lastNotifiedPrice21Egp: Double = 0.0,
    val isPortfolioInitialized: Boolean = false,
    val securityPin: String = "", // PBKDF2 hash (empty = no PIN chosen yet)
    val isAppLockEnabled: Boolean = false,
    val requireAuthOnSale: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val preferredCurrency: String = "EGP", // "EGP" or "USD"
    val preferredWeightUnit: String = "GRAM", // "GRAM" or "OUNCE"
    val priceTargetHighEgp: Double = 0.0, // Alert when 21k >= priceTargetHighEgp (if > 0)
    val priceTargetLowEgp: Double = 0.0,  // Alert when 21k <= priceTargetLowEgp (if > 0)
    val lastHighAlertNotifiedPrice: Double = 0.0,
    val lastLowAlertNotifiedPrice: Double = 0.0
)
