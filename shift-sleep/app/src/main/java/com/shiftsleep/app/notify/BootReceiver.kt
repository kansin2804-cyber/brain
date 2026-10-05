package com.shiftsleep.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shiftsleep.app.data.AppDatabase
import com.shiftsleep.app.data.ShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ShiftRepository(AppDatabase.get(context))
                val prefs = repo.ensurePrefs()
                NotificationScheduler(context).rescheduleAll(repo, prefs)
            } finally {
                pending.finish()
            }
        }
    }
}
