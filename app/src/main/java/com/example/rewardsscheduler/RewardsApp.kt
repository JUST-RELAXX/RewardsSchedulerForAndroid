package com.example.rewardsscheduler

import android.app.Application
import com.example.rewardsscheduler.data.AccountRepository
import com.example.rewardsscheduler.data.DailyTracker
import com.example.rewardsscheduler.engine.SearchSessionEngine
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

    override fun onCreate() {
        super.onCreate()
        webViewManager = WebViewManager(this)
        tracker = DailyTracker(this)
        accountRepository = AccountRepository(this, tracker)
        searchEngine = SearchSessionEngine(webViewManager, tracker, accountRepository)
    }

    override fun onTerminate() {
        webViewManager.destroyAll()
        super.onTerminate()
    }
}
