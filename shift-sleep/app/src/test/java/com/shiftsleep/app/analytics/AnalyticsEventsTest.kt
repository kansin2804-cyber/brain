package com.shiftsleep.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventsTest {
    @Test
    fun event_names_are_snake_case() {
        val names = listOf(
            AnalyticsEvents.APP_OPEN,
            AnalyticsEvents.ONBOARDING_COMPLETE,
            AnalyticsEvents.TRIAL_STARTED,
            AnalyticsEvents.SHIFT_UPSERTED,
            AnalyticsEvents.PATTERN_FILL,
            AnalyticsEvents.PAYWALL_OPEN,
            AnalyticsEvents.PURCHASE_CLICK,
            AnalyticsEvents.PRO_DEBUG_UNLOCK,
            AnalyticsEvents.TIP_DISMISSED,
            AnalyticsEvents.TUTORIAL_REPLAY,
        )
        names.forEach { name ->
            assertTrue(name, name.matches(Regex("^[a-z][a-z0-9_]*$")))
            assertTrue(name, name.length <= 40)
        }
        assertEquals(10, names.distinct().size)
    }
}
