package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ValuationRecord
import com.example.data.remote.HistoricalPricePoint
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ChartTimeframe(val labelAr: String, val code: String, val durationMillis: Long) {
    ONE_DAY("يومي 1D", "1D", 24L * 60 * 60 * 1000),
    ONE_WEEK("أسبوعي 1W", "1W", 7L * 24 * 60 * 60 * 1000),
    ONE_MONTH("شهري 1M", "1M", 30L * 24 * 60 * 60 * 1000),
    THREE_MONTHS("٣ شهور 3M", "3M", 90L * 24 * 60 * 60 * 1000)
}

data class ChartPoint(
    val timestamp: Long,
    val value: Double,
    val label: String
)

/** Where the plotted curve comes from, so the UI never presents an estimate as real history. */
enum class ChartKind { REAL_SNAPSHOTS, MARKET_ESTIMATE, NONE }

data class ChartData(val points: List<ChartPoint>, val kind: ChartKind)

@Composable
fun PortfolioValuationChart(
    currentLiquidation: Double,
    baselineEgp: Double,
    valuations: List<ValuationRecord>,
    historicalMarketPoints: Map<String, List<HistoricalPricePoint>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.ONE_MONTH) }
    var scrubbedPoint by remember { mutableStateOf<ChartPoint?>(null) }

    // Generate/filter data points based on selected timeframe
    val chartData = remember(selectedTimeframe, valuations, currentLiquidation, baselineEgp, historicalMarketPoints) {
        generateChartPoints(
            timeframe = selectedTimeframe,
            records = valuations,
            marketPoints = historicalMarketPoints,
            currentValue = currentLiquidation,
            baselineValue = baselineEgp
        )
    }
    val points = chartData.points

    val minVal = points.minOfOrNull { it.value } ?: (currentLiquidation * 0.98)
    val maxVal = points.maxOfOrNull { it.value } ?: (currentLiquidation * 1.02)
    val valRange = (maxVal - minVal).coerceAtLeast(100.0)

    val firstVal = points.firstOrNull()?.value ?: baselineEgp
    val lastVal = points.lastOrNull()?.value ?: currentLiquidation
    val periodDiff = lastVal - firstVal
    val periodPct = if (firstVal > 0) (periodDiff / firstVal) * 100.0 else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_valuation_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "حركة قيمة المحفظة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (historicalMarketPoints[selectedTimeframe.code]?.isNotEmpty() == true) "بيانات تداول الذهب الفعلي (XAU/USD) 🟢" else "متابعة تطور التصفية عبر الفترات الزمنية",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (historicalMarketPoints[selectedTimeframe.code]?.isNotEmpty() == true) ProfitGreen else TextMuted
                        )
                    }
                }
            }

            // Timeframe Filter Buttons (1D, 1W, 1M, 3M)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTimeframe.entries.forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GoldContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTimeframe = timeframe
                                scrubbedPoint = null
                            }
                            .border(
                                1.dp,
                                if (isSelected) GoldPrimary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("filter_${timeframe.code.lowercase()}")
                    ) {
                        Text(
                            text = timeframe.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) GoldPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Text(
                text = when (chartData.kind) {
                    ChartKind.REAL_SNAPSHOTS -> "المنحنى مبني على تقييمات محفوظة فعلياً."
                    ChartKind.MARKET_ESTIMATE -> "تقدير: قيمة محفظتك الحالية مُسقَطة على حركة سعر الذهب العالمي، ولا تعكس عمليات الشراء أو البيع السابقة. احفظ تقييماً دورياً للحصول على سجل حقيقي."
                    ChartKind.NONE -> "لا توجد بيانات كافية لرسم المنحنى. احفظ تقييماً من لوحة التحكم، أو تحقق من اتصال الإنترنت."
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            // Period Summary Statistics (High, Low, Change)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "أدنى تقييم", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = Formatters.formatCurrency(minVal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "أعلى تقييم", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = Formatters.formatCurrency(maxVal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "تغير الفترة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (periodDiff >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (periodDiff >= 0) ProfitGreen else LossRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (periodDiff >= 0) "+" else ""}${String.format(Locale.US, "%.2f", periodPct)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (periodDiff >= 0) ProfitGreen else LossRed
                        )
                    }
                }
            }

            // Interactive Scrubber Tooltip
            AnimatedVisibility(visible = scrubbedPoint != null) {
                scrubbedPoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pt.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = Formatters.formatCurrency(pt.value),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary
                                )
                            }
                            val diffVsBase = pt.value - baselineEgp
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "مقارنة بالأساس",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${if (diffVsBase >= 0) "+" else ""}${Formatters.formatCurrency(diffVsBase)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diffVsBase >= 0) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }
            }

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(points) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val x = change.position.x
                                val fraction = (x / size.width).coerceIn(0f, 1f)
                                val index = ((points.size - 1) * fraction).toInt()
                                scrubbedPoint = points.getOrNull(index)
                            },
                            onDragEnd = {
                                // Keep or reset
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (points.size < 2) return@Canvas

                    val w = size.width
                    val h = size.height
                    val paddingBottom = 20.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingLeft = 16.dp.toPx()
                    val paddingRight = 16.dp.toPx()

                    val chartW = w - paddingLeft - paddingRight
                    val chartH = h - paddingTop - paddingBottom

                    // Draw subtle grid lines
                    val gridColor = Color.White.copy(alpha = 0.06f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    for (i in 1..3) {
                        val y = paddingTop + chartH * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Map points to canvas coordinates
                    val coords = points.mapIndexed { idx, pt ->
                        val x = paddingLeft + (idx.toFloat() / (points.size - 1)) * chartW
                        val normalized = ((pt.value - minVal) / valRange).coerceIn(0.0, 1.0).toFloat()
                        val y = paddingTop + (1f - normalized) * chartH
                        Offset(x, y)
                    }

                    // Build smooth Bézier curve path
                    val curvePath = Path()
                    val fillPath = Path()

                    coords.forEachIndexed { i, pt ->
                        if (i == 0) {
                            curvePath.moveTo(pt.x, pt.y)
                            fillPath.moveTo(pt.x, pt.y)
                        } else {
                            val prev = coords[i - 1]
                            val midX = (prev.x + pt.x) / 2f
                            curvePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                            fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                        }
                    }

                    // Close fill path down to chart baseline
                    val lastCoord = coords.last()
                    val firstCoord = coords.first()
                    fillPath.lineTo(lastCoord.x, h - paddingBottom)
                    fillPath.lineTo(firstCoord.x, h - paddingBottom)
                    fillPath.close()

                    // Draw Gradient Fill under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GoldPrimary.copy(alpha = 0.35f),
                                GoldPrimary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = h - paddingBottom
                        )
                    )

                    // Draw Stroke Line
                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(GoldSecondary, GoldPrimary, GoldSecondary)
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw first and last points circles
                    drawCircle(
                        color = GoldPrimary,
                        radius = 4.dp.toPx(),
                        center = coords.last()
                    )

                    // Draw scrub indicator if active
                    scrubbedPoint?.let { activePt ->
                        val activeIdx = points.indexOf(activePt).coerceAtLeast(0)
                        val activeCoord = coords[activeIdx]

                        drawLine(
                            color = GoldPrimary.copy(alpha = 0.7f),
                            start = Offset(activeCoord.x, paddingTop),
                            end = Offset(activeCoord.x, h - paddingBottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        drawCircle(
                            color = Color(0xFF1E1700),
                            radius = 6.dp.toPx(),
                            center = activeCoord
                        )
                        drawCircle(
                            color = GoldPrimary,
                            radius = 4.5.dp.toPx(),
                            center = activeCoord
                        )
                    }
                }
            }

            // Chart footer note
            Text(
                text = "💡 يمكنك سحب إصبعك فوق الرسم البياني لمعاينة قيمة وتاريخ المحفظة عند أي نقطة.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Chart data from recorded snapshots (preferred) or, clearly labelled, a market-movement estimate.
 * It never fabricates points.
 */
private fun generateChartPoints(
    timeframe: ChartTimeframe,
    records: List<ValuationRecord>,
    marketPoints: Map<String, List<HistoricalPricePoint>> = emptyMap(),
    currentValue: Double,
    baselineValue: Double
): ChartData {
    val now = System.currentTimeMillis()
    val startTime = now - timeframe.durationMillis
    val dateFormat = when (timeframe) {
        ChartTimeframe.ONE_DAY -> SimpleDateFormat("hh:mm a", Locale("ar"))
        ChartTimeframe.ONE_WEEK -> SimpleDateFormat("EEE dd MMM", Locale("ar"))
        ChartTimeframe.ONE_MONTH -> SimpleDateFormat("dd MMM", Locale("ar"))
        ChartTimeframe.THREE_MONTHS -> SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
    }

    // 1. Prioritize real snapshots within window if available
    val windowRecords = records.filter { it.timestamp in startTime..now }.sortedBy { it.timestamp }
    if (windowRecords.size >= 2) {
        val list = windowRecords.map {
            ChartPoint(it.timestamp, it.totalLiquidationEgp, dateFormat.format(Date(it.timestamp)))
        }.toMutableList()
        if (list.last().timestamp < now - 60000) {
            list.add(ChartPoint(now, currentValue, "الآن"))
        }
        return ChartData(list, ChartKind.REAL_SNAPSHOTS)
    }

    // 2. Real Market Candles from Live Gold Exchange (XAU / PAXG spot candles from Binance feed)
    val candles = marketPoints[timeframe.code]
    if (candles != null && candles.isNotEmpty()) {
        val lastCandlePrice = candles.last().priceUsd
        if (lastCandlePrice > 0.0) {
            val list = mutableListOf<ChartPoint>()
            for (candle in candles) {
                val ratio = candle.priceUsd / lastCandlePrice
                val valuationAtCandle = currentValue * ratio
                val label = dateFormat.format(Date(candle.timestamp))
                list.add(ChartPoint(candle.timestamp, valuationAtCandle, label))
            }
            if (list.isNotEmpty()) {
                list[list.lastIndex] = list.last().copy(label = "الآن")
                return ChartData(list, ChartKind.MARKET_ESTIMATE)
            }
        }
    }

    // No invented data: without real snapshots or market candles, nothing is drawn.
    return ChartData(emptyList(), ChartKind.NONE)
}
