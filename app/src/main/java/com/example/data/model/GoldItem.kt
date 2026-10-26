package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

enum class GoldType {
    BULLION,   // سبائك
    JEWELRY    // مشغولات
}

/**
 * Room Entity: GoldItem
 * Fields: id, name, weight, karat, cost, status, type
 */
@Entity(tableName = "gold_items")
data class GoldItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val weight: Double,
    val karat: Int = 24,
    val cost: Double? = null,
    val status: String = "مغلفة وسليمة",
    val type: GoldType = GoldType.BULLION,
    val brand: String = "BTC",
    val notes: String = "",
    val cashBackRatePerGram: Double? = if (type == GoldType.BULLION) 28.0 else null,
    val serialNumber: String? = null,
    val barcode: String? = null,
    val imageUri: String? = null,
    val invoiceUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convenience constructor for callers using (title, type, weightGrams, ...)
    constructor(
        id: Long = 0,
        title: String,
        type: GoldType,
        weightGrams: Double,
        karat: Int = 24,
        purchaseCostEgp: Double? = null,
        brand: String = "BTC",
        condition: String = "مغلفة وسليمة",
        notes: String = "",
        cashBackRatePerGram: Double? = if (type == GoldType.BULLION) 28.0 else null,
        serialNumber: String? = null,
        barcode: String? = null,
        imageUri: String? = null,
        invoiceUri: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        name = title,
        weight = weightGrams,
        karat = karat,
        cost = purchaseCostEgp,
        status = condition,
        type = type,
        brand = brand,
        notes = notes,
        cashBackRatePerGram = cashBackRatePerGram,
        serialNumber = serialNumber,
        barcode = barcode,
        imageUri = imageUri,
        invoiceUri = invoiceUri,
        createdAt = createdAt
    )

    // Backwards-compatible aliases
    @get:Ignore
    val title: String get() = name

    @get:Ignore
    val weightGrams: Double get() = weight

    @get:Ignore
    val purchaseCostEgp: Double? get() = cost

    @get:Ignore
    val condition: String get() = status
}
