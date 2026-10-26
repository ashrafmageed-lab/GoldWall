# خطة عمل وتطوير خزانة الذهب (الدفعة الثانية: إعادة الهيكلة والتمرير والبيع الذكي)

تهدف هذه الخطة إلى إعادة تنظيم وهيكلة تطبيق "خزانة الذهب" لإلغاء أي تكرار في عرض البيانات، توفير تجربة تصفح تفاعلية سلسة عبر السحب الأفقي (Horizontal Swipe Pager) بين 3 صفحات رئيسية محددة، حل مشكلة التفاف نص الكاش باك في كارت التسييل، وتفعيل إمكانية تعديل وحذف السبائك مع نافذة بيع ذكية تسجل العمليات في سجل المعاملات المالية.

---

## مراجعة قرارات المستخدم المؤكدة (Confirmed Decisions)

> [!IMPORTANT]
> تم تأكيد القرارات التالية مع المستخدم عبر الأسئلة التوضيحية وتُعتمد كأساس للتنفيذ:

- **تنظيم الصفحات والتمرير**: اعتماد 3 صفحات رئيسية داخل شريط التمرير الأفقي الموحد (`HorizontalPager`):
  1. **الرئيسية (Dashboard)**: ملخص التسييل المالي الصافي، الرسم البياني اللحظي المعتمد على شموع السوق الحية، وبطاقة الأونصة العالمية المحدثة.
  2. **الأصول (المحفظة - السبائك والمشغولات)**: إدارة تفصيلية لقطع الذهب (سبائك ومشغولات) مع زر البيع الذكي وتعديل وحذف القطع.
  3. **الأسعار والسوق (Market)**: أسعار العيارات المفصلة (24، 21، 18، الجنيه الذهب) ومصادر الأسعار مع الحاسبة السريعة.
- **آلية إدارة وبيع السبائك**: زر "بيع ذكي" تفاعلي يتيح بيع جزء أو كل السبيكة بسعر التسييل الحالي، وإنقاص رصيد السبيكة آلياً مع تسجيل حركة بيع مالية في جدول `TransactionRecord`. مع توفير خيار الحذف والتعديل المباشر.
- **إلغاء التكرار**: تنقية الشاشة الرئيسية لتكون مخصصة للملخص المالي الكلي والرسوم البيانية فقط، ونقل تفاصيل العيارات لصفحة الأسعار، وتفاصيل القطع لصفحة الأصول.

---

## البرومبت الاحترافي للهندسة والتطوير (Professional Engineering Prompt)

```text
Act as a Senior Android & Jetpack Compose Financial Software Engineer. Refactor the Gold Portfolio Tracking application with the following production specifications:

1. Horizontal Gesture Navigation & Unified Pager:
   - Implement Accompanist/Foundation HorizontalPager with 3 primary screens: Dashboard (Financial Summary & Real-market Chart), Assets (Bullions & Jewelry with segmented switcher), and Market (Karats & Live Feeds).
   - Synchronize top/bottom TabBar with PagerState, supporting smooth bidirectional swiping with fling physics and haptic feedback.

2. UI Layout Polish & Text Wrap Elimination:
   - Fix the Cash Back breakdown row inside HeroLiquidationCard: prevent vertical string wrapping or squished amounts by applying flexible weights, single-line typography, and responsive auto-sizing container math.

3. Redundancy Elimination:
   - Dedicate the Dashboard strictly to high-level financial health: Net Liquidation, Profit/Loss against benchmark, real-market candle Bézier chart, and global spot ounce banner.
   - Move granular Karat rate cards and quick calculators to the Market screen.
   - Consolidate Bullion bars and Jewelry inventory into the Assets screen with comprehensive CRUD and filtering.

4. Smart Bullion Liquidation & Inventory Management:
   - Add a "Smart Sell" action to Bullion and Jewelry cards: opens a confirmation dialog showing current jeweler buy price + expected cash back.
   - Allow partial quantity liquidation (e.g. sell 1 out of 2 bars) or full sale.
   - Automatically log a Sale Transaction in TransactionRecord (audit history) and adjust/delete inventory in Room DB.
   - Provide direct Edit (purchase price, weight, serial) and Delete actions.
```

---

## 1. نظرة عامة وتجربة المستخدم (User Experience & Flow)

### رحلة المستخدم والتنقل الأفقي:
```
┌─────────────────────────────────────────────────────────────────┐
│                     Top Bar: خزانة الذهب                         │
├─────────────────────────────────────────────────────────────────┤
│ [ الرئيسية (ملخص) ]  │ [ أصول الذهب (إدارة) ] │ [ الأسعار والسوق ] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ◄──────────── Swipe Left / Swipe Right Gestures ─────────────►  │
│                                                                 │
│  [Page 0: Dashboard]   [Page 1: Assets Hub]   [Page 2: Market]  │
│  - كارت التسييل الشامل   - تبويب السبائك الـ5   - عيار 24، 21، 18  │
│  - تفصيل الكاش باك      - تبويب المشغولات      - الجنيه الذهب     │
│  - شارت الشموع الحية     - أزرار: بيع، تعديل، حذف - مصادر الأسعار   │
│  - بطاقة الأونصة        - إضافة قطع جديدة      - حاسبة التحويل    │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                   Bottom Navigation Bar / Tabs                  │
└─────────────────────────────────────────────────────────────────┘
```

