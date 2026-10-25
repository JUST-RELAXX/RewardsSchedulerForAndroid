package com.example.rewardsscheduler.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Repository for managing Account Slots.
 * Persists accounts and syncs with DailyTracker.
 */
class AccountRepository(
    private val context: Context,
    val tracker: DailyTracker
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private val file = File(context.filesDir, "accounts_v1.json")

    private val _profiles = MutableStateFlow<List<AccountProfile>>(emptyList())
    val profiles: StateFlow<List<AccountProfile>> = _profiles.asStateFlow()

    init {
        // Load initial state
        loadProfilesSync()
    }

    private fun loadProfilesSync() {
        if (file.exists()) {
            try {
                val list = json.decodeFromString<List<AccountProfile>>(file.readText())
                if (list.isNotEmpty()) {
                    _profiles.value = list
                    return
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        // Default 4 initial slots for showcase as requested
        val defaults = listOf(
            AccountProfile(id = "profile_1", label = "Account 1", maxPoints = 60),
            AccountProfile(id = "profile_2", label = "Account 2", maxPoints = 60),
            AccountProfile(id = "profile_3", label = "Account 3", maxPoints = 60),
            AccountProfile(id = "profile_4", label = "Account 4", maxPoints = 60)
        )
        _profiles.value = defaults
        saveSync(defaults)
    }

    private fun saveSync(list: List<AccountProfile>) {
        try {
            file.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun refreshWithTracker() = mutex.withLock {
        val todayStatuses = tracker.getAllStatusForDate()
        val current = _profiles.value
        val updated = current.map { p ->
            val s = todayStatuses[p.id] ?: DailyProfileStatus()
            p.copy(
                searchesDone = s.searchesDone,
                onlineTimeDone = s.onlineTimeDone,
                completedAt = s.completedAt,
                searchCount = s.searchCount,
                onlineMinutes = s.onlineMinutes,
                inProgress = s.inProgress
            )
        }
        _profiles.value = updated
    }

    suspend fun addProfile(label: String, maxPoints: Int = 60): AccountProfile = mutex.withLock {
        val newId = "profile_${System.currentTimeMillis()}"
        val newProfile = AccountProfile(
            id = newId,
            label = label.ifBlank { "Account ${_profiles.value.size + 1}" },
            maxPoints = maxPoints
        )
        val updated = _profiles.value + newProfile
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
        newProfile
    }

    suspend fun renameProfile(id: String, newLabel: String) = mutex.withLock {
        val updated = _profiles.value.map {
            if (it.id == id) it.copy(label = newLabel) else it
        }
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
    }

    suspend fun deleteProfile(id: String) = mutex.withLock {
        val updated = _profiles.value.filter { it.id != id }
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
    }

    suspend fun updateProfile(profile: AccountProfile) = mutex.withLock {
        val updated = _profiles.value.map {
            if (it.id == profile.id) profile else it
        }
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
    }

    suspend fun updateProfileProgress(
        id: String,
        searchCount: Int,
        totalTarget: Int,
        currentQuery: String,
        pacingDelay: Float?
    ) = mutex.withLock {
        val updated = _profiles.value.map { p ->
            if (p.id == id) {
                val newDelays = if (pacingDelay != null) {
                    (p.recentPacingDelays + pacingDelay).takeLast(60)
                } else p.recentPacingDelays

                p.copy(
                    searchCount = searchCount,
                    totalTargetSearches = totalTarget,
                    currentQuery = currentQuery,
                    recentPacingDelays = newDelays,
                    searchesDone = searchCount >= totalTarget && totalTarget > 0
                )
            } else p
        }
        _profiles.value = updated
    }

    fun setProfileInProgress(id: String, inProgress: Boolean) {
        val updated = _profiles.value.map {
            if (it.id == id) it.copy(inProgress = inProgress) else it
        }
        _profiles.value = updated
    }

    suspend fun resetAllGlobalPoints() = mutex.withLock {
        tracker.resetAllGlobalPoints()
        val current = _profiles.value
        val updated = current.map { p ->
            p.copy(
                searchesDone = false,
                onlineTimeDone = false,
                completedAt = null,
                searchCount = 0,
                onlineMinutes = 0,
                inProgress = false,
                currentQuery = "",
                recentPacingDelays = emptyList()
            )
        }
        _profiles.value = updated
    }

    suspend fun markProfileDoneManual(id: String) = mutex.withLock {
        val p = _profiles.value.find { it.id == id } ?: return@withLock
        val maxSearches = if (p.maxPoints > 0) p.maxPoints / 3 else 20
        tracker.markSearchesDone(id, maxSearches)
        val updated = _profiles.value.map {
            if (it.id == id) {
                it.copy(
                    searchesDone = true,
                    searchCount = maxSearches,
                    totalTargetSearches = maxSearches,
                    inProgress = false,
                    completedAt = System.currentTimeMillis()
                )
            } else it
        }
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
    }

    suspend fun markProfileUndoneManual(id: String) = mutex.withLock {
        tracker.markSearchesUndone(id)
        val updated = _profiles.value.map {
            if (it.id == id) {
                it.copy(
                    searchesDone = false,
                    searchCount = 0,
                    inProgress = false,
                    completedAt = null
                )
            } else it
        }
        _profiles.value = updated
        withContext(Dispatchers.IO) { saveSync(updated) }
    }
}
