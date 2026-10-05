package com.shiftsleep.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.data.UserPrefsEntity
import com.shiftsleep.plan.LocalTimeOfDay
import com.shiftsleep.plan.SleepPlan
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationScheduler(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "shiftsleep_plan"
        private const val REQ_BASE = 41000
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "교대 수면 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "취침·기상·카페인 컷오프 가이드 알림"
        }
        mgr.createNotificationChannel(channel)
    }

    suspend fun rescheduleAll(repo: ShiftRepository, prefs: UserPrefsEntity) {
        ensureChannel()
        cancelAll()
        val entitlement = com.shiftsleep.app.billing.Entitlement.evaluate(prefs)
        val plans = repo.planWeek()
        plans.forEachIndexed { index, plan ->
            schedulePlan(plan, prefs, index, entitlement.canUseFullNotifications)
        }
    }

    private fun schedulePlan(
        plan: SleepPlan,
        prefs: UserPrefsEntity,
        weekIndex: Int,
        fullAccess: Boolean,
    ) {
        val base = REQ_BASE + weekIndex * 10
        // Free (expired): sleep notification only. Trial/Pro: respect user toggles.
        val sleepOn = prefs.notifySleep
        val wakeOn = prefs.notifyWake && fullAccess
        val caffeineOn = prefs.notifyCaffeine && fullAccess
        val windDownOn = prefs.notifyWindDown && fullAccess &&
            plan.windDown != null && plan.windDownDate != null

        if (sleepOn) {
            schedule(
                base + 1,
                plan.sleepStartDate,
                plan.sleepStart,
                "취침 시간",
                "권장 취침 ${plan.sleepStart.format()} — ${plan.reason}",
            )
        }
        if (wakeOn) {
            schedule(
                base + 2,
                plan.sleepEndDate,
                plan.sleepEnd,
                "기상 시간",
                "권장 기상 ${plan.sleepEnd.format()}",
            )
        }
        if (caffeineOn) {
            schedule(
                base + 3,
                plan.caffeineCutoffDate,
                plan.caffeineCutoff,
                "카페인 컷오프",
                "이제부터 카페인 줄이기 가이드 ${plan.caffeineCutoff.format()}",
            )
        }
        if (windDownOn) {
            schedule(
                base + 4,
                plan.windDownDate!!,
                plan.windDown!!,
                "취침 준비",
                "취침 ${plan.windDownMinutesLabel()} 전 준비 시간입니다.",
            )
        }
    }

    private fun SleepPlan.windDownMinutesLabel(): String = "약 30분"

    private fun schedule(
        requestCode: Int,
        date: String,
        time: LocalTimeOfDay,
        title: String,
        body: String,
    ) {
        val trigger = LocalDateTime.of(
            LocalDate.parse(date),
            java.time.LocalTime.of(time.hour, time.minute),
        )
        val millis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
            putExtra(AlarmReceiver.EXTRA_ID, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarm = context.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    fun cancelAll() {
        val alarm = context.getSystemService(AlarmManager::class.java)
        for (code in REQ_BASE until REQ_BASE + 80) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            alarm.cancel(pi)
            pi.cancel()
        }
    }
}
