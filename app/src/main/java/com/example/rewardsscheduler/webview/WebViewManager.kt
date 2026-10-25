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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped WebView manager.
 * One WebView per account slot, each bound to its own Profile for cookie/storage isolation.
 * Configured with Edge Mobile UA or Desktop Windows Edge UA (Desktop Site toggle),
 * third-party cookie persistence, and DOM storage.
 * WebViews must NEVER be created/destroyed by composition recomposition.
 */
class WebViewManager(private val context: Context) {

    companion object {
        private const val TAG = "WebViewManager"
        const val EDGE_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 EdgA/131.0.0.0"
        const val EDGE_DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

        val DESKTOP_INJECTION_SCRIPT = """
            (function() {
                function applyDesktopMode() {
                    try {
                        // 1. Force desktop viewport 1280px wide
                        let meta = document.querySelector('meta[name="viewport"]');
                        if (!meta) {
                            meta = document.createElement('meta');
                            meta.name = 'viewport';
                            (document.head || document.documentElement).appendChild(meta);
                        }
                        const targetContent = 'width=1280, initial-scale=0.32, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes';
                        if (meta.getAttribute('content') !== targetContent) {
                            meta.setAttribute('content', targetContent);
                        }

                        // 2. Set min-width on root elements so layout never squishes into mobile column
                        if (document.documentElement) {
                            document.documentElement.style.minWidth = '1200px';
                            document.documentElement.style.width = '1280px';
                        }
                        if (document.body) {
                            document.body.style.minWidth = '1200px';
                            document.body.style.width = '1280px';
                        }

                        // 3. Ensure desktop header, rewards medallion and points counter are visible
                        const bHeader = document.getElementById('b_header');
                        if (bHeader) {
                            bHeader.style.display = 'flex';
                            bHeader.style.visibility = 'visible';
                        }
                        const idH = document.getElementById('id_h');
                        if (idH) {
                            idH.style.display = 'block';
                            idH.style.visibility = 'visible';
                        }
                        const rhRwm = document.getElementById('rh_rwm');
                        if (rhRwm) {
                            rhRwm.style.display = 'inline-block';
                            rhRwm.style.visibility = 'visible';
                        }
                        const idRhW = document.getElementById('id_rh_w');
                        if (idRhW) {
                            idRhW.style.display = 'inline-block';
                            idRhW.style.visibility = 'visible';
                        }
                        const bHeaderMobile = document.getElementById('b_header_mobile');
                        if (bHeaderMobile) {
                            bHeaderMobile.style.display = 'none';
                        }

                        // 4. Client-side client hints override for navigator.userAgentData
                        try {
                            Object.defineProperty(navigator, 'userAgentData', {
                                get: () => ({
                                    brands: [
                                        { brand: 'Microsoft Edge', version: '131' },
                                        { brand: 'Chromium', version: '131' },
                                        { brand: 'Not_A Brand', version: '24' }
                                    ],
                                    mobile: false,
                                    platform: 'Windows'
                                }),
                                configurable: true
                            });
                        } catch(e) {}

                        // 5. Inject CSS overrides for Bing desktop header and rewards display
                        if (!document.getElementById('agy_pc_mode_styles')) {
                            const style = document.createElement('style');
                            style.id = 'agy_pc_mode_styles';
                            style.textContent = `
                                html, body {
                                    min-width: 1200px !important;
                                    width: 1280px !important;
                                    overflow-x: auto !important;
                                }
                                #b_header {
                                    display: flex !important;
                                    visibility: visible !important;
                                    opacity: 1 !important;
                                }
                                #id_h {
                                    display: block !important;
                                    visibility: visible !important;
                                    opacity: 1 !important;
                                }
                                #id_rh_w, #rh_rwm {
                                    display: inline-block !important;
                                    visibility: visible !important;
                                    opacity: 1 !important;
                                }
                                #b_header_mobile, .m_hdr {
                                    display: none !important;
                                }
                            `;
                            (document.head || document.documentElement).appendChild(style);
                        }
                    } catch(e) {}
                }

                applyDesktopMode();
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', applyDesktopMode);
                }
                window.addEventListener('load', applyDesktopMode);
                setTimeout(applyDesktopMode, 400);
                setTimeout(applyDesktopMode, 1200);
                setTimeout(applyDesktopMode, 2500);
            })();
        """.trimIndent()
    }

    private val prefs by lazy {
        context.getSharedPreferences("app_webview_settings", Context.MODE_PRIVATE)
    }

    private val _desktopModeFlow = MutableStateFlow(false)
    val desktopModeFlow: StateFlow<Boolean> = _desktopModeFlow.asStateFlow()

    var isDesktopSite: Boolean
        get() = _desktopModeFlow.value
        private set(value) {
            _desktopModeFlow.value = value
        }

    init {
        val saved = prefs.getBoolean("desktop_site_enabled", false)
        _desktopModeFlow.value = saved
    }