1. **إصلاح بطاقة التسييل والكاش باك**:
   - يتم تنظيم صف الكاش باك بحيث يأخذ العنوان `Modifier.weight(1f)` مع `softWrap = true`، بينما يأخذ المبلغ مساحة مخصصة غير قابلة للالتفاف `softWrap = false` و `maxLines = 1` بخط مالي واضح `FontWeight.SemiBold`.
   - ضبط الهوامش الداخلية (Padding) لتبدو البطاقة متناسقة بدون أي فراغات غير مبررة أو اختناق بصري.

2. **نافذة البيع الذكي (Smart Sell Dialog)**:
   - عند النقر على أيقونة "بيع" بجوار السبيكة:
     - تظهر نافذة تعرض السعر الحالي لشراء الصائغ (عيار 24) + الكاش باك المسترد لكل جرام.
     - اختيار عدد القطع المراد بيعها (إذا كانت الكمية أكثر من 1) أو الوزن.
     - حساب ناتج التسييل الإجمالي للعملية والأرباح المحققة مقارنة بسعر الشراء المرجعي.
     - عند التأكيد:
       - إذا تم بيع جزء: تحديث الكمية في جدول السبائك `GoldItem`.
       - إذا بيعت كاملة: حذف القطعة أو تعليمها كمباعة.
       - تسجيل قيد محاسبي فوري في `TransactionDao` كحركة بيع مع التاريخ والتوقيت والربح المحقق.

3. **إلغاء التكرار**:
   - نقل بطاقات أسعار العيارات المفصلة وحاسبة التحويل السريع من الشاشة الرئيسية إلى شاشة **"الأسعار والسوق"**.
   - تركيز الشاشة الرئيسية على المؤشرات التنفيذية: (رأس المال، إجمالي التسييل، الأرباح، شارت السوق الحقيقي، وسعر الأونصة الفوري).

---

## 2. الهيكلية التقنية وتدفق البيانات (Technical Architecture)

```
┌──────────────────────────────────────────────────────────────────┐
│                           MainViewModel                          │
│  - uiState: Combined Flow (Items, Settings, Prices, Candles)     │
│  - sellBullionItem(item, quantityToSell, salePricePerGram)       │
│  - deleteBullionItem(item) / updateBullionItem(item)             │
└───────────────┬──────────────────────────────────┬───────────────┘
                │                                  │
       ┌────────▼────────┐                ┌────────▼────────┐
       │     Room DB     │                │   Remote Feeds  │
       │ - GoldDao       │                │ - Gold-API (XAU)│
       │ - TransactionDao│                │ - Binance PAXG  │
       │ - SettingsDao   │                │ - iSagha EGP    │
       └─────────────────┘                └─────────────────┘
```

---

## 3. خطة التنفيذ الممنهجة (Implementation Steps)

1. **الخطوة 1: إصلاح كارت التسييل وتفاصيل الكاش باك**:
   - تعديل `HeroLiquidationCard` في `DashboardScreen.kt` لضبط أبعاد ومحاذاة صف الكاش باك والتسييل ومنع التفاف الأرقام بالطول.
2. **الخطوة 2: بناء الشاشة الموحدة للأصول مع البيع والتعديل**:
   - دمج إدارتي السبائك والمشغولات في شاشة أصول متطورة `AssetsScreen.kt` تحتوي على `TabRow` للتبديل بينهما.
   - إضافة حوار `SmartSellDialog` وحوار `EditItemDialog`، وتزويد `MainViewModel` بدوال البيع والتعديل والحذف وتوثيق المعاملات.
3. **الخطوة 3: تطبيق شريط التمرير الأفقي `HorizontalPager`**:
   - استخدام `HorizontalPager` و `rememberPagerState` في `MainActivity.kt` لربط الشاشات الثلاث بسلاسة مع دعم إيماءات السحب والتبديل التلقائي لتبويبات الشريط السفلي/العلوي.
4. **الخطوة 4: تنقية الشاشة الرئيسية وصفحة الأسعار**:
   - إزالة البطاقات المكررة من `DashboardScreen` ونقل أدوات التسعير والحاسبة إلى `PricesScreen`.
5. **الخطوة 5: التحقق والبناء**:
   - تشغيل اختبارات الوحدة للتأكد من سلامة حسابات البيع الجزئي والكاش باك وسجل المعاملات.
   - التحقق عبر `compile_applet` وتجميع ملف الـ APK النهائي.
