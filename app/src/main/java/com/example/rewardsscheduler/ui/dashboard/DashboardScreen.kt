package com.example.rewardsscheduler.ui.dashboard

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.RewardsApp
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.service.SearchForegroundService
import com.example.rewardsscheduler.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DashboardActiveView {
    PROFILES,
    SESSION
}

/**
 * Main dashboard screen matching the PC Electron app:
 * - PROFILES view: AppTitleBar, StatusBanner, GlobalPointsCanvas, QuickActionBar, AccountCardsGrid
 * - SESSION view: Dashboard/NutshellCard, ControlsRow, HolisticSettingsCard, ConsoleCard, SessionTiles
 * - Zero emojis! Uses clean vector icons.
 */
@Composable
fun DashboardScreen(
    app: RewardsApp = LocalContext.current.applicationContext as RewardsApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Observable states from repositories / services
    val profiles by app.accountRepository.profiles.collectAsState()
    val consoleLogs by app.searchEngine.logs.collectAsState()
    val isSessionRunning by app.searchEngine.isSessionRunning.collectAsState()
    val isSessionPaused by app.searchEngine.isSessionPaused.collectAsState()
    val isFetchUnlocked by app.searchEngine.isFetchUnlocked.collectAsState()
    val engineActiveProfileIds by app.searchEngine.activeProfileIds.collectAsState()
    val isGlobalFlashActive by app.searchEngine.isGlobalFlashActive.collectAsState()
    val flashProfileIds by app.searchEngine.flashProfileIds.collectAsState()

    // View mode: PROFILES (Dashboard) or SESSION
    var activeView by remember { mutableStateOf(DashboardActiveView.PROFILES) }
    var isSessionActive by rememberSaveable { mutableStateOf(false) }

    // Local UI states
    var selectedProfileIds by remember { mutableStateOf(setOf<String>()) }
    var hasInitializedSelection by rememberSaveable { mutableStateOf(false) }
    var globalTimeframeMinutes by remember { mutableIntStateOf(30) }
    var maxConcurrentProfiles by remember { mutableIntStateOf(app.searchEngine.maxConcurrentProfiles) }
    var isPreviewsCollapsed by remember { mutableStateOf(false) }

    var isDesktopSite by rememberSaveable {
        mutableStateOf(app.webViewManager.isDesktopSite)
    }

    val onDesktopSiteToggle: (Boolean) -> Unit = { enabled ->
        isDesktopSite = enabled
        app.webViewManager.setDesktopMode(enabled)
        if (enabled) {
            app.searchEngine.addLog("Desktop Site enabled: All WebViews converted to PC desktop layout.", "success")
        } else {
            app.searchEngine.addLog("Desktop Site disabled: WebViews reverted to mobile layout.", "info")
        }
    }

    // Dialog states
    var signInProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var configProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var graphProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var deleteCandidateProfile by remember { mutableStateOf<AccountProfile?>(null) }

    // Click-to-locate state
    var locatedProfileId by remember { mutableStateOf<String?>(null) }

    // Calculate total points earned today across all profiles
    val totalPoints = profiles.sumOf { it.searchCount * 3 }

    // Automatically select all showcase profiles by default once on initial launch
    LaunchedEffect(profiles) {
        if (!hasInitializedSelection && profiles.isNotEmpty()) {
            selectedProfileIds = profiles.map { it.id }.toSet()
            hasInitializedSelection = true
        }
    }

    // Refresh tracker data on mount
    LaunchedEffect(Unit) {
        app.accountRepository.refreshWithTracker()
    }

    // Initialize search session for specifically selected profiles (opens session, loads WebViews, does not start search yet)
    fun startSearchSession(targetProfileIds: Set<String>) {
        if (targetProfileIds.isEmpty()) return
        selectedProfileIds = targetProfileIds
        isSessionActive = true
        val targets = profiles.filter { targetProfileIds.contains(it.id) }
        app.searchEngine.initSession(targets)
        activeView = DashboardActiveView.SESSION
    }

    // Active profiles strictly filtered by what the search engine is running or what the user selected
    val activeProfiles = profiles.filter { profile ->
        if (engineActiveProfileIds.isNotEmpty()) {
            engineActiveProfileIds.contains(profile.id) || selectedProfileIds.contains(profile.id)
        } else {
            selectedProfileIds.contains(profile.id)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 1. Top Custom Title Bar (Replicating PC .titlebar)
        AppTitleBar(
            isSessionView = activeView == DashboardActiveView.SESSION,
            isSessionRunning = isSessionRunning,
            canNavigateToSession = isSessionActive,
            onNavigateBack = { activeView = DashboardActiveView.PROFILES },
            onNavigateToSession = { activeView = DashboardActiveView.SESSION }
        )

        // 2. Main Scrollable Container (Matches PC #dashboardView and #sessionView)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeView) {
                DashboardActiveView.PROFILES -> {
                    // ═══════════════════════════════════════════════════
                    // DASHBOARD VIEW (Shown when inspecting profiles)
                    // ═══════════════════════════════════════════════════

                    // Status Banner (.status-banner)
                    StatusBanner(
                        statusText = when {
                            isSessionRunning && isSessionPaused -> "Paused — Session on hold"
                            isSessionRunning -> "Searching — ${engineActiveProfileIds.size} accounts active"
                            profiles.all { it.searchesDone && it.searchCount > 0 } -> "Completed — All profiles done today"
                            else -> "Ready — Select accounts and start your session"
                        },
                        totalPoints = totalPoints
                    )

                    // Global Points Canvas (#globalPointsCanvas)
                    GlobalPointsCanvas(
                        totalPoints = totalPoints,
                        onResetGlobalPoints = {
                            coroutineScope.launch {
                                app.accountRepository.resetAllGlobalPoints()
                            }
                        }
                    )

                    // Quick Actions Bar (.action-bar)
                    QuickActionBar(
                        onRunAllClick = {
                            val allIds = profiles.map { it.id }.toSet()
                            startSearchSession(allIds)
                        },
                        onRunSelectedClick = {
                            val targets = if (selectedProfileIds.isNotEmpty()) selectedProfileIds else profiles.map { it.id }.toSet()
                            startSearchSession(targets)
                        },
                        onRefreshClick = {
                            coroutineScope.launch {
                                app.accountRepository.refreshWithTracker()
                            }
                        }
                    )

                    // Profiles 2-Column Grid with Showcase Cards & AddProfileCard
                    AccountCardsGrid(
                        profiles = profiles,
                        selectedProfileIds = selectedProfileIds,
                        onToggleSelect = { id ->
                            selectedProfileIds = if (selectedProfileIds.contains(id)) {
                                selectedProfileIds - id
                            } else {
                                selectedProfileIds + id
                            }
                        },
                        onSelectAll = {
                            selectedProfileIds = profiles.map { it.id }.toSet()
                        },
                        onDeselectAll = {
                            selectedProfileIds = emptySet()
                        },
                        onAddProfile = { name, maxPts ->
                            coroutineScope.launch {
                                val newProf = app.accountRepository.addProfile(name, maxPts)
                                selectedProfileIds = selectedProfileIds + newProf.id
                            }
                        },
                        onSignInClick = { prof -> signInProfile = prof },
                        onConfigClick = { prof -> configProfile = prof },
                        onDeleteClick = { prof -> deleteCandidateProfile = prof },
                        onMarkDone = { prof ->
                            coroutineScope.launch {
                                app.accountRepository.markProfileDoneManual(prof.id)
                                app.searchEngine.addLog("Marked searches done for ${prof.label}", "success")
                            }
                        },
                        onMarkPending = { prof ->
                            coroutineScope.launch {
                                app.accountRepository.markProfileUndoneManual(prof.id)
                                app.searchEngine.addLog("Marked searches pending for ${prof.label}", "info")
                            }
                        }
                    )
                }

                DashboardActiveView.SESSION -> {
                    // ═══════════════════════════════════════════════════
                    // SESSION VIEW (Shown when search session is active)
                    // Matches PC #sessionView (Top Modules + WebViews)
                    // ═══════════════════════════════════════════════════

                    // Module 1: Dashboard (NutshellCard with 0 PTS and NUTSHELL Live list)
                    NutshellCard(
                        profiles = if (activeProfiles.isNotEmpty()) activeProfiles else profiles,
                        totalPoints = totalPoints,
                        onLocateProfile = { profileId ->
                            locatedProfileId = profileId
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                delay(2500)
                                if (locatedProfileId == profileId) {
                                    locatedProfileId = null
                                }
                            }
                        }
                    )

                    // Module 2: Controls (.controls-module)
                    ControlsRow(
                        isSessionRunning = isSessionRunning,
                        isSessionPaused = isSessionPaused,
                        isFetchUnlocked = isFetchUnlocked,
                        selectedCount = activeProfiles.size,
                        isDesktopSite = isDesktopSite,
                        onDesktopSiteToggle = { onDesktopSiteToggle(!isDesktopSite) },
                        onFetchClick = {
                            app.searchEngine.unlockFetchGate()
                        },
                        onStopClick = {
                            val intent = Intent(context, SearchForegroundService::class.java).apply {
                                action = SearchForegroundService.ACTION_PAUSE_GLOBAL
                            }
                            context.startService(intent)
                        },
                        onExitClick = {
                            val intent = Intent(context, SearchForegroundService::class.java).apply {
                                action = SearchForegroundService.ACTION_EXIT_GLOBAL
                            }
                            context.startService(intent)
                            isSessionActive = false
                            activeView = DashboardActiveView.PROFILES
                        },
                        onRefreshAllClick = {
                            coroutineScope.launch {
                                app.accountRepository.refreshWithTracker()
                            }
                        },
                        onStartGlobalClick = {
                            val targets = if (activeProfiles.isNotEmpty()) activeProfiles else profiles.filter { selectedProfileIds.contains(it.id) }
                            val intent = Intent(context, SearchForegroundService::class.java).apply {
                                action = SearchForegroundService.ACTION_START_GLOBAL
                                putExtra(SearchForegroundService.EXTRA_IS_FLASH, isGlobalFlashActive)
                                putStringArrayListExtra(SearchForegroundService.EXTRA_PROFILE_IDS, ArrayList(targets.map { it.id }))
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            app.searchEngine.startGlobal(targets, isGlobalFlashActive)
                        }
                    )

                    // Module 3: Holistic Settings (.holistic-module)
                    HolisticSettingsCard(
                        globalTimeframeMinutes = globalTimeframeMinutes,
                        onTimeframeChange = { mins ->
                            globalTimeframeMinutes = mins
                        },
                        maxConcurrentProfiles = maxConcurrentProfiles,
                        onConcurrentChange = {
                            maxConcurrentProfiles = it
                            app.searchEngine.maxConcurrentProfiles = it
                        },
                        isFlashMode = isGlobalFlashActive,
                        onFlashModeToggle = {
                            if (isGlobalFlashActive) {
                                app.searchEngine.stopGlobalFlash()
                            } else {
                                val targets = if (activeProfiles.isNotEmpty()) activeProfiles else profiles.filter { selectedProfileIds.contains(it.id) }.ifEmpty { profiles }
                                val intent = Intent(context, SearchForegroundService::class.java).apply {
                                    action = SearchForegroundService.ACTION_START_GLOBAL
                                    putExtra(SearchForegroundService.EXTRA_IS_FLASH, true)
                                    putStringArrayListExtra(SearchForegroundService.EXTRA_PROFILE_IDS, ArrayList(targets.map { it.id }))
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                app.searchEngine.startGlobalFlash(targets, 5)
                            }
                        },
                        isDesktopSite = isDesktopSite,
                        onDesktopSiteToggle = onDesktopSiteToggle,
                        runningFlashProfileIds = flashProfileIds,
                        onStartProfileFlash = { profId, delaySec ->
                            val intent = Intent(context, SearchForegroundService::class.java).apply {
                                action = SearchForegroundService.ACTION_START_PROFILE
                                putExtra(SearchForegroundService.EXTRA_PROFILE_ID, profId)
                                putExtra(SearchForegroundService.EXTRA_IS_FLASH, true)
                                putExtra(SearchForegroundService.EXTRA_FLASH_DELAY, delaySec)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            app.searchEngine.startProfileFlash(profId, delaySec)
                        },
                        onStopProfileFlash = { profId ->
                            app.searchEngine.stopProfileFlash(profId)
                        },
                        onResetAll = {
                            globalTimeframeMinutes = 30
                            app.searchEngine.addLog("🔄 Holistic Reset: Global timeframe → 30m default, all profile settings cleared.", "success")
                        },
                        onResetProfile = { profId ->
                            val prof = profiles.find { it.id == profId }
                            val label = prof?.label ?: "profile"
                            app.searchEngine.addLog("🔄 Profile Reset: Settings cleared for $label.", "success")
                        },
                        profiles = activeProfiles
                    )

                    // Module 4: Console (.console-module)
                    ConsoleCard(
                        logs = consoleLogs,
                        onClear = {
                            app.searchEngine.clearLogs()
                        }
                    )

                    // ═══════════════════════════════════════════════════
                    // SESSION WEBVIEWS CONTAINER
                    // ═══════════════════════════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSION WEBVIEWS (${activeProfiles.size})",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPreviewsCollapsed) "Expand (Normal)" else "Collapse (1dp)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = isPreviewsCollapsed,
                                onCheckedChange = { isPreviewsCollapsed = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Accent,
                                    checkedTrackColor = AccentGlow
                                )
                            )
                        }
                    }

                    // Running Profile WebViews (NON-lazy Column layout so WebViews are never paused or destroyed)
                    if (activeProfiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(BgCard, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active session tiles. Run accounts to launch WebViews.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeProfiles.forEach { profile ->
                                SessionTile(
                                    profile = profile,
                                    isCollapsed = isPreviewsCollapsed,
                                    isLocateGlow = locatedProfileId == profile.id,
                                    webViewManager = app.webViewManager,
                                    searchEngine = app.searchEngine,
                                    onCloseClick = {
                                        app.searchEngine.removeProfileFromSession(profile.id)
                                        val remaining = selectedProfileIds - profile.id
                                        selectedProfileIds = remaining
                                        coroutineScope.launch {
                                            app.accountRepository.updateProfile(profile.copy(inProgress = false))
                                        }
                                        if (remaining.isEmpty()) {
                                            isSessionActive = false
                                            activeView = DashboardActiveView.PROFILES
                                            val intent = Intent(context, SearchForegroundService::class.java).apply {
                                                action = SearchForegroundService.ACTION_EXIT_GLOBAL
                                            }
                                            context.startService(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ─── Dialogs ───

    // Sign-in Dialog
    signInProfile?.let { prof ->
        SignInDialog(
            profile = prof,
            webViewManager = app.webViewManager,
            onDismiss = { signInProfile = null },
            onSignedIn = { detectedEmail ->
                coroutineScope.launch {
                    app.accountRepository.updateProfile(
                        prof.copy(email = detectedEmail, isSignedIn = true)
                    )
                }
            }
        )
    }

    // Config Dialog
    configProfile?.let { prof ->
        ProfileConfigDialog(
            profile = prof,
            onDismiss = { configProfile = null },
            onSave = { updated ->
                coroutineScope.launch {
                    app.accountRepository.updateProfile(updated)
                }
            }
        )
    }

    // Pacing Graph Dialog
    graphProfile?.let { prof ->
        PacingChartDialog(
            profile = prof,
            onDismiss = { graphProfile = null }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidateProfile?.let { prof ->
        AlertDialog(
            onDismissRequest = { deleteCandidateProfile = null },
            title = { Text("Delete Account Slot?", color = TextPrimary) },
            text = {
                Text("This will delete ${prof.label} and wipe its profile data. This cannot be undone.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            app.accountRepository.deleteProfile(prof.id)
                            app.webViewManager.deleteProfile(prof.id)
                        }
                        deleteCandidateProfile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateProfile = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BgCard
        )
    }
}
