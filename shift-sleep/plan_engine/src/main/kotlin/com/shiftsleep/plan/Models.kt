package com.shiftsleep.plan

/**
 * Platform-agnostic domain models. Keep IO/Android out of this module
 * so iOS/KMP can reuse the same rules later.
 */
enum class ShiftType {
    DAY,
    EVENING,
    NIGHT,
    OFF,
    CUSTOM,
}

enum class TemplatePreset {
    HOSPITAL_3SHIFT,
    FACTORY_12H,
    CUSTOM,
}

data class LocalTimeOfDay(
    val hour: Int,
    val minute: Int = 0,
) {
    init {
        require(hour in 0..23) { "hour must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }
    }

    fun toMinutes(): Int = hour * 60 + minute

    fun format(): String = "%02d:%02d".format(hour, minute)

    companion object {
        fun fromMinutes(total: Int): LocalTimeOfDay {
            val normalized = ((total % (24 * 60)) + (24 * 60)) % (24 * 60)
            return LocalTimeOfDay(normalized / 60, normalized % 60)
        }
    }
}

data class ShiftHours(
    val start: LocalTimeOfDay,
    val end: LocalTimeOfDay,
) {
    /** Duration in minutes; handles overnight shifts (e.g. 23:00–07:00). */
    fun durationMinutes(): Int {
        val startM = start.toMinutes()
        val endM = end.toMinutes()
        return if (endM > startM) endM - startM else (24 * 60 - startM) + endM
    }

    fun isOvernight(): Boolean = end.toMinutes() <= start.toMinutes()
}

data class DayShift(
    /** ISO date yyyy-MM-dd */
    val date: String,
    val type: ShiftType,
    val hours: ShiftHours? = null,
)

data class SleepPlan(
    val date: String,
    val shiftType: ShiftType,
    val sleepStart: LocalTimeOfDay,
    /** Calendar date of sleep start when it falls on the previous day. */
    val sleepStartDate: String,
    val sleepEnd: LocalTimeOfDay,
    val sleepEndDate: String,
    val caffeineCutoff: LocalTimeOfDay,
    val caffeineCutoffDate: String,
    val windDown: LocalTimeOfDay? = null,
    val windDownDate: String? = null,
    /** Short Korean reason shown on the home card — not medical advice. */
    val reason: String,
    val allowEveningNap: Boolean = true,
)

data class RuleConstants(
    val targetSleepHours: Double = 7.5,
    val caffeineLeadHours: Double = 8.0,
    val eveningBufferHours: Double = 1.25,
    val nightSleepWithinMinutes: Int = 90,
    val windDownMinutes: Int = 30,
)
