package com.example.rewardsscheduler

import android.app.Application
import com.example.rewardsscheduler.data.AccountRepository
import com.example.rewardsscheduler.data.DailyTracker
import com.example.rewardsscheduler.engine.BackgroundSearchClient
import com.example.rewardsscheduler.engine.SearchSessionEngine
import com.example.rewardsscheduler.lifecycle.AppLifecycleObserver
import com.example.rewardsscheduler.webview.WebViewManager

/**
 * Application-scoped singleton container.
 * WebViewManager and SearchSessionEngine MUST be application-scoped — not Activity-scoped —
 * so the foreground service can access WebViews even when the Activity is backgrounded.
 */
class RewardsApp : Application() {

    lateinit var webViewManager: WebViewManager
        private set

    lateinit var tracker: DailyTracker
        private set

    lateinit var accountRepository: AccountRepository
        private set

    lateinit var searchEngine: SearchSessionEngine
        private set

    lateinit var backgroundSearchClient: BackgroundSearchClient
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize lifecycle observer FIRST so components can read isAppInForeground
        AppLifecycleObserver.init()

        webViewManager = WebViewManager(this)
        tracker = DailyTracker(this)
        accountRepository = AccountRepository(this, tracker)
        backgroundSearchClient = BackgroundSearchClient()
        searchEngine = SearchSessionEngine(
            webViewManager, tracker, accountRepository,
            backgroundSearchClient = backgroundSearchClient
        )

        // When app goes to background, cache cookies for all active profiles
        // so BackgroundSearchClient can use them for direct HTTP searches.
        AppLifecycleObserver.onForegroundChanged = { isForeground ->
            if (!isForeground) {
                // Cache cookies on main thread before we lose WebView access
                val activeIds = searchEngine.activeProfileIds.value
                if (activeIds.isNotEmpty()) {
                    backgroundSearchClient.cacheCookiesForProfiles(activeIds)
                    searchEngine.addLog("Cached cookies for ${activeIds.size} profiles (background mode)", "info")
                }
            }
        }
    }

    override fun onTerminate() {
        webViewManager.destroyAll()
        super.onTerminate()
    }
}
