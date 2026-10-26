package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.ui.PortfolioUiState
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.util.Formatters
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class DistributionViewMode(val titleAr: String, val subtitleAr: String, val icon: ImageVector) {
    BY_KARAT("حسب العيار", "نقاء الذهب (24k / 21k / 18k)", Icons.Default.DonutLarge),
    BY_ASSET_TYPE("حسب النوع", "سبائك استثمارية ومصوغات", Icons.Default.PieChart),
    BY_MARKET_VALUE("حسب القيمة", "توزيع التسييل والكاش باك", Icons.Default.TrendingUp)
}

data class DistributionSegment(
    val id: String,
    val title: String,
    val subtitle: String,
    val metricValue: Double,
    val weightGrams: Double,
    val pureGoldGrams: Double,
    val marketValueEgp: Double,
    val percentage: Double,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector,
    val itemCount: Int = 1
)

@Composable
fun PortfolioDistributionDashboardView(
    state: PortfolioUiState,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(DistributionViewMode.BY_KARAT) }
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }

    val totalWeight = state.totalPhysicalWeight
    val totalLiquidation = state.totalLiquidation

    // Empty state safeguard
    if (state.bullionList.isEmpty() && state.jewelryList.isEmpty()) {
        return
    }

    // Build segments according to mode
    val segments = remember(selectedMode, state.bullionList, state.jewelryList, state.activeGram24BuyPrice, state.activeGram21BuyPrice, state.activeGram18BuyPrice, totalWeight, totalLiquidation) {
        buildDistributionSegments(
            mode = selectedMode,
            state = state
        )
    }

    // Reset selected segment if it does not exist in current mode
    LaunchedEffect(selectedMode) {
        selectedSegmentId = null
    }

    val activeSegment = segments.find { it.id == selectedSegmentId }

    // Pure 24k gold total calculation
    val totalPureGoldGrams = remember(state.bullionList, state.jewelryList) {
        val allItems = state.bullionList + state.jewelryList
        allItems.sumOf { it.weight * it.karat / 24.0 }
    }
    val purityPercentage = if (totalWeight > 0.0) (totalPureGoldGrams / totalWeight) * 100.0 else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .testTag("portfolio_distribution_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = GoldContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = selectedMode.icon,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "توزيع محفظة الذهب التفاعلي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = selectedMode.subtitleAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldContainer.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "نقاء ${String.format(Locale.US, "%.1f", purityPercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top Summary KPI Row (Weight, Pure Gold 24k, Market Value)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributionMetricTile(
                    title = "إجمالي الوزن",
                    value = Formatters.formatWeightShort(totalWeight, state.settings.preferredWeightUnit),
                    subtitle = "${(state.bullionList.size + state.jewelryList.size)} قطعة",
                    color = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )

                DistributionMetricTile(
                    title = "ذهب صافي 24k",
                    value = Formatters.formatWeightShort(totalPureGoldGrams, state.settings.preferredWeightUnit),
                    subtitle = "مكافئ نقي 999.9",
                    color = GoldTertiary,
                    modifier = Modifier.weight(1f)
                )

                DistributionMetricTile(
                    title = "قيمة التسييل",
                    value = Formatters.formatCurrency(totalLiquidation, state.settings.preferredCurrency),
                    subtitle = "بسعر شراء اليوم",
                    color = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Mode Selector Chips (Karat / Type / Value)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributionViewMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = mode },
                        label = {
                            Text(
                                text = mode.titleAr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) GoldPrimary else TextSecondary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = GoldPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else Color.Transparent
                        ),
                        modifier = Modifier.testTag("distribution_tab_${mode.name.lowercase()}")
                    )
                }
            }

            // Interactive D3/Recharts-inspired Donut Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                InteractiveDonutChart(
                    segments = segments,
                    selectedSegmentId = selectedSegmentId,
                    onSelectSegment = { id ->
                        selectedSegmentId = if (selectedSegmentId == id) null else id
                    },
                    modifier = Modifier.size(230.dp)
                )

                // Center Hole Callout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (activeSegment != null) {
                        Text(
                            text = activeSegment.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = activeSegment.color,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", activeSegment.percentage)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedMode == DistributionViewMode.BY_MARKET_VALUE)
                                Formatters.formatCurrency(activeSegment.marketValueEgp, state.settings.preferredCurrency)
                            else
                                Formatters.formatWeightShort(activeSegment.weightGrams, state.settings.preferredWeightUnit),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = when (selectedMode) {
                                DistributionViewMode.BY_KARAT -> "الوزن الكلي"
                                DistributionViewMode.BY_ASSET_TYPE -> "إجمالي القطع"
                                DistributionViewMode.BY_MARKET_VALUE -> "قيمة المحفظة"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = when (selectedMode) {
                                DistributionViewMode.BY_MARKET_VALUE -> Formatters.formatCurrency(totalLiquidation, state.settings.preferredCurrency)
                                else -> Formatters.formatWeightShort(totalWeight, state.settings.preferredWeightUnit)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "انقر على أي شريحة للتفاصيل",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Selected Segment Inspection Card (Tool-tip style callout)
            AnimatedVisibility(visible = activeSegment != null) {
                if (activeSegment != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeSegment.containerColor.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, activeSegment.color.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("segment_detail_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(activeSegment.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeSegment.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = activeSegment.color.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", activeSegment.percentage)}% من المحفظة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = activeSegment.color,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = activeSegment.color.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("الوزن الفعلي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        Formatters.formatWeight(activeSegment.weightGrams, state.settings.preferredWeightUnit),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("مكافئ 24k الصافي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        Formatters.formatWeight(activeSegment.pureGoldGrams, state.settings.preferredWeightUnit),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldTertiary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("القيمة السوقية للتسييل", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        Formatters.formatCurrency(activeSegment.marketValueEgp, state.settings.preferredCurrency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Distribution Breakdown Bars (Segment List)
            Text(
                text = "تفصيل الشرائح والنسب المئوية:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GoldSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                segments.forEach { segment ->
                    val isSelected = segment.id == selectedSegmentId
                    SegmentProgressItem(
                        segment = segment,
                        isSelected = isSelected,
                        preferredCurrency = state.settings.preferredCurrency,
                        preferredWeightUnit = state.settings.preferredWeightUnit,
                        onClick = {
                            selectedSegmentId = if (isSelected) null else segment.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionMetricTile(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SegmentProgressItem(
    segment: DistributionSegment,
    isSelected: Boolean,
    preferredCurrency: String,
    preferredWeightUnit: String,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (segment.percentage / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) segment.containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) segment.color else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("segment_item_${segment.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(segment.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = segment.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${segment.subtitle})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatCurrency(segment.marketValueEgp, preferredCurrency),
                        style = MaterialTheme.typography.labelSmall,
                        color = ProfitGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", segment.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = segment.color,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = segment.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الوزن: ${Formatters.formatWeightShort(segment.weightGrams, preferredWeightUnit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "ذهب صافي: ${Formatters.formatWeightShort(segment.pureGoldGrams, preferredWeightUnit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun InteractiveDonutChart(
    segments: List<DistributionSegment>,
    selectedSegmentId: String?,
    onSelectSegment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val totalMetric = segments.sumOf { it.metricValue }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(segments) {
                detectTapGestures { tapOffset ->
                    if (totalMetric <= 0.0) return@detectTapGestures
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = tapOffset.x - center.x
                    val dy = tapOffset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val outerRadius = size.width / 2f
                    val innerRadius = outerRadius * 0.58f

                    // If tap is within the donut band
                    if (distance in innerRadius..outerRadius * 1.15f) {
                        var angleDeg = (atan2(dy, dx) * 180f / PI).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Segments start at 270 deg (-90 deg, top)
                        var normalizedTapAngle = (angleDeg - 270f)
                        if (normalizedTapAngle < 0) normalizedTapAngle += 360f

                        var accumulatedAngle = 0f
                        for (seg in segments) {
                            val sweep = (seg.metricValue / totalMetric * 360f).toFloat()
                            if (normalizedTapAngle in accumulatedAngle..(accumulatedAngle + sweep)) {
                                onSelectSegment(seg.id)
                                break
                            }
                            accumulatedAngle += sweep
                        }
                    }
                }
            }
    ) {
        if (totalMetric <= 0.0) return@Canvas

        val strokeWidthNormal = size.width * 0.16f
        val strokeWidthSelected = size.width * 0.22f
        val radius = (size.width - strokeWidthSelected) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        var currentStartAngle = 270f // start from top (12 o'clock)
        val gapAngle = if (segments.size > 1) 2.5f else 0f

        segments.forEach { segment ->
            val isSelected = segment.id == selectedSegmentId
            val rawSweep = (segment.metricValue / totalMetric * 360f).toFloat()
            val animatedSweep = rawSweep * animationProgress.value
            val effectiveSweep = (animatedSweep - gapAngle).coerceAtLeast(0.5f)

            val currentStrokeWidth = if (isSelected) strokeWidthSelected else strokeWidthNormal

            // Radial shift outward for selected slice
            val midAngleRad = ((currentStartAngle + (effectiveSweep / 2f)) * PI / 180.0).toFloat()
            val offsetDistance = if (isSelected) 8.dp.toPx() else 0f
            val segmentCenter = Offset(
                x = center.x + cos(midAngleRad) * offsetDistance,
                y = center.y + sin(midAngleRad) * offsetDistance
            )

            drawArc(
                color = segment.color,
                startAngle = currentStartAngle + (gapAngle / 2f),
                sweepAngle = effectiveSweep,
                useCenter = false,
                topLeft = Offset(segmentCenter.x - radius, segmentCenter.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round
                )
            )

            currentStartAngle += rawSweep
        }
    }
}

private fun buildDistributionSegments(
    mode: DistributionViewMode,
    state: PortfolioUiState
): List<DistributionSegment> {
    val allItems = state.bullionList + state.jewelryList
    val totalWeight = allItems.sumOf { it.weight }
    if (totalWeight <= 0.0) return emptyList()

    return when (mode) {
        DistributionViewMode.BY_KARAT -> {
            val grouped = allItems.groupBy { it.karat }
            val list = mutableListOf<DistributionSegment>()

            // 24 Karat
            val k24Items = grouped[24] ?: emptyList()
            if (k24Items.isNotEmpty()) {
                val weight = k24Items.sumOf { it.weight }
                val marketVal = weight * state.activeGram24BuyPrice
                list.add(
                    DistributionSegment(
                        id = "karat_24",
                        title = "عيار 24 (ذهب خالص)",
                        subtitle = "سبائك 999.9",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = weight,
                        marketValueEgp = marketVal,
                        percentage = (weight / totalWeight) * 100.0,
                        color = Color(0xFFFFD700), // Pure Gold
                        containerColor = GoldContainer,
                        icon = Icons.Default.Diamond,
                        itemCount = k24Items.size
                    )
                )
            }

            // 21 Karat
            val k21Items = grouped[21] ?: emptyList()
            if (k21Items.isNotEmpty()) {
                val weight = k21Items.sumOf { it.weight }
                val pureGold = weight * 21.0 / 24.0
                val marketVal = weight * state.activeGram21BuyPrice
                list.add(
                    DistributionSegment(
                        id = "karat_21",
                        title = "عيار 21 (الشعبي والاستثماري)",
                        subtitle = "نقاء 875",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = pureGold,
                        marketValueEgp = marketVal,
                        percentage = (weight / totalWeight) * 100.0,
                        color = Color(0xFFF59E0B), // Vibrant Gold
                        containerColor = Color(0xFF382305),
                        icon = Icons.Default.Scale,
                        itemCount = k21Items.size
                    )
                )
            }

            // 18 Karat
            val k18Items = grouped[18] ?: emptyList()
            if (k18Items.isNotEmpty()) {
                val weight = k18Items.sumOf { it.weight }
                val pureGold = weight * 18.0 / 24.0
                val marketVal = weight * state.activeGram18BuyPrice
                list.add(
                    DistributionSegment(
                        id = "karat_18",
                        title = "عيار 18 (مشغولات فاخرة)",
                        subtitle = "نقاء 750",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = pureGold,
                        marketValueEgp = marketVal,
                        percentage = (weight / totalWeight) * 100.0,
                        color = Color(0xFFE07A5F), // Rose Gold / Bronze
                        containerColor = Color(0xFF35160E),
                        icon = Icons.Default.AccountBalance,
                        itemCount = k18Items.size
                    )
                )
            }

            // Other karats (if any)
            grouped.filter { it.key !in listOf(24, 21, 18) }.forEach { (karat, items) ->
                val weight = items.sumOf { it.weight }
                val pureGold = weight * (karat / 24.0)
                val marketVal = weight * state.activeGram24BuyPrice * (karat / 24.0)
                list.add(
                    DistributionSegment(
                        id = "karat_$karat",
                        title = "عيار $karat",
                        subtitle = "نقاء ${(karat * 1000 / 24)}",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = pureGold,
                        marketValueEgp = marketVal,
                        percentage = (weight / totalWeight) * 100.0,
                        color = Color(0xFF94A3B8),
                        containerColor = Color(0xFF1E293B),
                        icon = Icons.Default.Info,
                        itemCount = items.size
                    )
                )
            }

            list
        }

        DistributionViewMode.BY_ASSET_TYPE -> {
            val list = mutableListOf<DistributionSegment>()

            // Bullions
            if (state.bullionList.isNotEmpty()) {
                val weight = state.totalBullionWeight
                val pureGold = state.bullionList.sumOf { it.weight * it.karat / 24.0 }
                list.add(
                    DistributionSegment(
                        id = "type_bullion",
                        title = "السبائك الاستثمارية",
                        subtitle = "${state.bullionList.size} سبيكة مغلفة",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = pureGold,
                        marketValueEgp = state.bullionLiquidation,
                        percentage = (weight / totalWeight) * 100.0,
                        color = GoldPrimary,
                        containerColor = GoldContainer,
                        icon = Icons.Default.Diamond,
                        itemCount = state.bullionList.size
                    )
                )
            }

            // Jewelry
            if (state.jewelryList.isNotEmpty()) {
                val weight = state.totalJewelryWeight
                val pureGold = state.jewelryList.sumOf { it.weight * it.karat / 24.0 }
                list.add(
                    DistributionSegment(
                        id = "type_jewelry",
                        title = "المشغولات الذهبية",
                        subtitle = "${state.jewelryList.size} قطعة وحلي",
                        metricValue = weight,
                        weightGrams = weight,
                        pureGoldGrams = pureGold,
                        marketValueEgp = state.jewelryLiquidation,
                        percentage = (weight / totalWeight) * 100.0,
                        color = Color(0xFFE07A5F),
                        containerColor = Color(0xFF35160E),
                        icon = Icons.Default.Scale,
                        itemCount = state.jewelryList.size
                    )
                )
            }

            list
        }

        DistributionViewMode.BY_MARKET_VALUE -> {
            val list = mutableListOf<DistributionSegment>()
            val totalLiq = state.totalLiquidation.coerceAtLeast(1.0)

            // Bullion liquidation
            if (state.bullionLiquidation > 0.0) {
                list.add(
                    DistributionSegment(
                        id = "val_bullion",
                        title = "تسييل السبائك",
                        subtitle = "شراء الصائغ (24k)",
                        metricValue = state.bullionLiquidation,
                        weightGrams = state.totalBullionWeight,
                        pureGoldGrams = state.bullionList.sumOf { it.weight * it.karat / 24.0 },
                        marketValueEgp = state.bullionLiquidation,
                        percentage = (state.bullionLiquidation / totalLiq) * 100.0,
                        color = GoldPrimary,
                        containerColor = GoldContainer,
                        icon = Icons.Default.Diamond,
                        itemCount = state.bullionList.size
                    )
                )
            }

            // Jewelry liquidation
            if (state.jewelryLiquidation > 0.0) {
                list.add(
                    DistributionSegment(
                        id = "val_jewelry",
                        title = "تسييل المشغولات",
                        subtitle = "ذهب كسر بدون مصنعية",
                        metricValue = state.jewelryLiquidation,
                        weightGrams = state.totalJewelryWeight,
                        pureGoldGrams = state.jewelryList.sumOf { it.weight * it.karat / 24.0 },
                        marketValueEgp = state.jewelryLiquidation,
                        percentage = (state.jewelryLiquidation / totalLiq) * 100.0,
                        color = Color(0xFFE07A5F),
                        containerColor = Color(0xFF35160E),
                        icon = Icons.Default.Scale,
                        itemCount = state.jewelryList.size
                    )
                )
            }

            // Cashback buffer (if active)
            if (state.effectiveCashBack > 0.0) {
                list.add(
                    DistributionSegment(
                        id = "val_cashback",
                        title = "عوائد الكاش باك",
                        subtitle = "استرداد غلاف السبائك",
                        metricValue = state.effectiveCashBack,
                        weightGrams = 0.0,
                        pureGoldGrams = 0.0,
                        marketValueEgp = state.effectiveCashBack,
                        percentage = (state.effectiveCashBack / totalLiq) * 100.0,
                        color = ProfitGreen,
                        containerColor = ProfitGreenContainer,
                        icon = Icons.Default.Redeem,
                        itemCount = 1
                    )
                )
            }

            list
        }
    }
}
