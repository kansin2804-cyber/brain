package com.shiftsleep.app.billing

import com.shiftsleep.app.data.UserPrefsEntity
import java.util.concurrent.TimeUnit

data class EntitlementStatus(
    val isPro: Boolean,
    val inTrial: Boolean,
    val trialDaysLeft: Int,
    val source: String,
) {
    val canUseFullNotifications: Boolean get() = isPro || inTrial
    val canUsePatternFill: Boolean get() = isPro || inTrial
    val freeWeekDayLimit: Int get() = if (isPro || inTrial) 7 else 3
}

object Entitlement {
    fun evaluate(prefs: UserPrefsEntity, nowMs: Long = System.currentTimeMillis()): EntitlementStatus {
        if (prefs.proUnlocked || prefs.subscriptionActive) {
            return EntitlementStatus(
                isPro = true,
                inTrial = false,
                trialDaysLeft = 0,
                source = if (prefs.subscriptionActive) "play_subscription" else "unlock",
            )
        }
        val started = prefs.trialStartedAtMs
        if (started <= 0L) {
            return EntitlementStatus(false, false, BillingProducts.TRIAL_DAYS.toInt(), "none")
        }
        val elapsed = nowMs - started
        val limit = TimeUnit.DAYS.toMillis(BillingProducts.TRIAL_DAYS)
        val leftMs = (limit - elapsed).coerceAtLeast(0L)
        val daysLeft = TimeUnit.MILLISECONDS.toDays(leftMs).toInt()
        val inTrial = elapsed < limit
        return EntitlementStatus(
            isPro = false,
            inTrial = inTrial,
            trialDaysLeft = if (inTrial) daysLeft.coerceAtLeast(0) else 0,
            source = if (inTrial) "trial" else "expired",
        )
    }
}
