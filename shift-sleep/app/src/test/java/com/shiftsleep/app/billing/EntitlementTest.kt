package com.shiftsleep.app.billing

import com.shiftsleep.app.data.UserPrefsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class EntitlementTest {
    @Test
    fun unlocked_isPro() {
        val s = Entitlement.evaluate(UserPrefsEntity(proUnlocked = true))
        assertTrue(s.isPro)
        assertTrue(s.canUseFullNotifications)
    }

    @Test
    fun subscription_isPro() {
        val s = Entitlement.evaluate(UserPrefsEntity(subscriptionActive = true))
        assertTrue(s.isPro)
        assertEquals("play_subscription", s.source)
    }

    @Test
    fun trial_active() {
        val now = System.currentTimeMillis()
        val s = Entitlement.evaluate(
            UserPrefsEntity(trialStartedAtMs = now - TimeUnit.DAYS.toMillis(2)),
            nowMs = now,
        )
        assertTrue(s.inTrial)
        assertFalse(s.isPro)
        assertTrue(s.canUsePatternFill)
        assertTrue(s.trialDaysLeft in 4..5)
    }

    @Test
    fun trial_expired_limits() {
        val now = System.currentTimeMillis()
        val s = Entitlement.evaluate(
            UserPrefsEntity(trialStartedAtMs = now - TimeUnit.DAYS.toMillis(10)),
            nowMs = now,
        )
        assertFalse(s.inTrial)
        assertFalse(s.canUseFullNotifications)
        assertEquals(3, s.freeWeekDayLimit)
        assertEquals("expired", s.source)
    }

    @Test
    fun trial_not_started() {
        val s = Entitlement.evaluate(UserPrefsEntity())
        assertFalse(s.inTrial)
        assertEquals("none", s.source)
    }
}
