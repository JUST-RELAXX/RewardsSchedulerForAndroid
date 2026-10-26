package com.example.rewardsscheduler.ui.dashboard

import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
 * 4.2: Small toolbar: Back, Bing home, Reload, Forward.
 */
@Composable
fun FocusedProfileView(
    profile: AccountProfile,
    webViewManager: WebViewManager,
    onDismiss: () -> Unit
) {
    val webView = remember { webViewManager.getOrCreateWebView(profile.id) }

    DisposableEffect(Unit) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
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
            // Task 4.2 Toolbar: Back, Bing home, Reload, Forward
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1523))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back (webview history)
                    ToolbarButton(
                        icon = AppIcons.ArrowBack,
                        label = "Back",
                        onClick = {
                            if (webViewManager.canGoBack(profile.id)) {
                                webViewManager.goBack(profile.id)
                            } else {
                                onDismiss()
                            }
                        }
                    )

                    // Bing home
                    ToolbarButton(
                        icon = AppIcons.Home,
                        label = "Bing",
                        onClick = {
                            webViewManager.loadUrl(profile.id, "https://www.bing.com")
                        }
                    )

                    // Reload
                    ToolbarButton(
                        icon = AppIcons.Refresh,
                        label = "Reload",
                        onClick = {
                            webViewManager.reload(profile.id)
                        }
                    )

                    // Forward (webview forward history)
                    ToolbarButton(
                        icon = AppIcons.ArrowForward,
                        label = "Forward",
                        onClick = {
                            webViewManager.goForward(profile.id)
                        }
                    )
                }

                // Done / Exit Focus button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Accent.copy(alpha = 0.15f))
                        .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close Focus",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFCFD8DC),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
