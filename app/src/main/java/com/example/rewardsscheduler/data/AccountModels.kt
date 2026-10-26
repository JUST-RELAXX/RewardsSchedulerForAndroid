package com.example.rewardsscheduler.data

import kotlinx.serialization.Serializable

/**
 * Representation of an Account Slot.
 * Each account slot corresponds to an isolated WebView profile (e.g., "prof_1").
 */
@Serializable
data class AccountProfile(
    val id: String,                     // e.g., "profile_1"
    val label: String,                  // Display name e.g., "Account 1"
    val email: String = "",             // Microsoft email if detected/entered
    val isSignedIn: Boolean = false,
    val maxPoints: Int = 60,            // Per-profile daily point cap
    val timeframeMinutes: Int? = null,    // Per-profile timeframe override (null = use global)
    val customMinDelay: Int? = null,
    val customMaxDelay: Int? = null,
    val customCategories: List<String> = emptyList(),
    val selectedCategories: List<String> = emptyList(),
    val searchesDone: Boolean = false,
    val onlineTimeDone: Boolean = false,
    val completedAt: Long? = null,
    val searchCount: Int = 0,
    val onlineMinutes: Int = 0,
    val inProgress: Boolean = false,
    val currentQuery: String = "",
    val totalTargetSearches: Int = 0,
    val recentPacingDelays: List<Float> = emptyList()
)

/**
 * Tracker data schema matching PC tracker.js.
 * Keyed by profile ID and local date (YYYY-MM-DD).
 */
@Serializable
data class DailyProfileStatus(
    val searchesDone: Boolean = false,
    val onlineTimeDone: Boolean = false,
    val completedAt: Long? = null,
    val searchCount: Int = 0,
    val onlineMinutes: Int = 0,
    val inProgress: Boolean = false
)

/**
 * Console log message.
 */
data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR, FALLBACK
}
