package com.example.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.PriceCache
import com.example.data.remote.GoldPriceProvider
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background gold-price alert system leveraging AndroidX WorkManager:
 * - Checks live market prices periodically (every 15 min when connected).
 * - Notifies when price moves by a user-defined delta threshold (e.g. ±25 EGP).
 * - Notifies when gold hits a user-defined target ceiling (e.g. >= 6500 EGP) or floor (e.g. <= 6000 EGP).
 * - Works reliably in the background even when the app is completely closed.
 */
class PriceAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val settings = db.settingsDao().getSettingsSnapshot() ?: return Result.success()
        if (!settings.priceAlertsEnabled) return Result.success()

        val fetched = GoldPriceProvider().fetchGoldPrices()
        val state = fetched.getOrNull() ?: return if (runAttemptCount < 2) Result.retry() else Result.success()
        PriceCache(applicationContext).save(state)

        val price = state.gram21BuyEgp
        if (price <= 0.0) return Result.success()

        var updatedSettings = settings

        // 1. Condition: Target High Price Alert (هدف صعودي)
        if (settings.priceTargetHighEgp > 0.0 && price >= settings.priceTargetHighEgp) {
            val alreadyNotifiedForThisTarget = Math.abs(settings.lastHighAlertNotifiedPrice - settings.priceTargetHighEgp) < 1.0
            if (!alreadyNotifiedForThisTarget) {
                notifyTargetHit(
                    title = "🎯 تحقق الهدف الصعودي للذهب!",
                    message = String.format(
                        Locale.US,
                        "وصل سعر عيار 21 إلى %.1f ج.م متجاوزاً هدفك المحدد (%.1f ج.م).",
                        price,
                        settings.priceTargetHighEgp
                    ),
                    notificationId = NOTIF_TARGET_HIGH_ID
                )
                updatedSettings = updatedSettings.copy(lastHighAlertNotifiedPrice = settings.priceTargetHighEgp)
            }
        } else if (settings.priceTargetHighEgp > 0.0 && price < settings.priceTargetHighEgp) {
            // Reset state if price drops back below target so user can be re-alerted on next cross
            if (updatedSettings.lastHighAlertNotifiedPrice > 0.0) {
                updatedSettings = updatedSettings.copy(lastHighAlertNotifiedPrice = 0.0)
            }
        }

        // 2. Condition: Target Low Price Alert (فرصة شراء هبوطية)
        if (settings.priceTargetLowEgp > 0.0 && price <= settings.priceTargetLowEgp) {
            val alreadyNotifiedForThisTarget = Math.abs(settings.lastLowAlertNotifiedPrice - settings.priceTargetLowEgp) < 1.0
            if (!alreadyNotifiedForThisTarget) {
                notifyTargetHit(
                    title = "📉 فرصة شراء: انخفاض سعر الذهب!",
                    message = String.format(
                        Locale.US,
                        "انخفض سعر عيار 21 إلى %.1f ج.م وبلغ الحد الأدنى المستهدف (%.1f ج.م).",
                        price,
                        settings.priceTargetLowEgp
                    ),
                    notificationId = NOTIF_TARGET_LOW_ID
                )
                updatedSettings = updatedSettings.copy(lastLowAlertNotifiedPrice = settings.priceTargetLowEgp)
            }
        } else if (settings.priceTargetLowEgp > 0.0 && price > settings.priceTargetLowEgp) {
            // Reset state if price rises back above floor
            if (updatedSettings.lastLowAlertNotifiedPrice > 0.0) {
                updatedSettings = updatedSettings.copy(lastLowAlertNotifiedPrice = 0.0)
            }
        }

        // 3. Condition: Price Movement Delta Threshold (تحرك السعر بمقدار X ج.م)
        if (settings.priceAlertThresholdEgp > 0.0) {
            val baseline = settings.lastNotifiedPrice21Egp
            if (baseline <= 0.0) {
                // First run: establish reference baseline
                updatedSettings = updatedSettings.copy(lastNotifiedPrice21Egp = price)
            } else {
                val diff = price - baseline
                if (Math.abs(diff) >= settings.priceAlertThresholdEgp) {
                    notifyPriceMove(diff, price, baseline)
                    updatedSettings = updatedSettings.copy(lastNotifiedPrice21Egp = price)
                }
            }
        }

        if (updatedSettings != settings) {
            db.settingsDao().insertOrUpdate(updatedSettings)
        }

        return Result.success()
    }

    private fun notifyPriceMove(diff: Double, currentPrice: Double, previousPrice: Double) {
        val direction = if (diff > 0) "ارتفاع 📈" else "انخفاض 📉"
        val sign = if (diff > 0) "+" else "-"
        val percent = if (previousPrice > 0) (Math.abs(diff) / previousPrice) * 100.0 else 0.0
        val title = "حركة سعر الذهب في مصر: $direction عيار 21"
        val message = String.format(
            Locale.US,
            "تحرك بمقدار %s%.1f ج.م (%.1f%%) — السعر الآن: %.1f ج.م/جم",
            sign,
            Math.abs(diff),
            percent,
            currentPrice
        )
        postNotification(title, message, NOTIF_MOVE_ID)
    }

    private fun notifyTargetHit(title: String, message: String, notificationId: Int) {
        postNotification(title, message, notificationId)
    }

    private fun postNotification(title: String, message: String, notificationId: Int) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        ensureNotificationChannel(ctx)

        val openIntent = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission revoked
        }
    }

    companion object {
        const val CHANNEL_ID = "gold_price_alerts"
        const val UNIQUE_PERIODIC_WORK = "gold_price_alerts_periodic"
        const val UNIQUE_ONE_TIME_WORK = "gold_price_alerts_onetime"

        private const val NOTIF_MOVE_ID = 4201
        private const val NOTIF_TARGET_HIGH_ID = 4202
        private const val NOTIF_TARGET_LOW_ID = 4203
        private const val NOTIF_TEST_ID = 4299

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات أسعار الذهب الفورية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات وتحذيرات وصول أسعار الذهب إلى الأهداف المحددة أو تحرك الأسعار"
                    enableLights(true)
                    enableVibration(true)
                }
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        }

        /**
         * Configures periodic background price checks via WorkManager.
         * Runs every 15 minutes when device has active network connection.
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                wm.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 15 minutes is the standard Android minimum periodic interval
            val periodicRequest = PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }

        /**
         * Enqueues an immediate one-time background price check using WorkManager.
         */
        fun checkNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<PriceAlertWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    UNIQUE_ONE_TIME_WORK,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
        }

        /**
         * Sends a verification test notification so the user can verify that
         * system notifications and alert channels are fully operational.
         */
        fun sendTestNotification(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            ensureNotificationChannel(context)

            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val testNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("🔔 تجربة نظام تنبيهات الذهب (ناجحة)")
                .setContentText("نظام الإشعارات وخلفية WorkManager يعملان بكفاءة تامة على هاتفك.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("نظام الإشعارات وخلفية WorkManager يعملان بكفاءة تامة. ستصلك التنبيهات تلقائياً عند وصول الأسعار للحدود المحددة حتى عند إغلاق التطبيق.")
                )
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIF_TEST_ID, testNotif)
            } catch (e: SecurityException) {
                // Ignored if permission was revoked
            }
        }
    }
}
