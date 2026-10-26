package com.example.data.model

enum class PriceStatus(val labelAr: String) {
    LIVE("مباشر"),
    RECENT("حديث"),
    STALE("قديم"),
    UNAVAILABLE("غير متاح"),
    MANUAL("إدخال يدوي")
}

data class GoldPriceState(
    val gram24BuyEgp: Double = 7131.5,    // سعر شراء الصائغ من العميل (عيار 24)
    val gram24SellEgp: Double = 7177.25,  // سعر بيع المحل للمستهلك (عيار 24)
    val gram21BuyEgp: Double = 6240.0,    // سعر شراء الصائغ من العميل (عيار 21)
    val gram21SellEgp: Double = 6280.0,   // سعر بيع المحل للمستهلك (عيار 21)
    val gram18BuyEgp: Double = 5348.5,    // سعر شراء الصائغ من العميل (عيار 18)
    val gram18SellEgp: Double = 5382.75,  // سعر بيع المحل للمستهلك (عيار 18)
    val ounceUsd: Double = 4026.90,       // سعر أونصة الذهب عالمياً بالدولار (Spot XAU/USD)
    val lastUpdated: Long = System.currentTimeMillis(),
    val sourceName: String = "آي صاغة (الصاغة المصرية)",
    val ounceSourceName: String = "بورصة الذهب العالمية (Gold-API.com XAU/USD)",
    val ounceSourceUrl: String = "https://api.gold-api.com/price/XAU",
    val sourceStatus: PriceStatus = PriceStatus.RECENT,
    val isManual: Boolean = false,
    val warningMessage: String? = null
)
