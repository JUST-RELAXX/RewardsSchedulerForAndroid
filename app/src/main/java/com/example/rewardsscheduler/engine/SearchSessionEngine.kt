package com.example.rewardsscheduler.engine

import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.data.AccountRepository
import com.example.rewardsscheduler.data.DailyTracker
import com.example.rewardsscheduler.data.LogEntry
import com.example.rewardsscheduler.data.LogLevel
import com.example.rewardsscheduler.webview.WebViewManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Global Engine coordinating multi-profile search sessions.
 * Runs all active session profiles concurrently (no artificial profile limits).
 * Maintains global console log history with color-typed entries.
 * Maintains isolated session search counters for current active session (Task 2).
 */
class SearchSessionEngine(
    private val webViewManager: WebViewManager,
    private val tracker: DailyTracker,
    private val accountRepository: AccountRepository,
    val backendClient: BackendClient = BackendClient()
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // Console logs observable flow
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Concurrency setting (unlimited by default — all profiles run in parallel)
    var maxConcurrentProfiles: Int = Int.MAX_VALUE

    // Global timeframe minutes (default 30)
    var globalTimeframeMinutes: Int = 30

    // Task 2: Session search counts (strictly per-profile searches executed in THIS session)
    // Starts at 0 for every profile when session starts, cleared on session exit.
    private val _sessionSearchCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sessionSearchCounts: StateFlow<Map<String, Int>> = _sessionSearchCounts.asStateFlow()

    // Global session state
    private val _isSessionRunning = MutableStateFlow(false)
    val isSessionRunning: StateFlow<Boolean> = _isSessionRunning.asStateFlow()

    private val _isSessionPaused = MutableStateFlow(false)
    val isSessionPaused: StateFlow<Boolean> = _isSessionPaused.asStateFlow()

    private val _isFetchUnlocked = MutableStateFlow(false)
    val isFetchUnlocked: StateFlow<Boolean> = _isFetchUnlocked.asStateFlow()

    // Active profiles currently in session
    private val _activeProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val activeProfileIds: StateFlow<Set<String>> = _activeProfileIds.asStateFlow()

    // Running profiles actively searching right now
    private val _runningProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val runningProfileIds: StateFlow<Set<String>> = _runningProfileIds.asStateFlow()

    // Flash mode states
    private val _isGlobalFlashActive = MutableStateFlow(false)
    val isGlobalFlashActive: StateFlow<Boolean> = _isGlobalFlashActive.asStateFlow()

    private val _flashProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val flashProfileIds: StateFlow<Set<String>> = _flashProfileIds.asStateFlow()

    // Waiting profiles currently in delay pacing
    private val _waitingProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val waitingProfileIds: StateFlow<Set<String>> = _waitingProfileIds.asStateFlow()

    // Active runner map: profileId -> SearchRunner
    private val runners = ConcurrentHashMap<String, SearchRunner>()
    private var globalSessionJob: Job? = null

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun addLog(message: String, typeStr: String = "info") {
        val level = when (typeStr.lowercase()) {
            "success" -> LogLevel.SUCCESS
            "warn", "warning" -> LogLevel.WARN
            "error" -> LogLevel.ERROR
            "fallback" -> LogLevel.FALLBACK
            else -> LogLevel.INFO
        }
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            message = message,
            level = level
        )
        // Keep last 300 logs
        val updated = (_logs.value + entry).takeLast(300)
        _logs.value = updated
    }

    /**
     * Record a search executed in the current session for a profile.
     */
    fun recordSessionSearch(profileId: String) {
        val current = _sessionSearchCounts.value.toMutableMap()
        current[profileId] = (current[profileId] ?: 0) + 1
        _sessionSearchCounts.value = current
    }

    /**
     * Unlock the global Fetch gate. ONLY fetches prompts — NEVER starts searching!
     */
    fun unlockFetchGate() {
        backendClient.isGlobalFetchAllowed = true
        _isFetchUnlocked.value = true
        addLog("Fetch gate opened — prompts fetched for all profiles.", "info")
        scope.launch {
            _activeProfileIds.value.forEach { id ->
                runners[id]?.fetchPromptsOnly()
            }
        }
    }

    /**
     * Refreshes prompts live from backend for a specific profile with the given topics.
     */
    suspend fun refreshProfilePrompts(profileId: String, topics: List<String>): Int {
        val runner = runners[profileId] ?: run {
            val prof = accountRepository.profiles.value.find { it.id == profileId } ?: return 0
            val newRunner = createRunner(prof)
            runners[profileId] = newRunner
            newRunner
        }
        return runner.refreshPrompts(topics)
    }

    /**
     * Injects a user custom query directly into a profile's queue to be searched next.
     */
    fun addCustomQueryToProfile(profileId: String, query: String) {
        val runner = runners[profileId] ?: run {
            val prof = accountRepository.profiles.value.find { it.id == profileId } ?: return
            val newRunner = createRunner(prof)
            runners[profileId] = newRunner
            newRunner
        }
        runner.addCustomQuery(query)
    }

    /**
     * Update max points for a profile mid-run or prior to run.
     * Persists to repository, updates active runner, and logs change to Console.
     */
    fun updateProfileMaxPoints(profileId: String, newMaxPoints: Int) {
        scope.launch {
            accountRepository.updateProfileMaxPoints(profileId, newMaxPoints)
            runners[profileId]?.updateMaxPoints(newMaxPoints)
            val prof = accountRepository.profiles.value.find { it.id == profileId }
            val label = prof?.label ?: profileId
            addLog("[$label] Max points updated to $newMaxPoints", "info")
        }
    }

    /**
     * Initializes the session: creates WebViews & runners, and resets session counters.
     * DOES NOT start searching.
     */
    fun initSession(selectedProfiles: List<AccountProfile>) {
        if (selectedProfiles.isEmpty()) return
        _activeProfileIds.value = selectedProfiles.map { it.id }.toSet()

        // Look up progress: initialize session search counters with today's completed searches
        val initialCounts = selectedProfiles.associate { it.id to it.searchCount }
        _sessionSearchCounts.value = initialCounts

        selectedProfiles.forEach { profile ->
            if (!runners.containsKey(profile.id)) {
                runners[profile.id] = createRunner(profile)
            }
            scope.launch(Dispatchers.Main) {
                webViewManager.getOrCreateWebView(profile.id)
            }
        }
    }

    private fun createRunner(profile: AccountProfile): SearchRunner {
        return SearchRunner(
            profile = profile,
            webViewManager = webViewManager,
            tracker = tracker,
            accountRepository = accountRepository,
            backendClient = backendClient,
            globalTimeframeMinutes = globalTimeframeMinutes,
            onLog = { msg, type -> addLog(msg, type) },
            onStatusChange = { profileId, status ->
                when (status) {
                    "Completed", "Finished", "Stopped", "Idle", "Error" -> {
                        _runningProfileIds.update { it - profileId }
                        _flashProfileIds.update { it - profileId }
                        _waitingProfileIds.update { it - profileId }
                        if (_flashProfileIds.value.isEmpty()) {
                            _isGlobalFlashActive.value = false
                        }
                    }
                    "Searching" -> {
                        _runningProfileIds.update { it + profileId }
                    }
                }
            },
            onSearchExecuted = { id -> recordSessionSearch(id) },
            onWaitingChange = { profileId, isWaiting ->
                if (isWaiting) {
                    _waitingProfileIds.update { it + profileId }
                } else {
                    _waitingProfileIds.update { it - profileId }
                }
            }
        )
    }

    /**
     * Start global search for selected profiles.
     */
    fun startGlobal(
        selectedProfiles: List<AccountProfile>,
        isFlash: Boolean = false,
        flashDelaySec: Int = 5
    ) {
        if (selectedProfiles.isEmpty()) {
            addLog("No profiles selected to start", "warn")
            return
        }

        if (!_isSessionRunning.value) {
            initSession(selectedProfiles)
        }

        backendClient.isGlobalFetchAllowed = true
        _isFetchUnlocked.value = true

        // If the global session coroutine is already actively running:
        if (globalSessionJob?.isActive == true && _isSessionRunning.value) {
            if (isFlash) {
                _isGlobalFlashActive.value = true
                _flashProfileIds.value = selectedProfiles.map { it.id }.toSet()
                selectedProfiles.forEach { p -> runners[p.id]?.setFlash(true, flashDelaySec) }
                if (_isSessionPaused.value) {
                    _isSessionPaused.value = false
                    runners.values.forEach { if (it.isPaused) it.start(scope, true, flashDelaySec) }
                }
                addLog("⚡ Global Flash activated for all running profiles (Delay: ${flashDelaySec}s)...", "success")
                return
            } else {
                if (_isGlobalFlashActive.value) {
                    _isGlobalFlashActive.value = false
                    _flashProfileIds.value = emptySet()
                    selectedProfiles.forEach { p -> runners[p.id]?.setFlash(false) }
                }
                if (_isSessionPaused.value) {
                    _isSessionPaused.value = false
                    runners.values.forEach { if (it.isPaused) it.start(scope, false) }
                }
                addLog("Global session running with ${selectedProfiles.size} profiles", "info")
                return
            }
        }

        // Fresh launch or restart
        globalSessionJob?.cancel()
        _isSessionRunning.value = true
        _isSessionPaused.value = false
        _runningProfileIds.value = selectedProfiles.map { it.id }.toSet()
        if (isFlash) {
            _isGlobalFlashActive.value = true
            _flashProfileIds.value = selectedProfiles.map { it.id }.toSet()
        } else {
            _isGlobalFlashActive.value = false
            _flashProfileIds.value = emptySet()
        }

        addLog("Starting global session with ${selectedProfiles.size} profiles", "info")

        globalSessionJob = scope.launch {
            val tasks = selectedProfiles.map { profile ->
                async {
                    if (!_isSessionRunning.value) return@async

                    val runner = runners[profile.id] ?: createRunner(profile).also { runners[profile.id] = it }

                    withContext(Dispatchers.Main) {
                        webViewManager.getOrCreateWebView(profile.id)
                    }

                    _runningProfileIds.update { it + profile.id }
                    if (isFlash) {
                        _flashProfileIds.update { it + profile.id }
                    }
                    runner.start(this, isFlash, flashDelaySec)
                    while (runner.isRunning && _isSessionRunning.value) {
                        delay(500)
                    }
                    _runningProfileIds.update { it - profile.id }
                    _flashProfileIds.update { it - profile.id }
                }
            }

            tasks.awaitAll()
            _isSessionRunning.value = false
            _isSessionPaused.value = false
            _isGlobalFlashActive.value = false
            _runningProfileIds.value = emptySet()
            _flashProfileIds.value = emptySet()
            _waitingProfileIds.value = emptySet()
            addLog("Global search session complete", "success")
        }
    }

    fun startGlobalFlash(selectedProfiles: List<AccountProfile>, flashDelaySec: Int = 5) {
        _isGlobalFlashActive.value = true
        addLog("⚡ GLOBAL FLASH TRIGGERED! Bypassing constraints for all profiles (Delay: ${flashDelaySec}s)...", "success")
        startGlobal(selectedProfiles, isFlash = true, flashDelaySec = flashDelaySec)
    }

    fun stopGlobalFlash() {
        _isGlobalFlashActive.value = false
        _flashProfileIds.value = emptySet()
        _waitingProfileIds.value = emptySet()
        runners.values.forEach { it.setFlash(false) }
        addLog("Global Flash STOPPED manually; returning to normal pacing.", "warn")
    }

    /**
     * Stop searching: halts all searching instantly, keeps sessions/windows open.
     */
    fun pauseGlobal() {
        _isSessionPaused.value = true
        _isGlobalFlashActive.value = false
        _runningProfileIds.value = emptySet()
        _flashProfileIds.value = emptySet()
        _waitingProfileIds.value = emptySet()
        runners.values.forEach { it.pause() }
        addLog("Searching paused for all accounts (windows remain open)", "warn")
    }

    fun triggerSearch() {
        if (_isSessionPaused.value) {
            _isSessionPaused.value = false
            runners.values.forEach { if (it.isPaused) it.start(scope) }
        }
    }

    /**
     * Exit session: cancels everything and clears session counters (Task 2).
     */
    fun exitGlobal() {
        _isSessionRunning.value = false
        _isSessionPaused.value = false
        _isGlobalFlashActive.value = false
        _isFetchUnlocked.value = false
        backendClient.isGlobalFetchAllowed = false
        _activeProfileIds.value = emptySet()
        _runningProfileIds.value = emptySet()
        _flashProfileIds.value = emptySet()
        _waitingProfileIds.value = emptySet()
        _sessionSearchCounts.value = emptyMap()
        globalSessionJob?.cancel()
        globalSessionJob = null
        runners.values.forEach { it.stop() }
        runners.clear()
        addLog("Global session exited", "error")
    }

    fun isProfileRunning(profileId: String): Boolean {
        return runners[profileId]?.isRunning == true
    }

    fun isProfileActive(profileId: String): Boolean {
        return _runningProfileIds.value.contains(profileId)
    }

    fun startProfile(profileId: String, isFlash: Boolean = false, flashDelaySec: Int = 5) {
        val prof = accountRepository.profiles.value.find { it.id == profileId } ?: return
        _activeProfileIds.value = _activeProfileIds.value + profileId
        _isSessionRunning.value = true
        _isSessionPaused.value = false
        backendClient.isGlobalFetchAllowed = true
        _isFetchUnlocked.value = true

        if (!_sessionSearchCounts.value.containsKey(profileId)) {
            val current = _sessionSearchCounts.value.toMutableMap()
            current[profileId] = prof.searchCount
            _sessionSearchCounts.value = current
        }

        scope.launch(Dispatchers.Main) {
            webViewManager.getOrCreateWebView(profileId)
        }
        val runner = runners[profileId] ?: run {
            val newRunner = createRunner(prof)
            runners[profileId] = newRunner
            newRunner
        }
        _runningProfileIds.update { it + profileId }
        if (isFlash) {
            _flashProfileIds.update { it + profileId }
        }
        accountRepository.setProfileInProgress(profileId, true)
        runner.start(scope, isFlash, flashDelaySec)
        if (!isFlash) {
            addLog("[${prof.label}] Search started individually", "info")
        }
    }

    fun startProfileFlash(profileId: String, flashDelaySec: Int = 5) {
        val prof = accountRepository.profiles.value.find { it.id == profileId }
        val label = prof?.label ?: profileId
        val runner = runners[profileId]
        if (runner != null && runner.isRunning) {
            _flashProfileIds.update { it + profileId }
            runner.setFlash(true, flashDelaySec)
            addLog("⚡ FLASH MODE TRIGGERED for $label! (Delay: ${flashDelaySec}s)...", "success")
        } else {
            addLog("⚡ FLASH MODE TRIGGERED for $label! Bypassing normal constraints (Delay: ${flashDelaySec}s)...", "success")
            startProfile(profileId, isFlash = true, flashDelaySec = flashDelaySec)
        }
    }

    fun stopProfileFlash(profileId: String) {
        val prof = accountRepository.profiles.value.find { it.id == profileId }
        val label = prof?.label ?: profileId
        _flashProfileIds.value = _flashProfileIds.value - profileId
        if (_flashProfileIds.value.isEmpty()) {
            _isGlobalFlashActive.value = false
        }
        val runner = runners[profileId]
        if (runner != null && runner.isRunning) {
            runner.setFlash(false)
            addLog("Flash Mode STOPPED for $label; returned to normal pacing.", "warn")
        } else {
            pauseProfile(profileId)
        }
    }

    fun pauseProfile(profileId: String) {
        runners[profileId]?.pause()
        _runningProfileIds.value = _runningProfileIds.value - profileId
        _flashProfileIds.value = _flashProfileIds.value - profileId
        _waitingProfileIds.value = _waitingProfileIds.value - profileId
        if (_flashProfileIds.value.isEmpty()) {
            _isGlobalFlashActive.value = false
        }
        accountRepository.setProfileInProgress(profileId, false)
        addLog("[$profileId] Search stopped individually", "warn")
    }

    fun removeProfileFromSession(profileId: String) {
        runners[profileId]?.stop()
        runners.remove(profileId)
        _runningProfileIds.value = _runningProfileIds.value - profileId
        _flashProfileIds.value = _flashProfileIds.value - profileId
        _waitingProfileIds.value = _waitingProfileIds.value - profileId
        _activeProfileIds.value = _activeProfileIds.value - profileId
        _sessionSearchCounts.value = _sessionSearchCounts.value - profileId
        if (_flashProfileIds.value.isEmpty()) {
            _isGlobalFlashActive.value = false
        }
        accountRepository.setProfileInProgress(profileId, false)
        addLog("Closed profile: $profileId", "warn")
    }
}
