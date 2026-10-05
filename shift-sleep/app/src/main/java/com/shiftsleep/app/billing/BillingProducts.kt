package com.shiftsleep.app.billing

/** Play Console subscription product IDs — create matching products before release. */
object BillingProducts {
    const val MONTHLY = "shiftsleep_pro_monthly"
    const val YEARLY = "shiftsleep_pro_yearly"

    val all = listOf(MONTHLY, YEARLY)

    const val TRIAL_DAYS = 7L
}
