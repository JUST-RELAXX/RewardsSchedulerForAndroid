package com.example.rewardsscheduler.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.rewardsscheduler.MainActivity
import com.example.rewardsscheduler.RewardsApp
import java.util.Calendar

/**
 * Port of scheduler.js: Smart notification reminder engine.
 * - First reminder ~1 min after launch
 * - Recurring every 2 hours while accounts are incomplete
 * - Urgency levels: casual -> medium (8 PM) -> high (9 PM) -> critical (10 PM - 10:30 PM)
 * - No reminders before 6 AM or after 10:30 PM
 * - Automatically stops when all accounts are done today
 * - Actions: "Open App", "Snooze 30m"
 */
class RewardNotificationScheduler(private val context: Context) {

    companion object {
        private const val TAG = "RewardScheduler"
        const val CHANNEL_ID = "rewards_reminders_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_REMINDER_TICK = "com.example.rewardsscheduler.ACTION_REMINDER_TICK"
        const val ACTION_SNOOZE = "com.example.rewardsscheduler.ACTION_SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"

        private var snoozedUntil: Long = 0
        private var firstReminderFired = false
    }

    init {
        createNotificationChannel()
    }

    fun scheduleInitialReminder() {
        if (!firstReminderFired) {
            scheduleAlarm(60 * 1000L) // 1 minute after start
        }
    }

    fun scheduleNextReminder(delayMs: Long = 2 * 60 * 60 * 1000L) {
        scheduleAlarm(delayMs)
    }

    fun snooze(minutes: Int = 30) {
        snoozedUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleAlarm(minutes * 60 * 1000L)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID)
    }

    private fun scheduleAlarm(delayMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_TICK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, 100, intent, flags)

        val triggerAt = System.currentTimeMillis() + delayMs
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun checkAndFireReminder() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)

        // Don't remind before 6 AM or after 10:30 PM
        if (hours < 6) return
        if (hours > 22 || (hours == 22 && minutes > 30)) return

        // Don't remind if snoozed
        if (now < snoozedUntil) return

        // Check if all accounts are done today
        val app = context.applicationContext as? RewardsApp
        if (app != null) {
            val profiles = app.accountRepository.profiles.value
            if (profiles.isNotEmpty() && profiles.all { it.searchesDone }) {
                Log.i(TAG, "All accounts done today — suppressing reminder")
                return
            }
        }

        firstReminderFired = true
        val urgency = getUrgencyLevel(hours, minutes)
        fireNotification(urgency, minutes)

        // Schedule next reminder in 2 hours
        scheduleNextReminder()
    }

    private fun getUrgencyLevel(hours: Int, minutes: Int): String {
        return when {
            hours == 22 && minutes <= 30 -> "critical"
            hours >= 21 -> "high"
            hours >= 20 -> "medium"
            else -> "casual"
        }
    }

    private fun fireNotification(urgency: String, minutes: Int) {
        val (title, body) = when (urgency) {
            "critical" -> "LAST CHANCE!" to "MS Rewards resets in ${30 - minutes} minutes! Do it RIGHT NOW or lose your streak!"
            "high" -> "Getting Late!" to "It's past 9 PM! Your MS Rewards streak is at risk. Do the searches NOW!"
            "medium" -> "Time is ticking!" to "You should probably get those MS Rewards searches done... Evening is flying by!"
            else -> "Daily Reminder" to "Time to complete your Microsoft Rewards searches for today!"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, 201, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_SNOOZE_MINUTES, 30)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context, 202, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (urgency == "critical" || urgency == "high") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_send, "Open App", openPi)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 30m", snoozePi)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Rewards Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Smart reminders to ensure you complete your daily MS Rewards searches"
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}

/**
 * BroadcastReceiver for scheduled reminders and snooze actions.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduler = RewardNotificationScheduler(context)
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                scheduler.scheduleInitialReminder()
            }
            RewardNotificationScheduler.ACTION_SNOOZE -> {
                val mins = intent.getIntExtra(RewardNotificationScheduler.EXTRA_SNOOZE_MINUTES, 30)
                scheduler.snooze(mins)
            }
            RewardNotificationScheduler.ACTION_REMINDER_TICK -> {
                scheduler.checkAndFireReminder()
            }
        }
    }
}
