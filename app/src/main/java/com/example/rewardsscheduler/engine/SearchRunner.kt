package com.example.rewardsscheduler.engine

import android.os.Handler
import android.os.Looper
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.data.AccountRepository
import com.example.rewardsscheduler.data.DailyTracker
import com.example.rewardsscheduler.webview.WebViewManager
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Executes automated Bing searches for a single account profile.
 * - Bing is loaded first so user can view/sign-in.
 * - Auto-fetch is strictly held until user clicks "Fetch" or "START GLOBAL".
 * - Zero simulated scrolling (only searching).
 * - Human-like typing into #sb_form_q and form submission.
 */
class SearchRunner(
    private val profile: AccountProfile,
    private val webViewManager: WebViewManager,
    private val tracker: DailyTracker,
    private val accountRepository: AccountRepository,
    private val backendClient: BackendClient,
    private val onLog: (message: String, type: String) -> Unit,
    private val onStatusChange: (profileId: String, status: String) -> Unit
) {

    companion object {
        fun getSearchScript(query: String): String {
            val sanitized = query.replace("\\", "\\\\").replace("'", "\\'")
            val submitDelay = Random.nextInt(150, 400)
            return """
                (function() {
                    try {
                        const input = document.querySelector('#sb_form_q') || document.querySelector('input[name="q"]');
                        if (!input) {
                            window.location.href = 'https://www.bing.com/search?q=' + encodeURIComponent('$sanitized');
                            return 'navigated';
                        }
                        input.value = '';
                        input.focus();
                        input.value = '$sanitized';
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        const form = document.querySelector('#sb_form');
                        if (form) {
                            setTimeout(() => form.submit(), $submitDelay);
                        } else {
                            window.location.href = 'https://www.bing.com/search?q=' + encodeURIComponent('$sanitized');
                        }
                        return 'submitted';
                    } catch(e) {
                        return 'error: ' + e.message;
                    }
                })();
            """.trimIndent()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile var isRunning: Boolean = false
        private set
    @Volatile var isPaused: Boolean = false
        private set

    private var cachedPrompts: List<String>? = null
    private var cachedPlan: DelayGenerator.Plan? = null
    private var cachedQueue: List<String>? = null
    private var executedCount: Int = 0

    suspend fun preparePrompts(): List<String> {
        val existing = cachedQueue
        if (existing != null && existing.isNotEmpty()) return existing

        onStatusChange(profile.id, "Fetching Prompts")
        val fetchResult = backendClient.fetchPrompts(
            profileId = profile.id,
            topics = profile.selectedCategories,
            isManual = false,
            onLog = onLog
        )

        val fetchedPrompts = when (fetchResult) {
            is BackendClient.FetchResult.Success -> fetchResult.prompts
            is BackendClient.FetchResult.Fallback -> fetchResult.prompts
        }
        cachedPrompts = fetchedPrompts

        val plan = DelayGenerator.createPlan(
            maxPoints = profile.maxPoints,
            timeframeMinutes = profile.timeframeMinutes,
            customMinDelay = profile.customMinDelay,
            customMaxDelay = profile.customMaxDelay,
            onAdjustment = { msg -> onLog("[${profile.label}] $msg", "warn") }
        )
        cachedPlan = plan

        val totalQueriesNeeded = plan.totalSearches
        val queryQueue = PromptQueue.buildSessionQueue(fetchedPrompts, totalQueriesNeeded)
        cachedQueue = queryQueue

        onLog(
            "[${profile.label}] ${profile.maxPoints}pts → ${plan.requiredCount} base + ${plan.extraCount} extra = $totalQueriesNeeded total searches",
            "info"
        )
        onLog(
            "[${profile.label}] Pacing plan: ${plan.requiredCount} delays summing to ${plan.clampedTimeframeSeconds / 60}m: [${plan.requiredDelays.joinToString(", ")}s]",
            "success"
        )

        accountRepository.updateProfileProgress(
            id = profile.id,
            searchCount = executedCount,
            totalTarget = totalQueriesNeeded,
            currentQuery = queryQueue.getOrNull(executedCount) ?: "Ready...",
            pacingDelay = null
        )
        onStatusChange(profile.id, "Ready")
        return queryQueue
    }

    fun fetchPromptsOnly() {
        runnerScope.launch {
            try {
                preparePrompts()
                onLog("[${profile.label}] Prompts fetched and ready", "success")
            } catch (e: Exception) {
                onLog("[${profile.label}] Fetch prompts error: ${e.message}", "error")
            }
        }
    }

    var isFlash: Boolean = false
        private set
    var flashDelaySeconds: Int = 5
        private set

    fun start(scope: CoroutineScope, isFlash: Boolean = false, flashDelaySec: Int = 5) {
        this.isFlash = isFlash
        this.flashDelaySeconds = flashDelaySec

        if (isRunning && !isPaused) return

        if (isPaused) {
            isPaused = false
            isRunning = true
            runnerScope.launch {
                tracker.setInProgress(profile.id, true)
                accountRepository.setProfileInProgress(profile.id, true)
            }
            onLog("[${profile.label}] Resumed searching", "info")
            onStatusChange(profile.id, "Searching")
            return
        }

        isRunning = true
        isPaused = false
        onStatusChange(profile.id, "Starting")

        job = scope.launch(Dispatchers.Default) {
            try {
                runSearchSession(isFlash)
            } catch (ce: CancellationException) {
                onLog("[${profile.label}] Session cancelled", "warn")
                onStatusChange(profile.id, "Stopped")
            } catch (e: Exception) {
                onLog("[${profile.label}] Error: ${e.message}", "error")
                onStatusChange(profile.id, "Error")
            } finally {
                isRunning = false
                withContext(NonCancellable) {
                    tracker.setInProgress(profile.id, false)
                    accountRepository.setProfileInProgress(profile.id, false)
                }
            }
        }
    }

    fun pause() {
        isPaused = true
        runnerScope.launch {
            tracker.setInProgress(profile.id, false)
            accountRepository.setProfileInProgress(profile.id, false)
        }
        onStatusChange(profile.id, "Paused")
        onLog("[${profile.label}] Searching paused", "warn")
    }

    fun stop() {
        isRunning = false
        isPaused = false
        runnerScope.launch {
            tracker.setInProgress(profile.id, false)
            accountRepository.setProfileInProgress(profile.id, false)
        }
        job?.cancel()
        job = null
        onStatusChange(profile.id, "Idle")
        onLog("[${profile.label}] Searching stopped", "info")
    }

    private suspend fun runSearchSession(isFlash: Boolean) {
        tracker.setInProgress(profile.id, true)
        accountRepository.setProfileInProgress(profile.id, true)

        // 1. Ensure Bing is loaded first so user can view/login
        ensureBingLoaded()

        // 2. Prepare prompts if not already cached
        val queryQueue = preparePrompts()
        val plan = cachedPlan ?: DelayGenerator.createPlan(
            maxPoints = profile.maxPoints,
            timeframeMinutes = profile.timeframeMinutes,
            customMinDelay = profile.customMinDelay,
            customMaxDelay = profile.customMaxDelay,
            onAdjustment = { msg -> onLog("[${profile.label}] $msg", "warn") }
        )
        val totalQueriesNeeded = plan.totalSearches

        onStatusChange(profile.id, "Searching")

        var executedCount = 0

        for (i in 0 until totalQueriesNeeded) {
            if (!isRunning) break

            while (isPaused && isRunning) {
                delay(500)
            }
            if (!isRunning) break

            val query = queryQueue.getOrElse(i) { PromptQueue.FALLBACK_QUERIES.random() }
            val delaySeconds = if (isFlash) flashDelaySeconds else plan.allDelays.getOrElse(i) { Random.nextInt(10, 40) }

            // Execute search: Type query and submit (NO scrolling)
            onLog("[${profile.label}] (${i + 1}/$totalQueriesNeeded) \"$query\"", "info")
            val jsScript = getSearchScript(query)
            evalJs(jsScript)

            executedCount++

            // Update status and progress
            tracker.recordSearch(profile.id, plan.requiredCount)
            accountRepository.updateProfileProgress(
                id = profile.id,
                searchCount = executedCount,
                totalTarget = totalQueriesNeeded,
                currentQuery = query,
                pacingDelay = delaySeconds.toFloat()
            )

            // Wait for pacing delay before next query
            if (i < totalQueriesNeeded - 1) {
                val waitMs = (delaySeconds * 1000L).coerceAtLeast(1000L)
                val intervalCheck = 250L
                var elapsed = 0L
                while (elapsed < waitMs && isRunning) {
                    if (isPaused) {
                        while (isPaused && isRunning) {
                            delay(500)
                        }
                    }
                    delay(intervalCheck)
                    elapsed += intervalCheck
                }
            }
        }

        if (executedCount >= plan.requiredCount) {
            onLog("[${profile.label}] Completed all searches! Earned ~${plan.requiredCount * 3} points", "success")
            onStatusChange(profile.id, "Completed")
        } else {
            onLog("[${profile.label}] Session finished with $executedCount searches", "info")
            onStatusChange(profile.id, "Finished")
        }
    }

    private suspend fun ensureBingLoaded() = withContext(Dispatchers.Main) {
        val checkScript = "window.location.hostname"
        val host = evalJsAsync(checkScript)
        if (!host.contains("bing.com")) {
            onLog("[${profile.label}] Navigating to bing.com...", "info")
            webViewManager.loadUrl(profile.id, "https://www.bing.com")
            delay(3000)
        }
    }

    private suspend fun evalJs(script: String): String = withContext(Dispatchers.Main) {
        evalJsAsync(script)
    }

    private suspend fun evalJsAsync(script: String): String = suspendCancellableCoroutine { cont ->
        mainHandler.post {
            try {
                webViewManager.evaluateJavascript(profile.id, script) { result ->
                    if (cont.isActive) cont.resume(result ?: "")
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume("error: ${e.message}")
            }
        }
    }
}
