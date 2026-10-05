package com.shiftsleep.app

import android.app.Application
import com.shiftsleep.app.data.AppDatabase
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.notify.NotificationScheduler

class ShiftSleepApp : Application() {
    lateinit var repository: ShiftRepository
        private set
    lateinit var notificationScheduler: NotificationScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ShiftRepository(AppDatabase.get(this))
        notificationScheduler = NotificationScheduler(this).also { it.ensureChannel() }
    }
}
