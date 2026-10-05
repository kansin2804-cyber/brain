package com.shiftsleep.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.shiftsleep.app.BuildConfig

/**
 * Thin analytics + crash facade.
 *
 * - Always logs to Logcat (tag [TAG]).
 * - Forwards to Firebase only when [BuildConfig.FIREBASE_ENABLED] is true
 *   (i.e. `app/google-services.json` was present at build time) and Firebase
 *   initializes successfully.
 */
object AppAnalytics {
    private const val TAG = "ShiftSleepAnalytics"

    @Volatile
    private var firebaseReady = false

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (!BuildConfig.FIREBASE_ENABLED) {
            Log.i(TAG, "Firebase disabled (no google-services.json at build). Logcat-only.")
            return
        }
        try {
            val app = FirebaseApp.initializeApp(context.applicationContext)
                ?: FirebaseApp.getInstance()
            analytics = FirebaseAnalytics.getInstance(app.applicationContext)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            firebaseReady = true
            Log.i(TAG, "Firebase Analytics + Crashlytics ready")
        } catch (t: Throwable) {
            firebaseReady = false
            Log.w(TAG, "Firebase init failed; continuing Logcat-only", t)
        }
    }

    fun event(name: String, params: Map<String, String> = emptyMap()) {
        Log.i(TAG, "$name ${params.ifEmpty { "" }}")
        val fa = analytics
        if (!firebaseReady || fa == null) return
        try {
            val bundle = Bundle()
            params.forEach { (k, v) -> bundle.putString(k, v.take(100)) }
            fa.logEvent(name, bundle)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to send event $name", t)
        }
    }

    fun recordNonFatal(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        if (!firebaseReady) return
        try {
            val crash = FirebaseCrashlytics.getInstance()
            crash.log(message)
            if (throwable != null) crash.recordException(throwable)
        } catch (_: Throwable) {
            // ignore
        }
    }

    val isFirebaseReady: Boolean get() = firebaseReady
}
