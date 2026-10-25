package com.example.rewardsscheduler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.rewardsscheduler.RewardsApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground service that runs the search engine.
 * Owns a PARTIAL_WAKE_LOCK to keep the CPU awake while searches run.
 * Uses foregroundServiceType = specialUse (sideload-only app, not Play Store).
 *
 * SPIKE B mode: evaluates JS every 30s on a collapsed WebView and logs timestamps.
 * Full engine integration happens in Step 7.
 */
class SearchForegroundService : Service() {

    companion object {
        private const val TAG = "SearchFGService"
        private const val CHANNEL_ID = "rewards_search_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_GLOBAL = "ACTION_START_GLOBAL"
        const val ACTION_START_PROFILE = "ACTION_START_PROFILE"
        const val ACTION_PAUSE_GLOBAL = "ACTION_PAUSE_GLOBAL"
        const val ACTION_EXIT_GLOBAL = "ACTION_EXIT_GLOBAL"
        const val ACTION_START_SPIKE_B = "ACTION_START_SPIKE_B"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PROFILE_NAME = "EXTRA_PROFILE_NAME"
        const val EXTRA_PROFILE_ID = "EXTRA_PROFILE_ID"
        const val EXTRA_PROFILE_IDS = "EXTRA_PROFILE_IDS"
        const val EXTRA_IS_FLASH = "EXTRA_IS_FLASH"
        const val EXTRA_FLASH_DELAY = "EXTRA_FLASH_DELAY"

        // Broadcast action for spike B log entries
        const val BROADCAST_SPIKE_LOG = "com.example.rewardsscheduler.SPIKE_LOG"
        const val EXTRA_LOG_MESSAGE = "EXTRA_LOG_MESSAGE"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var spikeBRunnable: Runnable? = null
    private var spikeCallCount = 0
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as RewardsApp
        when (intent?.action) {
            ACTION_START_GLOBAL -> {
                val isFlash = intent.getBooleanExtra(EXTRA_IS_FLASH, false)
                val targetIds = intent.getStringArrayListExtra(EXTRA_PROFILE_IDS)?.toSet()
                startForegroundNow("MS Rewards: Automated searches running...")
                acquireWakeLock()
                val allProfiles = app.accountRepository.profiles.value
                val profilesToRun = if (!targetIds.isNullOrEmpty()) {
                    allProfiles.filter { targetIds.contains(it.id) }
                } else {
                    allProfiles
                }
                app.searchEngine.startGlobal(profilesToRun, isFlash)
            }
            ACTION_START_PROFILE -> {
                val profId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profId != null) {
                    val isFlash = intent.getBooleanExtra(EXTRA_IS_FLASH, false)
                    val delaySec = intent.getIntExtra(EXTRA_FLASH_DELAY, 5)
                    startForegroundNow("MS Rewards: Profile search active...")
                    acquireWakeLock()
                    if (isFlash) {
                        app.searchEngine.startProfileFlash(profId, delaySec)
                    } else {
                        app.searchEngine.startProfile(profId, false)
                    }
                }
            }
            ACTION_PAUSE_GLOBAL -> {
                app.searchEngine.pauseGlobal()
                updateNotification("MS Rewards: Searches paused")
            }
            ACTION_EXIT_GLOBAL -> {
                app.searchEngine.exitGlobal()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_SPIKE_B -> {
                val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "spike_bg_test"
                startForegroundNow("SPIKE B — Background test running...")
                acquireWakeLock()
                startSpikeBTest(profileName)
            }
            ACTION_STOP -> {
                stopSpikeBTest()
                app.searchEngine.exitGlobal()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForegroundNow("Rewards search running...")
                acquireWakeLock()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSpikeBTest()
        releaseWakeLock()
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    // ─── SPIKE B: Background JS Execution Test ───

    private fun startSpikeBTest(profileName: String) {
        spikeCallCount = 0
        val app = application as RewardsApp
        val wvm = app.webViewManager

        // Create a WebView for the test if it doesn't exist
        mainHandler.post {
            wvm.getOrCreateWebView(profileName)
            wvm.loadUrl(profileName, "https://www.bing.com")
            broadcastLog("SPIKE B started. Profile: $profileName. Will evaluate JS every 30s.")
            broadcastLog("Background the app and turn off the screen. Check logs after 10+ min.")
        }

        // Schedule JS evaluation every 30 seconds
        spikeBRunnable = object : Runnable {
            override fun run() {
                spikeCallCount++
                val timestamp = dateFormat.format(Date())
                val jsScript = "'SPIKE_B_PING_${spikeCallCount}_at_${timestamp}'"

                mainHandler.post {
                    wvm.evaluateJavascript(profileName, jsScript) { result ->
                        val msg = "#$spikeCallCount [$timestamp] JS result: $result"
                        Log.i(TAG, msg)
                        broadcastLog(msg)
                        updateNotification("SPIKE B: $spikeCallCount calls | Last: $timestamp")
                    }
                }

                // Schedule next call in 30 seconds
                mainHandler.postDelayed(this, 30_000L)
            }
        }

        // First call after 5 seconds (let Bing load)
        mainHandler.postDelayed(spikeBRunnable!!, 5_000L)
    }

    private fun stopSpikeBTest() {
        spikeBRunnable?.let { mainHandler.removeCallbacks(it) }
        spikeBRunnable = null
        broadcastLog("🛑 SPIKE B stopped after $spikeCallCount calls.")
        Log.i(TAG, "SPIKE B stopped after $spikeCallCount calls")
    }

    private fun broadcastLog(message: String) {
        val intent = Intent(BROADCAST_SPIKE_LOG).apply {
            setPackage(packageName)
            putExtra(EXTRA_LOG_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    // ─── Foreground + Notification ───

    private fun startForegroundNow(text: String) {
        val notification = buildNotification(text)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rewards Search",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Running automated Bing searches for Microsoft Rewards"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MS Rewards Scheduler")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // ─── Wake Lock ───

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RewardsScheduler::SearchWakeLock"
            ).apply {
                acquire(2 * 60 * 60 * 1000L) // 2 hours max
            }
            Log.i(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.i(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}
