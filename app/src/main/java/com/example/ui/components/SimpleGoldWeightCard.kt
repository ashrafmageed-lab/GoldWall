package com.example.ui.components

import com.example.util.toDoubleOrNullLocalized
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoldType
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters

/**
 * Compose UI component displaying total portfolio gold weight
 * with input fields to add a new gold piece (weight, karat).
 */
@Composable
fun SimpleGoldWeightCard(
    totalWeight: Double,
    bullionWeight: Double = 0.0,
    jewelryWeight: Double = 0.0,
    onAddGoldPiece: (weight: Double, karat: Int, type: GoldType) -> Unit,
    modifier: Modifier = Modifier
) {
    var weightInput by remember { mutableStateOf("") }
    var karatInput by remember { mutableStateOf("24") }
    var selectedType by remember { mutableStateOf(GoldType.BULLION) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val quickWeights = listOf(
        Pair("0.5", "0.5 جم"),
        Pair("1", "1 جم"),
        Pair("5", "5 جم"),
        Pair("10", "10 جم"),
        Pair("31.1", "أونصة (31.1 جم)"),
        Pair("50", "50 جم")
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simple_gold_weight_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Total Weight Header Display
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إجمالي وزن الذهب في المحفظة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = Formatters.formatWeight(totalWeight),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = GoldPrimary,
                        modifier = Modifier.testTag("total_gold_weight_text")
                    )

                    if (bullionWeight > 0.0 || jewelryWeight > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "السبائك: ${Formatters.formatWeightShort(bullionWeight)} | المشغولات: ${Formatters.formatWeightShort(jewelryWeight)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 2. Add New Gold Piece Form Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إضافة قطعة ذهبية جديدة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
            }

            // Quick Weight Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "أوزان شائعة سريعة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickWeights.take(3).forEach { (value, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (weightInput == value) GoldContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    weightInput = value
                                    errorMessage = null
                                }
                                .border(
                                    1.dp,
                                    if (weightInput == value) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (weightInput == value) GoldPrimary else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickWeights.drop(3).forEach { (value, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (weightInput == value) GoldContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    weightInput = value
                                    errorMessage = null
                                }
                                .border(
                                    1.dp,
                                    if (weightInput == value) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (weightInput == value) GoldPrimary else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Weight Input Field
            OutlinedTextField(
                value = weightInput,
                onValueChange = {
                    weightInput = it
                    errorMessage = null
                    successMessage = null
                },
                label = { Text("وزن القطعة بالجرام (Weight)") },
                placeholder = { Text("مثال: 10 أو 31.1") },
                trailingIcon = { Text("جم", color = GoldSecondary, modifier = Modifier.padding(end = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_piece_weight"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    cursorColor = GoldPrimary
                )
            )

            // Karat Selector & Custom Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عيار الذهب (Karat):",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(24, "عيار 24 (خالص)", GoldType.BULLION),
                        Triple(21, "عيار 21 (صاغة)", GoldType.JEWELRY),
                        Triple(18, "عيار 18 (مشغولات)", GoldType.JEWELRY)
                    ).forEach { (karatVal, label, defaultType) ->
                        val isSelected = karatInput == karatVal.toString()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GoldContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    karatInput = karatVal.toString()
                                    selectedType = defaultType
                                    errorMessage = null
                                }
                                .border(
                                    1.dp,
                                    if (isSelected) GoldPrimary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("karat_chip_$karatVal")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) GoldPrimary else TextSecondary,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Piece Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedType == GoldType.BULLION) GoldContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedType = GoldType.BULLION }
                        .border(
                            1.dp,
                            if (selectedType == GoldType.BULLION) GoldPrimary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "سبيكة (Bullion)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedType == GoldType.BULLION) GoldPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = if (selectedType == GoldType.BULLION) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedType == GoldType.JEWELRY) GoldContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedType = GoldType.JEWELRY }
                        .border(
                            1.dp,
                            if (selectedType == GoldType.JEWELRY) GoldPrimary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "مشغولات (Jewelry)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedType == GoldType.JEWELRY) GoldPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = if (selectedType == GoldType.JEWELRY) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Error & Success Feedback Messages
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = LossRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = successMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = successMessage ?: "",
                        color = ProfitGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val weight = weightInput.toDoubleOrNullLocalized()
                    val karat = karatInput.trim().toIntOrNull()

                    when {
                        weight == null || weight <= 0.0 -> {
                            errorMessage = "يرجى إدخال وزن صحيح أكبر من صفر."
                            successMessage = null
                        }
                        karat == null || karat !in 1..24 -> {
                            errorMessage = "يرجى تحديد أو إدخال عيار صحيح بين 1 و 24."
                            successMessage = null
                        }
                        else -> {
                            errorMessage = null
                            onAddGoldPiece(weight, karat, selectedType)
                            successMessage = "تمت إضافة القطعة ($weight جم - عيار $karat) بنجاح!"
                            weightInput = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_add_gold_piece_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إضافة القطعة إلى المحفظة",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
