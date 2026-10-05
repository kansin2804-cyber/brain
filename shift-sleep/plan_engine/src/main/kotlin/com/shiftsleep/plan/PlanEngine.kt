package com.shiftsleep.plan

/**
 * Pure rule engine: DayShift (+ neighbors) → SleepPlan.
 * No Android, no IO. Tunable via [RuleConstants].
 */
class PlanEngine(
    private val constants: RuleConstants = RuleConstants(),
    private val preset: TemplatePreset = TemplatePreset.HOSPITAL_3SHIFT,
) {
    fun planFor(
        day: DayShift,
        previous: DayShift? = null,
        next: DayShift? = null,
    ): SleepPlan {
        val hours = day.hours ?: Templates.defaultHours(day.type, preset)
        return when (day.type) {
            ShiftType.DAY -> planDay(day, hours)
            ShiftType.EVENING -> planEvening(day, hours)
            ShiftType.NIGHT -> planNight(day, hours, previous)
            ShiftType.OFF -> planOff(day, previous, next)
            ShiftType.CUSTOM -> planCustom(day, hours)
        }
    }

    fun planWeek(days: List<DayShift>): List<SleepPlan> {
        val byDate = days.associateBy { it.date }
        return days.sortedBy { it.date }.map { day ->
            val prev = byDate[DateMath.minusDays(day.date, 1)]
            val next = byDate[DateMath.plusDays(day.date, 1)]
            planFor(day, prev, next)
        }
    }

    private fun planDay(day: DayShift, hours: ShiftHours?): SleepPlan {
        val start = hours?.start ?: LocalTimeOfDay(7)
        // Sleep the night before so wake ≈ shift start.
        val sleepEnd = start
        val sleepStart = LocalTimeOfDay.fromMinutes(
            sleepEnd.toMinutes() - (constants.targetSleepHours * 60).toInt(),
        )
        val sleepStartDate = if (sleepStart.toMinutes() > sleepEnd.toMinutes()) {
            DateMath.minusDays(day.date, 1)
        } else {
            DateMath.minusDays(day.date, 1)
        }
        // For day shift, sleep is always previous evening → morning of shift day.
        val caffeine = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() - (constants.caffeineLeadHours * 60).toInt(),
        )
        val caffeineDate = DateMath.minusDays(day.date, 1)
        val windDown = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() - constants.windDownMinutes,
        )
        return SleepPlan(
            date = day.date,
            shiftType = day.type,
            sleepStart = sleepStart,
            sleepStartDate = sleepStartDate,
            sleepEnd = sleepEnd,
            sleepEndDate = day.date,
            caffeineCutoff = caffeine,
            caffeineCutoffDate = caffeineDate,
            windDown = windDown,
            windDownDate = sleepStartDate,
            reason = "데이 근무에 맞춰 전날 취침 → 근무 시작 전 기상 가이드입니다.",
            allowEveningNap = true,
        )
    }

    private fun planEvening(day: DayShift, hours: ShiftHours?): SleepPlan {
        val end = hours?.end ?: LocalTimeOfDay(23)
        val sleepStart = LocalTimeOfDay.fromMinutes(
            end.toMinutes() + (constants.eveningBufferHours * 60).toInt(),
        )
        val sleepStartDate = if (sleepStart.toMinutes() < end.toMinutes()) {
            DateMath.plusDays(day.date, 1)
        } else {
            day.date
        }
        val sleepEnd = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() + (constants.targetSleepHours * 60).toInt(),
        )
        val sleepEndDate = if (sleepEnd.toMinutes() <= sleepStart.toMinutes()) {
            DateMath.plusDays(sleepStartDate, 1)
        } else {
            sleepStartDate
        }
        val caffeine = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() - (constants.caffeineLeadHours * 60).toInt(),
        )
        val caffeineDate = if (caffeine.toMinutes() > sleepStart.toMinutes()) {
            DateMath.minusDays(sleepStartDate, 1)
        } else {
            sleepStartDate
        }
        val windDown = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() - constants.windDownMinutes,
        )
        return SleepPlan(
            date = day.date,
            shiftType = day.type,
            sleepStart = sleepStart,
            sleepStartDate = sleepStartDate,
            sleepEnd = sleepEnd,
            sleepEndDate = sleepEndDate,
            caffeineCutoff = caffeine,
            caffeineCutoffDate = caffeineDate,
            windDown = windDown,
            windDownDate = sleepStartDate,
            reason = "이브닝 종료 후 완충 시간을 두고 취침하는 가이드입니다.",
            allowEveningNap = true,
        )
    }

    private fun planNight(
        day: DayShift,
        hours: ShiftHours?,
        previous: DayShift?,
    ): SleepPlan {
        val end = hours?.end ?: LocalTimeOfDay(7)
        // Primary sleep after night shift ends — same calendar morning.
        val sleepStart = LocalTimeOfDay.fromMinutes(
            end.toMinutes() + 30, // short wind-down after clock-out
        )
        // Night ends on next calendar morning for 23–07.
        val sleepStartDate = if (hours?.isOvernight() == true) {
            DateMath.plusDays(day.date, 1)
        } else {
            day.date
        }
        var sleepEnd = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() + (constants.targetSleepHours * 60).toInt(),
        )
        var sleepEndDate = if (sleepEnd.toMinutes() <= sleepStart.toMinutes()) {
            DateMath.plusDays(sleepStartDate, 1)
        } else {
            sleepStartDate
        }

        // Consecutive nights: keep sleep window similar to previous night plan.
        if (previous?.type == ShiftType.NIGHT) {
            val prevPlan = planNight(previous, previous.hours ?: hours, null)
            sleepEnd = prevPlan.sleepEnd
            // Keep duration; anchor start near previous pattern on this sleepStartDate.
            sleepEnd = LocalTimeOfDay.fromMinutes(
                sleepStart.toMinutes() + (constants.targetSleepHours * 60).toInt(),
            )
            sleepEndDate = if (sleepEnd.toMinutes() <= sleepStart.toMinutes()) {
                DateMath.plusDays(sleepStartDate, 1)
            } else {
                sleepStartDate
            }
        }

        // Caffeine mid-shift (before midpoint of night).
        val start = hours?.start ?: LocalTimeOfDay(23)
        val mid = LocalTimeOfDay.fromMinutes(
            start.toMinutes() + ((hours?.durationMinutes() ?: (8 * 60)) / 2),
        )
        val caffeineDate = day.date
        val windDown = LocalTimeOfDay.fromMinutes(
            sleepStart.toMinutes() - constants.windDownMinutes,
        )
        val windDownDate = if (windDown.toMinutes() > sleepStart.toMinutes()) {
            DateMath.minusDays(sleepStartDate, 1)
        } else {
            sleepStartDate
        }

        return SleepPlan(
            date = day.date,
            shiftType = day.type,
            sleepStart = sleepStart,
            sleepStartDate = sleepStartDate,
            sleepEnd = sleepEnd,
            sleepEndDate = sleepEndDate,
            caffeineCutoff = mid,
            caffeineCutoffDate = caffeineDate,
            windDown = windDown,
            windDownDate = windDownDate,
            reason = "나이트 종료 후 ${constants.nightSleepWithinMinutes}분 이내 주수면을 권장하는 가이드입니다.",
            allowEveningNap = false,
        )
    }

    private fun planOff(
        day: DayShift,
        previous: DayShift?,
        next: DayShift?,
    ): SleepPlan {
        if (previous?.type == ShiftType.NIGHT) {
            // Recovery day after night: main sleep already planned on night end morning;
            // suggest evening bedtime for day rhythm return, discourage long evening nap.
            val sleepStart = LocalTimeOfDay(22, 30)
            val sleepEnd = LocalTimeOfDay(6, 30)
            val caffeine = LocalTimeOfDay(14, 30)
            return SleepPlan(
                date = day.date,
                shiftType = day.type,
                sleepStart = sleepStart,
                sleepStartDate = day.date,
                sleepEnd = sleepEnd,
                sleepEndDate = DateMath.plusDays(day.date, 1),
                caffeineCutoff = caffeine,
                caffeineCutoffDate = day.date,
                windDown = LocalTimeOfDay(22, 0),
                windDownDate = day.date,
                reason = "나이트→오프: 낮 리듬 복귀를 위해 저녁 취침을 권장하고, 긴 저녁 낮잠은 피하세요.",
                allowEveningNap = false,
            )
        }
        if (next?.type == ShiftType.DAY) {
            return planDay(
                DayShift(day.date, ShiftType.DAY, next.hours),
                next.hours ?: Templates.defaultHours(ShiftType.DAY, preset),
            ).copy(
                date = day.date,
                shiftType = ShiftType.OFF,
                reason = "내일 데이를 위해 전날 오프에 맞춘 취침 가이드입니다.",
            )
        }
        // Default off: normal night sleep
        return SleepPlan(
            date = day.date,
            shiftType = day.type,
            sleepStart = LocalTimeOfDay(23, 0),
            sleepStartDate = day.date,
            sleepEnd = LocalTimeOfDay(6, 30),
            sleepEndDate = DateMath.plusDays(day.date, 1),
            caffeineCutoff = LocalTimeOfDay(15, 0),
            caffeineCutoffDate = day.date,
            windDown = LocalTimeOfDay(22, 30),
            windDownDate = day.date,
            reason = "오프일 기본 수면 리듬 가이드입니다.",
            allowEveningNap = true,
        )
    }

    private fun planCustom(day: DayShift, hours: ShiftHours?): SleepPlan {
        if (hours == null) {
            return planOff(day, null, null).copy(
                shiftType = ShiftType.CUSTOM,
                reason = "커스텀 근무 시간이 없어 오프 가이드를 적용했습니다.",
            )
        }
        return if (hours.isOvernight()) {
            planNight(day.copy(type = ShiftType.NIGHT), hours, null).copy(
                shiftType = ShiftType.CUSTOM,
                reason = "커스텀 야간형 근무에 맞춘 취침 가이드입니다.",
            )
        } else if (hours.start.hour < 12) {
            planDay(day.copy(type = ShiftType.DAY), hours).copy(
                shiftType = ShiftType.CUSTOM,
                reason = "커스텀 주간형 근무에 맞춘 취침 가이드입니다.",
            )
        } else {
            planEvening(day.copy(type = ShiftType.EVENING), hours).copy(
                shiftType = ShiftType.CUSTOM,
                reason = "커스텀 저녁형 근무에 맞춘 취침 가이드입니다.",
            )
        }
    }
}
