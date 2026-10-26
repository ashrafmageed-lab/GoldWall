package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricAuth
import com.example.security.PinResult
import com.example.security.findFragmentActivity
import com.example.security.secondsLeft
import com.example.security.toUserMessage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VaultLockScreen(
    isBiometricAvailable: Boolean,
    onVerifyPin: suspend (String) -> PinResult,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHint by remember { mutableStateOf(false) }

    var isChecking by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findFragmentActivity()

    // Counts the temporary lock-out down once too many wrong PINs were entered.
    LaunchedEffect(lockoutSeconds > 0) {
        while (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds -= 1
        }
        if (errorMessage?.startsWith("محاولات كثيرة") == true) errorMessage = null
    }

    fun checkPin(pin: String) {
        if (isChecking) return
        isChecking = true
        scope.launch {
            val result = onVerifyPin(pin)
            when (result) {
                PinResult.Success -> {
                    errorMessage = null
                    onUnlockSuccess()
                }
                is PinResult.Wrong -> {
                    errorMessage = result.toUserMessage()
                    enteredPin = ""
                }
                is PinResult.LockedOut -> {
                    lockoutSeconds = result.secondsLeft()
                    errorMessage = result.toUserMessage()
                    enteredPin = ""
                }
            }
            isChecking = false
        }
    }

    fun promptBiometric() {
        if (activity == null || !isBiometricAvailable) return
        BiometricAuth.authenticate(
            activity = activity,
            title = "فتح خزنة الذهب",
            subtitle = "تحقق من هويتك للمتابعة",
            negativeText = "استخدام الرمز",
            onSuccess = onUnlockSuccess
        )
    }

    LaunchedEffect(Unit) {
        if (isBiometricAvailable) promptBiometric()
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1B1710), Color(0xFF100E0A), Color(0xFF090806))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(24.dp)
            .testTag("vault_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Shield Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(GoldContainer)
                    .border(2.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "خزانة الذهب محمية",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "أدخل رمز المرور أو استخدم البصمة لفتح محفظتك وتفاصيل التسييل",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) GoldPrimary else Color.Transparent)
                            .border(
                                2.dp,
                                if (filled) GoldPrimary else TextMuted.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error Message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = LossRed,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(if (isBiometricAvailable) GoldContainer else Color.Transparent)
                                            .clickable(enabled = isBiometricAvailable) {
                                                promptBiometric()
                                            }
                                            .testTag("biometric_unlock_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "استخدام البصمة",
                                            tint = if (isBiometricAvailable) GoldPrimary else TextMuted,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                            .testTag("pin_delete_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "حذف",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                                            .clickable {
                                                if (enteredPin.length < 4 && lockoutSeconds == 0 && !isChecking) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin
                                                    errorMessage = null
                                                    if (newPin.length == 4) {
                                                        checkPin(newPin)
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { showHint = !showHint }) {
                Text(
                    text = if (showHint) "إن نسيت الرمز: امسح بيانات التطبيق من إعدادات الجهاز ثم استعد محفظتك من نسخة احتياطية (سيُحذف كل شيء غير المحفوظ)" else "نسيت رمز المرور؟",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary
                )
            }
        }
    }
}
