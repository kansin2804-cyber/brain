package com.shiftsleep.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.ShiftType
import com.shiftsleep.plan.TemplatePreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShiftRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ShiftRepository
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ShiftRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ensurePrefs_createsDefaults() = runBlocking {
        val prefs = repo.ensurePrefs()
        assertFalse(prefs.onboardingDone)
        assertEquals(TemplatePreset.HOSPITAL_3SHIFT.name, prefs.templatePreset)
        assertTrue(prefs.notifySleep)
        assertTrue(prefs.notifyWake)
        assertFalse(prefs.notifyCaffeine)
    }

    @Test
    fun seedWeekIfEmpty_insertsSevenDays() = runBlocking {
        repo.seedWeekIfEmpty(TemplatePreset.HOSPITAL_3SHIFT)
        val all = repo.observeAllShifts().first()
        assertEquals(7, all.size)
        // second call should not duplicate
        repo.seedWeekIfEmpty(TemplatePreset.HOSPITAL_3SHIFT)
        assertEquals(7, repo.observeAllShifts().first().size)
    }

    @Test
    fun upsertAndPlanForDate_returnsSleepPlan() = runBlocking {
        val today = LocalDate.now().format(iso)
        repo.upsertShift(DayShift(today, ShiftType.NIGHT))
        val plan = repo.planForDate(today)
        assertNotNull(plan)
        assertEquals(ShiftType.NIGHT, plan!!.shiftType)
        assertFalse(plan.allowEveningNap)
    }

    @Test
    fun planWeek_afterSeed_hasPlans() = runBlocking {
        repo.seedWeekIfEmpty(TemplatePreset.HOSPITAL_3SHIFT)
        val plans = repo.planWeek()
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.size <= 7)
    }

    @Test
    fun savePrefs_persistsTemplateChange() = runBlocking {
        val prefs = repo.ensurePrefs().copy(
            onboardingDone = true,
            templatePreset = TemplatePreset.FACTORY_12H.name,
        )
        repo.savePrefs(prefs)
        val loaded = repo.observePrefs().first()
        assertTrue(loaded.onboardingDone)
        assertEquals(TemplatePreset.FACTORY_12H.name, loaded.templatePreset)
    }

    @Test
    fun deleteShift_removesDay() = runBlocking {
        val date = LocalDate.now().format(iso)
        repo.upsertShift(DayShift(date, ShiftType.DAY))
        assertNotNull(repo.planForDate(date))
        repo.deleteShift(date)
        assertEquals(null, repo.planForDate(date))
    }
}
