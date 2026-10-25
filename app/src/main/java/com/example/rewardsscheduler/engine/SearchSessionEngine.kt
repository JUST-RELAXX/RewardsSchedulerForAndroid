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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Global Engine coordinating multi-profile search sessions.
 * Enforces concurrent profile limit (default 3, user-configurable).
 * Maintains global console log history with color-typed entries.
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

    // Concurrency setting (default 3)
    var maxConcurrentProfiles: Int = 3

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
     * Initializes the session: creates WebViews & runners, but DOES NOT search.
     */
    fun initSession(selectedProfiles: List<AccountProfile>) {
        if (selectedProfiles.isEmpty()) return
        _activeProfileIds.value = selectedProfiles.map { it.id }.toSet()
        selectedProfiles.forEach { profile ->
            if (!runners.containsKey(profile.id)) {
                runners[profile.id] = SearchRunner(
                    profile = profile,
                    webViewManager = webViewManager,
                    tracker = tracker,
                    accountRepository = accountRepository,
                    backendClient = backendClient,
                    onLog = { msg, type -> addLog(msg, type) },
                    onStatusChange = { _, _ -> }
                )
            }
            scope.launch(Dispatchers.Main) {
                webViewManager.getOrCreateWebView(profile.id)
            }
        }
    }

    /**
     * Start global search for selected profiles.
     */
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

        initSession(selectedProfiles)
        backendClient.isGlobalFetchAllowed = true
        _isFetchUnlocked.value = true

        if (_isSessionRunning.value && _isSessionPaused.value) {
            // Resume paused runners
            _isSessionPaused.value = false
            _runningProfileIds.value = selectedProfiles.map { it.id }.toSet()
            if (isFlash) {
                _isGlobalFlashActive.value = true
                _flashProfileIds.value = selectedProfiles.map { it.id }.toSet()
            }
            runners.values.forEach { if (it.isPaused) it.start(scope, isFlash, flashDelaySec) }
            addLog("Global session resumed", "info")
            return
        }

        _isSessionRunning.value = true
        _isSessionPaused.value = false
        _runningProfileIds.value = selectedProfiles.map { it.id }.toSet()
        if (isFlash) {
            _isGlobalFlashActive.value = true
            _flashProfileIds.value = selectedProfiles.map { it.id }.toSet()
        }

        addLog("Starting global session with ${selectedProfiles.size} profiles (Concurrency: $maxConcurrentProfiles)", "info")

        globalSessionJob = scope.launch {
            val semaphore = Semaphore(maxConcurrentProfiles)

            val tasks = selectedProfiles.map { profile ->
                async {
                    semaphore.withPermit {
                        if (!_isSessionRunning.value) return@withPermit

                        val runner = runners[profile.id] ?: SearchRunner(
                            profile = profile,
                            webViewManager = webViewManager,
                            tracker = tracker,
                            accountRepository = accountRepository,
                            backendClient = backendClient,
                            onLog = { msg, type -> addLog(msg, type) },
                            onStatusChange = { _, _ -> }
                        ).also { runners[profile.id] = it }

                        withContext(Dispatchers.Main) {
                            webViewManager.getOrCreateWebView(profile.id)
                        }

                        _runningProfileIds.value = _runningProfileIds.value + profile.id
                        if (isFlash) {
                            _flashProfileIds.value = _flashProfileIds.value + profile.id
                        }
                        runner.start(this, isFlash, flashDelaySec)
                        while (runner.isRunning && _isSessionRunning.value) {
                            delay(500)
                        }
                        _runningProfileIds.value = _runningProfileIds.value - profile.id
                        _flashProfileIds.value = _flashProfileIds.value - profile.id
                    }
                }
            }

            tasks.awaitAll()
            _isSessionRunning.value = false
            _isSessionPaused.value = false
            _isGlobalFlashActive.value = false
            _runningProfileIds.value = emptySet()
            _flashProfileIds.value = emptySet()
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
        pauseGlobal()
        addLog("Global Flash STOPPED manually.", "warn")
    }

    /**
     * Stop searching: halts all searching instantly, keeps sessions/windows open.
     */
    fun pauseGlobal() {
        _isSessionPaused.value = true
        _isGlobalFlashActive.value = false
        _runningProfileIds.value = emptySet()
        _flashProfileIds.value = emptySet()
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
     * Exit session: cancels everything.
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
        scope.launch(Dispatchers.Main) {
            webViewManager.getOrCreateWebView(profileId)
        }
        val runner = runners[profileId] ?: run {
            val newRunner = SearchRunner(
                profile = prof,
                webViewManager = webViewManager,
                tracker = tracker,
                accountRepository = accountRepository,
                backendClient = backendClient,
                onLog = { msg, type -> addLog(msg, type) },
                onStatusChange = { _, _ -> }
            )
            runners[profileId] = newRunner
            newRunner
        }
        _runningProfileIds.value = _runningProfileIds.value + profileId
        if (isFlash) {
            _flashProfileIds.value = _flashProfileIds.value + profileId
        }
        accountRepository.setProfileInProgress(profileId, true)
        runner.start(scope, isFlash, flashDelaySec)
        if (!isFlash) {
            addLog("[$profileId] Search started individually", "info")
        }
    }

    fun startProfileFlash(profileId: String, flashDelaySec: Int = 5) {
        val prof = accountRepository.profiles.value.find { it.id == profileId }
        val label = prof?.label ?: profileId
        addLog("⚡ FLASH MODE TRIGGERED for $label! Bypassing normal constraints (Delay: ${flashDelaySec}s)...", "success")
        startProfile(profileId, isFlash = true, flashDelaySec = flashDelaySec)
    }

    fun stopProfileFlash(profileId: String) {
        val prof = accountRepository.profiles.value.find { it.id == profileId }
        val label = prof?.label ?: profileId
        pauseProfile(profileId)
        _flashProfileIds.value = _flashProfileIds.value - profileId
        addLog("Flash Mode STOPPED manually for $label.", "warn")
    }

    fun pauseProfile(profileId: String) {
        runners[profileId]?.pause()
        _runningProfileIds.value = _runningProfileIds.value - profileId
        _flashProfileIds.value = _flashProfileIds.value - profileId
        accountRepository.setProfileInProgress(profileId, false)
        addLog("[$profileId] Search stopped individually", "warn")
    }

    fun removeProfileFromSession(profileId: String) {
        runners[profileId]?.stop()
        runners.remove(profileId)
        _runningProfileIds.value = _runningProfileIds.value - profileId
        _flashProfileIds.value = _flashProfileIds.value - profileId
        _activeProfileIds.value = _activeProfileIds.value - profileId
        accountRepository.setProfileInProgress(profileId, false)
        addLog("Closed profile: $profileId", "warn")
    }
}
