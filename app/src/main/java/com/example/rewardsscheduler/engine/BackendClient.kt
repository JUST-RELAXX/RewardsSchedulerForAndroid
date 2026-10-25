package com.example.rewardsscheduler.engine

import android.util.Log
import com.example.rewardsscheduler.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Backend Client for fetching AI-generated prompts.
 * Endpoints:
 *   POST {backend}/auth with { key } -> { token }
 *   GET  {backend}/getPrompts?topics=... with Bearer token -> { prompts: [...], key_used }
 *
 * Implements:
 * - Gated auto-fetch (blocked until global "Fetch" is pressed; per-profile manual fetch bypasses gate).
 * - Single-flight per profile (concurrent requests for same profile share the same flight).
 * - Single retry on failure with exponential backoff (2-3s delay).
 * - Graceful fallback to offline FALLBACK_QUERIES on persistent failure.
 * - Real error logging.
 */
class BackendClient(
    private val baseUrl: String = BuildConfig.BACKEND_URL.ifEmpty { "https://curiosity-typer.vercel.app" },
    private val defaultKey: String = BuildConfig.BACKEND_KEY.ifEmpty { "@Test01" }
) {

    companion object {
        private const val TAG = "BackendClient"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Gated auto-fetch state: false initially, becomes true when global "Fetch" is clicked
    var isGlobalFetchAllowed: Boolean = false

    // Token caching
    private var cachedToken: String? = null
    private val authMutex = Mutex()

    // Single-flight deduplication per profile
    private val activeFetches = ConcurrentHashMap<String, Deferred<FetchResult>>()

    @Serializable
    private data class AuthReq(val key: String)

    @Serializable
    private data class AuthResp(val token: String)

    @Serializable
    private data class PromptsResp(
        val prompts: List<String> = emptyList(),
        val key_used: String? = null
    )

    sealed class FetchResult {
        data class Success(val prompts: List<String>, val keyUsed: String) : FetchResult()
        data class Fallback(val prompts: List<String>, val reason: String) : FetchResult()
    }

    /**
     * Fetch auth token with caching.
     */
    suspend fun getAuthToken(key: String = defaultKey): String = authMutex.withLock {
        cachedToken?.let { return it }

        withContext(Dispatchers.IO) {
            val reqBody = json.encodeToString(AuthReq.serializer(), AuthReq(key)).toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$baseUrl/auth")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .post(reqBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Auth failed (HTTP ${response.code}): $body")
            }

            val parsed = json.decodeFromString(AuthResp.serializer(), body)
            cachedToken = parsed.token
            parsed.token
        }
    }

    /**
     * Invalidate cached token.
     */
    fun invalidateToken() {
        cachedToken = null
    }

    /**
     * Fetch prompts for a profile with single-flight deduplication and retry.
     *
     * @param profileId Unique profile identifier
     * @param topics Optional list of topic categories
     * @param isManual True if initiated manually from profile config (bypasses global gate)
     * @param onLog Callback for formatted console logs
     */
    suspend fun fetchPrompts(
        profileId: String,
        topics: List<String> = emptyList(),
        isManual: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onLog: (message: String, type: String) -> Unit = { _, _ -> }
    ): FetchResult {
        // Gate check for automated requests
        if (!isManual && !isGlobalFetchAllowed) {
            onLog("[$profileId] Auto-fetch held until global 'Fetch' is tapped", "info")
            val fallbacks = PromptQueue.FALLBACK_QUERIES.shuffled().take(35)
            return FetchResult.Fallback(fallbacks, "Auto-fetch held by gate")
        }

        // Single-flight deduplication: reuse running deferred if one already exists
        val existing = activeFetches[profileId]
        if (existing != null && existing.isActive) {
            onLog("[$profileId] Fetch already in progress, awaiting result...", "info")
            return existing.await()
        }

        val deferred = scope.async {
            try {
                executeFetchWithRetry(profileId, topics, onLog)
            } finally {
                activeFetches.remove(profileId)
            }
        }

        activeFetches[profileId] = deferred
        return deferred.await()
    }

    private suspend fun executeFetchWithRetry(
        profileId: String,
        topics: List<String>,
        onLog: (String, String) -> Unit
    ): FetchResult {
        onLog("[$profileId] Fetching prompts from API...", "info")

        // First attempt
        try {
            val token = getAuthToken()
            val result = queryPromptsApi(token, topics)
            val prompts = result.prompts.take(35)
            val keyUsed = result.key_used ?: "unknown"
            onLog("[$profileId] ${prompts.size} prompts fetched (key: $keyUsed)", "success")
            return FetchResult.Success(prompts, keyUsed)
        } catch (firstErr: Exception) {
            onLog("[$profileId] API error: ${firstErr.message ?: "Unknown error"}. Retrying in 2.5s...", "warn")
            delay(2500)
        }

        // Second attempt (retry once)
        try {
            invalidateToken() // Invalidate token in case of expiration
            val token = getAuthToken()
            val result = queryPromptsApi(token, topics)
            val prompts = result.prompts.take(35)
            val keyUsed = result.key_used ?: "unknown"
            onLog("[$profileId] ${prompts.size} prompts fetched on retry (key: $keyUsed)", "success")
            return FetchResult.Success(prompts, keyUsed)
        } catch (retryErr: Exception) {
            val reason = retryErr.message ?: "Unknown error"
            val fallbackList = PromptQueue.FALLBACK_QUERIES.shuffled().take(35)
            onLog("[$profileId] Retry failed ($reason) — falling back to ${fallbackList.size} offline queries", "fallback")
            return FetchResult.Fallback(fallbackList, reason)
        }
    }

    private suspend fun queryPromptsApi(token: String, topics: List<String>): PromptsResp = withContext(Dispatchers.IO) {
        val selectedTopics = if (topics.isNotEmpty()) {
            topics
        } else {
            PromptQueue.DEFAULT_TOPICS.shuffled().take(3)
        }

        val url = "$baseUrl/getPrompts?topics=${selectedTopics.joinToString(",")}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IOException("GetPrompts HTTP ${response.code}: $body")
        }

        json.decodeFromString(PromptsResp.serializer(), body)
    }
}
