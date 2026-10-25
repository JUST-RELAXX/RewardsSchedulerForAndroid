package com.example.rewardsscheduler.data

import android.content.Context
import com.example.rewardsscheduler.data.DailyProfileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily tracker matching PC app tracker.js.
 * Tracks searchesDone, onlineTimeDone, completedAt, searchCount, onlineMinutes, inProgress
 * per profile, strictly keyed by LOCAL date (YYYY-MM-DD) since Rewards resets at local midnight.
 */
class DailyTracker(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private val file = File(context.filesDir, "daily_tracker.json")

    // Map<LocalDateString, Map<ProfileId, DailyProfileStatus>>
    private var cache: MutableMap<String, MutableMap<String, DailyProfileStatus>> = mutableMapOf()
    private var loaded = false

    fun getTodayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (!loaded) {
            if (file.exists()) {
                try {
                    val content = file.readText()
                    cache = json.decodeFromString(content)
                } catch (e: Exception) {
                    cache = mutableMapOf()
                }
            }
            loaded = true
        }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(cache)
            file.writeText(content)
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun getStatus(profileId: String, date: String = getTodayKey()): DailyProfileStatus = mutex.withLock {
        ensureLoaded()
        cache[date]?.get(profileId) ?: DailyProfileStatus()
    }

    suspend fun getAllStatusForDate(date: String = getTodayKey()): Map<String, DailyProfileStatus> = mutex.withLock {
        ensureLoaded()
        cache[date]?.toMap() ?: emptyMap()
    }

    suspend fun updateStatus(
        profileId: String,
        status: DailyProfileStatus,
        date: String = getTodayKey()
    ) = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        dateMap[profileId] = status
        persist()
    }

    suspend fun recordSearch(
        profileId: String,
        targetSearches: Int,
        date: String = getTodayKey()
    ): DailyProfileStatus = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        val current = dateMap[profileId] ?: DailyProfileStatus()
        val newCount = current.searchCount + 1
        val isDone = newCount >= targetSearches
        val updated = current.copy(
            searchCount = newCount,
            searchesDone = isDone,
            completedAt = if (isDone && current.completedAt == null) System.currentTimeMillis() else current.completedAt,
            inProgress = !isDone
        )
        dateMap[profileId] = updated
        persist()
        updated
    }

    suspend fun setInProgress(
        profileId: String,
        inProgress: Boolean,
        date: String = getTodayKey()
    ) = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        val current = dateMap[profileId] ?: DailyProfileStatus()
        dateMap[profileId] = current.copy(inProgress = inProgress)
        persist()
    }

    suspend fun recordOnlineMinutes(
        profileId: String,
        minutes: Int,
        targetMinutes: Int = 30,
        date: String = getTodayKey()
    ): DailyProfileStatus = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        val current = dateMap[profileId] ?: DailyProfileStatus()
        val updated = current.copy(
            onlineMinutes = minutes,
            onlineTimeDone = minutes >= targetMinutes
        )
        dateMap[profileId] = updated
        persist()
        updated
    }

    suspend fun getTotalPointsEarnedToday(date: String = getTodayKey()): Int = mutex.withLock {
        ensureLoaded()
        val dateMap = cache[date] ?: return 0
        dateMap.values.sumOf { it.searchCount * 3 }
    }

    suspend fun resetAllGlobalPoints(date: String = getTodayKey()) = mutex.withLock {
        ensureLoaded()
        cache.remove(date)
        persist()
    }

    suspend fun markSearchesDone(
        profileId: String,
        searchCount: Int,
        date: String = getTodayKey()
    ): DailyProfileStatus = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        val current = dateMap[profileId] ?: DailyProfileStatus()
        val updated = current.copy(
            searchesDone = true,
            searchCount = searchCount,
            inProgress = false,
            completedAt = System.currentTimeMillis()
        )
        dateMap[profileId] = updated
        persist()
        updated
    }

    suspend fun markSearchesUndone(
        profileId: String,
        date: String = getTodayKey()
    ): DailyProfileStatus = mutex.withLock {
        ensureLoaded()
        val dateMap = cache.getOrPut(date) { mutableMapOf() }
        val current = dateMap[profileId] ?: DailyProfileStatus()
        val updated = current.copy(
            searchesDone = false,
            searchCount = 0,
            inProgress = false,
            completedAt = null
        )
        dateMap[profileId] = updated
        persist()
        updated
    }
}
