package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupCodec
import com.example.data.local.AppDatabase
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoldRepositoryDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: GoldRepository

    private fun newRepo(inTransaction: suspend (suspend () -> Unit) -> Unit = { block -> db.withTransaction { block() } }) =
        GoldRepository(db.goldDao(), db.settingsDao(), db.valuationDao(), db.transactionDao(), inTransaction)

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = newRepo()
    }

    @After fun tearDown() { db.close() }

    private fun item(title: String, grams: Double = 10.0) =
        GoldItem(title = title, type = GoldType.BULLION, weightGrams = grams, karat = 24)

    @Test fun firstRunSeedsExactlyOnce() = runBlocking {
        repo.ensureInitialDataSeeded()
        assertEquals(6, db.goldDao().getItemsCount())
        assertNotNull(db.settingsDao().getSettingsSnapshot())
        // A user edit afterwards must survive further seeding attempts.
        val extra = db.goldDao().insertItem(item("user item"))
        repo.ensureInitialDataSeeded()
        repo.ensureInitialDataSeeded()
        assertEquals(7, db.goldDao().getItemsCount())
        assertNotNull(db.goldDao().getItemById(extra))
    }

    @Test fun seedingNeverOverwritesExistingUserData() = runBlocking {
        db.settingsDao().insertOrUpdate(PortfolioSettingsEntity(id = 1, initialBaselineEgp = 5.0))
        db.goldDao().insertItem(item("mine"))
        repo.ensureInitialDataSeeded()
        assertEquals(1, db.goldDao().getItemsCount())
        assertEquals(5.0, db.settingsDao().getSettingsSnapshot()!!.initialBaselineEgp, 0.0)
    }

    @Test fun restoreReferenceOnlyWhenEmpty() = runBlocking {
        db.goldDao().insertItem(item("keep me"))
        assertFalse(repo.restoreReferencePortfolioIfEmpty())
        assertEquals(1, db.goldDao().getItemsCount())
        db.goldDao().clearAllItems()
        assertTrue(repo.restoreReferencePortfolioIfEmpty())
        assertEquals(6, db.goldDao().getItemsCount())
    }

    @Test fun validImportReplacesPortfolioAtomically() = runBlocking {
        db.goldDao().insertItem(item("old"))
        val json = BackupCodec.encode(listOf(item("new A", 3.0), item("new B", 4.0)), PortfolioSettingsEntity(), emptyList(), emptyList())
        val result = repo.importBackupJson(json)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        val titles = db.goldDao().getAllItems().first().map { it.title }.toSet()
        assertEquals(setOf("new A", "new B"), titles)
    }

    @Test fun invalidImportLeavesPortfolioUntouched() = runBlocking {
        db.goldDao().insertItem(item("precious"))
        val bad = """{"version":2,"items":[{"title":"x","type":"BULLION","weightGrams":-4,"karat":24}]}"""
        val result = repo.importBackupJson(bad)
        assertTrue(result.isFailure)
        assertEquals(1, db.goldDao().getItemsCount())
    }

    @Test fun failureInsideTransactionRollsEverythingBack() = runBlocking {
        db.goldDao().insertItem(item("precious"))
        val exploding = newRepo { block -> db.withTransaction { block(); throw IllegalStateException("boom") } }
        val json = BackupCodec.encode(listOf(item("replacement")), PortfolioSettingsEntity(), emptyList(), emptyList())
        val result = exploding.importBackupJson(json)
        assertTrue(result.isFailure)
        // The delete + insert were rolled back: the original item is still there.
        assertEquals(1, db.goldDao().getItemsCount())
        val only = db.goldDao().getAllItems().first().single()
        assertEquals("precious", only.title)
    }

    @Test fun migration6to7DropsTheDuplicateBarsTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE gold_bars (id INTEGER PRIMARY KEY NOT NULL, name TEXT)")
                    db.execSQL("CREATE TABLE gold_items (id INTEGER PRIMARY KEY NOT NULL, name TEXT)")
                    db.execSQL("INSERT INTO gold_items (id, name) VALUES (1, 'keep')")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val raw = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
        AppDatabase.MIGRATION_6_7.migrate(raw)
        raw.query("SELECT name FROM sqlite_master WHERE type='table' AND name='gold_bars'").use { assertFalse(it.moveToFirst()) }
        raw.query("SELECT COUNT(*) FROM gold_items").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        raw.close()
    }
}
