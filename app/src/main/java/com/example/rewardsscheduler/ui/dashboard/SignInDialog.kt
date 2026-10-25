package com.example.rewardsscheduler.ui.dashboard

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*
import com.example.rewardsscheduler.webview.WebViewManager

@Composable
fun SignInDialog(
    profile: AccountProfile,
    webViewManager: WebViewManager,
    onDismiss: () -> Unit,
    onSignedIn: (email: String) -> Unit
) {
    var detectedEmail by remember { mutableStateOf(profile.email) }
    var currentUrl by remember { mutableStateOf("") }

    val webView = remember {
        webViewManager.getOrCreateWebView(profile.id).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    currentUrl = url ?: ""
                    // Try to detect signed in user from Bing or Microsoft Account
                    view?.evaluateJavascript(
                        "(function() { " +
                        "  var el = document.querySelector('#id_s, .id_name, .mectrl_name, #mectrl_currentAccount');" +
                        "  return el ? el.innerText : '';" +
                        "})()"
                    ) { result ->
                        val clean = result?.replace("\"", "")?.trim().orEmpty()
                        if (clean.isNotBlank() && clean != "null" && clean != detectedEmail) {
                            detectedEmail = clean
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        webView.loadUrl("https://login.live.com/")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = BgPrimary
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgCard)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sign In: ${profile.label}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (detectedEmail.isNotBlank()) "Detected: $detectedEmail" else "Sign in with your Microsoft account",
                            color = if (detectedEmail.isNotBlank()) Success else TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSignedIn(detectedEmail)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Success),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(onClick = onDismiss) {
                            Text("✕", color = TextSecondary, fontSize = 16.sp)
                        }
                    }
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom shortcut bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSecondary)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { webView.loadUrl("https://login.live.com/") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Microsoft Login", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = { webView.loadUrl("https://www.bing.com/") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Bing Rewards", fontSize = 11.sp, color = Accent)
                    }
                }
            }
        }
    }
}
