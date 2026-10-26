---
name: gold-asset-tracker
description: Architectural patterns, domain logic, calculation formulas, real market candlestick aggregation, and UI recipes for building gold and precious metal portfolio tracking applications in Android using Jetpack Compose and Room.
---

# Gold and Precious Metal Asset Tracker Skill

## Overview
This skill provides the end-to-end domain expertise, mathematical foundations, architectural patterns, and Compose UI implementations required to build production-grade Gold and Precious Metal Portfolio Tracking apps on Android.

---

## 1. Domain Foundations & Financial Formulas

### A. Jeweler Buy Price vs Retail Sell Price (CRITICAL)
- **Retail Sell Price (سعر بيع المحل)**: The price a jeweler charges a consumer when selling gold. Includes workmanship (مصنعية), stamp tax (دمغة), and merchant profit margins.
- **Jeweler Buy Price (سعر شراء الصائغ / الكسر)**: The cash price a jeweler pays to buy gold back from the consumer. **Portfolio liquidation MUST ALWAYS be calculated using Jeweler Buy Price.** Using retail sell price inflates portfolio worth and misleads users.

### B. Bullion vs Jewelry Valuation Rules
1. **Bullion (السبائك)**:
   - Almost always 24 Karat (999.9 pure gold).
   - Typically packaged in tamper-evident security packaging with serial numbers (e.g., BTC, SAM).
   - Liquidates strictly at 24 Karat Jeweler Buy Price.
2. **Jewelry (المشغولات)**:
   - Typically 21 Karat (875) or 18 Karat (750).
   - Workmanship and stones (فصوص) are deducted.
   - Liquidates strictly as scrap gold (ذهب كسر) based on net gold weight multiplied by the Karat's Jeweler Buy Price.

### C. Bullion Cash Back Mechanics & Double-Count Prevention
- Certified bullion bars (e.g., BTC) offer a cash refund upon resale if the packaging is sealed:
  - Standard rate: **~28 EGP per gram** (`STANDARD_BULLION_CASH_BACK_PER_GRAM = 28.0`).
- **Rule of Single Addition**:
  - `calculateBullionLiquidation`: `weight * gram24BuyPrice`.
  - `calculateExpectedBullionCashBack`: `weight * 28.0`.
  - Cash back is added **strictly once** at the portfolio level in `calculateTotalLiquidation(bullionVal, jewelryVal, effectiveCashBack)`.
  - **Never** add cash back inside the individual item's liquidation price AND at the total level.
  - In the UI, display an explicit itemized breakdown surface inside the liquidation card proving to the user that Cash Back is included in the total.

### D. Zero-Division & Numerical Precision Safety
Always execute currency calculations using `BigDecimal` or guarded Double operations:
```kotlin
fun calculatePercentageChange(current: Double, baseline: Double): Double {
    if (baseline <= 0.0 || baseline.isNaN() || current.isNaN()) return 0.0
    return ((current - baseline) / baseline) * 100.0
}
```

---

## 2. Multi-Source Verified Spot Feeds & Real Market Candlesticks

### A. Global Spot Ounce Multi-Source Verification
Relying on a single HTML scraper (such as Investing.com) risks failures due to Cloudflare anti-bot bot challenges and HTML mutations. Use a verified multi-source JSON pipeline:
1. **Primary Spot Feed**: `https://api.gold-api.com/price/XAU` — Zero-auth, real-time XAU/USD spot rate (~$4,026+ USD).
2. **Secondary Spot Feed**: `https://api.coinbase.com/v2/prices/PAXG-USD/spot` — Real Paxos Gold asset backed 1:1 by physical London Good Delivery gold bars.
3. **Local Sagha Scraper / Manual Fallback**: Local market buy/sell rates for 24k, 21k, 18k in EGP with safe fallback figures.

### B. Real Market Historical Candlesticks (No Synthetic Curves)
To avoid synthetic straight or sine-wave lines on portfolio charts:
1. Fetch actual candlestick points from Binance public Kline feed (`PAXGUSDT`):
   - **1D (24 Hours)**: 24 hourly candles (`interval=1h`, `limit=24`).
   - **1W (7 Days)**: 42 four-hour candles (`interval=4h`, `limit=42`).
   - **1M (30 Days)**: 30 daily candles (`interval=1d`, `limit=30`).
   - **3M (90 Days)**: 90 daily candles (`interval=1d`, `limit=90`).
2. Scale portfolio valuation accurately over historical points:
   $$\text{HistoricalValuation}_i = \text{CurrentPortfolioValuation} \times \left(\frac{\text{CandlePrice}_i}{\text{LatestCandlePrice}}\right)$$
   This generates a genuine market curve mirroring real-world gold price highs, dips, and volatility.

---

## 3. Room Database Self-Seeding & Lifecycle Protection

### The Destructive Migration Pitfall
When bumping Room `version` with `.fallbackToDestructiveMigration()`:
- Room drops and recreates tables. Room's `onCreate()` callback is NOT invoked on destructive migration!
- Hook both `onDestructiveMigration` and `onOpen`, and always provide an explicit suspend method `ensureInitialDataSeeded()` invoked during `ViewModel.init`.

---

## 4. Jetpack Compose UI Patterns for Financial Portfolios

### A. Pull-to-Refresh with Material 3
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
PullToRefreshBox(
    isRefreshing = state.isRefreshing,
    onRefresh = onRefreshPrices,
    modifier = Modifier.fillMaxSize().testTag("dashboard_pull_to_refresh")
) { ... }
```

### B. Interactive Canvas Bézier Chart with Touch Scrubber
- Compute cubic Bézier control points between price points.
- Implement pointer gesture detection for interactive touch scrubbers showing exact date, valuation, and percentage delta.

---

## 5. Automated Verification Checklist

Every gold tracker implementation MUST pass these automated unit tests:
1. `sumOfBullionWeights_equalsExpected`: Sum of sample bullion items equals expected weight.
2. `liquidationValue_addsBullionAndJewelryAndConfirmedCashBackOnlyOnce`: Prevents double counting.
3. `referenceProfit_equalsLiquidationMinusBaseline`: Validates profit against initial benchmark capital.
4. `zeroDivisionProtection`: Checks that 0 baseline or target returns 0.0 without throwing exceptions.
5. `offlineNetworkFailure_handledGracefullyWithoutCrash`: Verifies offline fallback to manual mode.
6. `goldApiVerifiedOunceSource_configuredCorrectly`: Cites official ounce source URL.
7. `realMarketCandles_historicalPointsCalculation`: Validates calculation of portfolio value over real market candlestick points.
