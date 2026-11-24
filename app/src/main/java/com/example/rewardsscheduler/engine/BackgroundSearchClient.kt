package com.example.rewardsscheduler.engine

import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Performs Bing searches via direct OkHttp HTTP GET requests.
 * Used when the app is in the BACKGROUND where Android WebView suspends
 * its Chromium network stack and rendering pipeline.
 *
 * How it works:
 *   1. Extracts session cookies from CookieManager (the WebView cookie jar)
 *      while the WebView was still in foreground and logged in.
 *   2. Sends GET https://www.bing.com/search?q=<query> with those cookies
 *      + a matching User-Agent string.
 *   3. Bing's server sees the request as an authenticated search and credits
 *      the Rewards points — no rendering or JavaScript needed.
 *
 * This bypasses the WebView entirely, so it works from a foreground Service
 * regardless of Activity lifecycle state, connection type (Wi-Fi, cellular,
 * Ethernet), or screen on/off state.
 */
class BackgroundSearchClient {

    companion object {
        private const val TAG = "BgSearchClient"

        /** Standard mobile Chrome User-Agent matching what Android WebView sends. */
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Cache of cookies per profile, extracted from CookieManager while
     * the app was still in the foreground.
     */
    private val cookieCache = mutableMapOf<String, String>()

    /**
     * Extract and cache cookies for a profile from the WebView CookieManager.
     * MUST be called on the main thread (CookieManager is main-thread only).
     * Should be called while the app is still in the foreground so cookies are fresh.
     */
    fun cacheCookiesForProfile(profileId: String) {
        try {
            val cm = CookieManager.getInstance()
            val bingCookies = cm.getCookie("https://www.bing.com") ?: ""
            if (bingCookies.isNotEmpty()) {
                cookieCache[profileId] = bingCookies
                Log.i(TAG, "Cached ${bingCookies.length} chars of cookies for $profileId")
            } else {
                Log.w(TAG, "No Bing cookies found for $profileId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache cookies for $profileId: ${e.message}")
        }
    }

    /**
     * Cache cookies for all profiles that have WebViews.
     * Call this when the app is about to go to background.
     */
    fun cacheCookiesForProfiles(profileIds: Collection<String>) {
        profileIds.forEach { cacheCookiesForProfile(it) }
    }

    /**
     * Check whether we have cached cookies for a profile.
     */
    fun hasCookiesFor(profileId: String): Boolean {
        return cookieCache[profileId]?.isNotEmpty() == true
    }

    /**
     * Perform a Bing search via direct HTTP GET.
     * Returns true if the server responded with 200 (search credited),
     * false otherwise.
     *
     * This runs entirely on Dispatchers.IO — no main thread, no WebView.
     */
    suspend fun performSearch(profileId: String, query: String): Boolean = withContext(Dispatchers.IO) {
        val cookies = cookieCache[profileId]
        if (cookies.isNullOrEmpty()) {
            Log.w(TAG, "[$profileId] No cached cookies — cannot perform background search")
            return@withContext false
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/search?q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("Cookie", cookies)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Referer", "https://www.bing.com/")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            response.close() // We don't need the body, just the round-trip

            if (code in 200..399) {
                Log.d(TAG, "[$profileId] Background search OK ($code): \"$query\"")
                return@withContext true
            } else {
                Log.w(TAG, "[$profileId] Background search HTTP $code for \"$query\"")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$profileId] Background search error: ${e.message}")
            return@withContext false
        }
    }
}
