package com.example.rewardsscheduler.engine

import android.os.Handler
import android.os.Looper
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.data.AccountRepository
import com.example.rewardsscheduler.data.DailyTracker
import com.example.rewardsscheduler.webview.WebViewManager
import kotlinx.coroutines.*
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Executes automated Bing searches for a single account profile.
 * Standard default Android WebView mobile behavior — no scrolling, human-like typing into #sb_form_q.
 * Highly resilient:
 * - Runs ensureSearchPage before each search attempt (up to 3 attempts).
 * - Never stalls or crashes the session on individual failure.
 * - Records session points into session tracker.
 */
class SearchRunner(
    @Volatile var profile: AccountProfile,
    private val webViewManager: WebViewManager,
    private val tracker: DailyTracker,
    private val accountRepository: AccountRepository,
    private val backendClient: BackendClient,
    var globalTimeframeMinutes: Int = 30,
    private val onLog: (message: String, type: String) -> Unit,
    private val onStatusChange: (profileId: String, status: String) -> Unit,
    private val onSearchExecuted: (profileId: String) -> Unit = {},
    private val onWaitingChange: (profileId: String, isWaiting: Boolean) -> Unit = { _, _ -> }
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
                            return 'box_missing';
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
    @Volatile var isWaiting: Boolean = false
        private set

    private var cachedPrompts: List<String>? = null
    private var cachedPlan: DelayGenerator.Plan? = null
    private var cachedQueue: List<String>? = null
    private var executedCount: Int = profile.searchCount

    fun updateMaxPoints(newMaxPoints: Int) {
        profile = profile.copy(maxPoints = newMaxPoints)
    }

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

        val effectiveTimeframe = profile.timeframeMinutes ?: globalTimeframeMinutes
        val plan = DelayGenerator.createPlan(
            maxPoints = profile.maxPoints,
            timeframeMinutes = effectiveTimeframe,
            customMinDelay = profile.customMinDelay,
            customMaxDelay = profile.customMaxDelay,
            onAdjustment = { msg -> onLog("[${profile.label}] $msg", "warn") }
        )
        cachedPlan = plan

        val totalQueriesNeeded = plan.totalSearches
        val queryQueue = PromptQueue.buildSessionQueue(fetchedPrompts, totalQueriesNeeded)
        cachedQueue = queryQueue

        val tfMinutes = plan.clampedTimeframeSeconds / 60
        val tfSeconds = plan.clampedTimeframeSeconds % 60
        val tfFormatted = String.format("%02d:%02d", tfMinutes, tfSeconds)

        // Task 3.9 proof line
        onLog(
            "[${profile.label}] maxPoints=${profile.maxPoints} -> N=${plan.requiredCount} searches | timeframe=$tfFormatted | delays sum=${plan.clampedTimeframeSeconds}s | extras E=${plan.extraCount}",
            "info"
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

    @Volatile var isFlash: Boolean = false
        private set
    @Volatile var flashDelaySeconds: Int = 5
        private set

    fun setFlash(enabled: Boolean, delaySec: Int = 5) {
        this.isFlash = enabled
        this.flashDelaySeconds = delaySec
        if (enabled) {
            onLog("[${profile.label}] Flash mode activated (${delaySec}s delay)", "success")
        } else {
            onLog("[${profile.label}] Flash mode deactivated; returning to normal pacing", "info")
        }
    }

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
                runSearchSession()
            } catch (ce: CancellationException) {
                onLog("[${profile.label}] Session cancelled", "warn")
                onStatusChange(profile.id, "Stopped")
            } catch (e: Exception) {
                onLog("[${profile.label}] Error: ${e.message}", "error")
                onStatusChange(profile.id, "Error")
            } finally {
                isRunning = false
                isWaiting = false
                onWaitingChange(profile.id, false)
                withContext(NonCancellable) {
                    tracker.setInProgress(profile.id, false)
                    accountRepository.setProfileInProgress(profile.id, false)
                }
            }
        }
    }

    fun pause() {
        isPaused = true
        isWaiting = false
        onWaitingChange(profile.id, false)
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
        isWaiting = false
        onWaitingChange(profile.id, false)
        runnerScope.launch {
            tracker.setInProgress(profile.id, false)
            accountRepository.setProfileInProgress(profile.id, false)
        }
        job?.cancel()
        job = null
        onStatusChange(profile.id, "Idle")
        onLog("[${profile.label}] Searching stopped", "info")
    }

    /**
     * Task 4.3: Ensure search page before searching.
     * If URL is not on bing.com or search box #sb_form_q is missing:
     * Loads "https://www.bing.com" and waits for onPageFinished (timeout 15s).
     */
    private suspend fun ensureSearchPage(query: String): Boolean = withContext(Dispatchers.Main) {
        val currentUrl = webViewManager.getUrl(profile.id) ?: ""
        val isBing = currentUrl.contains("bing.com")

        var hasSearchBox = false
        if (isBing) {
            val checkInput = "!!(document.querySelector('#sb_form_q') || document.querySelector('input[name=\"q\"]'))"
            val res = evalJsAsync(checkInput)
            hasSearchBox = res.contains("true", ignoreCase = true)
        }

        if (isBing && hasSearchBox) {
            return@withContext true
        }

        // Need to load bing.com
        onLog("[${profile.label}] Navigating back to search page...", "info")
        webViewManager.loadUrl(profile.id, "https://www.bing.com")

        val finished = webViewManager.awaitPageFinished(profile.id, 15000L)
        if (!finished) {
            onLog("[${profile.label}] Page load timeout waiting for bing.com", "warn")
        }

        // Check search box again
        val checkInputAfter = "!!(document.querySelector('#sb_form_q') || document.querySelector('input[name=\"q\"]'))"
        val resAfter = evalJsAsync(checkInputAfter)
        if (resAfter.contains("true", ignoreCase = true)) {
            return@withContext true
        }

        // If search box still missing, fallback to direct search URL navigation
        val encoded = URLEncoder.encode(query, "UTF-8")
        val directUrl = "https://www.bing.com/search?q=$encoded"
        onLog("[${profile.label}] Search box missing after load; fallback navigating directly to search query URL", "info")
        webViewManager.loadUrl(profile.id, directUrl)
        webViewManager.awaitPageFinished(profile.id, 10000L)
        return@withContext true
    }

    private suspend fun runSearchSession() {
        tracker.setInProgress(profile.id, true)
        accountRepository.setProfileInProgress(profile.id, true)

        val queryQueue = preparePrompts()
        val effectiveTimeframe = profile.timeframeMinutes ?: globalTimeframeMinutes
        val plan = cachedPlan ?: DelayGenerator.createPlan(
            maxPoints = profile.maxPoints,
            timeframeMinutes = effectiveTimeframe,
            customMinDelay = profile.customMinDelay,
            customMaxDelay = profile.customMaxDelay,
            onAdjustment = { msg -> onLog("[${profile.label}] $msg", "warn") }
        )
        val totalQueriesNeeded = plan.totalSearches

        onStatusChange(profile.id, "Searching")

        for (i in executedCount until totalQueriesNeeded) {
            if (!isRunning) break

            while (isPaused && isRunning) {
                delay(500)
            }
            if (!isRunning) break

            val query = queryQueue.getOrElse(i) { PromptQueue.FALLBACK_QUERIES.random() }
            val delaySeconds = if (this.isFlash) this.flashDelaySeconds else plan.allDelays.getOrElse(i) { Random.nextInt(10, 40) }

            // Task 4.3: Up to 5 attempts per search, calling ensureSearchPage() before each.
            // If JS eval fails (WebView not attached), always fall back to direct URL navigation.
            var searchSuccess = false
            var lastError = "unknown"

            for (attempt in 1..5) {
                if (!isRunning) break
                try {
                    ensureSearchPage(query)
                    val jsScript = getSearchScript(query)
                    val result = evalJs(jsScript)

                    if (result.contains("submitted") || result.contains("navigated")) {
                        searchSuccess = true
                        break
                    } else if (result.contains("box_missing") || result.contains("webview_not_attached") || result.contains("null")) {
                        // Direct navigation fallback — always works regardless of WebView attachment
                        val encoded = URLEncoder.encode(query, "UTF-8")
                        withContext(Dispatchers.Main) {
                            webViewManager.loadUrl(profile.id, "https://www.bing.com/search?q=$encoded")
                        }
                        // Wait for page to load
                        webViewManager.awaitPageFinished(profile.id, 10000L)
                        searchSuccess = true
                        onLog("[${profile.label}] Search attempt $attempt used direct URL fallback", "info")
                        break
                    } else {
                        lastError = result
                        onLog("[${profile.label}] Search attempt $attempt/5 failed: $result. Retrying...", "warn")
                        delay(2000)
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "Exception"
                    onLog("[${profile.label}] Search attempt $attempt/5 error: ${e.message}. Retrying...", "warn")
                    delay(2000)
                    // On 3rd+ attempt: try a page reload before retrying
                    if (attempt >= 3) {
                        try {
                            withContext(Dispatchers.Main) {
                                webViewManager.reload(profile.id)
                            }
                            webViewManager.awaitPageFinished(profile.id, 10000L)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Ultimate fallback: if all 5 attempts failed, force a direct URL search
            if (!searchSuccess) {
                onLog("[${profile.label}] All 5 JS attempts failed for \"$query\": $lastError. Using direct URL as final fallback.", "warn")
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    withContext(Dispatchers.Main) {
                        webViewManager.loadUrl(profile.id, "https://www.bing.com/search?q=$encoded")
                    }
                    webViewManager.awaitPageFinished(profile.id, 10000L)
                    searchSuccess = true
                    onLog("[${profile.label}] Direct URL fallback succeeded for \"$query\"", "info")
                } catch (e: Exception) {
                    onLog("[${profile.label}] Even direct URL fallback failed: ${e.message}", "error")
                }
            }

            if (searchSuccess) {
                executedCount++
                onLog("[${profile.label}] (${executedCount}/$totalQueriesNeeded) \"$query\"", "info")

                // Update session counts and persistent tracker
                onSearchExecuted(profile.id)
                tracker.recordSearch(profile.id, plan.requiredCount)
                accountRepository.updateProfileProgress(
                    id = profile.id,
                    searchCount = executedCount,
                    totalTarget = totalQueriesNeeded,
                    currentQuery = query,
                    pacingDelay = delaySeconds.toFloat()
                )
            }

            // Wait for pacing delay before next query (checks volatile isFlash dynamically every 250ms)
            if (i < totalQueriesNeeded - 1 && isRunning) {
                isWaiting = true
                onWaitingChange(profile.id, true)
                val intervalCheck = 250L
                var elapsed = 0L
                try {
                    while (isRunning) {
                        while (isPaused && isRunning) {
                            delay(500)
                        }
                        if (!isRunning) break
                        val targetWaitMs = if (this.isFlash) {
                            (this.flashDelaySeconds * 1000L).coerceAtLeast(1000L)
                        } else {
                            (delaySeconds * 1000L).coerceAtLeast(1000L)
                        }
                        if (elapsed >= targetWaitMs) break
                        delay(intervalCheck)
                        elapsed += intervalCheck
                    }
                } finally {
                    isWaiting = false
                    onWaitingChange(profile.id, false)
                }
            }
        }

        if (executedCount >= plan.requiredCount) {
            onLog("[${profile.label}] Completed required searches! Earned ~${plan.requiredCount * 3} points", "success")
            onStatusChange(profile.id, "Completed")
        } else {
            onLog("[${profile.label}] Session finished with $executedCount searches", "info")
            onStatusChange(profile.id, "Finished")
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
