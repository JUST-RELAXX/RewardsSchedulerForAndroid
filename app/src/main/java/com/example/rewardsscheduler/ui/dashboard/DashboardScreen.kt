package com.example.rewardsscheduler.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
 * - PROFILES view (Home): AppTitleBar, StatusBanner, GlobalPointsCanvas, QuickActionBar, AccountCardsGrid
 *   (Home total = cumulative daily points across all accounts, resets at midnight)
 * - SESSION view (Session): Dashboard/NutshellCard, ControlsRow, HolisticSettingsCard, ConsoleCard, SessionTiles
 *   (Session total = ONLY points earned in CURRENT session, starts at 0, per-profile min(searches*3, maxPoints))
 * - Task 1: Complete removal of desktop mode
 * - Task 4: Back button resilience and focused profile navigation
 * - Zero emojis!
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
    val runningProfileIds by app.searchEngine.runningProfileIds.collectAsState()
    val waitingProfileIds by app.searchEngine.waitingProfileIds.collectAsState()

    // Task 2: Session search counts (isolated per session)
    val sessionSearchCounts by app.searchEngine.sessionSearchCounts.collectAsState()

    // View mode: PROFILES (Home) or SESSION
    var activeView by remember { mutableStateOf(DashboardActiveView.PROFILES) }
    var isSessionActive by rememberSaveable { mutableStateOf(false) }

    // Local UI states
    var selectedProfileIds by remember { mutableStateOf(setOf<String>()) }
    var hasInitializedSelection by rememberSaveable { mutableStateOf(false) }
    var globalTimeframeMinutes by remember { mutableIntStateOf(30) }
    var maxConcurrentProfiles by remember { mutableIntStateOf(app.searchEngine.maxConcurrentProfiles) }
    var isPreviewsCollapsed by remember { mutableStateOf(false) }

    // Task 4: Focused profile ID
    var focusedProfileId by remember { mutableStateOf<String?>(null) }
    var backPressedTimestamp by remember { mutableLongStateOf(0L) }

    // Dialog states
    var signInProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var configProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var graphProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var deleteCandidateProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var editMaxPointsProfile by remember { mutableStateOf<AccountProfile?>(null) }

    // Click-to-locate state
    var locatedProfileId by remember { mutableStateOf<String?>(null) }

    // Active profiles strictly filtered by what the session is running or what the user selected
    val activeProfiles = profiles.filter { profile ->
        if (engineActiveProfileIds.isNotEmpty()) {
            engineActiveProfileIds.contains(profile.id) || selectedProfileIds.contains(profile.id)
        } else {
            selectedProfileIds.contains(profile.id)
        }
    }

    // Task 2: HOME dashboard shows daily total across ALL accounts capped at maxPoints
    val homeDailyTotalPoints = profiles.sumOf { minOf(it.searchCount * 3, it.maxPoints) }

    // Task 2: SESSION dashboard shows points accumulated for participating profiles
    val sessionTotalPoints = activeProfiles.sumOf { prof ->
        val count = sessionSearchCounts[prof.id] ?: prof.searchCount
        minOf(count * 3, prof.maxPoints)
    }

    // Task 4.1: Compose BackHandler
    BackHandler(enabled = true) {
        if (focusedProfileId != null) {
            val pid = focusedProfileId!!
            if (app.webViewManager.canGoBack(pid)) {
                app.webViewManager.goBack(pid)
            } else {
                focusedProfileId = null // Leave focus and return to session screen
            }
        } else if (activeView == DashboardActiveView.SESSION) {
            // Return to Home screen (Session & foreground service keep running!)
            activeView = DashboardActiveView.PROFILES
        } else {
            // On Home
            if (isSessionRunning || isSessionActive) {
                // Background the app; session continues running in foreground service
                (context as? Activity)?.moveTaskToBack(true)
            } else {
                // Two quick presses to exit
                val now = System.currentTimeMillis()
                if (now - backPressedTimestamp < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedTimestamp = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    // Initialize search session for specifically selected profiles
    fun startSearchSession(targetProfileIds: Set<String>) {
        if (targetProfileIds.isEmpty()) return
        selectedProfileIds = targetProfileIds
        isSessionActive = true
        val targets = profiles.filter { targetProfileIds.contains(it.id) }
        app.searchEngine.initSession(targets)
        activeView = DashboardActiveView.SESSION
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
                    // HOME / PROFILES VIEW (Daily persistent stats)
                    // ═══════════════════════════════════════════════════

                    // Status Banner (.status-banner)
                    StatusBanner(
                        statusText = when {
                            isSessionRunning && isSessionPaused -> "Paused — Session on hold"
                            isSessionRunning -> "Searching — ${engineActiveProfileIds.size} accounts active"
                            profiles.all { it.searchesDone && it.searchCount > 0 } -> "Completed — All profiles done today"
                            else -> "Ready — Select accounts and start your session"
                        },
                        totalPoints = homeDailyTotalPoints
                    )

                    // Global Points Canvas (#globalPointsCanvas)
                    GlobalPointsCanvas(
                        totalPoints = homeDailyTotalPoints,
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
                    // SESSION VIEW (Current running session)
                    // ═══════════════════════════════════════════════════

                    // Module 1: Dashboard (NutshellCard showing SESSION points, starts at 0)
                    NutshellCard(
                        profiles = if (activeProfiles.isNotEmpty()) activeProfiles else profiles,
                        sessionSearchCounts = sessionSearchCounts,
                        sessionTotalPoints = sessionTotalPoints,
                        runningProfileIds = runningProfileIds,
                        waitingProfileIds = waitingProfileIds,
                        onLocateProfile = { profileId ->
                            locatedProfileId = profileId
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                delay(2500)
                                if (locatedProfileId == profileId) {
                                    locatedProfileId = null
                                }
                            }
                        },
                        onEditMaxPoints = { prof ->
                            editMaxPointsProfile = prof
                        }
                    )

                    // Module 2: Controls (.controls-module)
                    ControlsRow(
                        isSessionRunning = isSessionRunning,
                        isSessionPaused = isSessionPaused,
                        isFetchUnlocked = isFetchUnlocked,
                        selectedCount = activeProfiles.size,
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
                        profiles = if (activeProfiles.isNotEmpty()) activeProfiles else profiles,
                        globalTimeframeMinutes = globalTimeframeMinutes,
                        onTimeframeChange = { mins ->
                            globalTimeframeMinutes = mins
                            app.searchEngine.globalTimeframeMinutes = mins
                        },
                        maxConcurrentProfiles = maxConcurrentProfiles,
                        onConcurrentChange = {
                            maxConcurrentProfiles = it
                            app.searchEngine.maxConcurrentProfiles = it
                        },
                        isFlashMode = isGlobalFlashActive,
                        onFlashModeToggle = { delaySec ->
                            if (isGlobalFlashActive) {
                                app.searchEngine.stopGlobalFlash()
                            } else {
                                val targets = if (activeProfiles.isNotEmpty()) activeProfiles else profiles.filter { selectedProfileIds.contains(it.id) }.ifEmpty { profiles }
                                val intent = Intent(context, SearchForegroundService::class.java).apply {
                                    action = SearchForegroundService.ACTION_START_GLOBAL
                                    putExtra(SearchForegroundService.EXTRA_IS_FLASH, true)
                                    putExtra(SearchForegroundService.EXTRA_FLASH_DELAY, delaySec)
                                    putStringArrayListExtra(SearchForegroundService.EXTRA_PROFILE_IDS, ArrayList(targets.map { it.id }))
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                app.searchEngine.startGlobalFlash(targets, delaySec)
                            }
                        },
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
                            app.searchEngine.globalTimeframeMinutes = 30
                            coroutineScope.launch {
                                profiles.forEach { prof ->
                                    app.accountRepository.updateProfile(
                                        prof.copy(
                                            timeframeMinutes = null,
                                            customMinDelay = null,
                                            customMaxDelay = null
                                        )
                                    )
                                }
                            }
                            app.searchEngine.addLog("Holistic Reset: Global timeframe -> 30m default, all profile settings cleared.", "success")
                        },
                        onResetProfile = { profId ->
                            val prof = profiles.find { it.id == profId }
                            val label = prof?.label ?: "profile"
                            if (prof != null) {
                                coroutineScope.launch {
                                    app.accountRepository.updateProfile(
                                        prof.copy(
                                            timeframeMinutes = null,
                                            customMinDelay = null,
                                            customMaxDelay = null
                                        )
                                    )
                                }
                            }
                            app.searchEngine.addLog("Profile Reset: Settings cleared for $label.", "success")
                        },
                        onUpdateProfileDelays = { profId, minDelay, maxDelay ->
                            val prof = profiles.find { it.id == profId }
                            if (prof != null) {
                                coroutineScope.launch {
                                    app.accountRepository.updateProfile(
                                        prof.copy(
                                            customMinDelay = minDelay,
                                            customMaxDelay = maxDelay
                                        )
                                    )
                                }
                                val delayStr = if (minDelay != null && maxDelay != null) "${minDelay}s - ${maxDelay}s" else "Auto"
                                app.searchEngine.addLog("[${prof.label}] Pacing delay set to $delayStr", "info")
                            }
                        }
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                                    isFocused = focusedProfileId == profile.id,
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
                                    },
                                    onFocusClick = {
                                        focusedProfileId = profile.id
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

    // Task 4: Focused Profile View
    focusedProfileId?.let { pid ->
        val prof = profiles.find { it.id == pid }
        if (prof != null) {
            FocusedProfileView(
                profile = prof,
                webViewManager = app.webViewManager,
                onDismiss = { focusedProfileId = null }
            )
        }
    }

    // Task 3.1: Edit Max Points Dialog (Pencil action from Nutshell row)
    editMaxPointsProfile?.let { prof ->
        EditMaxPointsDialog(
            profile = prof,
            onDismiss = { editMaxPointsProfile = null },
            onSave = { newMaxPoints ->
                app.searchEngine.updateProfileMaxPoints(prof.id, newMaxPoints)
            }
        )
    }

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
                    if (updated.maxPoints != prof.maxPoints) {
                        app.searchEngine.updateProfileMaxPoints(updated.id, updated.maxPoints)
                    }
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
