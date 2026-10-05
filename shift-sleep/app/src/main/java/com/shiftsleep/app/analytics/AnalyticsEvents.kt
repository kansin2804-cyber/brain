package com.shiftsleep.app.analytics

/**
 * Product analytics event names. Keep snake_case for Firebase parity.
 * No PII — shift times / personal schedule details must not be logged.
 */
object AnalyticsEvents {
    const val APP_OPEN = "app_open"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val TRIAL_STARTED = "trial_started"
    const val SHIFT_UPSERTED = "shift_upserted"
    const val PATTERN_FILL = "pattern_fill"
    const val PAYWALL_OPEN = "paywall_open"
    const val PURCHASE_CLICK = "purchase_click"
    const val PRO_DEBUG_UNLOCK = "pro_debug_unlock"
    const val TIP_DISMISSED = "home_tip_dismissed"
    const val TUTORIAL_REPLAY = "tutorial_replay"
}

object AnalyticsParams {
    const val PRESET = "preset"
    const val PRODUCT_ID = "product_id"
    const val PATTERN = "pattern"
    const val SOURCE = "source"
    const val ENTITLEMENT = "entitlement"
}
