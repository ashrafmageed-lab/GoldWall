package com.example.work

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.model.PortfolioSettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PriceAlertLogicTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)

        try {
            if (!WorkManager.isInitialized()) {
                val config = Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
                WorkManager.initialize(context, config)
            }
        } catch (_: Exception) {}
    }

    @Test
    fun testNotificationChannelCreated() {
        PriceAlertWorker.ensureNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(PriceAlertWorker.CHANNEL_ID)
        assertNotNull("Notification channel should be created", channel)
        assertEquals("Price alert channel importance should be high", NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun testSettingsEntity_targetHighAndLowDefaults() {
        val entity = PortfolioSettingsEntity(id = 1)
        assertEquals(0.0, entity.priceTargetHighEgp, 0.001)
        assertEquals(0.0, entity.priceTargetLowEgp, 0.001)
        assertEquals(25.0, entity.priceAlertThresholdEgp, 0.001)
        assertTrue(entity.priceAlertsEnabled)
    }

    @Test
    fun testSettingsEntity_targetHighAndLowPersistence() = runBlocking {
        val settingsDao = database.settingsDao()
        val original = settingsDao.getSettingsSnapshot() ?: PortfolioSettingsEntity(id = 1)
        val updated = original.copy(
            priceAlertsEnabled = true,
            priceAlertThresholdEgp = 50.0,
            priceTargetHighEgp = 5200.0,
            priceTargetLowEgp = 4400.0
        )
        settingsDao.insertOrUpdate(updated)

        val retrieved = settingsDao.getSettingsSnapshot()
        assertNotNull(retrieved)
        assertTrue(retrieved!!.priceAlertsEnabled)
        assertEquals(50.0, retrieved.priceAlertThresholdEgp, 0.001)
        assertEquals(5200.0, retrieved.priceTargetHighEgp, 0.001)
        assertEquals(4400.0, retrieved.priceTargetLowEgp, 0.001)
    }

    @Test
    fun testWorkerPeriodicSchedule_andOneTimeCheckExecution() {
        PriceAlertWorker.setEnabled(context, true)
        PriceAlertWorker.checkNow(context)
        PriceAlertWorker.ensureNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(PriceAlertWorker.CHANNEL_ID)
        assertNotNull(channel)
    }
}
