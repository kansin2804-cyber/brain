package com.shiftsleep.app.data

import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.PlanEngine
import com.shiftsleep.plan.SleepPlan
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShiftRepository(
    private val db: AppDatabase,
) {
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    fun observePrefs(): Flow<UserPrefsEntity> =
        db.prefsDao().observe().map { it ?: UserPrefsEntity() }

    suspend fun ensurePrefs(): UserPrefsEntity {
        val existing = db.prefsDao().get()
        if (existing != null) return existing
        val created = UserPrefsEntity()
        db.prefsDao().upsert(created)
        return created
    }

    suspend fun savePrefs(prefs: UserPrefsEntity) {
        db.prefsDao().upsert(prefs)
    }

    fun observeWeek(anchor: LocalDate = LocalDate.now()): Flow<List<DayShift>> {
        val start = anchor.with(java.time.DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        return db.shiftDao()
            .observeRange(start.format(iso), end.format(iso))
            .map { list -> list.map { it.toDayShift() } }
    }

    fun observeAllShifts(): Flow<List<DayShift>> =
        db.shiftDao().observeAll().map { list -> list.map { it.toDayShift() } }

    suspend fun upsertShift(day: DayShift) {
        db.shiftDao().upsert(ShiftEntity.from(day))
    }

    suspend fun deleteShift(date: String) {
        db.shiftDao().delete(date)
    }

    suspend fun seedWeekIfEmpty(preset: TemplatePreset) {
        val today = LocalDate.now()
        val start = today.with(java.time.DayOfWeek.MONDAY)
        val existing = db.shiftDao().getAll()
        if (existing.isNotEmpty()) return
        val pattern = when (preset) {
            TemplatePreset.HOSPITAL_3SHIFT -> listOf(
                com.shiftsleep.plan.ShiftType.DAY,
                com.shiftsleep.plan.ShiftType.DAY,
                com.shiftsleep.plan.ShiftType.EVENING,
                com.shiftsleep.plan.ShiftType.EVENING,
                com.shiftsleep.plan.ShiftType.NIGHT,
                com.shiftsleep.plan.ShiftType.OFF,
                com.shiftsleep.plan.ShiftType.OFF,
            )
            TemplatePreset.FACTORY_12H -> listOf(
                com.shiftsleep.plan.ShiftType.DAY,
                com.shiftsleep.plan.ShiftType.DAY,
                com.shiftsleep.plan.ShiftType.NIGHT,
                com.shiftsleep.plan.ShiftType.NIGHT,
                com.shiftsleep.plan.ShiftType.OFF,
                com.shiftsleep.plan.ShiftType.OFF,
                com.shiftsleep.plan.ShiftType.OFF,
            )
            TemplatePreset.CUSTOM -> List(7) { com.shiftsleep.plan.ShiftType.OFF }
        }
        val entities = pattern.mapIndexed { index, type ->
            val date = start.plusDays(index.toLong()).format(iso)
            val hours = Templates.defaultHours(type, preset)
            ShiftEntity.from(DayShift(date, type, hours))
        }
        db.shiftDao().upsertAll(entities)
    }

    suspend fun planForDate(date: String): SleepPlan? {
        val prefs = ensurePrefs()
        val preset = TemplatePreset.valueOf(prefs.templatePreset)
        val engine = PlanEngine(preset = preset)
        val all = db.shiftDao().getAll().map { it.toDayShift() }.associateBy { it.date }
        val day = all[date] ?: return null
        val prev = all[LocalDate.parse(date).minusDays(1).format(iso)]
        val next = all[LocalDate.parse(date).plusDays(1).format(iso)]
        return engine.planFor(day, prev, next)
    }

    suspend fun planWeek(anchor: LocalDate = LocalDate.now()): List<SleepPlan> {
        val prefs = ensurePrefs()
        val preset = TemplatePreset.valueOf(prefs.templatePreset)
        val engine = PlanEngine(preset = preset)
        val start = anchor.with(java.time.DayOfWeek.MONDAY)
        val days = (0..6).mapNotNull { offset ->
            val date = start.plusDays(offset.toLong()).format(iso)
            db.shiftDao().get(date)?.toDayShift()
        }
        return engine.planWeek(days)
    }
}
