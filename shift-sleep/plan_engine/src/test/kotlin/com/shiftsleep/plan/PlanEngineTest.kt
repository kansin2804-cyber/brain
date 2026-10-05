package com.shiftsleep.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanEngineTest {
    private val engine = PlanEngine(preset = TemplatePreset.HOSPITAL_3SHIFT)

    @Test
    fun dayShift_sleepEndsAtShiftStart() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.DAY))
        assertEquals(LocalTimeOfDay(7, 0), plan.sleepEnd)
        assertEquals("2026-10-06", plan.sleepEndDate)
        assertEquals("2026-10-05", plan.sleepStartDate)
        assertEquals(LocalTimeOfDay(23, 30), plan.sleepStart) // 7:00 - 7.5h
        assertTrue(plan.reason.contains("데이"))
        assertTrue(plan.allowEveningNap)
    }

    @Test
    fun dayShift_caffeineIsEightHoursBeforeSleep() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.DAY))
        // sleep 23:30 previous day → caffeine 15:30 previous day
        assertEquals(LocalTimeOfDay(15, 30), plan.caffeineCutoff)
        assertEquals("2026-10-05", plan.caffeineCutoffDate)
    }

    @Test
    fun eveningShift_sleepAfterShiftEndBuffer() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.EVENING))
        // end 23:00 + 1.25h = 00:15 next day
        assertEquals(LocalTimeOfDay(0, 15), plan.sleepStart)
        assertEquals("2026-10-07", plan.sleepStartDate)
        assertTrue(plan.reason.contains("이브닝"))
    }

    @Test
    fun nightShift_sleepAfterEnd_noEveningNap() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.NIGHT))
        assertEquals("2026-10-07", plan.sleepStartDate)
        // night ends 07:00 + 30min = 07:30
        assertEquals(LocalTimeOfDay(7, 30), plan.sleepStart)
        assertFalse(plan.allowEveningNap)
        assertTrue(plan.reason.contains("나이트"))
    }

    @Test
    fun consecutiveNights_stillProducePlan() {
        val prev = DayShift("2026-10-06", ShiftType.NIGHT)
        val night = DayShift("2026-10-07", ShiftType.NIGHT)
        val plan = engine.planFor(night, previous = prev)
        assertEquals(ShiftType.NIGHT, plan.shiftType)
        assertFalse(plan.allowEveningNap)
        assertEquals("2026-10-08", plan.sleepStartDate)
    }

    @Test
    fun nightToOff_discouragesEveningNap() {
        val prev = DayShift("2026-10-06", ShiftType.NIGHT)
        val off = DayShift("2026-10-07", ShiftType.OFF)
        val plan = engine.planFor(off, previous = prev)
        assertFalse(plan.allowEveningNap)
        assertTrue(plan.reason.contains("나이트→오프") || plan.reason.contains("오프"))
        assertEquals(LocalTimeOfDay(22, 30), plan.sleepStart)
    }

    @Test
    fun offBeforeDay_usesDayStyleSleep() {
        val off = DayShift("2026-10-06", ShiftType.OFF)
        val next = DayShift("2026-10-07", ShiftType.DAY)
        val plan = engine.planFor(off, next = next)
        assertEquals(ShiftType.OFF, plan.shiftType)
        assertEquals(LocalTimeOfDay(7, 0), plan.sleepEnd)
        assertTrue(plan.reason.contains("데이") || plan.reason.contains("오프"))
    }

    @Test
    fun plainOff_defaultNightSleep() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.OFF))
        assertEquals(LocalTimeOfDay(23, 0), plan.sleepStart)
        assertEquals(LocalTimeOfDay(6, 30), plan.sleepEnd)
        assertTrue(plan.allowEveningNap)
    }

    @Test
    fun customOvernight_treatedAsNightStyle() {
        val hours = ShiftHours(LocalTimeOfDay(22), LocalTimeOfDay(6))
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.CUSTOM, hours))
        assertEquals(ShiftType.CUSTOM, plan.shiftType)
        assertFalse(plan.allowEveningNap)
        assertTrue(plan.reason.contains("커스텀"))
    }

    @Test
    fun customDaytime_treatedAsDayStyle() {
        val hours = ShiftHours(LocalTimeOfDay(8), LocalTimeOfDay(16))
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.CUSTOM, hours))
        assertEquals(ShiftType.CUSTOM, plan.shiftType)
        assertEquals(LocalTimeOfDay(8, 0), plan.sleepEnd)
        assertTrue(plan.allowEveningNap)
    }

    @Test
    fun customWithoutHours_fallsBackToOffGuide() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.CUSTOM, null))
        assertEquals(ShiftType.CUSTOM, plan.shiftType)
        assertTrue(plan.reason.contains("커스텀") || plan.reason.contains("오프"))
    }

    @Test
    fun planWeek_coversAllDaysAndUsesNeighbors() {
        val days = listOf(
            DayShift("2026-10-06", ShiftType.DAY),
            DayShift("2026-10-07", ShiftType.EVENING),
            DayShift("2026-10-08", ShiftType.NIGHT),
            DayShift("2026-10-09", ShiftType.OFF),
        )
        val plans = engine.planWeek(days)
        assertEquals(4, plans.size)
        assertEquals(ShiftType.NIGHT, plans[2].shiftType)
        // off after night should discourage evening nap
        assertFalse(plans[3].allowEveningNap)
    }

    @Test
    fun factoryTemplate_12hDayAndNight() {
        val factory = PlanEngine(preset = TemplatePreset.FACTORY_12H)
        val day = factory.planFor(DayShift("2026-10-06", ShiftType.DAY))
        assertEquals(LocalTimeOfDay(7, 0), day.sleepEnd)

        val night = factory.planFor(DayShift("2026-10-06", ShiftType.NIGHT))
        // factory night 19–07 → overnight sleep start date next day
        assertEquals("2026-10-07", night.sleepStartDate)
        assertEquals(LocalTimeOfDay(7, 30), night.sleepStart)
    }

    @Test
    fun windDown_isThirtyMinutesBeforeSleep() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.DAY))
        assertEquals(LocalTimeOfDay(23, 0), plan.windDown)
        assertEquals(plan.sleepStartDate, plan.windDownDate)
    }
}

