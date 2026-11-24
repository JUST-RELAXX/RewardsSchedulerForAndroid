package com.example.rewardsscheduler.lifecycle

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Application-wide foreground/background detector.
 *
 * Uses ProcessLifecycleOwner (from androidx.lifecycle) which tracks the
 * entire application process — not individual Activities. This correctly
 * handles multi-Activity apps and configuration changes.
 *
 * ON_START  = at least one Activity is visible (foreground)
 * ON_STOP   = all Activities are stopped (background / home screen)
 *
 * Any component can read [isAppInForeground] to decide whether to use
 * WebView-based search (foreground) or OkHttp-based search (background).
 */
object AppLifecycleObserver : DefaultLifecycleObserver {

    private const val TAG = "AppLifecycle"

    @Volatile
    var isAppInForeground: Boolean = true
        private set

    /**
     * Optional listener called when the app transitions between
     * foreground and background. Set by SearchSessionEngine.
     */
    var onForegroundChanged: ((Boolean) -> Unit)? = null

    /**
     * Call once from Application.onCreate() on the main thread.
     */
    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.i(TAG, "Lifecycle observer registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        isAppInForeground = true
        Log.i(TAG, "App → FOREGROUND")
        onForegroundChanged?.invoke(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background (all Activities stopped)
        isAppInForeground = false
        Log.i(TAG, "App → BACKGROUND")
        onForegroundChanged?.invoke(false)
    }
}
