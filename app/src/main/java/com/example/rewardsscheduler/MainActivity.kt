package com.example.rewardsscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewFeature
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.rewardsscheduler.service.RewardNotificationScheduler
import com.example.rewardsscheduler.service.SearchForegroundService
import com.example.rewardsscheduler.theme.*
import com.example.rewardsscheduler.theme.RewardsSchedulerTheme
import com.example.rewardsscheduler.ui.dashboard.DashboardScreen
import com.example.rewardsscheduler.webview.WebViewManager

/**
 * Main Activity — Single Activity architecture.
 * Combines the full Dashboard with tabs for Spike tests.
 */
class MainActivity : ComponentActivity() {

    private val spikeLogReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(SearchForegroundService.EXTRA_LOG_MESSAGE) ?: return
            spikeBLog.value += "$msg\n"
        }
    }

    // Observable state for spike B logs
    companion object {
        val spikeBLog = mutableStateOf("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize smart notification reminder engine
        RewardNotificationScheduler(this).scheduleInitialReminder()

        // Register broadcast receiver for spike B logs
        val filter = IntentFilter(SearchForegroundService.BROADCAST_SPIKE_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(spikeLogReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(spikeLogReceiver, filter)
        }

        val app = application as RewardsApp

        setContent {
            RewardsSchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainContainer(app)
                }
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(spikeLogReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}

// ═══════════════════════════════════════════════════
// App Main Container with Dashboard & Spike tabs
// ═══════════════════════════════════════════════════

@Composable
fun AppMainContainer(app: RewardsApp) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // Default to full Dashboard
    var showBatteryPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.getSystemService(PowerManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
            val prefs = context.getSharedPreferences("rewards_prefs", Context.MODE_PRIVATE)
            val alreadyPrompted = prefs.getBoolean("battery_prompted", false)
            if (!alreadyPrompted) {
                showBatteryPrompt = true
            }
        }
    }

    if (showBatteryPrompt) {
        AlertDialog(
            onDismissRequest = {
                val prefs = context.getSharedPreferences("rewards_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("battery_prompted", true).apply()
                showBatteryPrompt = false
            },
            title = { Text("Enable Background Searching", color = TextPrimary) },
            text = {
                Text(
                    "To ensure searches run reliably when your phone screen is off, please allow RewardsScheduler to run in the background without battery restrictions.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("rewards_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("battery_prompted", true).apply()
                        showBatteryPrompt = false
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Allow", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("rewards_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("battery_prompted", true).apply()
                        showBatteryPrompt = false
                    }
                ) {
                    Text("Later", color = TextMuted)
                }
            },
            containerColor = BgCard
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .statusBarsPadding()
    ) {
        DashboardScreen(app)
    }
}

// ═══════════════════════════════════════════════════
// SPIKE B — Background Execution Test
// ═══════════════════════════════════════════════════

@Composable
fun SpikeBScreen(webViewManager: WebViewManager) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isRunning by remember { mutableStateOf(false) }
    val logText by MainActivity.spikeBLog

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "SPIKE B — Background Execution",
            color = Color(0xFF00B0FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tests if evaluateJavascript works every 30s with app backgrounded + screen off",
            color = Color(0xFF556677),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        // Instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("How to test:", color = Color(0xFFFFAB00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("1. Tap START below", color = Color(0xFF8899AA), fontSize = 12.sp)
                Text("2. You'll see a persistent notification", color = Color(0xFF8899AA), fontSize = 12.sp)
                Text("3. Press HOME to background the app", color = Color(0xFF8899AA), fontSize = 12.sp)
                Text("4. Turn off the screen", color = Color(0xFF8899AA), fontSize = 12.sp)
                Text("5. Wait 10+ minutes", color = Color(0xFF8899AA), fontSize = 12.sp)
                Text("6. Come back and check the log below", color = Color(0xFF8899AA), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "[PASS] = calls every ~30s with no gaps -> background execution works",
                    color = Color(0xFF00E676), fontSize = 11.sp
                )
                Text(
                    "[FAIL] = gaps of 2-5min between calls -> JS is being throttled",
                    color = Color(0xFFFF1744), fontSize = 11.sp
                )
            }
        }

        // Collapsed (1dp) WebView physically attached to hierarchy for realistic background testing
        val spikeBWebView = remember { webViewManager.getOrCreateWebView("spike_bg_test") }
        AndroidView(
            factory = {
                (spikeBWebView.parent as? ViewGroup)?.removeView(spikeBWebView)
                spikeBWebView.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1 // 1 pixel height (collapsed)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Start/Stop buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        MainActivity.spikeBLog.value = ""
                        val intent = Intent(context, SearchForegroundService::class.java).apply {
                            action = SearchForegroundService.ACTION_START_SPIKE_B
                            putExtra(SearchForegroundService.EXTRA_PROFILE_NAME, "spike_bg_test")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isRunning = true
                    }
                },
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("▶ START", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val intent = Intent(context, SearchForegroundService::class.java).apply {
                        action = SearchForegroundService.ACTION_STOP
                    }
                    context.startService(intent)
                    isRunning = false
                },
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("⬛ STOP", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Log output
        Text(
            "Execution Log:",
            color = Color(0xFF8899AA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            logText.ifEmpty { "(tap START to begin the test)" },
            color = Color(0xFF8899AA),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(10.dp)
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════════════
// SPIKE A — Isolation Test (kept for reference)
// ═══════════════════════════════════════════════════

@Composable
fun SpikeAScreen(webViewManager: WebViewManager) {
    val scrollState = rememberScrollState()

    var multiProfileSupported by remember { mutableStateOf<Boolean?>(null) }
    var profile1Log by remember { mutableStateOf("") }
    var profile2Log by remember { mutableStateOf("") }
    var webViewVersion by remember { mutableStateOf("Unknown") }
    var androidVersion by remember { mutableStateOf("Unknown") }

    LaunchedEffect(Unit) {
        multiProfileSupported = webViewManager.isMultiProfileSupported
        try {
            val pi = WebView.getCurrentWebViewPackage()
            webViewVersion = "${pi?.packageName} v${pi?.versionName}"
        } catch (e: Exception) {
            webViewVersion = "Could not detect"
        }
        androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "SPIKE A — Isolation Test",
            color = Color(0xFF00E676),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text("PASSED — profiles are isolated", color = Color(0xFF00E676), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                InfoRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                InfoRow("Android", androidVersion)
                InfoRow("WebView", webViewVersion)
                InfoRow("Multi-Profile", if (multiProfileSupported == true) "SUPPORTED" else "NOT SUPPORTED")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Profile test cards
        ProfileTestCard("spike_profile_1", "Profile A (Blue)", webViewManager, profile1Log, { profile1Log += "$it\n" }, Color(0xFF00B0FF))
        Spacer(Modifier.height(16.dp))
        ProfileTestCard("spike_profile_2", "Profile B (Amber)", webViewManager, profile2Log, { profile2Log += "$it\n" }, Color(0xFFFFAB00))

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                profile1Log += "── Cross-Check ──\n"
                profile2Log += "── Cross-Check ──\n"
                webViewManager.evaluateJavascript("spike_profile_1",
                    "JSON.stringify({cookie: document.cookie, ls: localStorage.getItem('spike_test')})"
                ) { r -> profile1Log += "Profile A data: $r\n" }
                webViewManager.evaluateJavascript("spike_profile_2",
                    "JSON.stringify({cookie: document.cookie, ls: localStorage.getItem('spike_test')})"
                ) { r -> profile2Log += "Profile B data: $r\n" }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("🔍 CROSS-CHECK BOTH PROFILES", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", color = Color(0xFF556677), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFFE8EDF5), fontSize = 12.sp)
    }
}

@Composable
fun ProfileTestCard(
    profileName: String, displayName: String,
    webViewManager: WebViewManager, log: String,
    onLog: (String) -> Unit, accentColor: Color
) {
    val webView = remember { webViewManager.getOrCreateWebView(profileName) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2235)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(displayName, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("profile: $profileName", color = Color(0xFF556677), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            AndroidView(
                factory = { (webView.parent as? ViewGroup)?.removeView(webView); webView.apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }},
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { webView.loadUrl("https://www.bing.com"); onLog("→ Loaded bing.com") },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Load Bing", fontSize = 11.sp, maxLines = 1) }

                Button(onClick = {
                    webView.evaluateJavascript(
                        "document.cookie = 'spike_cookie=${profileName};path=/;max-age=86400'; localStorage.setItem('spike_test', '${profileName}'); 'SET OK for ${profileName}'"
                    ) { result -> onLog("Set: $result") }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Set Data", fontSize = 11.sp, color = Color.Black, maxLines = 1) }

                Button(onClick = {
                    webView.evaluateJavascript(
                        "JSON.stringify({cookie: document.cookie, ls: localStorage.getItem('spike_test')})"
                    ) { result -> onLog("Read: $result") }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Read Data", fontSize = 11.sp, color = Color.Black, maxLines = 1) }
            }
            Spacer(Modifier.height(8.dp))

            Text("Console:", color = Color(0xFF8899AA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                log.ifEmpty { "(no log entries yet)" },
                color = Color(0xFF8899AA), fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp, max = 120.dp)
                    .background(Color(0xFF0D1117), RoundedCornerShape(6.dp)).padding(8.dp)
            )
        }
    }
}
