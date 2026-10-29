package com.example.rewardsscheduler.ui.dashboard

import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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

/**
 * Full-screen focused-profile view:
 * 4.1: Compose BackHandler:
 *   - if WebView canGoBack() -> goBack()
 *   - if it has no history -> leave focus and return to session screen
 * 4.2: Compact toolbar using icons only for nav + short "X" button for close.
 *      All buttons are icon-only so the row NEVER wraps regardless of screen width.
 *
 * CRITICAL: onDispose does NOT remove the WebView from its parent any more.
 * Instead, the SessionTile's update lambda handles re-parenting seamlessly.
 * This prevents the black-screen bug on focus exit.
 */
@Composable
fun FocusedProfileView(
    profile: AccountProfile,
    webViewManager: WebViewManager,
    onDismiss: () -> Unit
) {
    val webView = remember { webViewManager.getOrCreateWebView(profile.id) }

    DisposableEffect(Unit) {
        // Resume the WebView when entering focus
        webView.onResume()
        onDispose {
            // Detach from focus container so SessionTile can re-parent
            (webView.parent as? ViewGroup)?.removeView(webView)
            // Make sure WebView is still alive
            webView.onResume()
        }
    }

    // Task 4.1 BackHandler for focused profile
    BackHandler(enabled = true) {
        if (webViewManager.canGoBack(profile.id)) {
            webViewManager.goBack(profile.id)
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (webViewManager.canGoBack(profile.id)) {
                webViewManager.goBack(profile.id)
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E18))
        ) {
            // Task 4.2 Compact Toolbar — icon-only nav buttons + icon close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1523))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Nav buttons (icon-only, compact)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back (webview history)
                    CompactNavIcon(
                        icon = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            if (webViewManager.canGoBack(profile.id)) {
                                webViewManager.goBack(profile.id)
                            } else {
                                onDismiss()
                            }
                        }
                    )

                    // Bing home
                    CompactNavIcon(
                        icon = AppIcons.Home,
                        contentDescription = "Bing",
                        onClick = {
                            webViewManager.loadUrl(profile.id, "https://www.bing.com")
                        }
                    )

                    // Reload
                    CompactNavIcon(
                        icon = AppIcons.Refresh,
                        contentDescription = "Reload",
                        onClick = {
                            webViewManager.reload(profile.id)
                        }
                    )

                    // Forward (webview forward history)
                    CompactNavIcon(
                        icon = AppIcons.ArrowForward,
                        contentDescription = "Forward",
                        onClick = {
                            webViewManager.goForward(profile.id)
                        }
                    )
                }

                // Right: Close focus — icon X with short label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF4D4D).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close Focus",
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Full WebView Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF000000))
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { container ->
                        if (webView.parent != container) {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            container.removeAllViews()
                            webView.layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webView.setOnTouchListener { view, event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }
                            container.addView(webView)
                        }
                        // Ensure WebView is resumed and visible
                        webView.onResume()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Compact icon-only navigation button for the focus toolbar.
 * No text label — just an icon in a subtle rounded box.
 */
@Composable
private fun CompactNavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFCFD8DC),
            modifier = Modifier.size(15.dp)
        )
    }
}
