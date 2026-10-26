package com.example.security

import androidx.compose.runtime.staticCompositionLocalOf

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsLeftBeforeDelay: Int) : PinResult
    data class LockedOut(val remainingMillis: Long) : PinResult
}

/** Arabic message for a failed result, or null on success. */
fun PinResult.toUserMessage(): String? = when (this) {
    PinResult.Success -> null
    is PinResult.Wrong ->
        if (attemptsLeftBeforeDelay > 0) "رمز المرور غير صحيح (متبقي $attemptsLeftBeforeDelay محاولات قبل التأخير)"
        else "رمز المرور غير صحيح"
    is PinResult.LockedOut -> "محاولات كثيرة خاطئة. حاول بعد ${secondsLeft()} ثانية"
}

fun PinResult.LockedOut.secondsLeft(): Int = ((remainingMillis + 999L) / 1000L).toInt().coerceAtLeast(1)

/**
 * Provided by MainActivity so deep composables (sale dialog, settings) can verify the PIN
 * through the single, rate-limited implementation in the ViewModel.
 */
val LocalPinVerifier = staticCompositionLocalOf<suspend (String) -> PinResult> {
    { PinResult.Wrong(0) }
}
