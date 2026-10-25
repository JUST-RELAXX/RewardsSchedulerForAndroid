package com.example.rewardsscheduler.spike

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.rewardsscheduler.webview.WebViewManager

/**
 * SPIKE A — Isolation Proof
 *
 * Two WebViews on two different Profiles.
 * Prove: set a cookie/localStorage value on bing.com in one Profile,
 * confirm the other can't see it. Confirm after app restart.
 *
 * This activity is only for the spike — it will be removed once proven.
 */
class SpikeIsolationActivity : ComponentActivity() {

    private lateinit var webViewManager: WebViewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webViewManager = WebViewManager(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0E1A),
                    surface = Color(0xFF1A2235),
                    primary = Color(0xFF00B0FF),
                )
            ) {
                SpikeIsolationScreen(webViewManager)
            }
        }
    }

    override fun onDestroy() {
        // Don't destroy WebViews on config change — only on real finish
        if (isFinishing) {
            webViewManager.destroyAll()
        }
        super.onDestroy()
    }
}

@Composable
fun SpikeIsolationScreen(webViewManager: WebViewManager) {
    val scrollState = rememberScrollState()

    var multiProfileSupported by remember { mutableStateOf<Boolean?>(null) }
    var profile1Log by remember { mutableStateOf("") }
    var profile2Log by remember { mutableStateOf("") }
    var webViewVersion by remember { mutableStateOf("Unknown") }

    // Check support on first composition
    LaunchedEffect(Unit) {
        multiProfileSupported = webViewManager.isMultiProfileSupported
        try {
            val pi = android.webkit.WebView.getCurrentWebViewPackage()
            webViewVersion = "${pi?.packageName} v${pi?.versionName}"
        } catch (e: Exception) {
            webViewVersion = "Could not detect"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Text(
            "SPIKE A — Isolation Test",
            color = Color(0xFF00B0FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        // System info
        Text("WebView: $webViewVersion", color = Color(0xFF8899AA), fontSize = 12.sp)
        Text(
            "Multi-Profile: ${multiProfileSupported?.let { if (it) "SUPPORTED" else "NOT SUPPORTED" } ?: "Checking..."}",
            color = if (multiProfileSupported == true) Color(0xFF00E676) else Color(0xFFFF1744),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (multiProfileSupported == false) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF331111)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Your Android System WebView does not support multi-profile. " +
                    "Please update Android System WebView from the Play Store to the latest version.",
                    color = Color(0xFFFF1744),
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
            return
        }

        // Profile 1 section
        ProfileTestSection(
            profileName = "spike_profile_1",
            webViewManager = webViewManager,
            log = profile1Log,
            onLog = { profile1Log += it + "\n" },
            accentColor = Color(0xFF00B0FF)
        )

        Spacer(Modifier.height(24.dp))

        // Profile 2 section
        ProfileTestSection(
            profileName = "spike_profile_2",
            webViewManager = webViewManager,
            log = profile2Log,
            onLog = { profile2Log += it + "\n" },
            accentColor = Color(0xFFFFAB00)
        )

        Spacer(Modifier.height(24.dp))

        // Cross-profile check
        Button(
            onClick = {
                // Read cookie from profile 1
                webViewManager.evaluateJavascript("spike_profile_1",
                    "document.cookie"
                ) { cookie1 ->
                    profile1Log += "Cookie read: $cookie1\n"
                }
                // Read cookie from profile 2
                webViewManager.evaluateJavascript("spike_profile_2",
                    "document.cookie"
                ) { cookie2 ->
                    profile2Log += "Cookie read: $cookie2\n"
                }
                // Read localStorage from profile 1
                webViewManager.evaluateJavascript("spike_profile_1",
                    "localStorage.getItem('spike_test')"
                ) { ls1 ->
                    profile1Log += "localStorage['spike_test']: $ls1\n"
                }
                // Read localStorage from profile 2
                webViewManager.evaluateJavascript("spike_profile_2",
                    "localStorage.getItem('spike_test')"
                ) { ls2 ->
                    profile2Log += "localStorage['spike_test']: $ls2\n"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔍 Cross-Check Both Profiles", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileTestSection(
    profileName: String,
    webViewManager: WebViewManager,
    log: String,
    onLog: (String) -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val webView = remember { webViewManager.getOrCreateWebView(profileName) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2235)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Profile: $profileName",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))

            // WebView — 200dp tall for visual inspection
            AndroidView(
                factory = {
                    // Detach from any previous parent first
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            600 // pixels, ~200dp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        webView.loadUrl("https://www.bing.com")
                        onLog("Loaded bing.com")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load Bing", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        webView.evaluateJavascript(
                            "document.cookie = 'spike_cookie=${profileName};path=/;max-age=86400'; " +
                            "localStorage.setItem('spike_test', '$profileName'); " +
                            "'SET OK: cookie + localStorage for $profileName'"
                        ) { result ->
                            onLog("Set: $result")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Data", fontSize = 11.sp, color = Color.Black)
                }

                Button(
                    onClick = {
                        webView.evaluateJavascript(
                            "JSON.stringify({cookie: document.cookie, ls: localStorage.getItem('spike_test')})"
                        ) { result ->
                            onLog("Read: $result")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Read Data", fontSize = 11.sp, color = Color.Black)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Log output
            Text(
                "Log:",
                color = Color(0xFF8899AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                log.ifEmpty { "(no log entries yet)" },
                color = Color(0xFF556677),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 150.dp)
            )
        }
    }
}