    private val webViews = mutableMapOf<String, WebView>()

    /** Whether the current WebView implementation supports multi-profile. */
    val isMultiProfileSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    fun getDesktopHeaders(): Map<String, String> {
        return if (isDesktopSite) {
            mapOf(
                "sec-ch-ua-mobile" to "?0",
                "sec-ch-ua-platform" to "\"Windows\"",
                "sec-ch-ua" to "\"Chromium\";v=\"131\", \"Microsoft Edge\";v=\"131\", \"Not_A Brand\";v=\"24\""
            )
        } else {
            emptyMap()
        }
    }

    fun injectDesktopModeIfNeeded(webView: WebView?) {
        if (webView == null || !isDesktopSite) return
        webView.evaluateJavascript(DESKTOP_INJECTION_SCRIPT, null)
    }

    /**
     * Switch all WebViews between Desktop and Mobile sites.
     * When Desktop Site is ON, Bing serves the full PC interface, enabling users to:
     * - View points counter & Rewards flyout
     * - Collect daily bonus tasks (+5/+10 points)
     */
    fun setDesktopMode(enabled: Boolean) {
        isDesktopSite = enabled
        prefs.edit().putBoolean("desktop_site_enabled", enabled).apply()
        val targetUA = if (enabled) EDGE_DESKTOP_UA else EDGE_MOBILE_UA

        webViews.values.forEach { webView ->
            webView.post {
                webView.settings.apply {
                    userAgentString = targetUA
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                val currentUrl = webView.url?.let {
                    if (enabled && it.contains("m.bing.com")) it.replace("m.bing.com", "www.bing.com") else it
                } ?: "https://www.bing.com"

                val headers = getDesktopHeaders()
                if (headers.isNotEmpty()) {
                    webView.loadUrl(currentUrl, headers)
                } else {
                    webView.loadUrl(currentUrl)
                }
            }
        }
    }

    /**
     * Get or create a WebView bound to the given profile name.
     * Must be called on the UI thread.
     */
    fun getOrCreateWebView(profileName: String): WebView {
        webViews[profileName]?.let { return it }

        val targetUA = if (isDesktopSite) EDGE_DESKTOP_UA else EDGE_MOBILE_UA

        val webView = WebView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            isScrollContainer = true
            overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = targetUA
                setSupportMultipleWindows(false)
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowFileAccess = false
                allowContentAccess = false
            }

            CookieManager.getInstance().setAcceptCookie(true)
            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {
                Log.w(TAG, "Third-party cookies not supported: ${e.message}")
            }

            // Register document start script if supported on this WebView version
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(
                        this,
                        DESKTOP_INJECTION_SCRIPT,
                        setOf("*")
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "addDocumentStartJavaScript failed: ${e.message}")
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val targetUrl = if (isDesktopSite && url.contains("m.bing.com")) {
                            url.replace("m.bing.com", "www.bing.com")
                        } else {
                            url
                        }
                        val headers = getDesktopHeaders()
                        if (headers.isNotEmpty()) {
                            view?.loadUrl(targetUrl, headers)
                        } else {
                            view?.loadUrl(targetUrl)
                        }
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    injectDesktopModeIfNeeded(view)
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    injectDesktopModeIfNeeded(view)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectDesktopModeIfNeeded(view)
                    try {
                        CookieManager.getInstance().flush()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to flush cookies: ${e.message}")
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress > 25) {
                        injectDesktopModeIfNeeded(view)
                    }
                }
            }
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

        // Preload Bing if empty
        if (webView.url.isNullOrBlank()) {
            val headers = getDesktopHeaders()
            if (headers.isNotEmpty()) {
                webView.loadUrl("https://www.bing.com", headers)
            } else {
                webView.loadUrl("https://www.bing.com")
            }
        }

        webViews[profileName] = webView
        return webView
    }

    /**
     * Evaluate JavaScript in the WebView for the given profile.
     * Returns the result string via callback.
     */
    fun evaluateJavascript(profileName: String, script: String, callback: ((String?) -> Unit)? = null) {
        val webView = webViews[profileName]
        if (webView == null) {
            Log.w(TAG, "No WebView found for profile '$profileName'")
            callback?.invoke(null)
            return
        }
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }

    /**
     * Load a URL in the WebView for the given profile with desktop headers if enabled.
     */
    fun loadUrl(profileName: String, url: String) {
        val webView = webViews[profileName]
        if (webView == null) {
            Log.w(TAG, "No WebView found for profile '$profileName'")
            return
        }
        val targetUrl = if (isDesktopSite && url.contains("m.bing.com")) {
            url.replace("m.bing.com", "www.bing.com")
        } else {
            url
        }
        val headers = getDesktopHeaders()
        if (headers.isNotEmpty()) {
            webView.loadUrl(targetUrl, headers)
        } else {
            webView.loadUrl(targetUrl)
        }
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
    }
}
