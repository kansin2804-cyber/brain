package com.shiftsleep.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.LocalTimeOfDay
import com.shiftsleep.plan.ShiftHours
import com.shiftsleep.plan.ShiftType
import com.shiftsleep.plan.TemplatePreset

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val date: String,
    val type: String,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
) {
    fun toDayShift(): DayShift {
        val hours = if (startHour != null && endHour != null) {
            ShiftHours(
                LocalTimeOfDay(startHour, startMinute ?: 0),
                LocalTimeOfDay(endHour, endMinute ?: 0),
            )
        } else {
            null
        }
        return DayShift(date, ShiftType.valueOf(type), hours)
    }

    companion object {
        fun from(day: DayShift): ShiftEntity = ShiftEntity(
            date = day.date,
            type = day.type.name,
            startHour = day.hours?.start?.hour,
            startMinute = day.hours?.start?.minute,
            endHour = day.hours?.end?.hour,
            endMinute = day.hours?.end?.minute,
        )
    }
}

@Entity(tableName = "user_prefs")
data class UserPrefsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingDone: Boolean = false,
    val templatePreset: String = TemplatePreset.HOSPITAL_3SHIFT.name,
    val notifySleep: Boolean = true,
    val notifyWake: Boolean = true,
    val notifyCaffeine: Boolean = false,
    val notifyWindDown: Boolean = false,
    val trialUsed: Boolean = false,
    val proUnlocked: Boolean = false,
    /** First-week tip banner on home. */
    val homeTipDismissed: Boolean = false,
    /** When true, show in-app tutorial overlay again. */
    val replayTutorial: Boolean = false,
)
