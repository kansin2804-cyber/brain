package com.shiftsleep.plan

import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue(plan.reason.contains("데이"))
    }

    @Test
    fun nightShift_sleepAfterEnd_noEveningNap() {
        val plan = engine.planFor(DayShift("2026-10-06", ShiftType.NIGHT))
        assertEquals("2026-10-07", plan.sleepStartDate)
        assertFalse(plan.allowEveningNap)
        assertTrue(plan.reason.contains("나이트"))
    }

    @Test
    fun nightToOff_discouragesEveningNap() {
        val prev = DayShift("2026-10-06", ShiftType.NIGHT)
        val off = DayShift("2026-10-07", ShiftType.OFF)
        val plan = engine.planFor(off, previous = prev)
        assertFalse(plan.allowEveningNap)
        assertTrue(plan.reason.contains("나이트→오프") || plan.reason.contains("오프"))
    }

    @Test
    fun planWeek_coversAllDays() {
        val days = listOf(
            DayShift("2026-10-06", ShiftType.DAY),
            DayShift("2026-10-07", ShiftType.EVENING),
            DayShift("2026-10-08", ShiftType.NIGHT),
            DayShift("2026-10-09", ShiftType.OFF),
        )
        val plans = engine.planWeek(days)
        assertEquals(4, plans.size)
        assertEquals(ShiftType.NIGHT, plans[2].shiftType)
    }

    @Test
    fun factoryTemplate_12hDay() {
        val factory = PlanEngine(preset = TemplatePreset.FACTORY_12H)
        val plan = factory.planFor(DayShift("2026-10-06", ShiftType.DAY))
        assertEquals(LocalTimeOfDay(7, 0), plan.sleepEnd)
    }
}
