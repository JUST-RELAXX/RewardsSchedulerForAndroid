package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*

/**
 * Homepage Account Settings Dialog:
 * When clicking the gear icon per profile on the homepage,
 * ONLY 3 options are shown:
 * 1) Profile's Name / Label
 * 2) Daily Point Cap / Max Points
 * 3) Profile Timeframe (minutes)
 * - Zero delays/prompts (those belong strictly to Profile Level Changes in session mode).
 * - Zero emojis!
 */
@Composable
fun ProfileConfigDialog(
    profile: AccountProfile,
    onDismiss: () -> Unit,
    onSave: (AccountProfile) -> Unit
) {
    var label by remember { mutableStateOf(profile.label) }
    var maxPoints by remember { mutableIntStateOf(profile.maxPoints) }
    var timeframeMinutes by remember { mutableIntStateOf(profile.timeframeMinutes) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = BgPrimary,
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Settings,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Account Settings: ${profile.label}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profile Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Profile Name",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // 2. Daily Point Cap / Max Points
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Point Cap", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("$maxPoints pts (${maxPoints / 3} searches)", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = maxPoints.toFloat(),
                            onValueChange = { maxPoints = (it / 3).toInt() * 3 },
                            valueRange = 30f..250f,
                            steps = 72,
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = BgSecondary
                            )
                        )
                    }

                    // 3. Profile Timeframe (minutes)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profile Timeframe", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${timeframeMinutes}m", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = timeframeMinutes.toFloat(),
                            onValueChange = { timeframeMinutes = it.toInt() },
                            valueRange = 10f..180f,
                            steps = 33,
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = BgSecondary
                            )
                        )
                    }
                }

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val updated = profile.copy(
                                label = label.ifBlank { profile.label },
                                maxPoints = maxPoints,
                                timeframeMinutes = timeframeMinutes
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Config", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
