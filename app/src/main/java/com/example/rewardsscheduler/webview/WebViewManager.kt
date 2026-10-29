package com.example.rewardsscheduler.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Application-scoped WebView manager.
 * One WebView per account slot, each bound to its own Profile for cookie/storage isolation.
 * Standard default Android WebView mobile behavior — no desktop overrides, no UA spoofing.
 * WebViews must NEVER be created/destroyed by composition recomposition.
 */
class WebViewManager(private val context: Context) {

    companion object {
        private const val TAG = "WebViewManager"
    }

    private val webViews = ConcurrentHashMap<String, WebView>()
    private val pageFinishedCallbacks = ConcurrentHashMap<String, MutableList<() -> Unit>>()

    init {
        // Task 1: Delete any leftover preference from earlier installs on startup
        try {
            val prefs = context.getSharedPreferences("app_webview_settings", Context.MODE_PRIVATE)
            if (prefs.contains("desktop_site_enabled")) {
                prefs.edit().remove("desktop_site_enabled").apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning legacy preferences: ${e.message}")
        }
    }

    /** Whether the current WebView implementation supports multi-profile. */
    val isMultiProfileSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    /**
     * Get or create a WebView bound to the given profile name.
     * Uses default Android WebView mobile settings.
     * Must be called on the UI thread.
     */
    fun getOrCreateWebView(profileName: String): WebView {
        webViews[profileName]?.let { return it }

        val webView = WebView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            isScrollContainer = true
            overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS

            // Default mobile WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportMultipleWindows(false)
                allowFileAccess = false
                allowContentAccess = false
            }

            CookieManager.getInstance().setAcceptCookie(true)
            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {
                Log.w(TAG, "Third-party cookies not supported: ${e.message}")
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        view?.loadUrl(url)
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        CookieManager.getInstance().flush()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to flush cookies: ${e.message}")
                    }

                    // Notify waiters for pageFinished
                    val callbacks = synchronized(pageFinishedCallbacks) {
                        pageFinishedCallbacks.remove(profileName)?.toList()
                    }
                    callbacks?.forEach { it.invoke() }
                }
            }

            webChromeClient = WebChromeClient()
        }

        if (isMultiProfileSupported) {
            try {
                WebViewCompat.setProfile(webView, profileName)
                Log.i(TAG, "Assigned profile '$profileName' to WebView")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set profile '$profileName': ${e.message}")
            }
        } else {
            Log.w(TAG, "Multi-profile not supported! WebViews will share cookies.")
        }

        // Preload Bing mobile if empty
        if (webView.url.isNullOrBlank()) {
            webView.loadUrl("https://www.bing.com")
        }

        webViews[profileName] = webView
        return webView
    }

    /**
     * Await onPageFinished for a given profile's WebView with a timeout.
     */
    suspend fun awaitPageFinished(profileName: String, timeoutMs: Long = 15000L): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val callback = {
                    if (cont.isActive) {
                        cont.resume(true)
                    }
                }
                synchronized(pageFinishedCallbacks) {
                    val list = pageFinishedCallbacks.getOrPut(profileName) { mutableListOf() }
                    list.add(callback)
                }
                cont.invokeOnCancellation {
                    synchronized(pageFinishedCallbacks) {
                        pageFinishedCallbacks[profileName]?.remove(callback)
                    }
                }
            }
        } ?: false
    }

    fun canGoBack(profileName: String): Boolean {
        return webViews[profileName]?.canGoBack() ?: false
    }

    fun goBack(profileName: String) {
        webViews[profileName]?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
            }
        }
    }

    fun canGoForward(profileName: String): Boolean {
        return webViews[profileName]?.canGoForward() ?: false
    }

    fun goForward(profileName: String) {
        webViews[profileName]?.let { wv ->
            if (wv.canGoForward()) {
                wv.goForward()
            }
        }
    }

    fun reload(profileName: String) {
        webViews[profileName]?.reload()
    }

    fun getUrl(profileName: String): String? {
        return webViews[profileName]?.url
    }

    /**
     * Evaluate JavaScript in the WebView for the given profile.
     * Returns the result string via callback.
     * If the WebView is not attached to a window (windowToken is null),
     * the JS call will silently fail on Android. In that case we return
     * a special error string so the caller can fall back to URL navigation.
     */
    fun evaluateJavascript(profileName: String, script: String, callback: ((String?) -> Unit)? = null) {
        val webView = webViews[profileName]
        if (webView == null) {
            Log.w(TAG, "No WebView found for profile '$profileName'")
            callback?.invoke(null)
            return
        }
        // Guard: WebView must be attached to a window for JS eval to work
        if (webView.windowToken == null) {
            Log.w(TAG, "WebView for '$profileName' is not attached to a window — JS eval will no-op")
            callback?.invoke("error: webview_not_attached")
            return
        }
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }

    /**
     * Load a URL in the WebView for the given profile.
     */
    fun loadUrl(profileName: String, url: String) {
        val webView = webViews[profileName]
        if (webView == null) {
            Log.w(TAG, "No WebView found for profile '$profileName'")
            return
        }
        webView.loadUrl(url)
    }

    /**
     * Delete a profile and its WebView. Clears all data for that profile.
     */
    fun deleteProfile(profileName: String) {
        webViews.remove(profileName)?.let { wv ->
            wv.stopLoading()
            wv.clearHistory()
            wv.clearCache(true)
            wv.destroy()
        }

        if (isMultiProfileSupported) {
            try {
                ProfileStore.getInstance().deleteProfile(profileName)
                Log.i(TAG, "Deleted profile '$profileName' from ProfileStore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete profile '$profileName': ${e.message}")
            }
        }
    }

    /**
     * Clear all WebViews on app exit.
     */
    fun destroyAll() {
        webViews.values.forEach { wv ->
            wv.stopLoading()
            wv.destroy()
        }
        webViews.clear()
        pageFinishedCallbacks.clear()
    }
}
