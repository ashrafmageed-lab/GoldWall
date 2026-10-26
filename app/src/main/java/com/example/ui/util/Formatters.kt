package com.example.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {

    private val cairoTimeZone = TimeZone.getTimeZone("Africa/Cairo")

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private val currencyFormat = DecimalFormat("#,##0.00", symbols)
    private val usdCurrencyFormat = DecimalFormat("$#,##0.00", symbols)
    private val weightFormat = DecimalFormat("#,##0.0000", symbols)
    private val shortWeightFormat = DecimalFormat("#,##0.00", symbols)
    private val percentFormat = DecimalFormat("#,##0.00", symbols)

    const val GRAMS_PER_TROY_OUNCE = 31.1034768

    @Volatile
    var preferredCurrency: String = "EGP" // "EGP" or "USD"

    @Volatile
    var preferredWeightUnit: String = "GRAM" // "GRAM" or "OUNCE"

    @Volatile
    var usdExchangeRate: Double = 50.0

    fun currencySymbol(currency: String = preferredCurrency): String {
        return if (currency == "USD") "$" else "ج.م"
    }

    fun weightUnitLabel(unit: String = preferredWeightUnit): String {
        return if (unit == "OUNCE") "أونصة" else "جم"
    }

    fun formatCurrency(amount: Double, currency: String = preferredCurrency): String {
        return if (currency == "USD") {
            val usd = if (usdExchangeRate > 0) amount / usdExchangeRate else amount
            usdCurrencyFormat.format(usd)
        } else {
            "${currencyFormat.format(amount)} ج.م"
        }
    }

    fun formatWeight(grams: Double, unit: String = preferredWeightUnit): String {
        return if (unit == "OUNCE") {
            val oz = grams / GRAMS_PER_TROY_OUNCE
            "${weightFormat.format(oz)} أونصة"
        } else {
            "${weightFormat.format(grams)} جم"
        }
    }

    fun formatWeightShort(grams: Double, unit: String = preferredWeightUnit): String {
        return if (unit == "OUNCE") {
            val oz = grams / GRAMS_PER_TROY_OUNCE
            "${shortWeightFormat.format(oz)} أونصة"
        } else {
            "${shortWeightFormat.format(grams)} جم"
        }
    }

    fun formatPercent(pct: Double): String {
        val sign = if (pct > 0) "+" else ""
        return "$sign${percentFormat.format(pct)}%"
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "غير محدد"
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar", "EG"))
        sdf.timeZone = cairoTimeZone
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        if (timestamp <= 0) return "غير محدد"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar", "EG"))
        sdf.timeZone = cairoTimeZone
        return sdf.format(Date(timestamp))
    }
}
