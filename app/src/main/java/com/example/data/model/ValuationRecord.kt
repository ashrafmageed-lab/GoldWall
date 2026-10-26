package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "valuation_records")
data class ValuationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bullionValueEgp: Double,
    val jewelryValueEgp: Double,
    val cashBackEgp: Double,
    val totalLiquidationEgp: Double,
    val gram24PriceEgp: Double,
    val gram21PriceEgp: Double,
    val ounceUsdPrice: Double,
    val profitVsBaselineEgp: Double,
    val percentChange: Double,
    val notes: String = ""
)