class ModelsTest {
    @Test
    fun localTime_rejectsInvalidHour() {
        assertFailsWith<IllegalArgumentException> { LocalTimeOfDay(24) }
        assertFailsWith<IllegalArgumentException> { LocalTimeOfDay(-1) }
    }

    @Test
    fun localTime_fromMinutesWraps() {
        assertEquals(LocalTimeOfDay(0, 30), LocalTimeOfDay.fromMinutes(24 * 60 + 30))
        assertEquals(LocalTimeOfDay(23, 0), LocalTimeOfDay.fromMinutes(-60))
    }

    @Test
    fun shiftHours_overnightDuration() {
        val hours = ShiftHours(LocalTimeOfDay(23), LocalTimeOfDay(7))
        assertTrue(hours.isOvernight())
        assertEquals(8 * 60, hours.durationMinutes())
    }

    @Test
    fun shiftHours_sameDayDuration() {
        val hours = ShiftHours(LocalTimeOfDay(7), LocalTimeOfDay(15))
        assertFalse(hours.isOvernight())
        assertEquals(8 * 60, hours.durationMinutes())
    }

    @Test
    fun templates_hospitalDefaults() {
        val day = Templates.defaultHours(ShiftType.DAY, TemplatePreset.HOSPITAL_3SHIFT)!!
        assertEquals(LocalTimeOfDay(7), day.start)
        assertEquals(LocalTimeOfDay(15), day.end)
        assertEquals("나이트", Templates.labelKo(ShiftType.NIGHT))
        assertEquals("병원 3교대", Templates.presetLabelKo(TemplatePreset.HOSPITAL_3SHIFT))
    }

    @Test
    fun dateMath_plusMinus() {
        assertEquals("2026-10-07", DateMath.plusDays("2026-10-06", 1))
        assertEquals("2026-10-05", DateMath.minusDays("2026-10-06", 1))
        assertEquals("2026-11-01", DateMath.plusDays("2026-10-31", 1))
    }
}
